package net.biome.api.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.biome.api.impl.BiomeApiChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

/**
 * Puts the registered surface rules in front of the dimension's own material rule. Since Minecraft 26.3 the rule
 * is passed to {@code buildSurface} (the ground) and {@code generateCarvers} (blocks at cave openings).
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin implements BiomeApiChunkGenerator {
	@Unique
	private volatile @Nullable MaterialRule jone500$surfaceRule;

	@Override
	public void jone500$setSurfaceRule(MaterialRule rule) {
		this.jone500$surfaceRule = rule;
	}

	@ModifyVariable(method = {
			"buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Ljava/util/Set;Lnet/minecraft/world/level/levelgen/material/rule/MaterialRule;)V",
			"generateCarvers"}, require = 2, at = @At("HEAD"), argsOnly = true)
	private MaterialRule jone500$surfaceRule(MaterialRule original) {
		MaterialRule rule = jone500$surfaceRule;
		return rule != null ? rule : original;
	}
}
