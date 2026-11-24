package net.biome.tutorial;

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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.biome.Biome;

/**
 * <h2>Lesson 7: making biomes in Java (data generation)</h2>
 *
 * Writing biome JSON by hand is long and easy to get wrong. {@link BiomeMaker} builds the biome in Java, with
 * vanilla's defaults already in: {@code BiomeMaker.overworld(context)} has the normal ores, caves, springs and
 * so on; {@code .nether(context)} and {@code .end(context)} start like vanilla's Nether and End biomes. Then you
 * set colors, music, mobs ({@code spawn}, {@code farmAnimals}, {@code defaultMonsters}) and features.
 *
 * <p><b>How it fits together:</b>
 * <ol>
 *   <li>{@code buildRegistry} adds the biomes to a registry that only exists during data generation
 *       ({@code bootstrapBiomes} below).</li>
 *   <li>A {@code FabricDynamicRegistryProvider} writes them as JSON files.</li>
 *   <li>A tag provider adds them to vanilla's biome tags with {@link VanillaBiomeTags}.</li>
 * </ol>
 * Run the Gradle task {@code runDatagen}; the files go to {@code src/tutorial/generated/}. The entrypoint is
 * registered in {@code fabric.mod.json} under {@code "fabric-datagen"}.
 *
 * <p><b>Features:</b> prefer vanilla's helpers ({@code .features(BiomeDefaultFeatures::addPlainGrass)}): they add
 * features in vanilla's order. Two biomes with the same features in a <b>different order</b> crash the game with
 * "Feature order cycle", and adding a feature that the defaults already have places it twice.
 *
 * <p><b>Tags:</b> structures (villages, mineshafts, strongholds), mob variants (warm cows, spotted wolves) and
 * {@code is_overworld} / {@code is_nether} / {@code is_end} come from vanilla biome tags, not from the biome.
 * {@code VanillaBiomeTags.copy(vanillaBiome, yourBiome, ...)} puts your biome in the same tags as a vanilla
 * biome, and lets you leave some out (here: orange hills are like a savanna, but without villages).
 */
public class TutorialDataGen implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) -> new FabricDynamicRegistryProvider(output, registries) {
			@Override
			protected void configure(HolderLookup.Provider lookup, Entries entries) {
				entries.add(lookup.lookupOrThrow(Registries.BIOME), TutorialBiomes.ORANGE_HILLS);
				entries.add(lookup.lookupOrThrow(Registries.BIOME), TutorialBiomes.EMBER_FOREST);
				entries.add(lookup.lookupOrThrow(Registries.BIOME), TutorialBiomes.SULFUR_WASTES);
				entries.add(lookup.lookupOrThrow(Registries.BIOME), TutorialBiomes.BONE_FIELDS);
				entries.add(lookup.lookupOrThrow(Registries.BIOME), TutorialBiomes.VOID_MEADOW);
				entries.add(lookup.lookupOrThrow(Registries.BIOME), TutorialBiomes.ASH_BARRENS);
			}

			@Override
			public String getName() {
				return "Biome Tutorial Biomes";
			}
		});

		// Vanilla biome tags: structures (villages, mineshafts, strongholds...) and mob variants.
		pack.addProvider((FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) -> new FabricTagsProvider<Biome>(output, Registries.BIOME, registries) {
			@Override
			protected void addTags(HolderLookup.Provider lookup) {
				// The 4 hand-written biomes are not in the datagen registry, so they must be "optional" entries.
				VanillaBiomeTags.copy(Biomes.PLAINS, TutorialBiomes.RED_PLAINS, (tag, biome) -> builder(tag).addOptional(biome));
				VanillaBiomeTags.copy(Biomes.FOREST, TutorialBiomes.BLUE_FOREST, (tag, biome) -> builder(tag).addOptional(biome));
				VanillaBiomeTags.copy(Biomes.MEADOW, TutorialBiomes.MINT_MEADOW, (tag, biome) -> builder(tag).addOptional(biome));
				VanillaBiomeTags.copy(Biomes.CHERRY_GROVE, TutorialBiomes.AMBER_MEADOW, (tag, biome) -> builder(tag).addOptional(biome));
				// Orange hills: like a savanna (warm cows/pigs/chickens, spotted wolves), but no villages.
				VanillaBiomeTags.copy(Biomes.SAVANNA, TutorialBiomes.ORANGE_HILLS, VanillaBiomeTags.NO_VILLAGES, (tag, biome) -> builder(tag).add(biome));
				// Nether / End biomes: fortresses, bastions, is_nether... / end cities, is_end
				VanillaBiomeTags.copy(Biomes.CRIMSON_FOREST, TutorialBiomes.EMBER_FOREST, (tag, biome) -> builder(tag).add(biome));
				VanillaBiomeTags.copy(Biomes.NETHER_WASTES, TutorialBiomes.SULFUR_WASTES, (tag, biome) -> builder(tag).add(biome));
				VanillaBiomeTags.copy(Biomes.NETHER_WASTES, TutorialBiomes.BONE_FIELDS, (tag, biome) -> builder(tag).add(biome));
				VanillaBiomeTags.copy(Biomes.END_HIGHLANDS, TutorialBiomes.VOID_MEADOW, (tag, biome) -> builder(tag).add(biome));
				VanillaBiomeTags.copy(Biomes.END_BARRENS, TutorialBiomes.ASH_BARRENS, (tag, biome) -> builder(tag).add(biome));
			}

			@Override
			public String getName() {
				return "Biome Tutorial Biome Tags";
			}
		});
	}

	@Override
	public void buildRegistry(RegistrySetBuilder builder) {
		builder.add(Registries.BIOME, TutorialDataGen::bootstrapBiomes);
	}

	private static void bootstrapBiomes(BootstrapContext<Biome> context) {
		context.register(TutorialBiomes.ORANGE_HILLS, BiomeMaker.overworld(context)
				.temperature(0.7F)
				.downfall(0.5F)
				.waterColor(0xE07A1F)
				.grassColor(0xF0A040)
				.foliageColor(0xE08A20)
				.skyColor(0xFFC080)
				.music(SoundEvents.MUSIC_BIOME_MEADOW)
				.farmAnimals()
				.defaultMonsters()
				.spawn(EntityTypes.FOX, 8, 2, 4)
				// Vanilla's own helpers keep vanilla's feature order. Adding the same features in a different
				// order than another biome (e.g. flowers before grass) causes a "Feature order cycle" crash.
				.features(BiomeDefaultFeatures::addPlainGrass)
				.features(BiomeDefaultFeatures::addMeadowVegetation)
				.build());

		// Nether biome with floating particles and its own loop sound.
		context.register(TutorialBiomes.EMBER_FOREST, BiomeMaker.nether(context)
				.fogColor(0x8A2A10)
				.waterFogColor(0x400A05)
				.music(SoundEvents.MUSIC_BIOME_CRIMSON_FOREST)
				.ambientLoop(SoundEvents.AMBIENT_CRIMSON_FOREST_LOOP)
				.particles(ParticleTypes.LAVA, 0.002F)
				.particles(ParticleTypes.WHITE_ASH, 0.05F)
				.spawn(EntityTypes.STRIDER, 60, 1, 2)
				.spawn(EntityTypes.MAGMA_CUBE, 20, 2, 4)
				.spawn(EntityTypes.ZOMBIFIED_PIGLIN, 10, 2, 4)
				.build());

		// Two simple Nether biomes for the weighted region test.
		context.register(TutorialBiomes.SULFUR_WASTES, BiomeMaker.nether(context)
				.fogColor(0x9A8A10)
				.particles(ParticleTypes.WHITE_ASH, 0.02F)
				.build());
		context.register(TutorialBiomes.BONE_FIELDS, BiomeMaker.nether(context)
				.fogColor(0xC8C0A8)
				.build());

		// End biome with chorus plants and portal particles.
		context.register(TutorialBiomes.VOID_MEADOW, BiomeMaker.end(context)
				.fogColor(0x2A0A3A)
				.particles(ParticleTypes.PORTAL, 0.02F)
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, EndPlacements.CHORUS_PLANT)
				.build());

		// End biome for the End region test: grey fog and ash.
		context.register(TutorialBiomes.ASH_BARRENS, BiomeMaker.end(context)
				.fogColor(0x3A3A3A)
				.particles(ParticleTypes.WHITE_ASH, 0.03F)
				.build());
	}
}
