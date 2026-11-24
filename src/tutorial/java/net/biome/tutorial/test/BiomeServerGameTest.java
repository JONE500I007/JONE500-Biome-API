package net.biome.tutorial.test;

import net.biome.tutorial.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.biome.api.JONE500BiomeAPI;
import net.biome.api.biome.VanillaBiomeTags;
import net.biome.api.impl.BiomeApiBiomeSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.biome.api.climate.ClimatePoints;
import net.minecraft.tags.BiomeTags;
import net.biome.api.climate.Continentalness;
import net.biome.api.climate.Depth;
import net.biome.api.climate.Erosion;
import net.biome.api.climate.Temperature;
import net.biome.api.climate.Terrain;
import net.biome.api.impl.RegionBiomeLookup;
import net.biome.api.impl.RegionLayout;
import net.biome.api.region.BiomeRegion;
import net.biome.api.region.BiomeRegions;
import net.biome.api.region.RegionDimension;
import net.biome.api.region.BiomeReplacements;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Automated checks of the region logic. The gametest server uses a flat world, so these tests build a
 * normal Overworld climate (vanilla preset + vanilla noise) themselves and run the API's lookup on it.
 *
 * <p>Run with: gradlew runGameTest
 */
public class BiomeServerGameTest {
	/** Biome mods (Lithostitched, TerraBlender, ...) add points to the vanilla climate lists, so vanilla biomes get rarer. */
	private static final boolean OTHER_WORLDGEN_MODS = !System.getProperty("biome_tutorial.compat", "").isEmpty();

	/** Lithostitched wraps the biome source in its own (InjectorBiomeSource) that calls the original; find the original. */
	static BiomeApiBiomeSource apiSource(BiomeSource source) {
		if (source instanceof BiomeApiBiomeSource api) {
			return api;
		}
		try {
			return apiSource((BiomeSource) source.getClass().getMethod("rootDelegate").invoke(source));
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("unknown biome source " + source.getClass().getName(), e);
		}
	}

	private static final long SEED = 12345L;

	/** Regions must be used sorted by id, not in registration order (tutorial registers meadows first). */
	@GameTest(maxTicks = 20)
	public void regionsAreSortedById(GameTestHelper ctx) {
		List<BiomeRegion> all = BiomeRegions.getAll(RegionDimension.OVERWORLD);
		ctx.assertTrue(all.size() == 2, Component.literal("expected 2 regions, got " + all.size()));
		ctx.assertTrue(all.get(0).getId().equals(Lesson2ExclusiveBiomes.MEADOWS) && all.get(1).getId().equals(Lesson1FirstRegion.WARM),
				Component.literal("regions not sorted by id: " + all));
		ctx.succeed();
	}

	/** Over a large area every test biome appears, and vanilla biomes still appear too. */
	@GameTest(maxTicks = 20)
	public void regionBiomesAppear(GameTestHelper ctx) {
		World w = World.create(ctx, SEED);
		Map<ResourceKey<Biome>, Integer> counts = new HashMap<>();
		Map<Long, Set<ResourceKey<Biome>>> meadowBiomesPerCell = new HashMap<>();

		long start = System.nanoTime();
		int samples = 0;
		for (int i = 0; i < 160; i++) {
			for (int j = 0; j < 160; j++) {
				int qx = (i - 80) * 40;
				int qz = (j - 80) * 40;
				for (int qy = 16; qy <= 40; qy += 8) {
					Holder<Biome> biome = w.biomeAt(qx, qy, qz);
					samples++;
					ResourceKey<Biome> key = biome.unwrapKey().orElseThrow();
					counts.merge(key, 1, Integer::sum);
					if (key.equals(TutorialBiomes.ORANGE_HILLS)) {
						BiomeRegion region = w.lookup.getRegionAt(qx, qz);
						ctx.assertTrue(region != null && region.getId().equals(Lesson1FirstRegion.WARM),
								Component.literal("orange hills outside of the warm region at " + qx + "," + qz));
					}
					if (key.equals(TutorialBiomes.MINT_MEADOW) || key.equals(TutorialBiomes.AMBER_MEADOW)) {
						RegionLayout.Cell cell = w.lookup.getLayout().cellAt(qx, qz);
						meadowBiomesPerCell.computeIfAbsent(((long) cell.x() << 32) ^ (cell.z() & 0xFFFFFFFFL), k -> new HashSet<>()).add(key);
					}
				}
			}
		}
		long micros = (System.nanoTime() - start) / 1000;
		JONE500BiomeAPI.LOGGER.info("[test] {} samples in {} ms, counts: red_plains={} blue_forest={} mint={} amber={} orange_hills={} plains={} forest={}",
				samples, micros / 1000, counts.get(TutorialBiomes.RED_PLAINS), counts.get(TutorialBiomes.BLUE_FOREST),
				counts.get(TutorialBiomes.MINT_MEADOW), counts.get(TutorialBiomes.AMBER_MEADOW), counts.get(TutorialBiomes.ORANGE_HILLS),
				counts.get(Biomes.PLAINS), counts.get(Biomes.FOREST));

		for (ResourceKey<Biome> key : List.of(TutorialBiomes.RED_PLAINS, TutorialBiomes.BLUE_FOREST, TutorialBiomes.MINT_MEADOW,
				TutorialBiomes.AMBER_MEADOW, TutorialBiomes.ORANGE_HILLS, Biomes.PLAINS, Biomes.FOREST)) {
			ctx.assertTrue(counts.getOrDefault(key, 0) > 0, Component.literal(key.identifier() + " never appeared"));
		}
		// exclusive(MINT_MEADOW, AMBER_MEADOW): never both in the same patch.
		for (Set<ResourceKey<Biome>> set : meadowBiomesPerCell.values()) {
			ctx.assertTrue(set.size() == 1, Component.literal("mint and amber meadow appeared in the same patch"));
		}
		ctx.succeed();
	}

	/** Same seed = same biomes, also with a freshly built lookup. Different seed = different map. */
	@GameTest(maxTicks = 20)
	public void sameSeedSameWorld(GameTestHelper ctx) {
		World a = World.create(ctx, SEED);
		World b = World.create(ctx, SEED);
		World other = World.create(ctx, SEED + 1);
		Random random = new Random(7);
		int differentFromOtherSeed = 0;
		for (int n = 0; n < 3000; n++) {
			int qx = random.nextInt(40000) - 20000;
			int qz = random.nextInt(40000) - 20000;
			int qy = 16;
			Holder<Biome> first = a.biomeAt(qx, qy, qz);
			ctx.assertTrue(first.equals(b.biomeAt(qx, qy, qz)), Component.literal("same seed gave different biomes at " + qx + "," + qz));
			if (a.lookup.getLayout().cellAt(qx, qz).region() != other.lookup.getLayout().cellAt(qx, qz).region()) {
				differentFromOtherSeed++;
			}
		}
		ctx.assertTrue(differentFromOtherSeed > 300, Component.literal("region map barely changes with the seed: " + differentFromOtherSeed));
		ctx.succeed();
	}

	/** Patch shares follow the weights: vanilla 10, meadows 2, warm 3 (= 66.7% / 13.3% / 20%). */
	@GameTest(maxTicks = 20)
	public void regionSharesFollowWeights(GameTestHelper ctx) {
		RegionLayout layout = World.create(ctx, SEED).lookup.getLayout();
		int[] counts = new int[3];
		int total = 0;
		for (int x = -150; x < 150; x++) {
			for (int z = -150; z < 150; z++) {
				counts[layout.pickRegion(x, z) + 1]++;
				total++;
			}
		}
		checkShare(ctx, "vanilla", counts[0], total, 10.0 / 15.0);
		checkShare(ctx, "meadows", counts[1], total, 2.0 / 15.0);
		checkShare(ctx, "warm", counts[2], total, 3.0 / 15.0);
		ctx.succeed();
	}

	/** Per-patch rolls: chance 0.5 / 0.1 hit about that share of patches; pick() is about uniform. */
	@GameTest(maxTicks = 20)
	public void patchRollsFollowChance(GameTestHelper ctx) {
		RegionLayout layout = World.create(ctx, SEED).lookup.getLayout();
		long salt = RegionLayout.salt("biome_tutorial:warm#biome_tutorial:blue_forest");
		int half = 0;
		int tenth = 0;
		int[] picks = new int[3];
		int total = 0;
		for (int x = -100; x < 100; x++) {
			for (int z = -100; z < 100; z++) {
				RegionLayout.Cell cell = new RegionLayout.Cell(x, z, 0);
				if (layout.roll(cell, salt, 0.5F)) half++;
				if (layout.roll(cell, salt ^ 1, 0.1F)) tenth++;
				picks[layout.pick(cell, salt ^ 2, 3)]++;
				total++;
			}
		}
		checkShare(ctx, "chance 0.5", half, total, 0.5);
		checkShare(ctx, "chance 0.1", tenth, total, 0.1);
		for (int p = 0; p < 3; p++) {
			checkShare(ctx, "pick " + p, picks[p], total, 1.0 / 3.0);
		}
		ctx.succeed();
	}

	/**
	 * Patches are about as big as configured: centers 128 quarts (512 blocks) apart; a straight line
	 * crosses into a new patch about every 50-100 quarts because patches are irregular.
	 */
	@GameTest(maxTicks = 20)
	public void patchesHaveTheConfiguredSize(GameTestHelper ctx) {
		RegionLayout layout = World.create(ctx, SEED).lookup.getLayout();
		// Walk a long line and count how often the patch changes.
		int changes = 0;
		RegionLayout.Cell last = layout.cellAt(0, 0);
		int length = 128 * 200;
		for (int x = 1; x < length; x++) {
			RegionLayout.Cell cell = layout.cellAt(x, 777);
			if (cell.x() != last.x() || cell.z() != last.z()) {
				changes++;
				last = cell;
			}
		}
		double average = (double) length / Math.max(1, changes);
		JONE500BiomeAPI.LOGGER.info("[test] average patch width along a line: {} quarts ({} changes)", (int) average, changes);
		ctx.assertTrue(average > 50 && average < 250, Component.literal("average patch width " + average + " quarts, expected around 128"));
		ctx.succeed();
	}

	/**
	 * Biomes are 3D: under a region the vanilla cave biomes (lush caves, dripstone caves, deep dark) must
	 * still appear, and the region's surface biomes must not replace them.
	 */
	@GameTest(maxTicks = 20)
	public void caveBiomesStayUnderRegions(GameTestHelper ctx) {
		World w = World.create(ctx, SEED);
		Map<ResourceKey<Biome>, Integer> underground = new HashMap<>();
		int samplesInRegions = 0;
		for (int i = 0; i < 200; i++) {
			for (int j = 0; j < 200; j++) {
				int qx = (i - 100) * 32;
				int qz = (j - 100) * 32;
				if (w.lookup.getRegionAt(qx, qz) == null) {
					continue;
				}
				for (int qy = -12; qy <= 0; qy += 6) { // y = -48 .. 0
					samplesInRegions++;
					underground.merge(w.biomeAt(qx, qy, qz).unwrapKey().orElseThrow(), 1, Integer::sum);
				}
			}
		}
		int caves = underground.getOrDefault(Biomes.LUSH_CAVES, 0) + underground.getOrDefault(Biomes.DRIPSTONE_CAVES, 0)
				+ underground.getOrDefault(Biomes.DEEP_DARK, 0);
		JONE500BiomeAPI.LOGGER.info("[test] underground in regions: {} samples, cave biomes={}, red_plains={}, orange_hills={}",
				samplesInRegions, caves, underground.get(TutorialBiomes.RED_PLAINS), underground.get(TutorialBiomes.ORANGE_HILLS));
		ctx.assertTrue(samplesInRegions > 1000, Component.literal("too few samples inside regions: " + samplesInRegions));
		ctx.assertTrue(caves > samplesInRegions / 50, Component.literal("cave biomes almost missing under regions: " + caves + " of " + samplesInRegions));
		ctx.succeed();
	}

	/** replaceWholeBiome: decided once per world. 0.5 = about half of all seeds, 1.0 = every seed. */
	@GameTest(maxTicks = 20)
	public void wholeBiomeSwapIsPerWorld(GameTestHelper ctx) {
		List<BiomeRegion.Replacement> swaps = BiomeReplacements.getAllWholeBiome();
		int darkForestSwapped = 0;
		int lushSwapped = 0;
		int worlds = 2000;
		for (long seed = 0; seed < worlds; seed++) {
			Map<ResourceKey<Biome>, ResourceKey<Biome>> active = RegionBiomeLookup.chooseWholeBiomeSwaps(seed * 7919L + 13L, swaps);
			if (TutorialBiomes.BLUE_FOREST.equals(active.get(Biomes.DARK_FOREST))) darkForestSwapped++;
			if (TutorialBiomes.MINT_MEADOW.equals(active.get(Biomes.LUSH_CAVES))) lushSwapped++;
		}
		JONE500BiomeAPI.LOGGER.info("[test] whole-biome swap over {} worlds: dark forest {} times, lush caves {} times",
				worlds, darkForestSwapped, lushSwapped);
		checkShare(ctx, "dark forest swap (0.5)", darkForestSwapped, worlds, 0.5);
		ctx.assertTrue(lushSwapped == worlds, Component.literal("lush caves (1.0) should be swapped in every world, got " + lushSwapped));

		// In one world it is all or nothing, also for a cave biome. Check a world where the swap is off
		// (seed 12345) and one where it is on (seed 12346).
		List<Boolean> activeInWorlds = new ArrayList<>();
		for (long worldSeed : new long[] {SEED, SEED + 1}) {
			activeInWorlds.add(checkWholeSwapInWorld(ctx, worldSeed));
		}
		ctx.assertTrue(activeInWorlds.contains(true) && activeInWorlds.contains(false),
				Component.literal("expected the dark forest swap on in one test world and off in the other: " + activeInWorlds));
		ctx.succeed();
	}

	private static boolean checkWholeSwapInWorld(GameTestHelper ctx, long worldSeed) {
		World w = World.create(ctx, worldSeed);
		boolean darkActive = w.lookup.getWholeBiomeSwaps().containsKey(Biomes.DARK_FOREST);
		int darkForest = 0;
		int blueAtDark = 0;
		int lush = 0;
		int mintUnderground = 0;
		for (int i = 0; i < 160; i++) {
			for (int j = 0; j < 160; j++) {
				int qx = (i - 80) * 40;
				int qz = (j - 80) * 40;
				for (int qy = -10; qy <= 20; qy += 10) {
					Climate.TargetPoint target = w.sampler.sample(qx, qy, qz);
					Holder<Biome> vanillaHere = w.vanilla.findValue(target);
					Holder<Biome> result = w.lookup.getBiome(target, qx, qz, w.vanilla);
					if (result.is(Biomes.DARK_FOREST)) darkForest++;
					if (result.is(Biomes.LUSH_CAVES)) lush++;
					if (vanillaHere.is(Biomes.DARK_FOREST) && result.is(TutorialBiomes.BLUE_FOREST)) blueAtDark++;
					if (vanillaHere.is(Biomes.LUSH_CAVES) && result.is(TutorialBiomes.MINT_MEADOW)) mintUnderground++;
				}
			}
		}
		JONE500BiomeAPI.LOGGER.info("[test] seed {}: dark forest swap active={}, dark_forest left={}, blue at dark forest={}, lush left={}, mint in lush caves={}",
				worldSeed, darkActive, darkForest, blueAtDark, lush, mintUnderground);
		ctx.assertTrue(lush == 0, Component.literal("lush caves still appear " + lush + " times"));
		ctx.assertTrue(mintUnderground > 0, Component.literal("the cave swap never happened"));
		if (darkActive) {
			ctx.assertTrue(darkForest == 0 && blueAtDark > 0, Component.literal("swap active but dark forest still appears " + darkForest + " times"));
		} else {
			ctx.assertTrue(darkForest > 0 && blueAtDark == 0, Component.literal("swap not active but dark forest was replaced " + blueAtDark + " times"));
		}
		return darkActive;
	}

	/**
	 * Feature order rules that the README warns about, checked with vanilla's own sorter (the one that stops a
	 * world with "Feature order cycle"): a vanilla feature added twice fails, two biomes with the same two
	 * features in opposite order fail, and the README's "diamond caves" example (empty + own ores) is fine.
	 */
	@GameTest(maxTicks = 20)
	public void featureOrderRules(GameTestHelper ctx) {
		RegistryAccess registries = ctx.getLevel().registryAccess();
		var placed = registries.lookupOrThrow(Registries.PLACED_FEATURE);
		var carvers = registries.lookupOrThrow(Registries.CONFIGURED_CARVER);
		Biome plains = registries.lookupOrThrow(Registries.BIOME).getValueOrThrow(Biomes.PLAINS);

		Biome doubleDiamonds = net.biome.api.biome.BiomeMaker.overworld(placed, carvers)
				.addFeature(net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES,
						net.minecraft.data.worldgen.placement.OrePlacements.ORE_DIAMOND)
				.build();
		// Biolith changes vanilla's feature sorter (MixinFeatureSorter), so the "must fail" checks only hold without it.
		boolean vanillaSorter = !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("biolith");
		if (vanillaSorter) {
			ctx.assertTrue(!featureOrderOk(List.of(plains, doubleDiamonds)), Component.literal("a vanilla ore added twice should fail"));
		}

		var step = net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION;
		var a = net.minecraft.data.worldgen.placement.VegetationPlacements.PATCH_PUMPKIN;
		var b = net.minecraft.data.worldgen.placement.VegetationPlacements.PATCH_SUGAR_CANE;
		Biome ab = net.biome.api.biome.BiomeMaker.empty(placed, carvers).addFeature(step, a).addFeature(step, b).build();
		Biome ba = net.biome.api.biome.BiomeMaker.empty(placed, carvers).addFeature(step, b).addFeature(step, a).build();
		ctx.assertTrue(featureOrderOk(List.of(ab)) && featureOrderOk(List.of(ba)), Component.literal("each order alone should be fine"));
		if (vanillaSorter) {
			ctx.assertTrue(!featureOrderOk(List.of(ab, ba)), Component.literal("opposite order in two biomes should fail"));
		}

		var ores = net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES;
		Biome diamondCaves = net.biome.api.biome.BiomeMaker.empty(placed, carvers)
				.vanillaCaves()
				.vanillaStoneVeins()
				.addFeature(net.minecraft.world.level.levelgen.GenerationStep.Decoration.LOCAL_MODIFICATIONS,
						net.minecraft.data.worldgen.placement.CavePlacements.AMETHYST_GEODE)
				.addFeature(ores, net.minecraft.data.worldgen.placement.OrePlacements.ORE_IRON_UPPER)
				.addFeature(ores, net.minecraft.data.worldgen.placement.OrePlacements.ORE_IRON_MIDDLE)
				.addFeature(ores, net.minecraft.data.worldgen.placement.OrePlacements.ORE_DIAMOND)
				.addFeature(ores, net.minecraft.data.worldgen.placement.OrePlacements.ORE_DIAMOND_LARGE)
				.addFeature(ores, net.minecraft.data.worldgen.placement.OrePlacements.ORE_DIAMOND_BURIED)
				.addFeature(ores, net.minecraft.data.worldgen.placement.MiscOverworldPlacements.DISK_CLAY)
				.build();
		List<Biome> all = new ArrayList<>();
		registries.lookupOrThrow(Registries.BIOME).forEach(all::add);
		all.add(diamondCaves);
		ctx.assertTrue(featureOrderOk(all), Component.literal("the README's diamond caves example conflicts with a vanilla biome"));
		ctx.succeed();
	}

	private static boolean featureOrderOk(List<Biome> biomes) {
		try {
			net.minecraft.world.level.biome.FeatureSorter.buildFeaturesPerStep(biomes, b -> b.getGenerationSettings().features(), true);
			return true;
		} catch (IllegalStateException e) {
			JONE500BiomeAPI.LOGGER.info("[test] feature order check failed as expected: {}", e.getMessage().lines().findFirst().orElse(""));
			return false;
		}
	}

	/** A modded biome (red plains, placed by a region) can be replaced too, by another modded biome. */
	@GameTest(maxTicks = 20)
	public void replaceOwnModdedBiome(GameTestHelper ctx) {
		RegistryAccess registries = ctx.getLevel().registryAccess();
		Climate.ParameterList<Holder<Biome>> vanilla = registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
				.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).value().parameters();
		Climate.Sampler sampler = RandomState.create(registries, NoiseGeneratorSettings.OVERWORLD, SEED).sampler();
		List<BiomeRegion.Replacement> replacements = List.of(new BiomeRegion.Replacement(TutorialBiomes.RED_PLAINS, TutorialBiomes.ORANGE_HILLS, 0.5F));
		RegionBiomeLookup without = RegionBiomeLookup.create(SEED, BiomeRegions.getRegionSize(), BiomeRegions.getVanillaWeight(),
				new ArrayList<>(BiomeRegions.getAll(RegionDimension.OVERWORLD)), List.of(), BiomeReplacements.getPatchSize(), List.of(), SEED,
				vanilla, registries.lookupOrThrow(Registries.BIOME));
		RegionBiomeLookup with = RegionBiomeLookup.create(SEED, BiomeRegions.getRegionSize(), BiomeRegions.getVanillaWeight(),
				new ArrayList<>(BiomeRegions.getAll(RegionDimension.OVERWORLD)), replacements, BiomeReplacements.getPatchSize(), List.of(), SEED,
				vanilla, registries.lookupOrThrow(Registries.BIOME));
		int red = 0;
		int replaced = 0;
		for (int i = 0; i < 260; i++) {
			for (int j = 0; j < 260; j++) {
				int qx = (i - 130) * 24;
				int qz = (j - 130) * 24;
				Climate.TargetPoint target = sampler.sample(qx, 16, qz);
				if (!without.getBiome(target, qx, qz, vanilla).is(TutorialBiomes.RED_PLAINS)) {
					continue;
				}
				Holder<Biome> result = with.getBiome(target, qx, qz, vanilla);
				if (result.is(TutorialBiomes.ORANGE_HILLS)) {
					replaced++;
				} else {
					ctx.assertTrue(result.is(TutorialBiomes.RED_PLAINS), Component.literal("red plains became " + result.getRegisteredName()));
				}
				red++;
			}
		}
		double share = (double) replaced / red;
		JONE500BiomeAPI.LOGGER.info("[test] own modded biome: {} of {} red plains samples became orange hills ({}%)", replaced, red, Math.round(share * 100));
		ctx.assertTrue(red > 200, Component.literal("too few red plains samples: " + red));
		ctx.assertTrue(Math.abs(share - 0.5) < 0.15, Component.literal("replaced share " + share + ", expected about 0.5"));
		ctx.succeed();
	}

	/** Only vanilla biomes: a vanilla biome can replace another one, so vanilla biome rates can be changed. */
	@GameTest(maxTicks = 20)
	public void vanillaToVanillaReplacement(GameTestHelper ctx) {
		RegistryAccess registries = ctx.getLevel().registryAccess();
		Climate.ParameterList<Holder<Biome>> vanilla = registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
				.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).value().parameters();
		Climate.Sampler sampler = RandomState.create(registries, NoiseGeneratorSettings.OVERWORLD, SEED).sampler();
		RegionBiomeLookup lookup = RegionBiomeLookup.create(SEED, BiomeRegions.getRegionSize(), BiomeRegions.getVanillaWeight(), List.of(),
				List.of(new BiomeRegion.Replacement(Biomes.DESERT, Biomes.SAVANNA, 0.5F)), BiomeReplacements.getPatchSize(), List.of(), SEED,
				vanilla, registries.lookupOrThrow(Registries.BIOME));
		int desert = 0;
		int savanna = 0;
		for (int i = 0; i < 260; i++) {
			for (int j = 0; j < 260; j++) {
				int qx = (i - 130) * 24;
				int qz = (j - 130) * 24;
				Climate.TargetPoint target = sampler.sample(qx, 16, qz);
				if (!vanilla.findValue(target).is(Biomes.DESERT)) {
					continue;
				}
				Holder<Biome> result = lookup.getBiome(target, qx, qz, vanilla);
				if (result.is(Biomes.SAVANNA)) {
					savanna++;
				} else {
					ctx.assertTrue(result.is(Biomes.DESERT), Component.literal("desert became " + result.getRegisteredName()));
				}
				desert++;
			}
		}
		double share = (double) savanna / desert;
		JONE500BiomeAPI.LOGGER.info("[test] vanilla to vanilla: {} of {} desert samples became savanna ({}%)", savanna, desert, Math.round(share * 100));
		ctx.assertTrue(desert > (OTHER_WORLDGEN_MODS ? 100 : 300), Component.literal("too few desert samples: " + desert));
		ctx.assertTrue(Math.abs(share - 0.5) < 0.12, Component.literal("savanna share " + share + ", expected about 0.5"));
		ctx.succeed();
	}

	/** BiomeReplacements: about 40% of birch forest becomes blue forest, everywhere (also in vanilla patches). */
	@GameTest(maxTicks = 20)
	public void worldWideReplacementFollowsChance(GameTestHelper ctx) {
		World w = World.create(ctx, SEED);
		int birch = 0;
		int replaced = 0;
		Set<Long> patches = new HashSet<>();
		for (int i = 0; i < 260; i++) {
			for (int j = 0; j < 260; j++) {
				int qx = (i - 130) * 24;
				int qz = (j - 130) * 24;
				if (w.lookup.getRegionAt(qx, qz) != null) {
					continue; // only vanilla patches, so regions don't mix in
				}
				Climate.TargetPoint target = w.sampler.sample(qx, 16, qz);
				if (!w.vanilla.findValue(target).is(Biomes.BIRCH_FOREST)) {
					continue;
				}
				ResourceKey<Biome> result = w.lookup.getBiome(target, qx, qz, w.vanilla).unwrapKey().orElseThrow();
				if (result.equals(TutorialBiomes.BLUE_FOREST)) {
					replaced++;
				} else {
					ctx.assertTrue(result.equals(Biomes.BIRCH_FOREST), Component.literal("birch forest became " + result.identifier()));
				}
				birch++;
				RegionLayout.Cell cell = w.lookup.getReplacementLayout().cellAt(qx, qz);
				patches.add(((long) cell.x() << 32) ^ (cell.z() & 0xFFFFFFFFL));
			}
		}
		double share = (double) replaced / birch;
		JONE500BiomeAPI.LOGGER.info("[test] world-wide replacement: {} of {} birch forest samples replaced ({}%), {} patches",
				replaced, birch, Math.round(share * 100), patches.size());
		ctx.assertTrue(birch > (OTHER_WORLDGEN_MODS ? 100 : 500), Component.literal("too few birch forest samples: " + birch));
		ctx.assertTrue(Math.abs(share - 0.4) < 0.1, Component.literal("replaced share " + share + ", expected about 0.4"));
		ctx.succeed();
	}

	/**
	 * The "biome in the sea" problem: an inland-only biome added with depth(-1, 1) (like many TerraBlender
	 * setups) wins in ocean water, because depth is negative above the sea floor. With SURFACE depth it
	 * stays out, and strict() guarantees it never appears outside its ranges.
	 */
	@GameTest(maxTicks = 20)
	public void inlandBiomeStaysOutOfTheSea(GameTestHelper ctx) {
		ClimatePoints.Builder inland = ClimatePoints.builder()
				.continentalness(Continentalness.INLAND)
				.temperature(Temperature.NEUTRAL, Temperature.WARM);
		int oldStyle = countOutsideInland(ctx, inland.depth(-1.0F, 1.0F), false);
		int surface = countOutsideInland(ctx, inland.depth(Depth.SURFACE), false);
		int strict = countOutsideInland(ctx, inland.depth(Depth.SURFACE), true);
		JONE500BiomeAPI.LOGGER.info("[test] inland biome outside inland climate: depth(-1,1)={} SURFACE={} SURFACE+strict={}",
				oldStyle, surface, strict);
		ctx.assertTrue(surface < oldStyle, Component.literal("SURFACE depth should leak less than depth(-1, 1): " + surface + " vs " + oldStyle));
		ctx.assertTrue(strict == 0, Component.literal("strict biome appeared outside its continentalness " + strict + " times"));
		ctx.succeed();
	}

	/** How often the biome appears where continentalness is NOT inland (ocean, coast). */
	private static int countOutsideInland(GameTestHelper ctx, ClimatePoints.Builder climate, boolean strict) {
		if (strict) {
			climate.strict();
		}
		BiomeRegion region = BiomeRegion.builder(TutorialMod.id("sea_test")).addBiome(climate, TutorialBiomes.ORANGE_HILLS).build();
		RegistryAccess registries = ctx.getLevel().registryAccess();
		Climate.ParameterList<Holder<Biome>> vanilla = registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
				.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).value().parameters();
		Climate.Sampler sampler = RandomState.create(registries, NoiseGeneratorSettings.OVERWORLD, SEED).sampler();
		// vanilla weight 0: the whole world is this region
		RegionBiomeLookup lookup = RegionBiomeLookup.create(SEED, 7, 0.0F, List.of(region), vanilla, registries.lookupOrThrow(Registries.BIOME));
		long inlandMin = Climate.quantizeCoord(-0.11F);
		int count = 0;
		for (int i = 0; i < 120; i++) {
			for (int j = 0; j < 120; j++) {
				int qx = (i - 60) * 48;
				int qz = (j - 60) * 48;
				for (int qy = 8; qy <= 20; qy += 4) { // y = 32 .. 80: sea floor up to above sea level
					Climate.TargetPoint target = sampler.sample(qx, qy, qz);
					if (target.continentalness() < inlandMin
							&& lookup.getBiome(target, qx, qz, vanilla).is(TutorialBiomes.ORANGE_HILLS)) {
						count++;
					}
				}
			}
		}
		return count;
	}

	/** Nether region on the server's real Nether: ember forest appears, only inside its region; vanilla stays elsewhere. */
	@GameTest(maxTicks = 20)
	public void netherRegionWorks(GameTestHelper ctx) {
		ServerLevel nether = ctx.getLevel().getServer().getLevel(Level.NETHER);
		ctx.assertTrue(nether != null, Component.literal("no Nether level"));
		BiomeSource source = nether.getChunkSource().getGenerator().getBiomeSource();
		RegionBiomeLookup lookup = apiSource(source).jone500$getLookup();
		ctx.assertTrue(lookup != null, Component.literal("the Nether got no region lookup"));
		Climate.Sampler sampler = nether.getChunkSource().randomState().sampler();
		int ember = 0;
		int crimson = 0;
		for (int i = 0; i < 150; i++) {
			for (int j = 0; j < 150; j++) {
				int qx = (i - 75) * 24;
				int qz = (j - 75) * 24;
				Holder<Biome> biome = source.getNoiseBiome(qx, 16, qz, sampler);
				if (biome.is(TutorialBiomes.EMBER_FOREST)) {
					ember++;
					BiomeRegion region = lookup.getRegionAt(qx, qz);
					ctx.assertTrue(region != null && region.getId().equals(Lesson5Nether.EMBERS),
							Component.literal("ember forest outside the embers region at " + qx + "," + qz));
				}
				if (biome.is(Biomes.CRIMSON_FOREST)) crimson++;
			}
		}
		JONE500BiomeAPI.LOGGER.info("[test] nether: ember_forest={} crimson_forest={}", ember, crimson);
		ctx.assertTrue(ember > 0, Component.literal("ember forest never appeared in the Nether"));
		ctx.assertTrue(crimson > 0, Component.literal("crimson forest should still exist outside the region"));
		ctx.assertTrue(source.possibleBiomes().stream().anyMatch(b -> b.is(TutorialBiomes.EMBER_FOREST)),
				Component.literal("ember forest is not a possible Nether biome"));
		ctx.succeed();
	}

	/**
	 * End on the server's real End: Fabric API's TheEndBiomes places void meadow in the highlands, and this API's
	 * replaceWholeBiome(SMALL_END_ISLANDS -> VOID_MEADOW, 1.0) removes every small island biome.
	 */
	@GameTest(maxTicks = 20)
	public void endReplacementWorks(GameTestHelper ctx) {
		ServerLevel end = ctx.getLevel().getServer().getLevel(Level.END);
		ctx.assertTrue(end != null, Component.literal("no End level"));
		BiomeSource source = end.getChunkSource().getGenerator().getBiomeSource();
		Climate.Sampler sampler = end.getChunkSource().randomState().sampler();
		Map<ResourceKey<Biome>, Integer> counts = new HashMap<>();
		for (int i = 0; i < 120; i++) {
			for (int j = 0; j < 120; j++) {
				int qx = 400 + i * 40; // far from the main island (1600+ blocks)
				int qz = (j - 60) * 40;
				counts.merge(source.getNoiseBiome(qx, 16, qz, sampler).unwrapKey().orElseThrow(), 1, Integer::sum);
			}
		}
		JONE500BiomeAPI.LOGGER.info("[test] end: {}", counts);
		ctx.assertTrue(counts.getOrDefault(Biomes.SMALL_END_ISLANDS, 0) == 0,
				Component.literal("small end islands still appear " + counts.get(Biomes.SMALL_END_ISLANDS) + " times"));
		ctx.assertTrue(counts.getOrDefault(TutorialBiomes.VOID_MEADOW, 0) > 0, Component.literal("void meadow never appeared in the End"));
		ctx.assertTrue(counts.getOrDefault(Biomes.END_HIGHLANDS, 0) > 0, Component.literal("vanilla end highlands disappeared"));
		ctx.succeed();
	}

	/**
	 * End region on the server's real End: inside "ashlands" patches every end barrens is ash barrens (and some
	 * midlands); outside them vanilla stays. addBiome(...) is refused for End regions.
	 */
	@GameTest(maxTicks = 20)
	public void endRegionWorks(GameTestHelper ctx) {
		ServerLevel end = ctx.getLevel().getServer().getLevel(Level.END);
		ctx.assertTrue(end != null, Component.literal("no End level"));
		BiomeSource source = end.getChunkSource().getGenerator().getBiomeSource();
		RegionBiomeLookup lookup = apiSource(source).jone500$getLookup();
		ctx.assertTrue(lookup != null, Component.literal("the End got no region lookup"));
		Climate.Sampler sampler = end.getChunkSource().randomState().sampler();
		int ash = 0;
		int ashOutside = 0;
		int barrensInside = 0;
		int barrensOutside = 0;
		int midlandsInside = 0;
		int regionSamples = 0;
		for (int i = 0; i < 160; i++) {
			for (int j = 0; j < 160; j++) {
				int qx = 400 + i * 32; // far from the main island
				int qz = (j - 80) * 32;
				Holder<Biome> biome = source.getNoiseBiome(qx, 16, qz, sampler);
				BiomeRegion region = lookup.getRegionAt(qx, qz);
				boolean inside = region != null && region.getId().equals(Lesson6End.ASHLANDS);
				if (inside) regionSamples++;
				if (biome.is(TutorialBiomes.ASH_BARRENS)) {
					ash++;
					if (!inside) ashOutside++;
				}
				if (biome.is(Biomes.END_BARRENS)) {
					if (inside) barrensInside++;
					else barrensOutside++;
				}
				if (inside && biome.is(Biomes.END_MIDLANDS)) midlandsInside++;
			}
		}
		JONE500BiomeAPI.LOGGER.info("[test] end region: samples in region={}, ash={}, ash outside={}, barrens inside={}, barrens outside={}, midlands inside={}",
				regionSamples, ash, ashOutside, barrensInside, barrensOutside, midlandsInside);
		ctx.assertTrue(regionSamples > 0, Component.literal("the ashlands region never appeared"));
		ctx.assertTrue(ash > 0, Component.literal("ash barrens never appeared"));
		ctx.assertTrue(ashOutside == 0, Component.literal("ash barrens outside its region " + ashOutside + " times"));
		ctx.assertTrue(barrensInside == 0, Component.literal("end barrens left inside the region " + barrensInside + " times"));
		ctx.assertTrue(barrensOutside > 0, Component.literal("vanilla end barrens disappeared outside the region"));
		ctx.assertTrue(source.possibleBiomes().stream().anyMatch(b -> b.is(TutorialBiomes.ASH_BARRENS)),
				Component.literal("ash barrens is not a possible End biome"));
		List<String> debug = new ArrayList<>();
		source.addDebugInfo(debug, new net.minecraft.core.BlockPos(2000, 60, 0), sampler);
		ctx.assertTrue(debug.stream().anyMatch(s -> s.startsWith("Biome API region: ")), Component.literal("no F3 region line in the End: " + debug));

		boolean refused;
		try {
			BiomeRegion.builder(TutorialMod.id("end_climate")).end()
					.addBiome(ClimatePoints.builder().temperature(Temperature.WARM), TutorialBiomes.ASH_BARRENS).build();
			refused = false;
		} catch (IllegalStateException e) {
			refused = true;
		}
		ctx.assertTrue(refused, Component.literal("addBiome(...) should be refused in an End region"));
		ctx.succeed();
	}

	/**
	 * End surface rule on the server's real End: the top block of ash barrens is smooth basalt (our rule),
	 * vanilla end barrens keep end stone (vanilla's rule still runs after ours).
	 */
	@GameTest(maxTicks = 400)
	public void endSurfaceRuleWorks(GameTestHelper ctx) {
		ServerLevel end = ctx.getLevel().getServer().getLevel(Level.END);
		ctx.assertTrue(end != null, Component.literal("no End level"));
		BiomeSource source = end.getChunkSource().getGenerator().getBiomeSource();
		Climate.Sampler sampler = end.getChunkSource().randomState().sampler();
		Map<String, Integer> ashTops = new HashMap<>();
		Map<String, Integer> barrensTops = new HashMap<>();
		int checkedChunks = 0;
		for (int i = 0; i < 80 && checkedChunks < 24; i++) {
			for (int j = 0; j < 80 && checkedChunks < 24; j++) {
				int qx = 400 + i * 32;
				int qz = (j - 40) * 32;
				Holder<Biome> biome = source.getNoiseBiome(qx, 16, qz, sampler);
				boolean ash = biome.is(TutorialBiomes.ASH_BARRENS);
				if (!ash && !(biome.is(Biomes.END_BARRENS) && barrensTops.values().stream().mapToInt(Integer::intValue).sum() < 200)) {
					continue;
				}
				int bx = qx << 2;
				int bz = qz << 2;
				net.minecraft.world.level.chunk.ChunkAccess chunk = end.getChunk(bx >> 4, bz >> 4);
				checkedChunks++;
				for (int dx = 0; dx < 16; dx++) {
					for (int dz = 0; dz < 16; dz++) {
						int x = (bx & ~15) + dx;
						int z = (bz & ~15) + dz;
						int top = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, x & 15, z & 15);
						if (top <= end.getMinY()) {
							continue; // void: no island here
						}
						net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, top, z);
						Holder<Biome> here = end.getBiome(pos);
						String block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos).getBlock()).getPath();
						if (here.is(TutorialBiomes.ASH_BARRENS)) ashTops.merge(block, 1, Integer::sum);
						else if (here.is(Biomes.END_BARRENS)) barrensTops.merge(block, 1, Integer::sum);
					}
				}
			}
		}
		JONE500BiomeAPI.LOGGER.info("[test] end surface: ash barrens tops {}, end barrens tops {}", ashTops, barrensTops);
		int ashTotal = ashTops.values().stream().mapToInt(Integer::intValue).sum();
		int barrensTotal = barrensTops.values().stream().mapToInt(Integer::intValue).sum();
		ctx.assertTrue(ashTotal > 50, Component.literal("too few ash barrens columns with ground: " + ashTotal));
		ctx.assertTrue(ashTops.getOrDefault("smooth_basalt", 0) > ashTotal * 0.9,
				Component.literal("ash barrens ground should be smooth basalt: " + ashTops));
		ctx.assertTrue(barrensTotal == 0 || barrensTops.getOrDefault("end_stone", 0) > barrensTotal * 0.9,
				Component.literal("vanilla end barrens ground should stay end stone: " + barrensTops));
		ctx.succeed();
	}

	/** WeightedBiomes in an Overworld region: plains become red plains / blue forest / orange hills about 5 : 3 : 1. */
	@GameTest(maxTicks = 20)
	public void weightedRegionFollowsWeights(GameTestHelper ctx) {
		RegistryAccess registries = ctx.getLevel().registryAccess();
		Climate.ParameterList<Holder<Biome>> vanilla = registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
				.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).value().parameters();
		Climate.Sampler sampler = RandomState.create(registries, NoiseGeneratorSettings.OVERWORLD, SEED).sampler();
		BiomeRegion region = BiomeRegion.builder(TutorialMod.id("weighted_test"))
				.replace(Biomes.PLAINS, net.biome.api.region.WeightedBiomes.builder()
						.add(TutorialBiomes.RED_PLAINS, 5)
						.add(TutorialBiomes.BLUE_FOREST, 3)
						.add(TutorialBiomes.ORANGE_HILLS, 1)
						.size(4)
						.build())
				.build();
		// vanilla weight 0: the whole world is this region
		RegionBiomeLookup lookup = RegionBiomeLookup.create(SEED, 7, 0.0F, List.of(region), vanilla, registries.lookupOrThrow(Registries.BIOME));
		Map<ResourceKey<Biome>, Integer> counts = new HashMap<>();
		int plains = 0;
		for (int i = 0; i < 300; i++) {
			for (int j = 0; j < 300; j++) {
				int qx = (i - 150) * 20;
				int qz = (j - 150) * 20;
				Climate.TargetPoint target = sampler.sample(qx, 16, qz);
				if (!vanilla.findValue(target).is(Biomes.PLAINS)) {
					continue;
				}
				plains++;
				counts.merge(lookup.getBiome(target, qx, qz, vanilla).unwrapKey().orElseThrow(), 1, Integer::sum);
			}
		}
		JONE500BiomeAPI.LOGGER.info("[test] weighted region over {} plains samples: {}", plains, counts);
		ctx.assertTrue(plains > (OTHER_WORLDGEN_MODS ? 300 : 2000), Component.literal("too few plains samples: " + plains));
		ctx.assertTrue(counts.getOrDefault(Biomes.PLAINS, 0) == 0, Component.literal("plains left inside the region: " + counts));
		checkShare(ctx, "red plains", counts.getOrDefault(TutorialBiomes.RED_PLAINS, 0), plains, 5.0 / 9.0);
		checkShare(ctx, "blue forest", counts.getOrDefault(TutorialBiomes.BLUE_FOREST, 0), plains, 3.0 / 9.0);
		checkShare(ctx, "orange hills", counts.getOrDefault(TutorialBiomes.ORANGE_HILLS, 0), plains, 1.0 / 9.0);
		ctx.succeed();
	}

	/** World-wide WeightedBiomes with keepOriginal: desert becomes savanna / badlands / stays desert about 1 : 1 : 2. */
	@GameTest(maxTicks = 20)
	public void weightedWorldWideKeepsOriginal(GameTestHelper ctx) {
		RegistryAccess registries = ctx.getLevel().registryAccess();
		Climate.ParameterList<Holder<Biome>> vanilla = registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
				.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).value().parameters();
		Climate.Sampler sampler = RandomState.create(registries, NoiseGeneratorSettings.OVERWORLD, SEED).sampler();
		net.biome.api.region.WeightedBiomes weighted = net.biome.api.region.WeightedBiomes.builder()
				.add(Biomes.SAVANNA, 1)
				.add(Biomes.BADLANDS, 1)
				.keepOriginal(2)
				.size(4)
				.build();
		RegionBiomeLookup lookup = RegionBiomeLookup.create(SEED, 7, 10.0F, List.of(), List.of(),
				List.of(new BiomeRegion.WeightedReplacement(Biomes.DESERT, weighted, 1.0F)), 6, List.of(), SEED, vanilla,
				registries.lookupOrThrow(Registries.BIOME));
		Map<ResourceKey<Biome>, Integer> counts = new HashMap<>();
		int desert = 0;
		for (int i = 0; i < 300; i++) {
			for (int j = 0; j < 300; j++) {
				int qx = (i - 150) * 20;
				int qz = (j - 150) * 20;
				Climate.TargetPoint target = sampler.sample(qx, 16, qz);
				if (!vanilla.findValue(target).is(Biomes.DESERT)) {
					continue;
				}
				desert++;
				counts.merge(lookup.getBiome(target, qx, qz, vanilla).unwrapKey().orElseThrow(), 1, Integer::sum);
			}
		}
		JONE500BiomeAPI.LOGGER.info("[test] weighted world-wide over {} desert samples: {}", desert, counts);
		ctx.assertTrue(desert > (OTHER_WORLDGEN_MODS ? 150 : 1000), Component.literal("too few desert samples: " + desert));
		checkShare(ctx, "savanna", counts.getOrDefault(Biomes.SAVANNA, 0), desert, 0.25);
		checkShare(ctx, "badlands", counts.getOrDefault(Biomes.BADLANDS, 0), desert, 0.25);
		checkShare(ctx, "desert kept", counts.getOrDefault(Biomes.DESERT, 0), desert, 0.5);
		ctx.succeed();
	}

	/** WeightedBiomes in an End region (no climate): end highlands become void meadow / ash barrens about 1 : 3. */
	@GameTest(maxTicks = 20)
	public void weightedEndRegion(GameTestHelper ctx) {
		var biomeLookup = ctx.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		BiomeRegion region = BiomeRegion.builder(TutorialMod.id("weighted_end_test")).end()
				.replace(Biomes.END_HIGHLANDS, net.biome.api.region.WeightedBiomes.builder()
						.add(TutorialBiomes.VOID_MEADOW, 1)
						.add(TutorialBiomes.ASH_BARRENS, 3)
						.size(3)
						.build())
				.build();
		RegionBiomeLookup lookup = RegionBiomeLookup.create(SEED, 7, 0.0F, List.of(region), List.of(), List.of(), 6, List.of(), SEED, null, biomeLookup);
		Holder<Biome> highlands = biomeLookup.getOrThrow(Biomes.END_HIGHLANDS);
		Holder<Biome> midlands = biomeLookup.getOrThrow(Biomes.END_MIDLANDS);
		Map<ResourceKey<Biome>, Integer> counts = new HashMap<>();
		int total = 0;
		for (int i = 0; i < 200; i++) {
			for (int j = 0; j < 200; j++) {
				int qx = (i - 100) * 12;
				int qz = (j - 100) * 12;
				counts.merge(lookup.replaceWithoutClimate(highlands, qx, qz).unwrapKey().orElseThrow(), 1, Integer::sum);
				total++;
				ctx.assertTrue(lookup.replaceWithoutClimate(midlands, qx, qz).is(Biomes.END_MIDLANDS),
						Component.literal("midlands should not be replaced"));
			}
		}
		JONE500BiomeAPI.LOGGER.info("[test] weighted End region: {}", counts);
		checkShare(ctx, "void meadow", counts.getOrDefault(TutorialBiomes.VOID_MEADOW, 0), total, 0.25);
		checkShare(ctx, "ash barrens", counts.getOrDefault(TutorialBiomes.ASH_BARRENS, 0), total, 0.75);
		ctx.succeed();
	}

	/** The tutorial's weighted Nether region on the real Nether: nether wastes become sulfur wastes / bone fields about 3 : 1. */
	@GameTest(maxTicks = 20)
	public void weightedNetherRegionWorks(GameTestHelper ctx) {
		ServerLevel nether = ctx.getLevel().getServer().getLevel(Level.NETHER);
		ctx.assertTrue(nether != null, Component.literal("no Nether level"));
		BiomeSource source = nether.getChunkSource().getGenerator().getBiomeSource();
		RegionBiomeLookup lookup = apiSource(source).jone500$getLookup();
		Climate.Sampler sampler = nether.getChunkSource().randomState().sampler();
		int sulfur = 0;
		int bone = 0;
		int wastesInside = 0;
		int outside = 0;
		for (int i = 0; i < 200; i++) {
			for (int j = 0; j < 200; j++) {
				int qx = (i - 100) * 16;
				int qz = (j - 100) * 16;
				Holder<Biome> biome = source.getNoiseBiome(qx, 16, qz, sampler);
				BiomeRegion region = lookup.getRegionAt(qx, qz);
				boolean inside = region != null && region.getId().equals(Lesson5Nether.WASTES);
				if (biome.is(TutorialBiomes.SULFUR_WASTES) || biome.is(TutorialBiomes.BONE_FIELDS)) {
					if (!inside) outside++;
					if (biome.is(TutorialBiomes.SULFUR_WASTES)) sulfur++;
					else bone++;
				}
				if (inside && biome.is(Biomes.NETHER_WASTES)) wastesInside++;
			}
		}
		JONE500BiomeAPI.LOGGER.info("[test] weighted nether: sulfur={} bone={} nether wastes inside={} weighted outside={}", sulfur, bone, wastesInside, outside);
		ctx.assertTrue(sulfur > 0 && bone > 0, Component.literal("weighted Nether biomes missing: sulfur " + sulfur + ", bone " + bone));
		ctx.assertTrue(outside == 0, Component.literal("weighted Nether biomes outside their region " + outside + " times"));
		ctx.assertTrue(wastesInside == 0, Component.literal("nether wastes left inside the region " + wastesInside + " times"));
		double share = (double) sulfur / (sulfur + bone);
		ctx.assertTrue(Math.abs(share - 0.75) < 0.1, Component.literal("sulfur share " + share + ", expected about 0.75"));
		ctx.assertTrue(source.possibleBiomes().stream().anyMatch(b -> b.is(TutorialBiomes.BONE_FIELDS)),
				Component.literal("bone fields is not a possible Nether biome"));
		ctx.succeed();
	}

	/** ClimatePoints makes one point per combination, with vanilla's values and SURFACE depth by default. */
	@GameTest(maxTicks = 20)
	public void climatePointsBuildEveryCombination(GameTestHelper ctx) {
		List<Climate.ParameterPoint> points = ClimatePoints.builder()
				.temperature(Temperature.NEUTRAL, Temperature.WARM)
				.continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
				.erosion(Erosion.EROSION_2, Erosion.EROSION_3, Erosion.EROSION_4)
				.terrain(Terrain.MID, Terrain.HIGH)
				.build();
		// 2 temperatures x 2 continentalness x 3 erosion x (4 + 4 weirdness slices) x 2 surface depths
		ctx.assertTrue(points.size() == 2 * 2 * 3 * 8 * 2, Component.literal("expected 192 points, got " + points.size()));

		Climate.ParameterPoint first = points.get(0);
		ctx.assertTrue(first.temperature().equals(Climate.Parameter.span(-0.15F, 0.2F)), Component.literal("NEUTRAL temperature range is wrong"));
		ctx.assertTrue(first.humidity().equals(Climate.Parameter.span(-1.0F, 1.0F)), Component.literal("unset humidity should be the full range"));
		ctx.assertTrue(first.depth().equals(Climate.Parameter.point(0.0F)), Component.literal("default depth should be SURFACE (0 and 1)"));
		ctx.assertTrue(points.stream().anyMatch(p -> p.depth().equals(Climate.Parameter.point(1.0F))),
				Component.literal("SURFACE depth should also add the depth 1 point, like vanilla"));
		ctx.assertTrue(Temperature.between(Temperature.COLD, Temperature.WARM).equals(Climate.Parameter.span(-0.45F, 0.55F)),
				Component.literal("Temperature.between is wrong"));

		// Number ranges work the same as named ones.
		List<Climate.ParameterPoint> numbers = ClimatePoints.builder().humidity(-0.5F, 0.5F).depth(0.0F, 1.0F).build();
		ctx.assertTrue(numbers.size() == 1, Component.literal("number ranges should give 1 point, got " + numbers.size()));
		ctx.assertTrue(numbers.get(0).humidity().equals(Climate.Parameter.span(-0.5F, 0.5F)), Component.literal("humidity(-0.5, 0.5) is wrong"));
		ctx.assertTrue(numbers.get(0).depth().equals(Climate.Parameter.span(0.0F, 1.0F)), Component.literal("depth(0, 1) is wrong"));

		// VanillaBiomeTags knows vanilla's tags.
		ctx.assertTrue(VanillaBiomeTags.of(Biomes.PLAINS).contains(BiomeTags.HAS_VILLAGE_PLAINS), Component.literal("plains should have village_plains"));
		ctx.assertTrue(VanillaBiomeTags.of(Biomes.TAIGA).contains(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS),
				Component.literal("taiga should spawn cold farm animals"));
		ctx.succeed();
	}

	private static void checkShare(GameTestHelper ctx, String name, int count, int total, double expected) {
		double share = (double) count / total;
		ctx.assertTrue(Math.abs(share - expected) < 0.03,
				Component.literal(name + ": share " + String.format("%.3f", share) + ", expected about " + String.format("%.3f", expected)));
	}

	/** A normal Overworld climate (vanilla preset + noise) with the registered regions on top. */
	private record World(Climate.Sampler sampler, Climate.ParameterList<Holder<Biome>> vanilla, RegionBiomeLookup lookup) {
		static World create(GameTestHelper ctx, long seed) {
			RegistryAccess registries = ctx.getLevel().registryAccess();
			Climate.ParameterList<Holder<Biome>> vanilla = registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
					.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).value().parameters();
			RandomState randomState = RandomState.create(registries, NoiseGeneratorSettings.OVERWORLD, seed);
			RegionBiomeLookup lookup = RegionBiomeLookup.create(seed, BiomeRegions.getRegionSize(), BiomeRegions.getVanillaWeight(),
					new ArrayList<>(BiomeRegions.getAll(RegionDimension.OVERWORLD)), BiomeReplacements.getAll(), BiomeReplacements.getPatchSize(), BiomeReplacements.getAllWholeBiome(), seed,
					vanilla, registries.lookupOrThrow(Registries.BIOME));
			return new World(randomState.sampler(), vanilla, lookup);
		}

		Holder<Biome> biomeAt(int qx, int qy, int qz) {
			return lookup.getBiome(sampler.sample(qx, qy, qz), qx, qz, vanilla);
		}
	}
}
