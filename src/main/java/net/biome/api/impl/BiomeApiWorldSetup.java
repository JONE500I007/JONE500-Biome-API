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
import net.biome.api.surface.SurfaceRuleContext;
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
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

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

		SurfaceRuleContext surfaceContext = new SurfaceRuleContext(registries.lookupOrThrow(Registries.BIOME),
				registries.lookupOrThrow(Registries.MATERIAL_CONDITION), registries.lookupOrThrow(Registries.MATERIAL_RULE));
		setUpMultiNoise(registries, stems, RegionDimension.OVERWORLD, seed, BiomeSurfaceRules.getOverworld(surfaceContext));
		setUpMultiNoise(registries, stems, RegionDimension.NETHER, seed, BiomeSurfaceRules.getNether(surfaceContext));
		setUpEnd(registries, stems, seed, BiomeSurfaceRules.getEnd(surfaceContext));
	}

	/** The seed a dimension's region map uses (each dimension differs, so the Nether doesn't mirror the Overworld). */
	public static long dimensionSeed(long worldSeed, RegionDimension dimension) {
		return dimension == RegionDimension.OVERWORLD ? worldSeed : worldSeed ^ RegionLayout.salt(dimension.levelStem().identifier().toString());
	}

	private static void setUpMultiNoise(RegistryAccess registries, Registry<LevelStem> stems, RegionDimension dimension, long seed,
			List<MaterialRule> rules) {
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
			dimensionBiomes.addAll(region.getAllBiomes());
		}
		List<BiomeRegion.Replacement> replacements = onlyFor(BiomeReplacements.getAll(), dimensionBiomes);
		List<BiomeRegion.Replacement> wholeSwaps = onlyFor(BiomeReplacements.getAllWholeBiome(), dimensionBiomes);
		List<BiomeRegion.WeightedReplacement> weighted = onlyForWeighted(BiomeReplacements.getAllWeighted(), dimensionBiomes);
		if (!regions.isEmpty() || !replacements.isEmpty() || !weighted.isEmpty() || !wholeSwaps.isEmpty()) {
			// Whole-biome swaps are decided with the world seed (same answer in every dimension).
			RegionBiomeLookup lookup = RegionBiomeLookup.create(dimensionSeed(seed, dimension), BiomeRegions.getRegionSize(),
					BiomeRegions.getVanillaWeight(), regions, replacements, weighted, BiomeReplacements.getPatchSize(), wholeSwaps, seed,
					source.jone500$getParameters(), registries.lookupOrThrow(Registries.BIOME));
			source.jone500$setLookup(lookup);

			// The biome list and feature order may already have been computed (world creation screen validates
			// the generator). Compute them again so they include the new biomes.
			refreshPossibleBiomes(biomeSource, generator);
			JONE500BiomeAPI.LOGGER.info("{}: {} region(s), {} area replacement(s), {} weighted replacement(s), {} whole-biome replacement(s) active ({} biome(s) added)",
					name, regions.size(), replacements.size(), weighted.size(), lookup.getWholeBiomeSwaps().size(), lookup.getAddedBiomes().size());
		}

		applySurfaceRules(noiseGenerator, rules);
	}

	/** Our rules first, then the dimension's own rule (vanilla or datapack). */
	private static void applySurfaceRules(NoiseBasedChunkGenerator noiseGenerator, List<MaterialRule> rules) {
		if (!rules.isEmpty()) {
			List<MaterialRule> all = new ArrayList<>(rules);
			all.add(noiseGenerator.generatorSettings().value().materialRule().value());
			((BiomeApiChunkGenerator) (Object) noiseGenerator).jone500$setSurfaceRule(MaterialRules.sequence(all));
		}
	}

	/**
	 * The End has no climate: End regions and world-wide replacements relabel the biome vanilla (or Fabric API's
	 * TheEndBiomes) picked.
	 */
	private static void setUpEnd(RegistryAccess registries, Registry<LevelStem> stems, long seed, List<MaterialRule> surfaceRules) {
		LevelStem stem = stems.getValue(LevelStem.END);
		if (stem != null && stem.generator() instanceof NoiseBasedChunkGenerator noiseGenerator) {
			// Surface rules work with any End biome source (also other mods' End generators that use noise).
			applySurfaceRules(noiseGenerator, surfaceRules);
		}
		if (stem == null || !(stem.generator().getBiomeSource() instanceof TheEndBiomeSource endSource)) {
			if (!BiomeRegions.getAll(RegionDimension.END).isEmpty()) {
				JONE500BiomeAPI.LOGGER.info("minecraft:the_end does not use the vanilla End biome source, End regions are not applied");
			}
			return;
		}
		List<BiomeRegion> regions = BiomeRegions.getAll(RegionDimension.END);
		Set<ResourceKey<Biome>> endBiomes = biomesOf(endSource);
		for (BiomeRegion region : regions) {
			endBiomes.addAll(region.getAllBiomes());
		}
		List<BiomeRegion.Replacement> replacements = onlyFor(BiomeReplacements.getAll(), endBiomes);
		List<BiomeRegion.Replacement> wholeSwaps = onlyFor(BiomeReplacements.getAllWholeBiome(), endBiomes);
		List<BiomeRegion.WeightedReplacement> weighted = onlyForWeighted(BiomeReplacements.getAllWeighted(), endBiomes);
		if (regions.isEmpty() && replacements.isEmpty() && weighted.isEmpty() && wholeSwaps.isEmpty()) {
			return;
		}
		BiomeApiBiomeSource source = (BiomeApiBiomeSource) endSource;
		if (source.jone500$getLookup() != null) {
			return;
		}
		RegionBiomeLookup lookup = RegionBiomeLookup.create(dimensionSeed(seed, RegionDimension.END), BiomeRegions.getRegionSize(),
				BiomeRegions.getVanillaWeight(), regions, replacements, weighted, BiomeReplacements.getPatchSize(), wholeSwaps, seed, null,
				registries.lookupOrThrow(Registries.BIOME));
		source.jone500$setLookup(lookup);
		refreshPossibleBiomes(endSource, stem.generator());
		JONE500BiomeAPI.LOGGER.info("minecraft:the_end: {} region(s), {} area replacement(s), {} weighted replacement(s), {} whole-biome replacement(s) active",
				regions.size(), replacements.size(), weighted.size(), lookup.getWholeBiomeSwaps().size());
	}

	/** The biomes a source can place right now (vanilla, datapacks, Fabric API), before this API adds any. */
	private static Set<ResourceKey<Biome>> biomesOf(BiomeSource source) {
		Set<ResourceKey<Biome>> keys = new HashSet<>();
		source.possibleBiomes().forEach(b -> b.unwrapKey().ifPresent(keys::add));
		return keys;
	}

	/** Only the weighted replacements whose original biome exists in this dimension. */
	private static List<BiomeRegion.WeightedReplacement> onlyForWeighted(List<BiomeRegion.WeightedReplacement> all, Set<ResourceKey<Biome>> dimensionBiomes) {
		return all.stream().filter(r -> dimensionBiomes.contains(r.original())).toList();
	}

	/** Only the replacements whose original biome exists in this dimension. */
	private static List<BiomeRegion.Replacement> onlyFor(List<BiomeRegion.Replacement> all, Set<ResourceKey<Biome>> dimensionBiomes) {
		return all.stream().filter(r -> dimensionBiomes.contains(r.original())).toList();
	}

	private static void refreshPossibleBiomes(BiomeSource biomeSource, ChunkGenerator generator) {
		BiomeSourceAccessor sourceAccess = (BiomeSourceAccessor) biomeSource;
		// Keep what is already in the list: other mods (TerraBlender) add their biomes to it directly.
		Set<Holder<Biome>> before = biomeSource.possibleBiomes();
		// Our biomes are added explicitly too: other mods (Biolith in the End) also change collectPossibleBiomes,
		// and depending on mixin order their result can replace ours.
		RegionBiomeLookup lookup = ((BiomeApiBiomeSource) biomeSource).jone500$getLookup();
		Set<Holder<Biome>> ours = lookup == null ? Set.of() : lookup.getAddedBiomes();
		sourceAccess.jone500$setPossibleBiomes(Suppliers.memoize(() -> ImmutableSet.<Holder<Biome>>builder()
				.addAll(before)
				.addAll(sourceAccess.jone500$collectPossibleBiomes().iterator())
				.addAll(ours)
				.build()));

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
