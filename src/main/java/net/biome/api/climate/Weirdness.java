package net.biome.api.climate;

import net.minecraft.world.level.biome.Climate;

/**
 * The 13 raw weirdness slices vanilla uses, from -1 to 1. Most mods only need {@link Terrain}, which groups
 * these by land shape.
 *
 * <p>"NORMAL" slices (weirdness below 0) and "VARIANT" slices (above 0) have the same land shape; vanilla
 * uses the variant side for some biome variants (e.g. sunflower plains). ASCENDING / DESCENDING tell on which
 * side of a peak the slice is.
 */
public enum Weirdness {
	MID_SLICE_NORMAL_ASCENDING(-1.0F, -0.93333334F),
	HIGH_SLICE_NORMAL_ASCENDING(-0.93333334F, -0.7666667F),
	PEAK_NORMAL(-0.7666667F, -0.56666666F),
	HIGH_SLICE_NORMAL_DESCENDING(-0.56666666F, -0.4F),
	MID_SLICE_NORMAL_DESCENDING(-0.4F, -0.26666668F),
	LOW_SLICE_NORMAL_DESCENDING(-0.26666668F, -0.05F),
	VALLEY(-0.05F, 0.05F),
	LOW_SLICE_VARIANT_ASCENDING(0.05F, 0.26666668F),
	MID_SLICE_VARIANT_ASCENDING(0.26666668F, 0.4F),
	HIGH_SLICE_VARIANT_ASCENDING(0.4F, 0.56666666F),
	PEAK_VARIANT(0.56666666F, 0.7666667F),
	HIGH_SLICE_VARIANT_DESCENDING(0.7666667F, 0.93333334F),
	MID_SLICE_VARIANT_DESCENDING(0.93333334F, 1.0F);

	private final float min;
	private final float max;

	Weirdness(float min, float max) {
		this.min = min;
		this.max = max;
	}

	public Climate.Parameter parameter() {
		return Climate.Parameter.span(min, max);
	}
}
