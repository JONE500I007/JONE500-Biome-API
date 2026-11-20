package net.biome.api.mixin;

import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.biome.api.impl.BiomeApiBiomeSource;
import net.biome.api.impl.RegionBiomeLookup;
import net.biome.api.region.BiomeRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin implements BiomeApiBiomeSource {
	@Unique
	private volatile @Nullable RegionBiomeLookup jone500$lookup;

	@Shadow
	protected abstract Climate.ParameterList<Holder<Biome>> parameters();

	@Override
	public @Nullable RegionBiomeLookup jone500$getLookup() {
		return jone500$lookup;
	}

	@Override
	public void jone500$setLookup(RegionBiomeLookup lookup) {
		this.jone500$lookup = lookup;
	}

	@Override
	public Climate.ParameterList<Holder<Biome>> jone500$getParameters() {
		return parameters();
	}

	/**
	 * Since Minecraft 26.3 every biome lookup of this source (chunk generation, /locate, F3) goes through a
	 * {@link BiomeResolver} made here (one per call or per chunk).
	 *
	 * <p>Wraps the whole method, including other mods' changes (TerraBlender, Biolith, ...): spots that belong to
	 * one of our regions get our biome; every other spot is left to the original resolver (vanilla or the other
	 * mods), and only our world-wide replacements are applied to its result.
	 */
	@WrapMethod(method = "createResolver")
	private BiomeResolver jone500$wrapResolver(Climate.Sampler sampler, Operation<BiomeResolver> original) {
		return jone500$wrap(original.call(sampler), sampler);
	}

	/** Same for the per-chunk resolver (it samples the whole chunk's climate at once for vanilla spots). */
	@WrapMethod(method = "createResolverForChunk")
	private BiomeResolver jone500$wrapChunkResolver(Climate.Sampler sampler, int quartX, int quartY, int quartZ, int sizeX, int sizeY, int sizeZ,
			Operation<BiomeResolver> original) {
		return jone500$wrap(original.call(sampler, quartX, quartY, quartZ, sizeX, sizeY, sizeZ), sampler);
	}

	@Unique
	private BiomeResolver jone500$wrap(BiomeResolver original, Climate.Sampler sampler) {
		RegionBiomeLookup lookup = jone500$lookup;
		if (lookup == null) {
			return original;
		}
		Climate.ParameterList<Holder<Biome>> parameters = parameters();
		return (quartX, quartY, quartZ) -> {
			if (!lookup.isVanillaCell(quartX, quartZ)) {
				return lookup.getBiome(sampler.sample(quartX, quartY, quartZ), quartX, quartZ, parameters);
			}
			return lookup.applyGlobalReplacements(original.getNoiseBiome(quartX, quartY, quartZ), quartX, quartZ);
		};
	}

	/** Region biomes are possible biomes too (features and structures need this). */
	@ModifyReturnValue(method = "collectPossibleBiomes", at = @At("RETURN"))
	private Stream<Holder<Biome>> jone500$addRegionBiomes(Stream<Holder<Biome>> original) {
		RegionBiomeLookup lookup = jone500$lookup;
		return lookup == null ? original : Stream.concat(original, lookup.getAddedBiomes().stream());
	}

	/** Shows the region on the F3 screen (singleplayer). */
	@Inject(method = "addDebugInfo", at = @At("TAIL"))
	private void jone500$debugInfo(List<String> result, BlockPos feetPos, Climate.Sampler sampler, CallbackInfo ci) {
		RegionBiomeLookup lookup = jone500$lookup;
		if (lookup != null) {
			BiomeRegion region = lookup.getRegionAt(QuartPos.fromBlock(feetPos.getX()), QuartPos.fromBlock(feetPos.getZ()));
			result.add("Biome API region: " + (region == null ? "vanilla" : region.getId()));
		}
	}
}
