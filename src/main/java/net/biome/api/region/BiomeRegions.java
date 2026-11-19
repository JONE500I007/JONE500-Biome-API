package net.biome.api.region;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import net.minecraft.resources.Identifier;

/**
 * All registered {@link BiomeRegion}s.
 *
 * <p>Regions are always used <b>sorted by id</b>, never in registration order, so the world does not
 * change when mods load in a different order.
 */
public final class BiomeRegions {
	private static final Map<Identifier, BiomeRegion> REGIONS = new TreeMap<>(Comparator.comparing(Identifier::toString));
	private static float vanillaWeight = 10.0F;
	private static int regionSize = 7;
	private static boolean frozen;

	private BiomeRegions() {
	}

	/** Registers a region. Usually called through {@link BiomeRegion.Builder#register()}. */
	public static synchronized void register(BiomeRegion region) {
		if (frozen) {
			throw new IllegalStateException("Region " + region.getId() + " was registered too late: register regions in onInitialize()");
		}
		if (REGIONS.putIfAbsent(region.getId(), region) != null) {
			throw new IllegalStateException("A region with id " + region.getId() + " is already registered");
		}
	}

	/** All regions, sorted by id. */
	public static synchronized List<BiomeRegion> getAll() {
		return List.copyOf(REGIONS.values());
	}

	/** The regions of one dimension, sorted by id. */
	public static synchronized List<BiomeRegion> getAll(RegionDimension dimension) {
		return REGIONS.values().stream().filter(r -> r.getDimension() == dimension).toList();
	}

	public static synchronized Optional<BiomeRegion> get(Identifier id) {
		return Optional.ofNullable(REGIONS.get(id));
	}

	/** Weight of vanilla patches. Default 10. */
	public static float getVanillaWeight() {
		return vanillaWeight;
	}

	/**
	 * Size of the patches as a power of two: patch centers are about {@code 4 << size} blocks apart.
	 * Default 7 (about 512 blocks apart; walking in a straight line you enter a new patch about every
	 * 250-300 blocks, because patches have irregular shapes).
	 */
	public static int getRegionSize() {
		return regionSize;
	}

	/** For modpacks / config: changes the vanilla weight. Must be called before a world loads. */
	public static synchronized void setVanillaWeight(float weight) {
		if (!(weight >= 0.0F) || Float.isInfinite(weight)) {
			throw new IllegalArgumentException("Vanilla weight must be 0 or more, got " + weight);
		}
		vanillaWeight = weight;
	}

	/** For modpacks / config: changes the patch size (4 to 10). Must be called before a world loads. */
	public static synchronized void setRegionSize(int size) {
		if (size < 4 || size > 10) {
			throw new IllegalArgumentException("Region size must be between 4 and 10, got " + size);
		}
		regionSize = size;
	}

	/** Internal: called when the first world loads. Registering after that is an error. */
	public static synchronized void freeze() {
		frozen = true;
	}
}
