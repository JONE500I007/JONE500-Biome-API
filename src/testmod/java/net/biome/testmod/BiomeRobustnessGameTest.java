package net.biome.testmod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import net.biome.api.JONE500BiomeAPI;
import net.biome.api.climate.ClimatePoints;
import net.biome.api.impl.RegionBiomeLookup;
import net.biome.api.impl.RegionLayout;
import net.biome.api.region.BiomeRegion;
import net.biome.api.region.BiomeRegions;
import net.biome.api.region.BiomeReplacements;
import net.biome.api.region.RegionDimension;
import net.biome.api.surface.BiomeSurfaceRules;
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
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Tries to break the API: wrong use, missing biome files, extreme coordinates, many threads, speed.
 * Run with: gradlew runGameTest
 */
public class BiomeRobustnessGameTest {
	private static final boolean OTHER_WORLDGEN_MODS = !System.getProperty("biome_test.compat", "").isEmpty();

	private static final long SEED = 987654321L;

	/** Every wrong use must fail early with a clear message (not crash later in world generation). */
	@GameTest(maxTicks = 20)
	public void wrongUseFailsWithClearMessages(GameTestHelper ctx) {
		ResourceKey<Biome> mine = BiomeTestMod.RED_PLAINS;
		List<String> problems = new ArrayList<>();

		expectError(problems, "chance 0", () -> BiomeRegion.builder(BiomeTestMod.id("x")).replace(Biomes.PLAINS, mine, 0.0F));
		expectError(problems, "chance above 1", () -> BiomeRegion.builder(BiomeTestMod.id("x")).replace(Biomes.PLAINS, mine, 1.5F));
		expectError(problems, "chance NaN", () -> BiomeRegion.builder(BiomeTestMod.id("x")).replace(Biomes.PLAINS, mine, Float.NaN));
		expectError(problems, "weight 0", () -> BiomeRegion.builder(BiomeTestMod.id("x")).weight(0.0F));
		expectError(problems, "weight negative", () -> BiomeRegion.builder(BiomeTestMod.id("x")).weight(-2.0F));
		expectError(problems, "weight infinite", () -> BiomeRegion.builder(BiomeTestMod.id("x")).weight(Float.POSITIVE_INFINITY));
		expectError(problems, "empty region", () -> BiomeRegion.builder(BiomeTestMod.id("x")).build());
		expectError(problems, "same vanilla biome replaced twice",
				() -> BiomeRegion.builder(BiomeTestMod.id("x")).replace(Biomes.PLAINS, mine).replace(Biomes.PLAINS, BiomeTestMod.BLUE_FOREST));
		expectError(problems, "exclusive with 1 biome", () -> BiomeRegion.builder(BiomeTestMod.id("x")).exclusive(mine));
		expectError(problems, "exclusive biome not in the region",
				() -> BiomeRegion.builder(BiomeTestMod.id("x")).replace(Biomes.PLAINS, mine).exclusive(mine, BiomeTestMod.BLUE_FOREST).build());
		expectError(problems, "null id", () -> BiomeRegion.builder(null));
		expectError(problems, "null biome", () -> BiomeRegion.builder(BiomeTestMod.id("x")).replace(Biomes.PLAINS, (ResourceKey<Biome>) null));
		expectError(problems, "weighted: no biomes", () -> net.biome.api.region.WeightedBiomes.builder().build());
		expectError(problems, "weighted: weight 0", () -> net.biome.api.region.WeightedBiomes.builder().add(mine, 0.0F));
		expectError(problems, "weighted: same biome twice", () -> net.biome.api.region.WeightedBiomes.builder().add(mine, 1).add(mine, 2));
		expectError(problems, "weighted: size too big", () -> net.biome.api.region.WeightedBiomes.builder().size(11));
		expectError(problems, "weighted: keepOriginal negative", () -> net.biome.api.region.WeightedBiomes.builder().keepOriginal(-1.0F));
		expectError(problems, "weighted + single replace of the same biome", () -> BiomeRegion.builder(BiomeTestMod.id("x")).replace(Biomes.PLAINS, mine)
				.replace(Biomes.PLAINS, net.biome.api.region.WeightedBiomes.builder().add(BiomeTestMod.BLUE_FOREST, 1).build()));
		expectError(problems, "exclusive with a weighted biome", () -> BiomeRegion.builder(BiomeTestMod.id("x")).replace(Biomes.FOREST, mine)
				.replace(Biomes.PLAINS, net.biome.api.region.WeightedBiomes.builder().add(BiomeTestMod.BLUE_FOREST, 1).build())
				.exclusive(mine, BiomeTestMod.BLUE_FOREST).build());
		expectError(problems, "weighted replacement registered too late", () -> BiomeReplacements.replace(Biomes.PLAINS,
				net.biome.api.region.WeightedBiomes.builder().add(mine, 1).build()));
		expectError(problems, "empty climate list", () -> BiomeRegion.builder(BiomeTestMod.id("x")).addBiome(List.of(), mine, 1.0F));
		expectError(problems, "climate range min > max", () -> ClimatePoints.builder().humidity(0.5F, -0.5F));
		expectError(problems, "climate with no values", () -> ClimatePoints.builder().temperature(new Climate.Parameter[0]));
		expectError(problems, "region size too small", () -> BiomeRegions.setRegionSize(2));
		expectError(problems, "vanilla weight negative", () -> BiomeRegions.setVanillaWeight(-1.0F));
		expectError(problems, "replacement patch size too big", () -> BiomeReplacements.setPatchSize(20));
		// The world is already running: registering now is too late and must say so.
		expectError(problems, "region registered too late",
				() -> BiomeRegion.builder(BiomeTestMod.id("late")).replace(Biomes.PLAINS, mine).register());
		expectError(problems, "replacement registered too late", () -> BiomeReplacements.replace(Biomes.PLAINS, mine, 0.5F));
		expectError(problems, "whole replacement registered too late", () -> BiomeReplacements.replaceWholeBiome(Biomes.PLAINS, mine, 0.5F));
		expectError(problems, "duplicate surface rule id",
				() -> BiomeSurfaceRules.addOverworld(BiomeTestMod.id("red_plains_sand"), SurfaceRules.state(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())));

		if (!problems.isEmpty()) {
			ctx.fail(Component.literal("wrong use was accepted: " + problems));
		}
		ctx.succeed();
	}

	/** A region that names a biome without a JSON file: logged and skipped, the rest keeps working. */
	@GameTest(maxTicks = 20)
	public void missingBiomeFileIsSkipped(GameTestHelper ctx) {
		ResourceKey<Biome> missing = ResourceKey.create(Registries.BIOME, BiomeTestMod.id("does_not_exist"));
		BiomeRegion region = BiomeRegion.builder(BiomeTestMod.id("broken"))
				.replace(Biomes.PLAINS, missing)
				.replace(Biomes.FOREST, BiomeTestMod.BLUE_FOREST)
				.addBiome(ClimatePoints.builder().temperature(-0.1F, 0.1F), missing)
				.build();
		Env env = Env.create(ctx, List.of(region), 0.0F);
		int blue = 0;
		for (int i = 0; i < 80; i++) {
			for (int j = 0; j < 80; j++) {
				Holder<Biome> biome = env.biomeAt((i - 40) * 64, 16, (j - 40) * 64);
				ctx.assertTrue(biome.isBound(), Component.literal("got an unbound biome"));
				if (biome.is(BiomeTestMod.BLUE_FOREST)) blue++;
			}
		}
		ctx.assertTrue(blue > 0, Component.literal("the working replacement stopped working because of the missing biome"));
		ctx.succeed();
	}

	/** Far lands: coordinates near the world border and beyond must not throw and stay deterministic. */
	@GameTest(maxTicks = 20)
	public void extremeCoordinates(GameTestHelper ctx) {
		Env env = Env.create(ctx, BiomeRegions.getAll(RegionDimension.OVERWORLD), BiomeRegions.getVanillaWeight());
		int border = 30_000_000 / 4; // world border in quarts
		int[] coords = {0, 1, -1, border, -border, border - 1, -border + 1, Integer.MAX_VALUE / 2, Integer.MIN_VALUE / 2,
				Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1};
		for (int x : coords) {
			for (int z : coords) {
				Holder<Biome> a = env.biomeAt(x, 16, z);
				RegionLayout.Cell cellA = env.lookup.getLayout().cellAt(x, z);
				RegionLayout.Cell cellB = env.lookup.getLayout().cellAt(x, z + 0); // cache hit path
				ctx.assertTrue(cellA.equals(cellB), Component.literal("cell changed at " + x + "," + z));
				ctx.assertTrue(a.equals(env.biomeAt(x, 16, z)), Component.literal("biome changed at " + x + "," + z));
				for (int y : new int[] {-16, 0, 16, 80}) {
					env.biomeAt(x, y, z); // below bedrock / above build height must also work
				}
			}
		}
		ctx.succeed();
	}

	/** Chunk generation runs on many threads: results must be the same as on one thread. */
	@GameTest(maxTicks = 20)
	public void manyThreadsGiveSameResults(GameTestHelper ctx) {
		Env env = Env.create(ctx, BiomeRegions.getAll(RegionDimension.OVERWORLD), BiomeRegions.getVanillaWeight());
		Random random = new Random(5);
		int n = 20_000;
		int[] xs = new int[n];
		int[] zs = new int[n];
		Holder<Biome>[] expected = new Holder[n];
		for (int i = 0; i < n; i++) {
			xs[i] = random.nextInt(200_000) - 100_000;
			zs[i] = random.nextInt(200_000) - 100_000;
			expected[i] = env.biomeAt(xs[i], 16, zs[i]);
		}
		ConcurrentLinkedQueue<String> wrong = new ConcurrentLinkedQueue<>();
		// Same points again, in random order, from many threads at once (also hits the per-thread cell cache).
		IntStream.range(0, n * 4).parallel().forEach(k -> {
			int i = (int) ((k * 2654435761L) % n);
			Holder<Biome> again = env.biomeAt(xs[i], 16, zs[i]);
			if (!again.equals(expected[i])) {
				wrong.add(xs[i] + "," + zs[i] + " (" + (env.lookup.isVanillaCell(xs[i], zs[i]) ? "vanilla cell" : "region cell") + ": "
						+ expected[i].getRegisteredName() + " vs " + again.getRegisteredName() + ")");
			}
		});
		if (!wrong.isEmpty()) JONE500BiomeAPI.LOGGER.warn("[test] thread differences: {}", wrong);
		// Same check for vanilla's own climate table only (no Biome API): ties between equally close points (another mod
		// can add such points) are picked differently per thread by vanilla's search cache too.
		Holder<Biome>[] vanillaExpected = new Holder[n];
		for (int i = 0; i < n; i++) {
			vanillaExpected[i] = env.vanilla.findValue(env.sampler.sample(xs[i], 16, zs[i]));
		}
		java.util.concurrent.atomic.AtomicInteger vanillaWrong = new java.util.concurrent.atomic.AtomicInteger();
		IntStream.range(0, n * 4).parallel().forEach(k -> {
			int i = (int) ((k * 2654435761L) % n);
			if (!env.vanilla.findValue(env.sampler.sample(xs[i], 16, zs[i])).equals(vanillaExpected[i])) vanillaWrong.incrementAndGet();
		});
		JONE500BiomeAPI.LOGGER.info("[test] thread check: {} differences with Biome API, {} with vanilla's table alone", wrong.size(), vanillaWrong.get());
		if (OTHER_WORLDGEN_MODS && vanillaWrong.get() > 0 && wrong.size() <= n / 1000) {
			// Other mods (Lithostitched) add climate points that tie exactly; vanilla itself is not stable there.
			ctx.succeed();
			return;
		}
		ctx.assertTrue(wrong.isEmpty(), Component.literal(wrong.size() + " different results on other threads, e.g. " + wrong.peek()));
		ctx.succeed();
	}

	/** How much slower is a lookup with regions than vanilla? (logged; fails only if it is way slower) */
	@GameTest(maxTicks = 20)
	public void speedComparedToVanilla(GameTestHelper ctx) {
		Env env = Env.create(ctx, BiomeRegions.getAll(RegionDimension.OVERWORLD), BiomeRegions.getVanillaWeight());
		int n = 200_000;
		Climate.TargetPoint[] targets = new Climate.TargetPoint[1000];
		for (int i = 0; i < targets.length; i++) {
			targets[i] = env.sampler.sample(i * 37, 16, i * 91);
		}
		// warm up
		long sink = 0;
		for (int i = 0; i < n; i++) {
			sink += env.vanilla.findValue(targets[i % 1000]).hashCode();
			sink += env.lookup.getBiome(targets[i % 1000], i * 3, i * 7, env.vanilla).hashCode();
		}
		long t0 = System.nanoTime();
		for (int i = 0; i < n; i++) {
			sink += env.vanilla.findValue(targets[i % 1000]).hashCode();
		}
		long t1 = System.nanoTime();
		for (int i = 0; i < n; i++) {
			sink += env.lookup.getBiome(targets[i % 1000], i * 3, i * 7, env.vanilla).hashCode();
		}
		long t2 = System.nanoTime();
		double vanillaNs = (t1 - t0) / (double) n;
		double apiNs = (t2 - t1) / (double) n;
		// Includes the climate sampling cost that both share in the game:
		long t3 = System.nanoTime();
		for (int i = 0; i < 20_000; i++) {
			sink += env.sampler.sample(i * 3, 16, i * 7).hashCode();
		}
		double sampleNs = (System.nanoTime() - t3) / 20_000.0;
		JONE500BiomeAPI.LOGGER.info("[test] speed: vanilla table lookup {} ns, with regions {} ns, climate sampling {} ns (ignore: {})",
				Math.round(vanillaNs), Math.round(apiNs), Math.round(sampleNs), sink & 1);
		ctx.assertTrue(apiNs < vanillaNs * 6 + 2000, Component.literal("region lookup is much slower than vanilla: " + apiNs + " ns vs " + vanillaNs));
		ctx.succeed();
	}

	private static void expectError(List<String> problems, String name, Runnable action) {
		try {
			action.run();
			problems.add(name);
		} catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
			if (e.getMessage() == null || e.getMessage().isBlank()) {
				problems.add(name + " (error without a message)");
			}
		}
	}

	private record Env(Climate.Sampler sampler, Climate.ParameterList<Holder<Biome>> vanilla, RegionBiomeLookup lookup) {
		static Env create(GameTestHelper ctx, List<BiomeRegion> regions, float vanillaWeight) {
			RegistryAccess registries = ctx.getLevel().registryAccess();
			Climate.ParameterList<Holder<Biome>> vanilla = registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
					.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).value().parameters();
			RandomState randomState = RandomState.create(registries, NoiseGeneratorSettings.OVERWORLD, SEED);
			RegionBiomeLookup lookup = RegionBiomeLookup.create(SEED, BiomeRegions.getRegionSize(), vanillaWeight, regions,
					BiomeReplacements.getAll(), BiomeReplacements.getPatchSize(), BiomeReplacements.getAllWholeBiome(), SEED,
					vanilla, registries.lookupOrThrow(Registries.BIOME));
			return new Env(randomState.sampler(), vanilla, lookup);
		}

		Holder<Biome> biomeAt(int qx, int qy, int qz) {
			return lookup.getBiome(sampler.sample(qx, qy, qz), qx, qz, vanilla);
		}
	}
}
