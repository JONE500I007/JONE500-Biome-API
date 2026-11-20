package net.biome.api.surface;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.material.VanillaMaterialConditions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

/**
 * What your surface rules need when a world loads (Minecraft 26.3 surface rules are "material rules" that refer to
 * biomes, named conditions and other rules through registries).
 *
 * <pre>{@code
 * BiomeSurfaceRules.addOverworld(Identifier.fromNamespaceAndPath("mymod", "surface"), ctx ->
 *         MaterialRules.ifTrue(ctx.isBiome(ModBiomes.RED_PLAINS),
 *                 MaterialRules.ifTrue(ctx.onFloor(), MaterialRules.state(Blocks.RED_SAND.defaultBlockState()))));
 * }</pre>
 *
 * @param biomes     the biome registry (for {@code MaterialRules.isBiome(biomes, ...)})
 * @param conditions the named conditions ({@code VanillaMaterialConditions.ON_FLOOR}, ...)
 * @param rules      the named material rules (vanilla's or a datapack's)
 */
public record SurfaceRuleContext(HolderGetter<Biome> biomes, HolderGetter<MaterialCondition> conditions, HolderGetter<MaterialRule> rules) {
	/** True in these biomes. */
	@SafeVarargs
	public final MaterialCondition isBiome(ResourceKey<Biome>... keys) {
		return MaterialRules.isBiome(biomes, keys);
	}

	/** A named condition, e.g. {@code VanillaMaterialConditions.ON_FLOOR}. */
	public MaterialCondition condition(ResourceKey<MaterialCondition> key) {
		return MaterialRules.getCondition(conditions, key);
	}

	/** A named material rule (vanilla's or a datapack's). */
	public MaterialRule rule(ResourceKey<MaterialRule> key) {
		return MaterialRules.getRule(rules, key);
	}

	/** The top block of the ground (like the old {@code SurfaceRules.ON_FLOOR}). */
	public MaterialCondition onFloor() {
		return condition(VanillaMaterialConditions.ON_FLOOR);
	}

	/** The few blocks under the top block (like the old {@code SurfaceRules.UNDER_FLOOR}). */
	public MaterialCondition underFloor() {
		return condition(VanillaMaterialConditions.UNDER_FLOOR);
	}

	/** The lowest block of a ceiling (cave roofs, the Nether's ceiling). */
	public MaterialCondition onCeiling() {
		return condition(VanillaMaterialConditions.ON_CEILING);
	}

	/** Not under water (like vanilla's grass vs. dirt choice). */
	public MaterialCondition notUnderwater() {
		return condition(VanillaMaterialConditions.NOT_UNDERWATER);
	}
}
