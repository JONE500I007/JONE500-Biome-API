package net.biome.tutorial.test;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/** Small helpers for the tests, for the Minecraft 26.3 world generation API. */
final class TestWorldgen {
	private TestWorldgen() {
	}

	/** A climate sampler without caches (fine for single lookups in tests). */
	static Climate.Sampler sampler(RandomState randomState) {
		return randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
	}

	static RandomState randomState(RegistryAccess registries, ResourceKey<NoiseGeneratorSettings> settings, long seed) {
		return RandomState.create(registries.lookupOrThrow(Registries.NOISE), seed,
				registries.lookupOrThrow(Registries.NOISE_SETTINGS).getValueOrThrow(settings));
	}

	/** The biome the source picks at a quart position (since 26.3 biome sources answer through a resolver). */
	static Holder<Biome> biome(BiomeSource source, int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		return source.createResolver(sampler).getNoiseBiome(quartX, quartY, quartZ);
	}
}
