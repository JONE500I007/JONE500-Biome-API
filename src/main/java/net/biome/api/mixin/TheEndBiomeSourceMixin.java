package net.biome.api.mixin;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.biome.api.impl.BiomeApiBiomeSource;
import net.biome.api.impl.RegionBiomeLookup;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;

/**
 * The End has no regions; only BiomeReplacements (area and whole-biome) are applied to the vanilla biome.
 * Fabric API's TheEndBiomes (modded End biomes with weights) still works for every biome we don't replace.
 */
@Mixin(value = TheEndBiomeSource.class, priority = 900)
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
	 * Fabric API's TheEndBiomes also changes the result at RETURN and returns early, so this runs first
	 * (lower priority): if our replacement applies, it returns right away; otherwise Fabric gets its turn.
	 */
	@Inject(method = "getNoiseBiome", at = @At("RETURN"), cancellable = true)
	private void jone500$replace(int quartX, int quartY, int quartZ, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
		RegionBiomeLookup lookup = jone500$lookup;
		if (lookup != null) {
			Holder<Biome> original = cir.getReturnValue();
			Holder<Biome> replaced = lookup.applyGlobalReplacements(original, quartX, quartZ);
			if (replaced != original) {
				cir.setReturnValue(replaced);
			}
		}
	}

	@ModifyReturnValue(method = "collectPossibleBiomes", at = @At("RETURN"))
	private Stream<Holder<Biome>> jone500$addBiomes(Stream<Holder<Biome>> original) {
		RegionBiomeLookup lookup = jone500$lookup;
		return lookup == null ? original : Stream.concat(original, lookup.getAddedBiomes().stream());
	}
}
