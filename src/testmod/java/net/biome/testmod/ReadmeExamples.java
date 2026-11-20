package net.biome.testmod;

import net.biome.api.biome.BiomeMaker;
import net.biome.api.climate.ClimatePoints;
import net.biome.api.climate.Continentalness;
import net.biome.api.climate.Depth;
import net.biome.api.climate.Erosion;
import net.biome.api.climate.Humidity;
import net.biome.api.climate.Temperature;
import net.biome.api.climate.Terrain;
import net.biome.api.region.BiomeRegion;
import net.biome.api.region.BiomeRegions;
import net.biome.api.region.BiomeReplacements;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.NetherPlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;

/** Compile check for the README examples (not used at runtime). */
@SuppressWarnings("unused")
final class ReadmeExamples {
	static final ResourceKey<Biome> CLAY_FOREST = key("clay_forest");
	static final ResourceKey<Biome> DIAMOND_CAVES = key("diamond_caves");
	static final ResourceKey<Biome> FROSTY_WOODS = key("frosty_woods");
	static final ResourceKey<Biome> SLIME_MARSH = key("slime_marsh");
	static final ResourceKey<Biome> DUNES = key("dunes");
	static final ResourceKey<Biome> EMBER_FOREST = key("ember_forest");
	static final ResourceKey<Biome> APPLE_GROVE = key("apple_grove");
	static final ResourceKey<Biome> GOLDEN_ORCHARD = key("golden_orchard");
	static final ResourceKey<Biome> CRYSTAL_CAVES = key("crystal_caves");
	static final ResourceKey<Biome> HIGHLAND_MEADOW = key("highland_meadow");
	static final ResourceKey<Biome> APPLE_FOREST = key("apple_forest");

	private static ResourceKey<Biome> key(String name) {
		return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("mymod", name));
	}

	static void biomes(BootstrapContext<Biome> context) {
		// A: vanilla helpers + your own features
		context.register(CLAY_FOREST, BiomeMaker.overworld(context)
				.temperature(0.7F)
				.downfall(0.8F)
				.farmAnimals()
				.defaultMonsters()
				.features(BiomeDefaultFeatures::addForestFlowers)
				.features(BiomeDefaultFeatures::addOtherBirchTrees)
				.features(BiomeDefaultFeatures::addDefaultFlowers)
				.features(BiomeDefaultFeatures::addForestGrass)
				.features(BiomeDefaultFeatures::addDefaultMushrooms)
				.emeralds()
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_PUMPKIN)
				.build());

		// B: pick only some underground parts with empty()
		context.register(DIAMOND_CAVES, BiomeMaker.empty(context)
				.temperature(0.8F)
				.downfall(0.4F)
				.vanillaCaves()
				.vanillaStoneVeins()
				.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, CavePlacements.AMETHYST_GEODE)
				.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_IRON_UPPER)
				.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_IRON_MIDDLE)
				.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIAMOND)
				.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIAMOND_LARGE)
				.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIAMOND_BURIED)
				.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_CLAY)
				.caveCreatures()
				.build());

		// C: mobs: vanilla helpers + your own
		context.register(SLIME_MARSH, BiomeMaker.overworld(context)
				.temperature(0.8F)
				.downfall(0.9F)
				.spawns(BiomeDefaultFeatures::farmAnimals)
				.spawns(BiomeDefaultFeatures::commonSpawns)
				.spawn(EntityType.SLIME, 100, 1, 4)
				.spawn(MobCategory.CREATURE, EntityType.FROG, 10, 2, 5)
				.spawn(MobCategory.WATER_AMBIENT, EntityType.TROPICAL_FISH, 25, 8, 8)
				.animalSpawnChance(0.07F)
				.features(BiomeDefaultFeatures::addSwampVegetation)
				.build());

		// D: dry biome
		context.register(DUNES, BiomeMaker.overworld(context)
				.temperature(2.0F)
				.downfall(0.0F)
				.noRain()
				.grassColor(0xBFB755)
				.foliageColor(0xAEA42A)
				.music(SoundEvents.MUSIC_BIOME_DESERT)
				.spawns(BiomeDefaultFeatures::desertSpawns)
				.features(BiomeDefaultFeatures::addDesertVegetation)
				.features(BiomeDefaultFeatures::addDesertExtraDecoration)
				.particles(ParticleTypes.WHITE_ASH, 0.01F)
				.build());

		// E: nether
		context.register(EMBER_FOREST, BiomeMaker.nether(context)
				.fogColor(0x6B1E00)
				.particles(ParticleTypes.CRIMSON_SPORE, 0.025F)
				.music(SoundEvents.MUSIC_BIOME_CRIMSON_FOREST)
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NetherPlacements.CRIMSON_FOREST_VEGETATION)
				.spawn(EntityType.STRIDER, 60, 1, 2)
				.build());
	}

	static void regions() {
		// names + numbers in one ClimatePoints
		BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "highlands"))
				.overworld()
				.weight(1.5F)
				.addBiome(ClimatePoints.builder()
						.temperature(Temperature.COLD, Temperature.NEUTRAL)
						.humidity(-0.35F, 0.3F)
						.continentalness(Continentalness.between(Continentalness.MID_INLAND, Continentalness.FAR_INLAND))
						.erosion(Erosion.EROSION_1, Erosion.EROSION_2)
						.terrain(Terrain.HIGH)
						.strict(), HIGHLAND_MEADOW)
				.register();

		// replace + own climate + chances
		BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "orchards"))
				.weight(3)
				.replace(Biomes.PLAINS, APPLE_GROVE, 0.6F)
				.replace(Biomes.SUNFLOWER_PLAINS, APPLE_GROVE)
				.addBiome(ClimatePoints.builder()
						.temperature(Temperature.WARM)
						.humidity(Humidity.NEUTRAL)
						.continentalness(Continentalness.INLAND)
						.offset(0.2F), GOLDEN_ORCHARD, 0.5F)
				.exclusive(APPLE_GROVE, GOLDEN_ORCHARD)
				.register();

		// rare cave region
		BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "crystals"))
				.weight(0.25F)
				.addBiome(ClimatePoints.builder()
						.humidity(Humidity.WET, Humidity.HUMID)
						.depth(Depth.UNDERGROUND), CRYSTAL_CAVES)
				.register();
	}

	static void endRegion() {
		BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "ashlands"))
				.end()
				.weight(5)
				.replace(Biomes.END_BARRENS, DUNES)
				.replace(Biomes.END_MIDLANDS, CLAY_FOREST, 0.5F)
				.replace(Biomes.END_HIGHLANDS, SLIME_MARSH)
				.exclusive(CLAY_FOREST, SLIME_MARSH)
				.register();
	}

	static void surfaceRules() {
		net.biome.api.surface.BiomeSurfaceRules.addEnd(Identifier.fromNamespaceAndPath("mymod", "ash_floor"),
				net.minecraft.world.level.levelgen.SurfaceRules.ifTrue(net.minecraft.world.level.levelgen.SurfaceRules.isBiome(DUNES),
						net.minecraft.world.level.levelgen.SurfaceRules.ifTrue(net.minecraft.world.level.levelgen.SurfaceRules.ON_FLOOR,
								net.minecraft.world.level.levelgen.SurfaceRules.state(net.minecraft.world.level.block.Blocks.SMOOTH_BASALT.defaultBlockState()))));
	}

	static void weighted() {
		BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "wastes"))
				.nether()
				.weight(4)
				.replace(Biomes.NETHER_WASTES, net.biome.api.region.WeightedBiomes.builder()
						.add(EMBER_FOREST, 5)
						.add(DUNES, 3)
						.add(SLIME_MARSH, 1)
						.build())
				.replace(Biomes.SOUL_SAND_VALLEY, net.biome.api.region.WeightedBiomes.builder()
						.add(CLAY_FOREST, 1)
						.keepOriginal(2)
						.size(6)
						.build())
				.register();
		BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "crystal_lands"))
				.end()
				.replace(Biomes.END_HIGHLANDS, net.biome.api.region.WeightedBiomes.builder()
						.add(CRYSTAL_CAVES, 3)
						.add(DIAMOND_CAVES, 2)
						.add(APPLE_GROVE, 1)
						.build())
				.register();
		BiomeReplacements.replace(Biomes.NETHER_WASTES, net.biome.api.region.WeightedBiomes.builder()
				.add(EMBER_FOREST, 2)
				.add(DUNES, 1)
				.keepOriginal(3)
				.build());
	}

	static void replacements() {
		BiomeReplacements.replace(Biomes.FOREST, APPLE_FOREST, 0.25F);
		BiomeReplacements.replace(Biomes.FLOWER_FOREST, APPLE_FOREST, 0.1F);
		BiomeReplacements.replaceWholeBiome(Biomes.LUSH_CAVES, CRYSTAL_CAVES, 0.3F);

		BiomeReplacements.replace(APPLE_GROVE, GOLDEN_ORCHARD, 0.2F);
		BiomeReplacements.replaceWholeBiome(APPLE_GROVE, HIGHLAND_MEADOW, 0.5F);

		BiomeReplacements.replace(Biomes.DESERT, Biomes.SAVANNA, 0.3F);
		BiomeReplacements.replace(Biomes.DESERT, Biomes.BADLANDS, 0.1F);
		BiomeReplacements.replace(Biomes.FOREST, Biomes.BIRCH_FOREST, 0.2F);
		BiomeReplacements.replace(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, 0.3F);
		BiomeReplacements.replaceWholeBiome(Biomes.MUSHROOM_FIELDS, Biomes.DEEP_OCEAN, 1.0F);
		BiomeReplacements.replaceWholeBiome(Biomes.CHERRY_GROVE, Biomes.MEADOW, 0.5F);

		BiomeRegions.setVanillaWeight(6.0F);
		BiomeRegions.setRegionSize(8);
		BiomeReplacements.setPatchSize(5);
	}

	private ReadmeExamples() {
	}
}
