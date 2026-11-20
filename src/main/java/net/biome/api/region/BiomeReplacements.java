package net.biome.api.region;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Replaces a vanilla biome with your biome in a <b>part</b> of the whole Overworld (not only inside a
 * region).
 *
 * <pre>{@code
 * BiomeReplacements.replace(Biomes.FOREST, ModBiomes.APPLE_FOREST, 0.3F);   // 30% of forest areas
 * BiomeReplacements.replace(Biomes.PLAINS, ModBiomes.APPLE_GROVE, 0.1F);    // 10% of plains areas
 * }</pre>
 *
 * <p>The world is split into small patches (about {@code 4 << size} blocks apart, see {@link #setPatchSize});
 * in each patch the seed decides whether the replacement happens. So 30% means "about 30% of all forest
 * area", in irregular pieces, the rest stays vanilla. The same vanilla biome can be replaced by several
 * biomes as long as the chances add up to 1 or less.
 *
 * <p>This also works inside regions, for the places where a region keeps the vanilla biome.
 *
 * <h2>Only vanilla biomes (no biome of your own)</h2>
 * The replacement can be a vanilla biome too, so you can change how often vanilla biomes appear:
 * <pre>{@code
 * BiomeReplacements.replace(Biomes.DESERT, Biomes.SAVANNA, 0.5F);         // half as much desert, more savanna
 * BiomeReplacements.replace(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, 0.3F); // more sunflower plains
 * BiomeReplacements.replaceWholeBiome(Biomes.MUSHROOM_FIELDS, Biomes.DEEP_OCEAN, 1.0F); // no mushroom islands
 * }</pre>
 * Pick a replacement that fits the place (similar temperature and terrain), e.g. desert to savanna, not
 * desert to snowy plains.
 *
 * <h2>Two kinds of replacement</h2>
 * <ul>
 *   <li>{@link #replace} - <b>part of the area</b>: in every world, about X% of that biome's area becomes
 *       yours (patch by patch). You find both biomes in the same world.</li>
 *   <li>{@link #replaceWholeBiome} - <b>all or nothing, per world</b>: decided once per world (seed). With
 *       X% chance, <b>every</b> place of that biome in that world becomes yours (surface, caves, oceans:
 *       any biome). Otherwise the world keeps the original biome everywhere.</li>
 * </ul>
 */
public final class BiomeReplacements {
	private static final List<BiomeRegion.Replacement> REPLACEMENTS = new ArrayList<>();
	private static final List<BiomeRegion.Replacement> WHOLE_BIOME = new ArrayList<>();
	private static int patchSize = 6;
	private static boolean frozen;

	private BiomeReplacements() {
	}

	/**
	 * <b>All or nothing, per world.</b> When a world is created, its seed decides once: with
	 * {@code chanceOfWorld} (more than 0, at most 1), <b>every</b> {@code original} in that world becomes
	 * {@code biome}. Otherwise that world has the normal {@code original} everywhere.
	 *
	 * <pre>{@code
	 * // half of all worlds have no cherry groves, but apple groves in exactly those places
	 * BiomeReplacements.replaceWholeBiome(Biomes.CHERRY_GROVE, ModBiomes.APPLE_GROVE, 0.5F);
	 * // every world: no lush caves, always crystal caves instead
	 * BiomeReplacements.replaceWholeBiome(Biomes.LUSH_CAVES, ModBiomes.CRYSTAL_CAVES, 1.0F);
	 * }</pre>
	 *
	 * <p>Works for any Overworld biome: surface, cave and ocean biomes. Several biomes can compete for the same
	 * original (at most one wins per world) as long as the chances add up to 1 or less. Call this in your mod's
	 * {@code onInitialize()}. Which swaps are active is written to the log when a world loads.
	 */
	public static synchronized void replaceWholeBiome(ResourceKey<Biome> original, ResourceKey<Biome> biome, float chanceOfWorld) {
		add(WHOLE_BIOME, "Whole-biome replacement", original, biome, chanceOfWorld);
	}

	/** All whole-biome replacements, sorted by original biome id, then by replacement id. */
	public static synchronized List<BiomeRegion.Replacement> getAllWholeBiome() {
		return sorted(WHOLE_BIOME);
	}

	/**
	 * <b>Part of the area.</b> Replaces {@code original} with {@code biome} in about {@code chance} (more than
	 * 0, at most 1) of the places where {@code original} would be, in every world. Call this in your mod's
	 * {@code onInitialize()}.
	 */
	public static synchronized void replace(ResourceKey<Biome> original, ResourceKey<Biome> biome, float chance) {
		add(REPLACEMENTS, "Replacement", original, biome, chance);
	}

	/** All area replacements, sorted by original biome id, then by replacement id (never by registration order). */
	public static synchronized List<BiomeRegion.Replacement> getAll() {
		return sorted(REPLACEMENTS);
	}

	private static void add(List<BiomeRegion.Replacement> list, String what, ResourceKey<Biome> original, ResourceKey<Biome> biome, float chance) {
		Objects.requireNonNull(original, "original");
		Objects.requireNonNull(biome, "biome");
		if (frozen) {
			throw new IllegalStateException(what + " of " + original.identifier() + " was registered too late: do it in onInitialize()");
		}
		if (!(chance > 0.0F && chance <= 1.0F)) {
			throw new IllegalArgumentException(what + " of " + original.identifier() + ": chance must be more than 0 and at most 1, got " + chance);
		}
		if (original.equals(biome)) {
			throw new IllegalArgumentException(what + ": " + original.identifier() + " can't replace itself");
		}
		float total = chance;
		for (BiomeRegion.Replacement r : list) {
			if (r.original().equals(original)) {
				if (r.biome().equals(biome)) {
					throw new IllegalStateException(original.identifier() + " is already replaced by " + biome.identifier());
				}
				total += r.chance();
			}
		}
		if (total > 1.0001F) {
			throw new IllegalStateException(what + "s of " + original.identifier() + " add up to " + total + ", more than 1 (100%)");
		}
		list.add(new BiomeRegion.Replacement(original, biome, chance));
	}

	private static List<BiomeRegion.Replacement> sorted(List<BiomeRegion.Replacement> list) {
		List<BiomeRegion.Replacement> sorted = new ArrayList<>(list);
		sorted.sort(Comparator.comparing((BiomeRegion.Replacement r) -> r.original().identifier().toString())
				.thenComparing(r -> r.biome().identifier().toString()));
		return sorted;
	}

	/** Size of the replacement patches as a power of two (default 6 = about 256 blocks apart). */
	public static int getPatchSize() {
		return patchSize;
	}

	/** Changes the replacement patch size (3 to 10). Smaller = more, smaller pieces. Must be called before a world loads. */
	public static synchronized void setPatchSize(int size) {
		if (size < 3 || size > 10) {
			throw new IllegalArgumentException("Patch size must be between 3 and 10, got " + size);
		}
		patchSize = size;
	}

	/** Internal: called when the first world loads. */
	public static synchronized void freeze() {
		frozen = true;
	}
}
