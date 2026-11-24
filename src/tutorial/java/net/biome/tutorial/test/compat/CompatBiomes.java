package net.biome.tutorial.test.compat;

import net.biome.tutorial.*;

import net.biome.tutorial.TutorialBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/** Compatibility tests only: biomes placed with TerraBlender / Biolith when those mods are installed. */
public final class CompatBiomes {
	public static final ResourceKey<Biome> TB_MEADOW = TutorialBiomes.key("tb_meadow");
	public static final ResourceKey<Biome> BIOLITH_MEADOW = TutorialBiomes.key("biolith_meadow");

	private CompatBiomes() {
	}
}
