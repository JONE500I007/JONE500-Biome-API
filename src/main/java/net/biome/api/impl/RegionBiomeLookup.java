package net.biome.api.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import net.biome.api.JONE500BiomeAPI;
import net.biome.api.region.BiomeRegion;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/**
 * Biome lookup for one Overworld biome source: regions + world-wide replacements.
 *
 * <p>Every region gets a copy of the original climate table where each point is relabeled
 * (replaced biome or original biome), plus its extra points. So a lookup inside a region is a
 * single tree search, like vanilla. World-wide replacements are applied afterwards, per small patch.
 *
 * <p>Internal class, not part of the API.
 */
public final class RegionBiomeLookup {
	private static final long REPLACEMENT_SEED_SALT = 0x3C6EF372FE94F82BL;

	private final RegionLayout layout;
	private final List<BiomeRegion> regions;
	private final Climate.ParameterList<Entry>[] trees;
	private final RegionLayout replacementLayout;
	private final Map<ResourceKey<Biome>, GlobalReplacement> globalReplacements;
	private final Map<ResourceKey<Biome>, Holder<Biome>> wholeBiomeSwaps;
	private final Set<Holder<Biome>> addedBiomes;

	private RegionBiomeLookup(RegionLayout layout, List<BiomeRegion> regions, Climate.ParameterList<Entry>[] trees,
			RegionLayout replacementLayout, Map<ResourceKey<Biome>, GlobalReplacement> globalReplacements,
			Map<ResourceKey<Biome>, Holder<Biome>> wholeBiomeSwaps, Set<Holder<Biome>> addedBiomes) {
		this.layout = layout;
		this.regions = regions;
		this.trees = trees;
		this.replacementLayout = replacementLayout;
		this.globalReplacements = globalReplacements;
		this.wholeBiomeSwaps = wholeBiomeSwaps;
		this.addedBiomes = addedBiomes;
	}

	/** Whole-biome replacements active in this world (original -> replacement). */
	public Map<ResourceKey<Biome>, Holder<Biome>> getWholeBiomeSwaps() {
		return wholeBiomeSwaps;
	}

	/** Regions only (used by tests). */
	public static RegionBiomeLookup create(long seed, int regionSize, float vanillaWeight, List<BiomeRegion> regions,
			Climate.ParameterList<Holder<Biome>> original, HolderGetter<Biome> biomes) {
		return create(seed, regionSize, vanillaWeight, regions, List.of(), 6, List.of(), seed, original, biomes);
	}

	/**
	 * Decides which whole-biome replacements are active for a world. Pure function of the seed: one roll per
	 * original biome, at most one replacement wins.
	 *
	 * @param wholeSwaps sorted whole-biome replacements
	 * @return original biome -> replacement, only the active ones
	 */
	public static Map<ResourceKey<Biome>, ResourceKey<Biome>> chooseWholeBiomeSwaps(long seed, List<BiomeRegion.Replacement> wholeSwaps) {
		Map<ResourceKey<Biome>, ResourceKey<Biome>> active = new LinkedHashMap<>();
		Map<ResourceKey<Biome>, Float> cumulative = new HashMap<>();
		Map<ResourceKey<Biome>, Double> rolls = new HashMap<>();
		for (BiomeRegion.Replacement swap : wholeSwaps) {
			ResourceKey<Biome> original = swap.original();
			double roll = rolls.computeIfAbsent(original,
					k -> (RegionLayout.mix(seed ^ RegionLayout.salt("whole#" + k.identifier())) >>> 11) * 0x1.0p-53);
			float before = cumulative.getOrDefault(original, 0.0F);
			float after = before + swap.chance();
			cumulative.put(original, after);
			if (!active.containsKey(original) && roll >= before && (roll < after || swap.chance() >= 1.0F)) {
				active.put(original, swap.biome());
			}
		}
		return active;
	}

	/**
	 * @param original     the dimension's own climate table (vanilla preset or datapack)
	 * @param regions      the regions, sorted by id
	 * @param replacements world-wide area replacements, sorted
	 * @param wholeSwaps   whole-biome replacements, sorted (only the ones active for this seed are used)
	 * @param wholeSwapSeed the world seed (whole-biome swaps are decided per world, the same in every dimension)
	 */
	@SuppressWarnings("unchecked")
	public static RegionBiomeLookup create(long seed, int regionSize, float vanillaWeight, List<BiomeRegion> regions,
			List<BiomeRegion.Replacement> replacements, int replacementPatchSize, List<BiomeRegion.Replacement> wholeSwaps, long wholeSwapSeed,
			Climate.@Nullable ParameterList<Holder<Biome>> original, HolderGetter<Biome> biomes) {
		if (original == null && !regions.isEmpty()) {
			throw new IllegalArgumentException("regions need the dimension's climate table");
		}
		float[] weights = new float[regions.size()];
		for (int i = 0; i < regions.size(); i++) {
			weights[i] = regions.get(i).getWeight();
		}
		RegionLayout layout = new RegionLayout(seed, regionSize, vanillaWeight, weights);

		Climate.ParameterList<Entry>[] trees = new Climate.ParameterList[regions.size()];
		Set<Holder<Biome>> added = new LinkedHashSet<>();

		for (int r = 0; r < regions.size(); r++) {
			trees[r] = buildRegionTree(regions.get(r), original, biomes, added);
		}

		// World-wide replacements: original biome -> biomes + cumulative chances
		Map<ResourceKey<Biome>, List<Pair<Holder<Biome>, Float>>> grouped = new LinkedHashMap<>();
		for (BiomeRegion.Replacement repl : replacements) {
			Optional<Holder.Reference<Biome>> holder = biomes.get(repl.biome());
			if (holder.isEmpty()) {
				JONE500BiomeAPI.LOGGER.warn("Replacement of {}: biome {} is not registered (missing biome JSON?), skipped",
						repl.original().identifier(), repl.biome().identifier());
				continue;
			}
			grouped.computeIfAbsent(repl.original(), k -> new ArrayList<>()).add(Pair.of(holder.get(), repl.chance()));
			added.add(holder.get());
		}
		Map<ResourceKey<Biome>, GlobalReplacement> global = new HashMap<>();
		grouped.forEach((key, list) -> {
			Holder<Biome>[] targets = new Holder[list.size()];
			float[] cumulative = new float[list.size()];
			float sum = 0.0F;
			for (int i = 0; i < list.size(); i++) {
				targets[i] = list.get(i).getFirst();
				sum += list.get(i).getSecond();
				cumulative[i] = sum;
			}
			global.put(key, new GlobalReplacement(targets, cumulative, RegionLayout.salt("replace#" + key.identifier())));
		});
		RegionLayout replacementLayout = new RegionLayout(seed ^ REPLACEMENT_SEED_SALT, replacementPatchSize, 1.0F, new float[0]);

		Map<ResourceKey<Biome>, Holder<Biome>> whole = new HashMap<>();
		chooseWholeBiomeSwaps(wholeSwapSeed, wholeSwaps).forEach((from, to) -> {
			Optional<Holder.Reference<Biome>> holder = biomes.get(to);
			if (holder.isEmpty()) {
				JONE500BiomeAPI.LOGGER.warn("Whole-biome replacement of {}: biome {} is not registered (missing biome JSON?), skipped",
						from.identifier(), to.identifier());
				return;
			}
			whole.put(from, holder.get());
			added.add(holder.get());
			JONE500BiomeAPI.LOGGER.info("This world: every {} is replaced by {}", from.identifier(), to.identifier());
		});

		return new RegionBiomeLookup(layout, List.copyOf(regions), trees, replacementLayout, Map.copyOf(global), Map.copyOf(whole),
				Collections.unmodifiableSet(added));
	}

	private static Climate.ParameterList<Entry> buildRegionTree(BiomeRegion region, Climate.ParameterList<Holder<Biome>> original,
			HolderGetter<Biome> biomes, Set<Holder<Biome>> added) {
		String regionId = region.getId().toString();

		// biome -> (index in group, group size) and group salt
		Map<ResourceKey<Biome>, int[]> groupOf = new HashMap<>();
		Map<ResourceKey<Biome>, Long> groupSaltOf = new HashMap<>();
		List<List<ResourceKey<Biome>>> groups = region.getExclusiveGroups();
		for (int g = 0; g < groups.size(); g++) {
			List<ResourceKey<Biome>> group = groups.get(g);
			long groupSalt = RegionLayout.salt(regionId + "#group" + g);
			for (int m = 0; m < group.size(); m++) {
				if (groupOf.putIfAbsent(group.get(m), new int[] {m, group.size()}) == null) {
					groupSaltOf.put(group.get(m), groupSalt);
				}
			}
		}

		Map<ResourceKey<Biome>, Entry> replacementFor = new HashMap<>();
		for (BiomeRegion.Replacement repl : region.getReplacements()) {
			Optional<Holder.Reference<Biome>> holder = biomes.get(repl.biome());
			if (holder.isEmpty()) {
				JONE500BiomeAPI.LOGGER.warn("Region {}: biome {} is not registered (missing biome JSON?), replacement of {} skipped",
						regionId, repl.biome().identifier(), repl.original().identifier());
				continue;
			}
			replacementFor.put(repl.original(), newEntry(holder.get(), null, null, repl.chance(), regionId, repl.biome(), groupOf, groupSaltOf));
			added.add(holder.get());
		}

		List<Pair<Climate.ParameterPoint, Entry>> points = new ArrayList<>(original.values().size() + region.getExtraPoints().size());
		// Extra points usually overlap vanilla points exactly (distance 0 for both). Give the original points
		// the smallest possible extra offset so the region's own points win those ties.
		boolean hasExtraPoints = !region.getExtraPoints().isEmpty();
		for (Pair<Climate.ParameterPoint, Holder<Biome>> pair : original.values()) {
			Holder<Biome> originalBiome = pair.getSecond();
			Entry template = originalBiome.unwrapKey().map(replacementFor::get).orElse(null);
			Entry entry = template == null ? Entry.original(originalBiome) : template.withOriginal(originalBiome);
			Climate.ParameterPoint point = pair.getFirst();
			if (hasExtraPoints) {
				point = new Climate.ParameterPoint(point.temperature(), point.humidity(), point.continentalness(), point.erosion(),
						point.depth(), point.weirdness(), point.offset() + 1L);
			}
			points.add(Pair.of(point, entry));
		}
		for (BiomeRegion.ExtraPoint extra : region.getExtraPoints()) {
			Optional<Holder.Reference<Biome>> holder = biomes.get(extra.biome());
			if (holder.isEmpty()) {
				JONE500BiomeAPI.LOGGER.warn("Region {}: biome {} is not registered (missing biome JSON?), extra point skipped",
						regionId, extra.biome().identifier());
				continue;
			}
			points.add(Pair.of(extra.point(), newEntry(holder.get(), null, extra.strict() ? extra.point() : null,
					extra.chance(), regionId, extra.biome(), groupOf, groupSaltOf)));
			added.add(holder.get());
		}
		return new Climate.ParameterList<>(points);
	}

	private static Entry newEntry(Holder<Biome> biome, @Nullable Holder<Biome> original, Climate.@Nullable ParameterPoint strictBox,
			float chance, String regionId, ResourceKey<Biome> key, Map<ResourceKey<Biome>, int[]> groupOf, Map<ResourceKey<Biome>, Long> groupSaltOf) {
		long chanceSalt = RegionLayout.salt(regionId + "#" + key.identifier());
		int[] group = groupOf.get(key);
		if (group == null) {
			return new Entry(biome, original, strictBox, chance, chanceSalt, 0L, -1, 0);
		}
		return new Entry(biome, original, strictBox, chance, chanceSalt, groupSaltOf.get(key), group[0], group[1]);
	}

	/**
	 * The biome at a quart position.
	 *
	 * @param vanilla the dimension's own climate table, used for vanilla patches and as fallback
	 */
	public Holder<Biome> getBiome(Climate.TargetPoint target, int quartX, int quartZ, Climate.ParameterList<Holder<Biome>> vanilla) {
		return applyGlobalReplacements(regionBiome(target, quartX, quartZ, vanilla), quartX, quartZ);
	}

	private Holder<Biome> regionBiome(Climate.TargetPoint target, int quartX, int quartZ, Climate.ParameterList<Holder<Biome>> vanilla) {
		RegionLayout.Cell cell = layout.cellAt(quartX, quartZ);
		if (cell.region() == RegionLayout.VANILLA) {
			return vanilla.findValue(target);
		}
		Entry entry = trees[cell.region()].findValue(target);
		if (entry.biome == null) {
			return entry.original;
		}
		if (isEnabled(entry, cell) && (entry.strictBox == null || isInside(entry.strictBox, target))) {
			return entry.biome;
		}
		return entry.original != null ? entry.original : vanilla.findValue(target);
	}

	/** True if this spot belongs to no region (vanilla, or whatever other mods place there). */
	public boolean isVanillaCell(int quartX, int quartZ) {
		return layout.cellAt(quartX, quartZ).region() == RegionLayout.VANILLA;
	}

	/** World-wide replacements only (also used for the End, which has no regions). */
	public Holder<Biome> applyGlobalReplacements(Holder<Biome> biome, int quartX, int quartZ) {
		if ((globalReplacements.isEmpty() && wholeBiomeSwaps.isEmpty()) || !(biome instanceof Holder.Reference<Biome> ref)) {
			return biome;
		}
		// All or nothing for this world: wins over area replacements of the same biome.
		Holder<Biome> whole = wholeBiomeSwaps.get(ref.key());
		if (whole != null) {
			return whole;
		}
		GlobalReplacement repl = globalReplacements.get(ref.key());
		if (repl == null) {
			return biome;
		}
		double roll = replacementLayout.random(replacementLayout.cellAt(quartX, quartZ), repl.salt);
		for (int i = 0; i < repl.cumulative.length; i++) {
			if (roll < repl.cumulative[i]) {
				return repl.targets[i];
			}
		}
		return biome;
	}

	/** Strict mode: inside every range except depth (depth is a single value at the surface). */
	private static boolean isInside(Climate.ParameterPoint box, Climate.TargetPoint t) {
		return box.temperature().distance(t.temperature()) == 0
				&& box.humidity().distance(t.humidity()) == 0
				&& box.continentalness().distance(t.continentalness()) == 0
				&& box.erosion().distance(t.erosion()) == 0
				&& box.weirdness().distance(t.weirdness()) == 0;
	}

	private boolean isEnabled(Entry entry, RegionLayout.Cell cell) {
		if (entry.groupIndex >= 0 && layout.pick(cell, entry.groupSalt, entry.groupSize) != entry.groupIndex) {
			return false;
		}
		return layout.roll(cell, entry.chanceSalt, entry.chance);
	}

	/** The region at a quart position, or null for vanilla. Used for F3 and tests. */
	public @Nullable BiomeRegion getRegionAt(int quartX, int quartZ) {
		int r = layout.cellAt(quartX, quartZ).region();
		return r == RegionLayout.VANILLA ? null : regions.get(r);
	}

	public RegionLayout getLayout() {
		return layout;
	}

	public RegionLayout getReplacementLayout() {
		return replacementLayout;
	}

	/** Biomes added by regions and replacements, in a fixed order (they must be part of the possible biomes). */
	public Set<Holder<Biome>> getAddedBiomes() {
		return addedBiomes;
	}

	/**
	 * One point in a region's climate table.
	 *
	 * @param biome     the region's biome, or null if this point keeps the original biome
	 * @param original  the original biome at this point, or null for extra points
	 * @param strictBox for strict extra points: the ranges the climate must really be inside
	 */
	record Entry(@Nullable Holder<Biome> biome, @Nullable Holder<Biome> original, Climate.@Nullable ParameterPoint strictBox,
			float chance, long chanceSalt, long groupSalt, int groupIndex, int groupSize) {
		static Entry original(Holder<Biome> original) {
			return new Entry(null, original, null, 1.0F, 0L, 0L, -1, 0);
		}

		Entry withOriginal(Holder<Biome> newOriginal) {
			return new Entry(biome, newOriginal, strictBox, chance, chanceSalt, groupSalt, groupIndex, groupSize);
		}
	}

	private record GlobalReplacement(Holder<Biome>[] targets, float[] cumulative, long salt) {
	}
}
