package net.biome.tutorial.test;

import net.biome.tutorial.*;

import net.biome.api.JONE500BiomeAPI;
import net.biome.tutorial.test.compat.BiolithCompat;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Test-only setup (not part of the tutorial): runs after {@link net.biome.tutorial.TutorialMod}. Logs which other
 * mods the compatibility tests loaded, and places a biome with Biolith when Biolith is installed.
 */
public class TestSetup implements ModInitializer {
	@Override
	public void onInitialize() {
		JONE500BiomeAPI.LOGGER.info("[compat-test] biome_tutorial.compat = '{}'", System.getProperty("biome_tutorial.compat", ""));
		if (FabricLoader.getInstance().isModLoaded("biolith")) {
			BiolithCompat.register();
		}
	}
}
