package net.biome.api.climate;

import net.minecraft.world.level.biome.Climate;

/**
 * Humidity bands (also called "vegetation"), the same 5 bands vanilla uses.
 *
 * <p>Examples: ARID = desert / plains, DRY = plains / savanna, NEUTRAL = forest, WET = birch / dark forest,
 * HUMID = jungle / dark forest.
 */
public enum Humidity {
	ARID(-1.0F, -0.35F),
	DRY(-0.35F, -0.1F),
	NEUTRAL(-0.1F, 0.1F),
	WET(0.1F, 0.3F),
	HUMID(0.3F, 1.0F);

	private final float min;
	private final float max;

	Humidity(float min, float max) {
		this.min = min;
		this.max = max;
	}

	public Climate.Parameter parameter() {
		return Climate.Parameter.span(min, max);
	}

	/** One range from {@code from} to {@code to}, e.g. {@code between(NEUTRAL, HUMID)}. */
	public static Climate.Parameter between(Humidity from, Humidity to) {
		return Climate.Parameter.span(Math.min(from.min, to.min), Math.max(from.max, to.max));
	}
}
