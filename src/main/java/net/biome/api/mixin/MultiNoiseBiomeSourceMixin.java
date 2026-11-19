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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.biome.api.impl.BiomeApiBiomeSource;
import net.biome.api.impl.RegionBiomeLookup;
import net.biome.api.region.BiomeRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
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

	/** Every biome lookup of this source (chunk generation, /locate, F3) goes through here. */
	@Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
			at = @At("HEAD"), cancellable = true)
	private void jone500$regionBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
		RegionBiomeLookup lookup = jone500$lookup;
		if (lookup != null) {
			cir.setReturnValue(lookup.getBiome(sampler.sample(quartX, quartY, quartZ), quartX, quartZ, parameters()));
		}
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
