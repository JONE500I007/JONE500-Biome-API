package net.biome.testmod.compat;

import com.terraformersmc.biolith.api.biome.BiomePlacement;

import net.biome.testmod.BiomeTestMod;
import net.minecraft.world.level.biome.Climate;

/** Compatibility test: a biome placed with Biolith. Only called when Biolith is installed. */
public final class BiolithCompat {
	private BiolithCompat() {
	}

	public static void register() {
		// Warm, dry, inland, flat-ish land.
		BiomePlacement.addOverworld(BiomeTestMod.BIOLITH_MEADOW, Climate.parameters(
				Climate.Parameter.span(0.2F, 0.55F), Climate.Parameter.span(-1.0F, -0.35F),
				Climate.Parameter.span(0.03F, 0.55F), Climate.Parameter.span(-0.2F, 0.45F),
				Climate.Parameter.point(0.0F), Climate.Parameter.span(-0.4F, 0.4F), 0L));
	}
}
