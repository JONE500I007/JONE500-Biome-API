package net.biome.api.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.biome.api.climate.ClimatePoints;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/**
 * A region of the Overworld, the Nether or the End that uses its own biomes.
 *
 * <p>The world is split into large patches ("region instances"). Each patch is either vanilla or one
 * of the registered regions, picked by weight. Inside a patch of your region, the climate works
 * exactly like vanilla, but the vanilla biomes you {@link Builder#replace replace} become your biomes.
 *
 * <pre>{@code
 * BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "orchards"))
 *         .weight(2)
 *         .replace(Biomes.PLAINS, ModBiomes.APPLE_GROVE)              // always
 *         .replace(Biomes.FOREST, ModBiomes.GOLDEN_ORCHARD, 0.3F)     // in 30% of the patches
 *         .register();
 * }</pre>
 *
 * <p>Everything is decided from the world seed and the region ids only, so the same seed always gives
 * the same world, no matter in which order mods are loaded.
 */
public final class BiomeRegion {
	private final Identifier id;
	private final RegionDimension dimension;
	private final float weight;
	private final List<Replacement> replacements;
	private final List<WeightedReplacement> weightedReplacements;
	private final List<ExtraPoint> extraPoints;
	private final List<List<ResourceKey<Biome>>> exclusiveGroups;

	private BiomeRegion(Builder b) {
		this.id = b.id;
		this.dimension = b.dimension;
		this.weight = b.weight;
		this.replacements = List.copyOf(b.replacements.values());
		this.weightedReplacements = List.copyOf(b.weightedReplacements.values());
		this.extraPoints = List.copyOf(b.extraPoints);
		this.exclusiveGroups = b.exclusiveGroups.stream().map(List::copyOf).toList();
	}

	/** Starts a new region. The id must be unique, for example {@code mymod:orchards}. */
	public static Builder builder(Identifier id) {
		return new Builder(id);
	}

	public Identifier getId() {
		return id;
	}

	/** Overworld (default) or Nether. */
	public RegionDimension getDimension() {
		return dimension;
	}

	/** How often patches of this region appear, compared to vanilla patches (see {@link BiomeRegions#getVanillaWeight()}). */
	public float getWeight() {
		return weight;
	}

	/** The vanilla biomes this region replaces, in the order they were added. */
	public List<Replacement> getReplacements() {
		return replacements;
	}

	/** The biomes this region replaces with several biomes by weight, in the order they were added. */
	public List<WeightedReplacement> getWeightedReplacements() {
		return weightedReplacements;
	}

	/** Every biome this region can place (single and weighted replacements, extra points). */
	public List<ResourceKey<Biome>> getAllBiomes() {
		List<ResourceKey<Biome>> all = new ArrayList<>();
		replacements.forEach(r -> all.add(r.biome()));
		weightedReplacements.forEach(w -> w.biomes().getEntries().forEach(e -> all.add(e.biome())));
		extraPoints.forEach(p -> all.add(p.biome()));
		return all;
	}

	/** Extra climate points added by {@link Builder#addBiome}. */
	public List<ExtraPoint> getExtraPoints() {
		return extraPoints;
	}

	/** Groups of biomes of which at most one may appear in the same patch. */
	public List<List<ResourceKey<Biome>>> getExclusiveGroups() {
		return exclusiveGroups;
	}

	@Override
	public String toString() {
		return "BiomeRegion[" + id + "]";
	}

	/**
	 * A vanilla (or datapack) biome that becomes {@code biome} inside this region.
	 *
	 * @param chance 0..1, in how many patches of this region the replacement happens. In the other
	 *               patches the original biome stays.
	 */
	public record Replacement(ResourceKey<Biome> original, ResourceKey<Biome> biome, float chance) {
	}

	/**
	 * A biome that becomes one of several biomes (by weight) inside this region.
	 *
	 * @param chance 0..1, in how many patches of this region the replacement happens
	 */
	public record WeightedReplacement(ResourceKey<Biome> original, WeightedBiomes biomes, float chance) {
	}

	/**
	 * A climate point added only inside this region.
	 *
	 * @param chance 0..1, in how many patches of this region the point is used. In the other patches the
	 *               area keeps the biome vanilla would have placed there.
	 * @param strict if true the biome is only used where the climate is really inside the point's ranges
	 *               (see {@link ClimatePoints.Builder#strict()})
	 */
	public record ExtraPoint(Climate.ParameterPoint point, ResourceKey<Biome> biome, float chance, boolean strict) {
	}

	public static final class Builder {
		private final Identifier id;
		private float weight = 1.0F;
		private RegionDimension dimension = RegionDimension.OVERWORLD;
		private final Map<ResourceKey<Biome>, Replacement> replacements = new LinkedHashMap<>();
		private final Map<ResourceKey<Biome>, WeightedReplacement> weightedReplacements = new LinkedHashMap<>();
		private final List<ExtraPoint> extraPoints = new ArrayList<>();
		private final List<List<ResourceKey<Biome>>> exclusiveGroups = new ArrayList<>();

		private Builder(Identifier id) {
			this.id = Objects.requireNonNull(id, "id");
		}

		/**
		 * Makes this an <b>Overworld</b> region. This is the default, so it is only needed to make the code easier
		 * to read ({@code BiomeRegion.builder(id).overworld()...}).
		 */
		public Builder overworld() {
			this.dimension = RegionDimension.OVERWORLD;
			return this;
		}

		/**
		 * Makes this a <b>Nether</b> region. Everything works the same as in the Overworld (weights, chances,
		 * exclusive, strict). The Nether has only 5 vanilla biomes, placed by temperature and humidity:
		 * replace them ({@code .replace(Biomes.CRIMSON_FOREST, ...)}) or add your biome at its own point with
		 * {@link ClimatePoints#netherPoint(float, float)}.
		 */
		public Builder nether() {
			this.dimension = RegionDimension.NETHER;
			return this;
		}

		/**
		 * Makes this an <b>End</b> region. The End has no climate (vanilla picks the main island, highlands,
		 * midlands, barrens or small islands from the terrain), so an End region can only <b>replace</b> those
		 * biomes inside its patches: {@code .replace(Biomes.END_HIGHLANDS, ModBiomes.VOID_FOREST)}. Weights,
		 * chances and {@link #exclusive} work like in the Overworld; {@code addBiome(...)} can't be used.
		 * Biomes placed with Fabric API's {@code TheEndBiomes} can be replaced too.
		 */
		public Builder end() {
			this.dimension = RegionDimension.END;
			return this;
		}

		/**
		 * How often patches of this region appear. Vanilla patches have weight
		 * {@link BiomeRegions#getVanillaWeight()} (10 by default), so weight 1 = about 1 patch of this
		 * region for every 10 vanilla patches (when no other mod adds regions). Decimals are allowed, so
		 * very rare regions are possible (e.g. {@code 0.25}). Default: 1.
		 */
		public Builder weight(float weight) {
			if (!(weight > 0.0F) || Float.isInfinite(weight)) {
				throw new IllegalArgumentException("Region " + id + ": weight must be a positive number, got " + weight);
			}
			this.weight = weight;
			return this;
		}

		/** Inside this region, every place where vanilla would put {@code original} gets {@code biome} instead. */
		public Builder replace(ResourceKey<Biome> original, ResourceKey<Biome> biome) {
			return replace(original, biome, 1.0F);
		}

		/**
		 * Like {@link #replace(ResourceKey, ResourceKey)}, but only in a part of the patches.
		 * For example {@code 0.3F} = the replacement happens in 30% of this region's patches; in the other
		 * 70% the original biome stays. Which patches is decided by the world seed.
		 */
		public Builder replace(ResourceKey<Biome> original, ResourceKey<Biome> biome, float chance) {
			Objects.requireNonNull(original, "original");
			Objects.requireNonNull(biome, "biome");
			checkChance(chance);
			if (replacements.containsKey(original) || weightedReplacements.containsKey(original)) {
				throw new IllegalStateException("Region " + id + " already replaces " + original.identifier());
			}
			replacements.put(original, new Replacement(original, biome, chance));
			return this;
		}

		/**
		 * Inside this region, {@code original} becomes <b>one of several biomes, picked by weight</b> (in small
		 * cells, see {@link WeightedBiomes}). Works in every dimension, also the Nether and the End:
		 * <pre>{@code
		 * .replace(Biomes.END_HIGHLANDS, WeightedBiomes.builder()
		 *         .add(ModBiomes.CRYSTAL_SPIRES, 3)
		 *         .add(ModBiomes.VOID_FOREST, 1)
		 *         .build())
		 * }</pre>
		 */
		public Builder replace(ResourceKey<Biome> original, WeightedBiomes biomes) {
			return replace(original, biomes, 1.0F);
		}

		/** Like {@link #replace(ResourceKey, WeightedBiomes)}, but only in {@code chance} of this region's patches. */
		public Builder replace(ResourceKey<Biome> original, WeightedBiomes biomes, float chance) {
			Objects.requireNonNull(original, "original");
			Objects.requireNonNull(biomes, "biomes");
			checkChance(chance);
			if (replacements.containsKey(original) || weightedReplacements.containsKey(original)) {
				throw new IllegalStateException("Region " + id + " already replaces " + original.identifier());
			}
			weightedReplacements.put(original, new WeightedReplacement(original, biomes, chance));
			return this;
		}

		/**
		 * Adds a biome at an exact climate point, only inside this region. Use this when your biome should
		 * have its own climate instead of taking the place of a vanilla biome.
		 */
		public Builder addBiome(Climate.ParameterPoint point, ResourceKey<Biome> biome) {
			return addBiome(point, biome, 1.0F);
		}

		/** Like {@link #addBiome(Climate.ParameterPoint, ResourceKey)}, but only in a part of the patches. */
		public Builder addBiome(Climate.ParameterPoint point, ResourceKey<Biome> biome, float chance) {
			return addPoint(point, biome, chance, false);
		}

		private Builder addPoint(Climate.ParameterPoint point, ResourceKey<Biome> biome, float chance, boolean strict) {
			Objects.requireNonNull(point, "point");
			Objects.requireNonNull(biome, "biome");
			checkChance(chance);
			extraPoints.add(new ExtraPoint(point, biome, chance, strict));
			return this;
		}

		/**
		 * Adds a biome at the climate described with {@link ClimatePoints}, only inside this region:
		 * <pre>{@code
		 * .addBiome(ClimatePoints.builder()
		 *         .temperature(Temperature.NEUTRAL, Temperature.WARM)
		 *         .continentalness(Continentalness.MID_INLAND)
		 *         .terrain(Terrain.MID), ModBiomes.APPLE_GROVE)
		 * }</pre>
		 */
		public Builder addBiome(ClimatePoints.Builder climate, ResourceKey<Biome> biome) {
			return addBiome(climate, biome, 1.0F);
		}

		/** Like {@link #addBiome(ClimatePoints.Builder, ResourceKey)}, but only in a part of the patches. */
		public Builder addBiome(ClimatePoints.Builder climate, ResourceKey<Biome> biome, float chance) {
			List<Climate.ParameterPoint> points = climate.build();
			if (points.isEmpty()) {
				throw new IllegalArgumentException("Region " + id + ": no climate points given for " + biome.identifier());
			}
			for (Climate.ParameterPoint point : points) {
				addPoint(point, biome, chance, climate.isStrict());
			}
			return this;
		}

		/** Adds a biome at several climate points, only inside this region. */
		public Builder addBiome(List<Climate.ParameterPoint> points, ResourceKey<Biome> biome, float chance) {
			if (points.isEmpty()) {
				throw new IllegalArgumentException("Region " + id + ": no climate points given for " + biome.identifier());
			}
			for (Climate.ParameterPoint point : points) {
				addBiome(point, biome, chance);
			}
			return this;
		}

		/**
		 * At most one of these biomes appears in the same patch of this region. In each patch one of
		 * them is picked (by seed); the others fall back to the original biome there. Use it for biomes
		 * that should not appear next to each other.
		 */
		@SafeVarargs
		public final Builder exclusive(ResourceKey<Biome>... biomes) {
			if (biomes.length < 2) {
				throw new IllegalArgumentException("Region " + id + ": exclusive(...) needs at least 2 biomes");
			}
			List<ResourceKey<Biome>> group = new ArrayList<>();
			Collections.addAll(group, biomes);
			exclusiveGroups.add(group);
			return this;
		}

		/** Builds the region without registering it. */
		public BiomeRegion build() {
			if (replacements.isEmpty() && weightedReplacements.isEmpty() && extraPoints.isEmpty()) {
				throw new IllegalStateException("Region " + id + " has no biomes: use replace(...) or addBiome(...)");
			}
			if (dimension == RegionDimension.END && !extraPoints.isEmpty()) {
				throw new IllegalStateException("Region " + id + ": addBiome(...) can't be used in the End (it has no climate). "
						+ "Use replace(...), e.g. replace(Biomes.END_HIGHLANDS, yourBiome)");
			}
			for (List<ResourceKey<Biome>> group : exclusiveGroups) {
				for (ResourceKey<Biome> biome : group) {
					boolean used = replacements.values().stream().anyMatch(r -> r.biome().equals(biome))
							|| extraPoints.stream().anyMatch(p -> p.biome().equals(biome));
					if (!used) {
						boolean weighted = weightedReplacements.values().stream()
								.anyMatch(w -> w.biomes().getEntries().stream().anyMatch(e -> e.biome().equals(biome)));
						throw new IllegalStateException("Region " + id + ": exclusive biome " + biome.identifier()
								+ (weighted ? " is part of WeightedBiomes; exclusive(...) only works with single replace(...) and addBiome(...) biomes"
										: " is not used by replace(...) or addBiome(...) in this region"));
					}
				}
			}
			return new BiomeRegion(this);
		}

		/** Builds and registers the region. Call this in your mod's {@code onInitialize()}. */
		public BiomeRegion register() {
			BiomeRegion region = build();
			BiomeRegions.register(region);
			return region;
		}

		private void checkChance(float chance) {
			if (!(chance > 0.0F && chance <= 1.0F)) {
				throw new IllegalArgumentException("Region " + id + ": chance must be more than 0 and at most 1, got " + chance);
			}
		}
	}
}
