package net.biome.tutorial;

import net.biome.api.climate.ClimatePoints;
import net.biome.api.climate.Continentalness;
import net.biome.api.climate.Erosion;
import net.biome.api.climate.Temperature;
import net.biome.api.climate.Terrain;
import net.biome.api.region.BiomeRegion;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biomes;

/**
 * <h2>Lesson 1: your first region</h2>
 *
 * The Overworld is split into big patches (about 512 blocks across). Each patch is either vanilla or one
 * <b>region</b>, picked by weight. Inside your region the terrain and climate stay exactly vanilla: only the
 * biomes you name change.
 *
 * <p>This region:
 * <ul>
 *   <li>turns <b>every</b> plains in its patches into red plains,</li>
 *   <li>turns forests into blue forests in <b>half</b> of its patches ({@code 0.5F} = 50%), so the two don't
 *       always come together,</li>
 *   <li>adds orange hills with a climate of their own ({@code addBiome}): warm, inland, hilly land. Where that
 *       climate is, orange hills replace whatever vanilla biome would be there.</li>
 * </ul>
 *
 * <p><b>Weight:</b> vanilla patches have weight 10. This region has 3, and the regions of Lessons 2 and 3 have 2
 * each, so with only this mod about 3 of every 17 patches are this region. Decimals work too: {@code weight(0.25F)} is a very rare region.
 *
 * <p><b>Climate:</b> {@link ClimatePoints} uses named values ({@link Temperature#WARM},
 * {@link Continentalness#FAR_INLAND}, ...) so you don't need vanilla's raw numbers. Leave a value out and any
 * value is fine. Lesson 2 shows more: your own numbers, {@code strict()}, cave biomes.
 *
 * <p><b>Ids:</b> regions are sorted by id, so the same seed always gives the same world, whatever order mods
 * load in.
 */
public final class Lesson1FirstRegion {
	public static final Identifier WARM = TutorialMod.id("warm");

	private Lesson1FirstRegion() {
	}

	static void init() {
		BiomeRegion.builder(WARM)
				.overworld()            // the default, so you may leave it out
				.weight(3)
				.replace(Biomes.PLAINS, TutorialBiomes.RED_PLAINS)
				.replace(Biomes.FOREST, TutorialBiomes.BLUE_FOREST, 0.5F)
				.addBiome(ClimatePoints.builder()
						.temperature(Temperature.NEUTRAL, Temperature.WARM)
						.continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
						.erosion(Erosion.EROSION_2, Erosion.EROSION_3, Erosion.EROSION_4)
						.terrain(Terrain.MID, Terrain.HIGH), TutorialBiomes.ORANGE_HILLS)
				.register();
	}
}
