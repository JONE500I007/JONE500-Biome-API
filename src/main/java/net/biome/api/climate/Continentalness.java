package net.biome.api.climate;

import net.minecraft.world.level.biome.Climate;

/**
 * How far from the ocean (continentalness), the same ranges vanilla uses.
 *
 * <p>From the sea to the middle of the continent: DEEP_OCEAN, OCEAN, COAST, NEAR_INLAND, MID_INLAND,
 * FAR_INLAND. {@link #INLAND} is vanilla's wide "inland" range (near + mid + the start of far inland).
 * {@link #ALL_LAND} = from the coast to the far inland.
 */
public enum Continentalness {
	MUSHROOM_FIELDS(-1.2F, -1.05F),
	DEEP_OCEAN(-1.05F, -0.455F),
	OCEAN(-0.455F, -0.19F),
	COAST(-0.19F, -0.11F),
	NEAR_INLAND(-0.11F, 0.03F),
	MID_INLAND(0.03F, 0.3F),
	FAR_INLAND(0.3F, 1.0F),
	INLAND(-0.11F, 0.55F),
	ALL_LAND(-0.19F, 1.0F);

	private final float min;
	private final float max;

	Continentalness(float min, float max) {
		this.min = min;
		this.max = max;
	}

	public Climate.Parameter parameter() {
		return Climate.Parameter.span(min, max);
	}

	/** One range from {@code from} to {@code to}, e.g. {@code between(NEAR_INLAND, FAR_INLAND)}. */
	public static Climate.Parameter between(Continentalness from, Continentalness to) {
		return Climate.Parameter.span(Math.min(from.min, to.min), Math.max(from.max, to.max));
	}
}
