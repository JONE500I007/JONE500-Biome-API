package net.biome.api.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.biome.api.impl.BiomeApiChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.SurfaceRules;

/** Puts the registered surface rules in front of the dimension's own rules. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin implements BiomeApiChunkGenerator {
	@Unique
	private volatile SurfaceRules.@Nullable RuleSource jone500$surfaceRule;

	@Override
	public void jone500$setSurfaceRule(SurfaceRules.RuleSource rule) {
		this.jone500$surfaceRule = rule;
	}

	@ModifyExpressionValue(method = {
			// The overload that calls surfaceRule() (the other one, with WorldGenRegion, only forwards to it).
			"buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/core/Registry;Lnet/minecraft/world/level/levelgen/blending/Blender;)V",
			"applyCarvers"}, require = 2,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;surfaceRule()Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;"))
	private SurfaceRules.RuleSource jone500$surfaceRule(SurfaceRules.RuleSource original) {
		SurfaceRules.RuleSource rule = jone500$surfaceRule;
		return rule != null ? rule : original;
	}
}
