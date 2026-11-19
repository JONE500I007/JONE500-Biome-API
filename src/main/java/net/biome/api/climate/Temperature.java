package net.biome.api.climate;

import net.minecraft.world.level.biome.Climate;

/**
 * Temperature bands, the same 5 bands vanilla uses for Overworld biomes.
 *
 * <p>Examples of vanilla biomes per band: FROZEN = snowy plains, COLD = taiga, NEUTRAL = plains / forest,
 * WARM = savanna edges / jungle, HOT = desert / badlands.
 */
public enum Temperature {
	FROZEN(-1.0F, -0.45F),
	COLD(-0.45F, -0.15F),
	NEUTRAL(-0.15F, 0.2F),
	WARM(0.2F, 0.55F),
	HOT(0.55F, 1.0F);

	private final float min;
	private final float max;

	Temperature(float min, float max) {
		this.min = min;
		this.max = max;
	}

	public Climate.Parameter parameter() {
		return Climate.Parameter.span(min, max);
	}

	/** One range from {@code from} to {@code to}, e.g. {@code between(NEUTRAL, HOT)}. */
	public static Climate.Parameter between(Temperature from, Temperature to) {
		return Climate.Parameter.span(Math.min(from.min, to.min), Math.max(from.max, to.max));
	}
}
