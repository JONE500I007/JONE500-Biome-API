package net.biome.api.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;

import net.biome.api.JONE500BiomeAPI;
import net.biome.api.mixin.BiomeSourceAccessor;
import net.biome.api.mixin.ChunkGeneratorAccessor;
import net.biome.api.region.BiomeRegion;
import net.biome.api.region.BiomeRegions;
import net.biome.api.region.BiomeReplacements;
import net.biome.api.region.RegionDimension;
import net.biome.api.surface.BiomeSurfaceRules;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Applies regions, replacements and surface rules to the Overworld, the Nether and the End when a server
 * starts (before any level is created).
 *
 * <p>Internal class, not part of the API.
 */
public final class BiomeApiWorldSetup {
	private BiomeApiWorldSetup() {
	}

	public static void onServerStarting(MinecraftServer server) {
		BiomeRegions.freeze();
		BiomeReplacements.freeze();

		RegistryAccess registries = server.registryAccess();
		Registry<LevelStem> stems = registries.lookupOrThrow(Registries.LEVEL_STEM);
		long seed = server.getWorldGenSettings().options().seed();

		setUpMultiNoise(registries, stems, RegionDimension.OVERWORLD, seed, BiomeSurfaceRules.getOverworld());
		setUpMultiNoise(registries, stems, RegionDimension.NETHER, seed, BiomeSurfaceRules.getNether());
		setUpEnd(registries, stems, seed);
	}

	/** The seed a dimension's region map uses (each dimension differs, so the Nether doesn't mirror the Overworld). */
	public static long dimensionSeed(long worldSeed, RegionDimension dimension) {
		return dimension == RegionDimension.OVERWORLD ? worldSeed : worldSeed ^ RegionLayout.salt(dimension.levelStem().identifier().toString());
	}

	private static void setUpMultiNoise(RegistryAccess registries, Registry<LevelStem> stems, RegionDimension dimension, long seed,
			List<SurfaceRules.RuleSource> rules) {
		LevelStem stem = stems.getValue(dimension.levelStem());
		if (stem == null) {
			return;
		}
		ChunkGenerator generator = stem.generator();
		String name = dimension.levelStem().identifier().toString();
		if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)
				|| !(generator.getBiomeSource() instanceof MultiNoiseBiomeSource biomeSource)) {
			JONE500BiomeAPI.LOGGER.info("{} does not use multi-noise biomes ({}), regions are not applied",
					name, generator.getBiomeSource().getClass().getSimpleName());
			return;
		}

		BiomeApiBiomeSource source = (BiomeApiBiomeSource) biomeSource;
		if (source.jone500$getLookup() != null) {
			return; // already set up for this world
		}

		List<BiomeRegion> regions = BiomeRegions.getAll(dimension);
		Set<ResourceKey<Biome>> dimensionBiomes = biomesOf(biomeSource);
		for (BiomeRegion region : regions) {
			region.getReplacements().forEach(r -> dimensionBiomes.add(r.biome()));
			region.getExtraPoints().forEach(p -> dimensionBiomes.add(p.biome()));
		}
		List<BiomeRegion.Replacement> replacements = onlyFor(BiomeReplacements.getAll(), dimensionBiomes);
		List<BiomeRegion.Replacement> wholeSwaps = onlyFor(BiomeReplacements.getAllWholeBiome(), dimensionBiomes);
		if (!regions.isEmpty() || !replacements.isEmpty() || !wholeSwaps.isEmpty()) {
			// Whole-biome swaps are decided with the world seed (same answer in every dimension).
			RegionBiomeLookup lookup = RegionBiomeLookup.create(dimensionSeed(seed, dimension), BiomeRegions.getRegionSize(),
					BiomeRegions.getVanillaWeight(), regions, replacements, BiomeReplacements.getPatchSize(), wholeSwaps, seed,
					source.jone500$getParameters(), registries.lookupOrThrow(Registries.BIOME));
			source.jone500$setLookup(lookup);

			// The biome list and feature order may already have been computed (world creation screen validates
			// the generator). Compute them again so they include the new biomes.
			refreshPossibleBiomes(biomeSource, generator);
			JONE500BiomeAPI.LOGGER.info("{}: {} region(s), {} area replacement(s), {} whole-biome replacement(s) active ({} biome(s) added)",
					name, regions.size(), replacements.size(), lookup.getWholeBiomeSwaps().size(), lookup.getAddedBiomes().size());
		}

		if (!rules.isEmpty()) {
			List<SurfaceRules.RuleSource> all = new ArrayList<>(rules);
			all.add(noiseGenerator.generatorSettings().value().surfaceRule());
			((BiomeApiChunkGenerator) (Object) noiseGenerator).jone500$setSurfaceRule(SurfaceRules.sequence(all.toArray(SurfaceRules.RuleSource[]::new)));
		}
	}

	/** The End has no regions; only world-wide replacements apply (biomes are placed with Fabric API's TheEndBiomes). */
	private static void setUpEnd(RegistryAccess registries, Registry<LevelStem> stems, long seed) {
		LevelStem stem = stems.getValue(LevelStem.END);
		if (stem == null || !(stem.generator().getBiomeSource() instanceof TheEndBiomeSource endSource)) {
			return;
		}
		Set<ResourceKey<Biome>> endBiomes = biomesOf(endSource);
		List<BiomeRegion.Replacement> replacements = onlyFor(BiomeReplacements.getAll(), endBiomes);
		List<BiomeRegion.Replacement> wholeSwaps = onlyFor(BiomeReplacements.getAllWholeBiome(), endBiomes);
		if (replacements.isEmpty() && wholeSwaps.isEmpty()) {
			return;
		}
		BiomeApiBiomeSource source = (BiomeApiBiomeSource) endSource;
		if (source.jone500$getLookup() != null) {
			return;
		}
		long endSeed = seed ^ RegionLayout.salt(LevelStem.END.identifier().toString());
		RegionBiomeLookup lookup = RegionBiomeLookup.create(endSeed, BiomeRegions.getRegionSize(), 0.0F, List.of(),
				replacements, BiomeReplacements.getPatchSize(), wholeSwaps, seed, null, registries.lookupOrThrow(Registries.BIOME));
		source.jone500$setLookup(lookup);
		refreshPossibleBiomes(endSource, stem.generator());
		JONE500BiomeAPI.LOGGER.info("minecraft:the_end: {} area replacement(s), {} whole-biome replacement(s) active",
				replacements.size(), lookup.getWholeBiomeSwaps().size());
	}

	/** The biomes a source can place right now (vanilla, datapacks, Fabric API), before this API adds any. */
	private static Set<ResourceKey<Biome>> biomesOf(BiomeSource source) {
		Set<ResourceKey<Biome>> keys = new HashSet<>();
		source.possibleBiomes().forEach(b -> b.unwrapKey().ifPresent(keys::add));
		return keys;
	}

	/** Only the replacements whose original biome exists in this dimension. */
	private static List<BiomeRegion.Replacement> onlyFor(List<BiomeRegion.Replacement> all, Set<ResourceKey<Biome>> dimensionBiomes) {
		return all.stream().filter(r -> dimensionBiomes.contains(r.original())).toList();
	}

	private static void refreshPossibleBiomes(BiomeSource biomeSource, ChunkGenerator generator) {
		BiomeSourceAccessor sourceAccess = (BiomeSourceAccessor) biomeSource;
		sourceAccess.jone500$setPossibleBiomes(Suppliers.memoize(
				() -> sourceAccess.jone500$collectPossibleBiomes().distinct().collect(ImmutableSet.toImmutableSet())));

		ChunkGeneratorAccessor generatorAccess = (ChunkGeneratorAccessor) generator;
		var settingsGetter = generatorAccess.jone500$getGenerationSettingsGetter();
		List<FeatureSorter.StepFeatureData> features;
		try {
			// Computed right away (like vanilla's validate()), so a problem stops the world from loading with a
			// clear message instead of crashing later while chunks generate.
			features = FeatureSorter.buildFeaturesPerStep(List.<Holder<Biome>>copyOf(biomeSource.possibleBiomes()),
					b -> settingsGetter.apply(b).features(), true);
		} catch (IllegalStateException e) {
			throw new IllegalStateException("Biome API: two biomes add the same features in a different order. "
					+ "Add features in the same order as vanilla (use vanilla's BiomeDefaultFeatures helpers). Details: "
					+ e.getMessage(), e);
		}
		generatorAccess.jone500$setFeaturesPerStep(() -> features);
	}
}
