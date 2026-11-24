package net.biome.tutorial;

import net.biome.api.surface.BiomeSurfaceRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * <h2>Lesson 4: surface rules (the ground of your biome)</h2>
 *
 * Grass, sand, terracotta: the top blocks of the ground are not in the biome JSON, they come from the
 * dimension's <b>surface rules</b>. With {@link BiomeSurfaceRules} you add your own rule, and the API puts it in
 * front of vanilla's rules (and other mods' rules), so it wins in your biome.
 *
 * <p>A surface rule is a {@code SurfaceRules.RuleSource}: "if (condition) then (block)". Wrap it in
 * {@code SurfaceRules.isBiome(biomes, yourBiome)} so it only changes <b>your</b> biome and never vanilla ground.
 *
 * <p>Why {@code biomes -> ...}? Since Minecraft 26.2, {@code isBiome} needs the biome registry, which does not
 * exist yet when your mod starts. So you give a <b>function</b>: the API calls it later, when the world loads,
 * with the registry ({@code biomes}).
 *
 * <p>This rule: in red plains, on the floor (the top block of the ground), where there is no water above it
 * ({@code waterBlockCheck(-1, 0)}), the block is red sand. Everything else stays vanilla.
 *
 * <p>The id ({@code red_plains_sand}) only names the rule: rules are sorted by id, like regions.
 * Nether and End rules work the same way with {@code addNether} and {@code addEnd} (Lessons 5 and 6).
 */
public final class Lesson4SurfaceRules {
	private Lesson4SurfaceRules() {
	}

	static void init() {
		BiomeSurfaceRules.addOverworld(TutorialMod.id("red_plains_sand"), biomes ->
				SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, TutorialBiomes.RED_PLAINS),
						SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
								SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0),
										SurfaceRules.state(Blocks.RED_SAND.defaultBlockState())))));
	}
}
