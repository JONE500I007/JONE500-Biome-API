package net.biome.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.biome.api.impl.BiomeApiWorldSetup;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;

/**
 * Entry point of JONE500 Biome API.
 *
 * <p>Modders don't call anything here. Use {@link net.biome.api.region.BiomeRegion} to add biomes and
 * {@link net.biome.api.surface.BiomeSurfaceRules} for their ground blocks.
 */
public class JONE500BiomeAPI implements ModInitializer {
	public static final String MOD_ID = "jone500-biome-api";
	public static final Logger LOGGER = LoggerFactory.getLogger("Biome API");

	@Override
	public void onInitialize() {
		// Runs before the server creates its levels, so the Overworld can be set up in time.
		ServerLifecycleEvents.SERVER_STARTING.register(BiomeApiWorldSetup::onServerStarting);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
