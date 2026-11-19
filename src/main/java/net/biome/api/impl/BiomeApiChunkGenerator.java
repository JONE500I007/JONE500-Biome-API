package net.biome.api.impl;

import net.minecraft.world.level.levelgen.SurfaceRules;

/** Added to NoiseBasedChunkGenerator by a mixin. Internal, not part of the API. */
public interface BiomeApiChunkGenerator {
	void jone500$setSurfaceRule(SurfaceRules.RuleSource rule);
}
