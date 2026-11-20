package net.biome.api.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.biome.api.impl.BiomeApiBiomeSource;
import net.biome.api.impl.RegionBiomeLookup;
import net.biome.api.region.BiomeRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;

/**
 * Shows the End region on the F3 screen (singleplayer). The End biome source has no debug info of its own, so
 * this hooks the base class. (The Overworld and the Nether: see {@link MultiNoiseBiomeSourceMixin}.)
 */
@Mixin(BiomeSource.class)
public abstract class BiomeSourceDebugMixin {
	@Inject(method = "addDebugInfo", at = @At("TAIL"))
	private void jone500$endDebugInfo(List<String> result, BlockPos feetPos, Climate.Sampler sampler, CallbackInfo ci) {
		if ((Object) this instanceof TheEndBiomeSource && this instanceof BiomeApiBiomeSource api) {
			RegionBiomeLookup lookup = api.jone500$getLookup();
			if (lookup != null) {
				BiomeRegion region = lookup.getRegionAt(QuartPos.fromBlock(feetPos.getX()), QuartPos.fromBlock(feetPos.getZ()));
				result.add("Biome API region: " + (region == null ? "vanilla" : region.getId()));
			}
		}
	}
}
