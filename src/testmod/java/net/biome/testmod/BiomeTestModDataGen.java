package net.biome.testmod;

import java.util.concurrent.CompletableFuture;

import net.biome.api.biome.BiomeMaker;
import net.biome.api.biome.VanillaBiomeTags;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.world.level.biome.Biomes;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.worldgen.placement.EndPlacements;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;

/** Generates the orange hills biome JSON with {@link BiomeMaker}. Run with: gradlew runDatagen */
public class BiomeTestModDataGen implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) -> new FabricDynamicRegistryProvider(output, registries) {
			@Override
			protected void configure(HolderLookup.Provider lookup, Entries entries) {
				entries.add(lookup.lookupOrThrow(Registries.BIOME), BiomeTestMod.ORANGE_HILLS);
				entries.add(lookup.lookupOrThrow(Registries.BIOME), BiomeTestMod.EMBER_FOREST);
				entries.add(lookup.lookupOrThrow(Registries.BIOME), BiomeTestMod.VOID_MEADOW);
			}

			@Override
			public String getName() {
				return "Biome Test Biomes";
			}
		});

		// Vanilla biome tags: structures (villages, mineshafts, strongholds...) and mob variants.
		pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) -> new FabricTagsProvider<Biome>(output, Registries.BIOME, registries) {
			@Override
			protected void addTags(HolderLookup.Provider lookup) {
				// These 4 biomes are hand-written JSON (not made in datagen), so they are added as "optional" entries.
				VanillaBiomeTags.copy(Biomes.PLAINS, BiomeTestMod.RED_PLAINS, (tag, biome) -> builder(tag).addOptional(biome));
				VanillaBiomeTags.copy(Biomes.FOREST, BiomeTestMod.BLUE_FOREST, (tag, biome) -> builder(tag).addOptional(biome));
				VanillaBiomeTags.copy(Biomes.MEADOW, BiomeTestMod.MINT_MEADOW, (tag, biome) -> builder(tag).addOptional(biome));
				VanillaBiomeTags.copy(Biomes.CHERRY_GROVE, BiomeTestMod.AMBER_MEADOW, (tag, biome) -> builder(tag).addOptional(biome));
				// Orange hills: like a savanna (warm cows/pigs/chickens, spotted wolves), but no villages.
				VanillaBiomeTags.copy(Biomes.SAVANNA, BiomeTestMod.ORANGE_HILLS, VanillaBiomeTags.NO_VILLAGES, (tag, biome) -> builder(tag).add(biome));
				// Nether / End biomes: fortresses, bastions, is_nether... / end cities, is_end
				VanillaBiomeTags.copy(Biomes.CRIMSON_FOREST, BiomeTestMod.EMBER_FOREST, (tag, biome) -> builder(tag).add(biome));
				VanillaBiomeTags.copy(Biomes.END_HIGHLANDS, BiomeTestMod.VOID_MEADOW, (tag, biome) -> builder(tag).add(biome));
			}

			@Override
			public String getName() {
				return "Biome Test Biome Tags";
			}
		});
	}

	@Override
	public void buildRegistry(RegistrySetBuilder builder) {
		builder.add(Registries.BIOME, BiomeTestModDataGen::bootstrapBiomes);
	}

	private static void bootstrapBiomes(BootstrapContext<Biome> context) {
		context.register(BiomeTestMod.ORANGE_HILLS, BiomeMaker.overworld(context)
				.temperature(0.7F)
				.downfall(0.5F)
				.waterColor(0xE07A1F)
				.grassColor(0xF0A040)
				.foliageColor(0xE08A20)
				.skyColor(0xFFC080)
				.music(SoundEvents.MUSIC_BIOME_MEADOW)
				.farmAnimals()
				.defaultMonsters()
				.spawn(EntityType.FOX, 8, 2, 4)
				// Vanilla's own helpers keep vanilla's feature order. Adding the same features in a different
				// order than another biome (e.g. flowers before grass) causes a "Feature order cycle" crash.
				.features(BiomeDefaultFeatures::addPlainGrass)
				.features(BiomeDefaultFeatures::addMeadowVegetation)
				.build());

		// Nether biome with floating particles and its own loop sound.
		context.register(BiomeTestMod.EMBER_FOREST, BiomeMaker.nether(context)
				.fogColor(0x8A2A10)
				.waterFogColor(0x400A05)
				.music(SoundEvents.MUSIC_BIOME_CRIMSON_FOREST)
				.ambientLoop(SoundEvents.AMBIENT_CRIMSON_FOREST_LOOP)
				.particles(ParticleTypes.LAVA, 0.002F)
				.particles(ParticleTypes.WHITE_ASH, 0.05F)
				.spawn(EntityType.STRIDER, 60, 1, 2)
				.spawn(EntityType.MAGMA_CUBE, 20, 2, 4)
				.spawn(EntityType.ZOMBIFIED_PIGLIN, 10, 2, 4)
				.build());

		// End biome with chorus plants and portal particles.
		context.register(BiomeTestMod.VOID_MEADOW, BiomeMaker.end(context)
				.fogColor(0x2A0A3A)
				.particles(ParticleTypes.PORTAL, 0.02F)
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, EndPlacements.CHORUS_PLANT)
				.build());
	}
}
