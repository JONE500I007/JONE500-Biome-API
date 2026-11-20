package net.biome.testmod.compat;

import java.util.function.Consumer;

import com.mojang.datafixers.util.Pair;

import net.biome.testmod.BiomeTestMod;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;
import terrablender.api.TerraBlenderApi;

/**
 * Compatibility test: a TerraBlender region that replaces plains (our API's "warm" region replaces plains
 * too), plus a TerraBlender surface rule. Only loaded when TerraBlender is installed.
 */
public class TerraBlenderCompat implements TerraBlenderApi {
	@Override
	public void onTerraBlenderInitialized() {
		Regions.register(new Region(BiomeTestMod.id("tb_test"), RegionType.OVERWORLD, 5) {
			@Override
			public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
				addModifiedVanillaOverworldBiomes(mapper, builder -> builder.replaceBiome(Biomes.PLAINS, BiomeTestMod.TB_MEADOW));
			}
		});
		SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, BiomeTestMod.MOD_ID, biomes ->
				SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, BiomeTestMod.TB_MEADOW),
						SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
								SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0),
										SurfaceRules.state(Blocks.DYED_TERRACOTTA.blue().defaultBlockState())))));
	}
}
