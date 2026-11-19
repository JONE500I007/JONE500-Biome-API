package net.biome.api.climate;

import net.minecraft.world.level.biome.Climate;

/**
 * How flat the land is (erosion), the same 7 levels vanilla uses.
 *
 * <p>EROSION_0 = least eroded = <b>mountains</b>, ... EROSION_6 = most eroded = <b>very flat</b> (swamps,
 * flat plains). Examples: EROSION_0-1 = peaks / slopes, EROSION_2-4 = hills and normal land,
 * EROSION_5-6 = flat land, rivers and swamps.
 */
public enum Erosion {
	EROSION_0(-1.0F, -0.78F),
	EROSION_1(-0.78F, -0.375F),
	EROSION_2(-0.375F, -0.2225F),
	EROSION_3(-0.2225F, 0.05F),
	EROSION_4(0.05F, 0.45F),
	EROSION_5(0.45F, 0.55F),
	EROSION_6(0.55F, 1.0F);

	private final float min;
	private final float max;

	Erosion(float min, float max) {
		this.min = min;
		this.max = max;
	}

	public Climate.Parameter parameter() {
		return Climate.Parameter.span(min, max);
	}

	/** One range from {@code from} to {@code to}, e.g. {@code between(EROSION_2, EROSION_4)}. */
	public static Climate.Parameter between(Erosion from, Erosion to) {
		return Climate.Parameter.span(Math.min(from.min, to.min), Math.max(from.max, to.max));
	}
}
