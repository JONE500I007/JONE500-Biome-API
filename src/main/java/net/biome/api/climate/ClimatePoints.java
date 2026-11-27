package net.biome.api.climate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import net.minecraft.world.level.biome.Climate;

/**
 * Describes where a biome goes, in words instead of numbers, and turns it into the climate points
 * Minecraft needs.
 *
 * <pre>{@code
 * List<Climate.ParameterPoint> points = ClimatePoints.builder()
 *         .temperature(Temperature.NEUTRAL, Temperature.WARM)
 *         .humidity(Humidity.NEUTRAL, Humidity.WET)
 *         .continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
 *         .erosion(Erosion.EROSION_5, Erosion.EROSION_6)
 *         .terrain(Terrain.MID, Terrain.LOW)
 *         .build();
 * }</pre>
 *
 * <p>Listing several values means "any of them" (one point is made for every combination).
 * Anything you leave out matches everything, except {@link #depth}, which defaults to
 * {@link Depth#SURFACE}.
 *
 * <p>Usually you don't call {@link #build()} yourself: pass the builder to
 * {@code BiomeRegion.Builder.addBiome(ClimatePoints.Builder, biome)}.
 */
public final class ClimatePoints {
	private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0F, 1.0F);

	private ClimatePoints() {
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A <b>Nether</b> biome point. Vanilla Nether biomes are points in temperature x humidity (everything else
	 * is 0); the closest point wins, so each Nether biome owns the area around its point.
	 *
	 * <p>Vanilla points (temperature, humidity): nether wastes (0, 0), soul sand valley (0, -0.5),
	 * crimson forest (0.4, 0), warped forest (0, 0.5), basalt deltas (-0.5, 0).
	 * Pick a point between them, e.g. {@code netherPoint(0.3F, -0.4F)} lands between crimson forest and
	 * soul sand valley.
	 */
	public static Climate.ParameterPoint netherPoint(float temperature, float humidity) {
		return netherPoint(temperature, humidity, 0.0F);
	}

	/**
	 * Like {@link #netherPoint(float, float)} with an offset: a higher offset makes the biome smaller (vanilla:
	 * warped forest 0.375, basalt deltas 0.175).
	 */
	public static Climate.ParameterPoint netherPoint(float temperature, float humidity, float offset) {
		return Climate.parameters(temperature, humidity, 0.0F, 0.0F, 0.0F, 0.0F, offset);
	}

	public static final class Builder {
		private List<Climate.Parameter> temperature = List.of(FULL_RANGE);
		private List<Climate.Parameter> humidity = List.of(FULL_RANGE);
		private List<Climate.Parameter> continentalness = List.of(FULL_RANGE);
		private List<Climate.Parameter> erosion = List.of(FULL_RANGE);
		private List<Climate.Parameter> weirdness = List.of(FULL_RANGE);
		private List<Climate.Parameter> depth = Depth.SURFACE.parameters();
		private float offset;
		private boolean strict;
		private boolean overlay;

		private Builder() {
		}

		// ---- temperature ----

		/** Any of these temperature bands. */
		public Builder temperature(Temperature... values) {
			this.temperature = map(values, Temperature::parameter);
			return this;
		}

		/** Exact temperature range(s), e.g. {@code Temperature.between(COLD, NEUTRAL)} or {@code Climate.Parameter.span(-0.2F, 0.2F)}. */
		public Builder temperature(Climate.Parameter... ranges) {
			this.temperature = list(ranges);
			return this;
		}

		/** One exact range with numbers from -1 to 1, e.g. {@code .temperature(-0.2F, 0.4F)}. */
		public Builder temperature(float min, float max) {
			return temperature(range(min, max));
		}

		// ---- humidity ----

		/** Any of these humidity bands. */
		public Builder humidity(Humidity... values) {
			this.humidity = map(values, Humidity::parameter);
			return this;
		}

		public Builder humidity(Climate.Parameter... ranges) {
			this.humidity = list(ranges);
			return this;
		}

		/** One exact range with numbers from -1 to 1, e.g. {@code .humidity(-0.5F, 0.5F)}. */
		public Builder humidity(float min, float max) {
			return humidity(range(min, max));
		}

		// ---- continentalness ----

		/** Any of these distances from the ocean. */
		public Builder continentalness(Continentalness... values) {
			this.continentalness = map(values, Continentalness::parameter);
			return this;
		}

		public Builder continentalness(Climate.Parameter... ranges) {
			this.continentalness = list(ranges);
			return this;
		}

		/** One exact range with numbers (about -1.2 = mushroom islands ... 1 = far inland), e.g. {@code .continentalness(0.0F, 0.6F)}. */
		public Builder continentalness(float min, float max) {
			return continentalness(range(min, max));
		}

		// ---- erosion ----

		/** Any of these erosion levels (EROSION_0 = mountains ... EROSION_6 = very flat). */
		public Builder erosion(Erosion... values) {
			this.erosion = map(values, Erosion::parameter);
			return this;
		}

		public Builder erosion(Climate.Parameter... ranges) {
			this.erosion = list(ranges);
			return this;
		}

		/** One exact range with numbers from -1 (mountains) to 1 (flat), e.g. {@code .erosion(0.3F, 0.8F)}. */
		public Builder erosion(float min, float max) {
			return erosion(range(min, max));
		}

		// ---- land shape (weirdness) ----

		/** Any of these land shapes (VALLEY, LOW, MID, HIGH, PEAK). Replaces earlier terrain/weirdness. */
		public Builder terrain(Terrain... values) {
			List<Climate.Parameter> result = new ArrayList<>();
			for (Terrain t : values) {
				result.addAll(t.parameters());
			}
			this.weirdness = requireNotEmpty(result);
			return this;
		}

		/** Any of these raw weirdness slices (advanced, see {@link Terrain} for the easy version). */
		public Builder weirdness(Weirdness... values) {
			this.weirdness = map(values, Weirdness::parameter);
			return this;
		}

		public Builder weirdness(Climate.Parameter... ranges) {
			this.weirdness = list(ranges);
			return this;
		}

		/** One exact weirdness range with numbers from -1 to 1, e.g. {@code .weirdness(-0.05F, 0.05F)} (= valleys). */
		public Builder weirdness(float min, float max) {
			return weirdness(range(min, max));
		}

		// ---- depth ----

		/** Where in the ground (default {@link Depth#SURFACE}). */
		public Builder depth(Depth... values) {
			List<Climate.Parameter> result = new ArrayList<>();
			for (Depth d : values) {
				result.addAll(d.parameters());
			}
			this.depth = requireNotEmpty(result);
			return this;
		}

		public Builder depth(Climate.Parameter... ranges) {
			this.depth = list(ranges);
			return this;
		}

		/**
		 * One exact depth range with numbers: 0 = the surface, higher = deeper (vanilla caves 0.2 - 0.9),
		 * negative = above the ground (air and water), e.g. {@code .depth(0.0F, 1.0F)} for the surface down to
		 * the bottom, or {@code .depth(-1.0F, 1.1F)} for every height (see {@link Depth#ALL_HEIGHTS}).
		 */
		public Builder depth(float min, float max) {
			return depth(range(min, max));
		}

		// ---- offset ----

		/**
		 * Makes this biome a bit "further away" from every climate, so neighbouring biomes win more often
		 * near its edges. 0 = normal (default). Vanilla uses 0.375 for some rare biomes.
		 */
		public Builder offset(float offset) {
			this.offset = offset;
			return this;
		}

		/**
		 * <b>Strict mode</b>: the biome is only used where the climate is really inside your ranges for
		 * temperature, humidity, continentalness, erosion and terrain/weirdness.
		 *
		 * <p>Normally Minecraft picks the biome whose climate is <i>closest</i>, so near the edge of your ranges
		 * (or when nothing else is closer) your biome can appear a little outside them, e.g. a bit into the
		 * ocean although you said inland. With strict mode those places get the biome vanilla would put
		 * there instead. Depth is not checked (it is a single value at the surface).
		 */
		public Builder strict() {
			this.strict = true;
			return this;
		}

		public boolean isStrict() {
			return strict;
		}

		/**
		 * <b>Overlay mode</b> (TerraBlender-style placement, optional): inside its region the biome also wins
		 * <i>around</i> its ranges, not only inside them, so it becomes bigger and more common.
		 *
		 * <p>Normally your biome has to beat the vanilla biome at each spot, and every vanilla biome fits its own
		 * spot exactly, so your biome only appears where <b>all</b> values are inside your ranges. With overlay
		 * mode it only has to beat "the rest": boxes made from the parts of each value outside your ranges (the
		 * same rule TerraBlender's {@code VanillaParameterOverlayBuilder} uses). Where the climate is closer to
		 * your ranges than to the rest, your biome is placed; elsewhere the normal rules apply (vanilla, other
		 * points of the region, replacements).
		 *
		 * <p>Use it when you move biomes from TerraBlender: the same values then give the same result. All overlay
		 * biomes of a region are worked out together, like the points of one TerraBlender region.
		 * Chances and {@code exclusive(...)} work as usual.
		 */
		public Builder overlay() {
			this.overlay = true;
			return this;
		}

		public boolean isOverlay() {
			return overlay;
		}

		/** One climate point for every combination of the values above. */
		public List<Climate.ParameterPoint> build() {
			List<Climate.ParameterPoint> points = new ArrayList<>();
			for (Climate.Parameter t : temperature) {
				for (Climate.Parameter h : humidity) {
					for (Climate.Parameter c : continentalness) {
						for (Climate.Parameter e : erosion) {
							for (Climate.Parameter d : depth) {
								for (Climate.Parameter w : weirdness) {
									points.add(Climate.parameters(t, h, c, e, d, w, offset));
								}
							}
						}
					}
				}
			}
			return points;
		}

		private static Climate.Parameter range(float min, float max) {
			if (Float.isNaN(min) || Float.isNaN(max) || min > max) {
				throw new IllegalArgumentException("Range min must be smaller than max, got " + min + " .. " + max);
			}
			return min == max ? Climate.Parameter.point(min) : Climate.Parameter.span(min, max);
		}

		private static <T> List<Climate.Parameter> map(T[] values, Function<T, Climate.Parameter> f) {
			return requireNotEmpty(Arrays.stream(values).map(f).toList());
		}

		private static List<Climate.Parameter> list(Climate.Parameter[] ranges) {
			return requireNotEmpty(List.of(ranges));
		}

		private static List<Climate.Parameter> requireNotEmpty(List<Climate.Parameter> list) {
			if (list.isEmpty()) {
				throw new IllegalArgumentException("Give at least one value");
			}
			return List.copyOf(list);
		}
	}
}
