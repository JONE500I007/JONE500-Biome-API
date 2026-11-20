package net.biome.api.surface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import net.minecraft.core.HolderGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Surface rules (which blocks make up the ground) for your Overworld, Nether and End biomes.
 *
 * <p>Your rules run <b>before</b> the vanilla rules. Wrap them in a biome condition so they only affect your own
 * biomes. Since Minecraft 26.2, {@code SurfaceRules.isBiome(...)} needs the biome registry, which does not exist
 * yet in {@code onInitialize()}. So you give a function that gets the registry ({@code biomes}) when a world loads:
 *
 * <pre>{@code
 * BiomeSurfaceRules.addOverworld(Identifier.fromNamespaceAndPath("mymod", "surface"), biomes ->
 *         SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, ModBiomes.RED_PLAINS),
 *                 SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.RED_SAND.defaultBlockState()))));
 * }</pre>
 *
 * <p>Rules that need no biome condition can also be given directly ({@code addOverworld(id, rule)}).
 * Rules from different mods run sorted by their id.
 */
public final class BiomeSurfaceRules {
	private static final Map<Identifier, Function<HolderGetter<Biome>, SurfaceRules.RuleSource>> OVERWORLD = newMap();
	private static final Map<Identifier, Function<HolderGetter<Biome>, SurfaceRules.RuleSource>> NETHER = newMap();
	private static final Map<Identifier, Function<HolderGetter<Biome>, SurfaceRules.RuleSource>> END = newMap();

	private BiomeSurfaceRules() {
	}

	private static Map<Identifier, Function<HolderGetter<Biome>, SurfaceRules.RuleSource>> newMap() {
		return new TreeMap<>(Comparator.comparing(Identifier::toString));
	}

	/**
	 * Adds Overworld surface rules, made when a world loads with the biome registry ({@code biomes}, needed by
	 * {@code SurfaceRules.isBiome(biomes, ...)}). Call this in your mod's {@code onInitialize()}.
	 */
	public static synchronized void addOverworld(Identifier id, Function<HolderGetter<Biome>, SurfaceRules.RuleSource> rule) {
		add(OVERWORLD, "Surface rules", id, rule);
	}

	/** Adds Overworld surface rules that need no biome registry (no {@code isBiome}). */
	public static synchronized void addOverworld(Identifier id, SurfaceRules.RuleSource rule) {
		add(OVERWORLD, "Surface rules", id, biomes -> rule);
	}

	/** All Overworld surface rules for this biome registry, sorted by id. */
	public static synchronized List<SurfaceRules.RuleSource> getOverworld(HolderGetter<Biome> biomes) {
		return make(OVERWORLD, biomes);
	}

	/** Adds Nether surface rules (e.g. the floor of your Nether biome). Call this in your mod's {@code onInitialize()}. */
	public static synchronized void addNether(Identifier id, Function<HolderGetter<Biome>, SurfaceRules.RuleSource> rule) {
		add(NETHER, "Nether surface rules", id, rule);
	}

	/** Adds Nether surface rules that need no biome registry (no {@code isBiome}). */
	public static synchronized void addNether(Identifier id, SurfaceRules.RuleSource rule) {
		add(NETHER, "Nether surface rules", id, biomes -> rule);
	}

	/** All Nether surface rules for this biome registry, sorted by id. */
	public static synchronized List<SurfaceRules.RuleSource> getNether(HolderGetter<Biome> biomes) {
		return make(NETHER, biomes);
	}

	/**
	 * Adds End surface rules (e.g. the ground of your End biome instead of end stone). Call this in your mod's
	 * {@code onInitialize()}. Vanilla's End rule makes everything end stone, so wrap yours in
	 * {@code SurfaceRules.isBiome(biomes, ...)}; {@code SurfaceRules.ON_FLOOR} works like in the Overworld.
	 */
	public static synchronized void addEnd(Identifier id, Function<HolderGetter<Biome>, SurfaceRules.RuleSource> rule) {
		add(END, "End surface rules", id, rule);
	}

	/** Adds End surface rules that need no biome registry (no {@code isBiome}). */
	public static synchronized void addEnd(Identifier id, SurfaceRules.RuleSource rule) {
		add(END, "End surface rules", id, biomes -> rule);
	}

	/** All End surface rules for this biome registry, sorted by id. */
	public static synchronized List<SurfaceRules.RuleSource> getEnd(HolderGetter<Biome> biomes) {
		return make(END, biomes);
	}

	private static void add(Map<Identifier, Function<HolderGetter<Biome>, SurfaceRules.RuleSource>> map, String what, Identifier id,
			Function<HolderGetter<Biome>, SurfaceRules.RuleSource> rule) {
		java.util.Objects.requireNonNull(id, "id");
		java.util.Objects.requireNonNull(rule, "rule");
		if (map.putIfAbsent(id, rule) != null) {
			throw new IllegalStateException(what + " with id " + id + " are already registered");
		}
	}

	private static List<SurfaceRules.RuleSource> make(Map<Identifier, Function<HolderGetter<Biome>, SurfaceRules.RuleSource>> map,
			HolderGetter<Biome> biomes) {
		List<SurfaceRules.RuleSource> rules = new ArrayList<>(map.size());
		map.forEach((id, factory) -> {
			SurfaceRules.RuleSource rule = factory.apply(biomes);
			if (rule == null) {
				throw new IllegalStateException("Surface rules " + id + " returned null");
			}
			rules.add(rule);
		});
		return rules;
	}
}
