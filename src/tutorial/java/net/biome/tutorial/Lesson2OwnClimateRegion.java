package net.biome.tutorial;

import net.biome.api.climate.ClimatePoints;
import net.biome.api.climate.Continentalness;
import net.biome.api.climate.Depth;
import net.biome.api.climate.Erosion;
import net.biome.api.climate.Humidity;
import net.biome.api.climate.Temperature;
import net.biome.api.climate.Terrain;
import net.biome.api.region.BiomeRegion;
import net.minecraft.resources.Identifier;

/**
 * <h2>Lesson 2: a region with only your own climate (no replace)</h2>
 *
 * A region does not have to replace anything. This one only <b>adds</b> biomes, each with a climate you choose.
 * Inside the region's patches, where the climate matches, your biome wins over the vanilla biome that would be
 * there; everywhere else the patch stays vanilla.
 *
 * <h3>Choosing the climate</h3>
 * Every {@link ClimatePoints} setting takes one of three forms, and you can mix them in one biome:
 * <ul>
 *   <li><b>names</b>: {@code .temperature(Temperature.COLD, Temperature.NEUTRAL)} (cold <b>or</b> neutral),</li>
 *   <li><b>a range of names</b>: {@code .continentalness(Continentalness.between(MID_INLAND, FAR_INLAND))},</li>
 *   <li><b>your own numbers</b>: {@code .humidity(-0.35F, 0.3F)} (vanilla's climate values go from -1 to 1).</li>
 * </ul>
 * Leave a setting out and any value is fine. The names are vanilla's own bands, for example
 * {@link Temperature}: {@code FROZEN, COLD, NEUTRAL, WARM, HOT}; {@link Humidity}: {@code ARID, DRY, NEUTRAL, WET,
 * HUMID}; {@link Erosion}: {@code EROSION_0} (mountains) to {@code EROSION_6} (flat). Open the enum to see the
 * numbers behind each name.
 *
 * <h3>Highland meadow: {@code strict()}</h3>
 * Normally the closest climate point wins, so a biome can show up a little outside the ranges you gave (for
 * example an inland biome on the coast). {@code .strict()} means "only inside these ranges, never outside".
 *
 * <h3>Crystal caves: a cave biome, and a chance</h3>
 * {@code .depth(Depth.UNDERGROUND)} puts the biome below the surface only: the surface above stays vanilla.
 * ({@link Depth} also has {@code SURFACE}, {@code DEEP_UNDERGROUND}, {@code ALL_HEIGHTS}, ...) The last argument
 * {@code 0.5F} is a chance: crystal caves are only in half of this region's patches.
 *
 * <h3>Smaller or rarer</h3>
 * <ul>
 *   <li>Narrower ranges make the biome smaller.</li>
 *   <li>{@code .offset(0.1F)} makes your point a bit "worse", so it only wins where vanilla's points are further
 *       away: the biome gets smaller without changing its ranges.</li>
 *   <li>A lower {@code weight} makes the whole region rarer.</li>
 * </ul>
 */
public final class Lesson2OwnClimateRegion {
	public static final Identifier HIGHLANDS = TutorialMod.id("highlands");

	private Lesson2OwnClimateRegion() {
	}

	static void init() {
		BiomeRegion.builder(HIGHLANDS)
				.weight(2)
				// cold, not too dry or wet, far from the sea, hilly and high up
				.addBiome(ClimatePoints.builder()
						.temperature(Temperature.COLD, Temperature.NEUTRAL)                  // names
						.humidity(-0.35F, 0.3F)                                              // your own numbers
						.continentalness(Continentalness.between(Continentalness.MID_INLAND, Continentalness.FAR_INLAND))
						.erosion(Erosion.EROSION_1, Erosion.EROSION_2)
						.terrain(Terrain.HIGH)
						.strict(), TutorialBiomes.HIGHLAND_MEADOW)                           // never outside these ranges
				// wet places underground, in 50% of this region's patches
				.addBiome(ClimatePoints.builder()
						.humidity(Humidity.WET, Humidity.HUMID)
						.depth(Depth.UNDERGROUND), TutorialBiomes.CRYSTAL_CAVES, 0.5F)
				.register();
	}
}
