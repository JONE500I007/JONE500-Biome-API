package net.biome.tutorial;

import net.biome.api.region.BiomeRegion;
import net.biome.api.region.BiomeReplacements;
import net.biome.api.surface.BiomeSurfaceRules;
import net.fabricmc.fabric.api.biome.v1.TheEndBiomes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * <h2>Lesson 6: the End</h2>
 *
 * The End has no climate: its biome comes from the terrain (highlands, midlands, barrens, small islands). So an
 * End region ({@code .end()}) can only <b>replace</b> those biomes inside its patches; {@code addBiome} can't be
 * used there. Weights, chances and {@code exclusive} work like in the Overworld.
 *
 * <p>This lesson shows three things together:
 * <ul>
 *   <li><b>Fabric API</b> still works next to Biome API: {@code TheEndBiomes.addHighlandsBiome} places void meadows
 *       among the highlands (weight 10).</li>
 *   <li><b>Ashlands region:</b> in its patches, barrens become ash barrens, and midlands too in half of the
 *       patches.</li>
 *   <li><b>Whole-biome swap:</b> in every world, small End islands become void meadows
 *       ({@code BiomeReplacements} from Lesson 3 works in the End too).</li>
 * </ul>
 * Plus an End surface rule ({@code addEnd}): the ground of ash barrens is smooth basalt instead of end stone.
 *
 * <p>Tip: the main End island is always vanilla. Fly to the outer islands (through an End gateway) to see these
 * biomes.
 */
public final class Lesson6End {
	public static final Identifier ASHLANDS = TutorialMod.id("ashlands");

	private Lesson6End() {
	}

	static void init() {
		TheEndBiomes.addHighlandsBiome(TutorialBiomes.VOID_MEADOW, 10.0);

		BiomeRegion.builder(ASHLANDS)
				.end()
				.weight(5)
				.replace(Biomes.END_BARRENS, TutorialBiomes.ASH_BARRENS)
				.replace(Biomes.END_MIDLANDS, TutorialBiomes.ASH_BARRENS, 0.5F)
				.register();

		BiomeSurfaceRules.addEnd(TutorialMod.id("ash_floor"), biomes ->
				SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, TutorialBiomes.ASH_BARRENS),
						SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.SMOOTH_BASALT.defaultBlockState()))));

		BiomeReplacements.replaceWholeBiome(Biomes.SMALL_END_ISLANDS, TutorialBiomes.VOID_MEADOW, 1.0F);
	}
}
