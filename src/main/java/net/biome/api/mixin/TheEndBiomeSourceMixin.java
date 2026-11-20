package net.biome.api.mixin;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.biome.api.impl.BiomeApiBiomeSource;
import net.biome.api.impl.RegionBiomeLookup;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;

/**
 * The End has no regions; only BiomeReplacements (area and whole-biome) are applied to the vanilla biome.
 * Biomes placed by Fabric API's TheEndBiomes (or other mods) can be replaced too.
 */
@Mixin(TheEndBiomeSource.class)
public abstract class TheEndBiomeSourceMixin implements BiomeApiBiomeSource {
	@Unique
	private volatile @Nullable RegionBiomeLookup jone500$lookup;

	@Override
	public @Nullable RegionBiomeLookup jone500$getLookup() {
		return jone500$lookup;
	}

	@Override
	public void jone500$setLookup(RegionBiomeLookup lookup) {
		this.jone500$lookup = lookup;
	}

	@Override
	public Climate.@Nullable ParameterList<Holder<Biome>> jone500$getParameters() {
		return null; // the End is not multi-noise
	}

	/**
	 * Wraps the whole method, including other mods' changes (Fabric API's TheEndBiomes, TerraBlender, ...), so
	 * the replacements apply to the biome they finally chose.
	 */
	@WrapMethod(method = "getNoiseBiome")
	private Holder<Biome> jone500$replace(int quartX, int quartY, int quartZ, Climate.Sampler sampler, Operation<Holder<Biome>> original) {
		Holder<Biome> biome = original.call(quartX, quartY, quartZ, sampler);
		RegionBiomeLookup lookup = jone500$lookup;
		return lookup == null ? biome : lookup.applyGlobalReplacements(biome, quartX, quartZ);
	}

	@ModifyReturnValue(method = "collectPossibleBiomes", at = @At("RETURN"))
	private Stream<Holder<Biome>> jone500$addBiomes(Stream<Holder<Biome>> original) {
		RegionBiomeLookup lookup = jone500$lookup;
		return lookup == null ? original : Stream.concat(original, lookup.getAddedBiomes().stream());
	}
}
