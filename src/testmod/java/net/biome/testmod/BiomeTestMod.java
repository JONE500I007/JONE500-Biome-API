package net.biome.testmod;

import net.biome.api.JONE500BiomeAPI;
import net.biome.api.climate.ClimatePoints;
import net.biome.api.climate.Continentalness;
import net.biome.api.climate.Erosion;
import net.biome.api.climate.Temperature;
import net.biome.api.climate.Terrain;
import net.biome.api.region.BiomeRegion;
import net.biome.api.region.BiomeReplacements;
import net.biome.api.surface.BiomeSurfaceRules;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.TheEndBiomes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Example: two regions with four recolored copies of vanilla biomes.
 *
 * <ul>
 *   <li>{@code biome_test:warm} - plains become red plains everywhere, forests become blue forests in half
 *       of the patches.</li>
 *   <li>{@code biome_test:meadows} - meadows become mint meadows and cherry groves become amber meadows, but
 *       never both in the same patch.</li>
 * </ul>
 * Red plains also get red sand on the ground (surface rule).
 */
public class BiomeTestMod implements ModInitializer {
	public static final String MOD_ID = "biome_test";

	public static final ResourceKey<Biome> RED_PLAINS = key("red_plains");
	public static final ResourceKey<Biome> BLUE_FOREST = key("blue_forest");
	public static final ResourceKey<Biome> MINT_MEADOW = key("mint_meadow");
	public static final ResourceKey<Biome> AMBER_MEADOW = key("amber_meadow");
	/** Made with BiomeMaker (data generation), placed with ClimatePoints. */
	public static final ResourceKey<Biome> ORANGE_HILLS = key("orange_hills");
	/** Nether biome (BiomeMaker.nether, with particles). */
	public static final ResourceKey<Biome> EMBER_FOREST = key("ember_forest");
	/** End biome (BiomeMaker.end, with particles). */
	public static final ResourceKey<Biome> VOID_MEADOW = key("void_meadow");
	/** Compatibility tests: placed with TerraBlender / Biolith when those mods are installed. */
	public static final ResourceKey<Biome> TB_MEADOW = key("tb_meadow");
	public static final ResourceKey<Biome> BIOLITH_MEADOW = key("biolith_meadow");

	public static final Identifier WARM = id("warm");
	public static final Identifier MEADOWS = id("meadows");
	public static final Identifier EMBERS = id("embers");

	@Override
	public void onInitialize() {
		JONE500BiomeAPI.LOGGER.info("[compat-test] biome_test.compat = '{}'", System.getProperty("biome_test.compat", ""));
		// Registered in "wrong" alphabetical order on purpose: the API must sort them by id.
		BiomeRegion.builder(MEADOWS)
				.weight(2)
				.replace(Biomes.MEADOW, MINT_MEADOW)
				.replace(Biomes.CHERRY_GROVE, AMBER_MEADOW)
				.exclusive(MINT_MEADOW, AMBER_MEADOW)
				.register();

		BiomeRegion.builder(WARM)
				.weight(3)
				.replace(Biomes.PLAINS, RED_PLAINS)
				.replace(Biomes.FOREST, BLUE_FOREST, 0.5F)
				// A biome with its own climate (made with BiomeMaker in data generation).
				.addBiome(ClimatePoints.builder()
						.temperature(Temperature.NEUTRAL, Temperature.WARM)
						.continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
						.erosion(Erosion.EROSION_2, Erosion.EROSION_3, Erosion.EROSION_4)
						.terrain(Terrain.MID, Terrain.HIGH), ORANGE_HILLS)
				.register();

		// World-wide: 40% of birch forest areas become blue forest (also outside the regions).
		BiomeReplacements.replace(Biomes.BIRCH_FOREST, BLUE_FOREST, 0.4F);

		// All or nothing per world: in half of all worlds every dark forest is a blue forest.
		BiomeReplacements.replaceWholeBiome(Biomes.DARK_FOREST, BLUE_FOREST, 0.5F);
		// Every world: lush caves (a cave biome) become mint meadow.
		BiomeReplacements.replaceWholeBiome(Biomes.LUSH_CAVES, MINT_MEADOW, 1.0F);

		// ---- Nether: a region with its own patches ----
		BiomeRegion.builder(EMBERS)
				.nether()
				.weight(5)
				.replace(Biomes.CRIMSON_FOREST, EMBER_FOREST)
				.addBiome(ClimatePoints.netherPoint(0.25F, -0.3F), EMBER_FOREST)
				.register();
		BiomeSurfaceRules.addNether(id("ember_floor"),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(EMBER_FOREST),
						SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.MAGMA_BLOCK.defaultBlockState()))));

		// ---- End: placed with Fabric API, plus a whole-biome swap from this API ----
		TheEndBiomes.addHighlandsBiome(VOID_MEADOW, 10.0);
		BiomeReplacements.replaceWholeBiome(Biomes.SMALL_END_ISLANDS, VOID_MEADOW, 1.0F);

		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("biolith")) {
			net.biome.testmod.compat.BiolithCompat.register();
		}

		BiomeSurfaceRules.addOverworld(id("red_plains_sand"),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(RED_PLAINS),
						SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
								SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0),
										SurfaceRules.state(Blocks.RED_SAND.defaultBlockState())))));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static ResourceKey<Biome> key(String path) {
		return ResourceKey.create(Registries.BIOME, id(path));
	}
}
