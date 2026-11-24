package net.biome.tutorial;

import net.biome.api.surface.BiomeSurfaceRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.material.MaterialRules;

/**
 * <h2>Lesson 5: surface rules (the ground of your biome)</h2>
 *
 * Grass, sand, terracotta: the top blocks of the ground are not in the biome JSON, they come from the
 * dimension's <b>surface rules</b>. With {@link BiomeSurfaceRules} you add your own rule, and the API puts it in
 * front of vanilla's rules (and other mods' rules), so it wins in your biome.
 *
 * <p>Since Minecraft 26.3, surface rules are <b>material rules</b> ({@code MaterialRules}): "if (condition) then
 * (block)". Wrap your rule in {@code ctx.isBiome(yourBiome)} so it only changes <b>your</b> biome and never vanilla
 * ground.
 *
 * <p>Why {@code ctx -> ...}? Some conditions need the game's registries, which do not exist yet when your mod
 * starts. So you give a <b>function</b>: the API calls it later, when the world loads. {@code ctx} (a
 * {@link net.biome.api.surface.SurfaceRuleContext}) makes those conditions for you: {@code isBiome},
 * {@code onFloor}, {@code underFloor}, {@code onCeiling}, {@code notUnderwater}. Conditions that need nothing,
 * like {@code MaterialRules.waterBlockCheck(-1, 0)}, are used directly.
 *
 * <p>This rule: in red plains, on the floor (the top block of the ground), where there is no water above it
 * ({@code waterBlockCheck(-1, 0)}), the block is red sand. Everything else stays vanilla.
 *
 * <p>The id ({@code red_plains_sand}) only names the rule: rules are sorted by id, like regions.
 * Nether and End rules work the same way with {@code addNether} and {@code addEnd} (Lessons 6 and 7).
 */
public final class Lesson5SurfaceRules {
	private Lesson5SurfaceRules() {
	}

	static void init() {
		BiomeSurfaceRules.addOverworld(TutorialMod.id("red_plains_sand"), ctx ->
				MaterialRules.ifTrue(ctx.isBiome(TutorialBiomes.RED_PLAINS),
						MaterialRules.ifTrue(ctx.onFloor(),
								MaterialRules.ifTrue(MaterialRules.waterBlockCheck(-1, 0),
										MaterialRules.state(Blocks.RED_SAND.defaultBlockState())))));
	}
}
