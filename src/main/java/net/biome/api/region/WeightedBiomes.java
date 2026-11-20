package net.biome.api.region;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Several biomes that share the place of one original biome, picked by <b>weight</b>. Use it with
 * {@link BiomeRegion.Builder#replace(ResourceKey, WeightedBiomes)} (inside a region) or
 * {@link BiomeReplacements#replace(ResourceKey, WeightedBiomes)} (everywhere):
 *
 * <pre>{@code
 * BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "wastes"))
 *         .nether()
 *         .replace(Biomes.NETHER_WASTES, WeightedBiomes.builder()
 *                 .add(ModBiomes.GRAVEL_DESERT, 5)     // 5 of 9
 *                 .add(ModBiomes.MAGMA_LAND, 3)        // 3 of 9
 *                 .add(ModBiomes.BONE_REEF, 1)         // 1 of 9
 *                 .build())
 *         .register();
 * }</pre>
 *
 * <p>The area of the original biome is split into small cells (about {@code 4 << size} blocks apart, see
 * {@link Builder#size}); every cell gets one of the biomes, picked by weight from the world seed. This needs no
 * climate, so it works the same in the Overworld, the Nether and the End.
 */
public final class WeightedBiomes {
	private final List<Entry> entries;
	private final float originalWeight;
	private final int size;

	private WeightedBiomes(Builder b) {
		this.entries = List.copyOf(b.entries);
		this.originalWeight = b.originalWeight;
		this.size = b.size;
	}

	public static Builder builder() {
		return new Builder();
	}

	/** The biomes with their weights, in the order they were added. */
	public List<Entry> getEntries() {
		return entries;
	}

	/** Weight of keeping the original biome (0 = never). */
	public float getOriginalWeight() {
		return originalWeight;
	}

	/** Cell size as a power of two (cells are about {@code 4 << size} blocks apart). */
	public int getSize() {
		return size;
	}

	/** Sum of all weights, including {@link #getOriginalWeight()}. */
	public float getTotalWeight() {
		float total = originalWeight;
		for (Entry entry : entries) {
			total += entry.weight();
		}
		return total;
	}

	/** One biome and its weight. */
	public record Entry(ResourceKey<Biome> biome, float weight) {
	}

	@Override
	public String toString() {
		return "WeightedBiomes" + entries + (originalWeight > 0 ? " + original " + originalWeight : "") + ", size " + size;
	}

	public static final class Builder {
		private final List<Entry> entries = new ArrayList<>();
		private float originalWeight;
		private int size = 5;

		private Builder() {
		}

		/**
		 * Adds a biome with a weight (more than 0, decimals allowed). A biome with weight 3 appears about three
		 * times as often as one with weight 1.
		 */
		public Builder add(ResourceKey<Biome> biome, float weight) {
			Objects.requireNonNull(biome, "biome");
			checkWeight(weight, biome.identifier().toString());
			for (Entry entry : entries) {
				if (entry.biome().equals(biome)) {
					throw new IllegalStateException("WeightedBiomes: " + biome.identifier() + " was added twice");
				}
			}
			entries.add(new Entry(biome, weight));
			return this;
		}

		/**
		 * Keeps the original biome in some cells, with this weight. For example {@code keepOriginal(2)} with two
		 * biomes of weight 1: half of the area stays the original biome.
		 */
		public Builder keepOriginal(float weight) {
			checkWeight(weight, "keepOriginal");
			this.originalWeight = weight;
			return this;
		}

		/**
		 * How big each biome piece is, as a power of two: cells are about {@code 4 << size} blocks apart.
		 * 3 to 10, default 5 (about 128 blocks). Bigger = bigger biomes.
		 */
		public Builder size(int size) {
			if (size < 3 || size > 10) {
				throw new IllegalArgumentException("WeightedBiomes: size must be between 3 and 10, got " + size);
			}
			this.size = size;
			return this;
		}

		public WeightedBiomes build() {
			if (entries.isEmpty()) {
				throw new IllegalStateException("WeightedBiomes: add at least one biome with add(biome, weight)");
			}
			return new WeightedBiomes(this);
		}

		private static void checkWeight(float weight, @Nullable String what) {
			if (!(weight > 0.0F) || Float.isInfinite(weight)) {
				throw new IllegalArgumentException("WeightedBiomes: weight of " + what + " must be more than 0, got " + weight);
			}
		}
	}
}
