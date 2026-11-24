package net.biome.tutorial;

import net.biome.api.climate.ClimatePoints;
import net.biome.api.region.BiomeRegion;
import net.biome.api.region.WeightedBiomes;
import net.biome.api.surface.BiomeSurfaceRules;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * <h2>Lesson 6: the Nether</h2>
 *
 * Add {@code .nether()} and a region works in the Nether, with its own patches, weights and chances, just like in
 * the Overworld.
 *
 * <p><b>Embers region:</b> crimson forests become ember forests, and ember forests also get a point of their own,
 * {@code ClimatePoints.netherPoint(temperature, humidity)}. Nether biomes are points: the closest point wins.
 * Vanilla's points are nether wastes (0, 0), soul sand valley (0, -0.5), crimson forest (0.4, 0),
 * warped forest (0, 0.5) and basalt deltas (-0.5, 0), so (0.25, -0.3) lies between crimson forest and soul sand
 * valley.
 *
 * <p><b>Wastes region:</b> nether wastes become sulfur wastes <b>or</b> bone fields, 3 : 1, with
 * {@link WeightedBiomes}. No climate needed: the region's patches are cut into cells and each cell picks one biome
 * by weight. Use {@code .keepOriginal(weight)} in the builder if some cells should stay vanilla.
 *
 * <p><b>Surface rule:</b> the floor of ember forests is magma, like Lesson 5 but with {@code addNether}.
 *
 * <p>Your Nether biome also needs vanilla's Nether tags (fortresses, bastions, {@code is_nether}): see
 * {@link TutorialDataGen}.
 */
public final class Lesson6Nether {
	public static final Identifier EMBERS = TutorialMod.id("embers");
	public static final Identifier WASTES = TutorialMod.id("wastes");

	private Lesson6Nether() {
	}

	static void init() {
		BiomeRegion.builder(EMBERS)
				.nether()
				.weight(5)
				.replace(Biomes.CRIMSON_FOREST, TutorialBiomes.EMBER_FOREST)
				.addBiome(ClimatePoints.netherPoint(0.25F, -0.3F), TutorialBiomes.EMBER_FOREST)
				.register();

		BiomeRegion.builder(WASTES)
				.nether()
				.weight(4)
				.replace(Biomes.NETHER_WASTES, WeightedBiomes.builder()
						.add(TutorialBiomes.SULFUR_WASTES, 3)
						.add(TutorialBiomes.BONE_FIELDS, 1)
						.build())
				.register();

		BiomeSurfaceRules.addNether(TutorialMod.id("ember_floor"),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(TutorialBiomes.EMBER_FOREST),
						SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.MAGMA_BLOCK.defaultBlockState()))));
	}
}
