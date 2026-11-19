package net.biome.api.impl;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/** Added to MultiNoiseBiomeSource and TheEndBiomeSource by mixins. Internal, not part of the API. */
public interface BiomeApiBiomeSource {
	@Nullable RegionBiomeLookup jone500$getLookup();

	void jone500$setLookup(RegionBiomeLookup lookup);

	/** The climate table, or null for sources without one (the End). */
	Climate.@Nullable ParameterList<Holder<Biome>> jone500$getParameters();
}
