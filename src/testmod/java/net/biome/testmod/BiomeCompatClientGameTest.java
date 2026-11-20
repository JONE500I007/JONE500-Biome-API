package net.biome.testmod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import com.mojang.datafixers.util.Pair;

import net.biome.api.JONE500BiomeAPI;
import net.biome.api.impl.BiomeApiBiomeSource;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Compatibility test with other mods. Does nothing unless the game was started with {@code -Pcompat=<group>}
 * (see build.gradle). Checks, in singleplayer and on a dedicated server:
 * <ul>
 *   <li>our biomes still appear next to the other mods' biomes (Overworld, Nether, End)</li>
 *   <li>biomes added with TerraBlender and Biolith appear too (when those mods are installed)</li>
 *   <li>our surface rule, TerraBlender's surface rule and vanilla's surface rules all still work</li>
 *   <li>chunks generate without errors; the client sees the same biomes as the server</li>
 * </ul>
 * All results are collected and printed as one table; the test fails at the end if anything failed.
 */
public class BiomeCompatClientGameTest implements FabricClientGameTest {
	public static final String COMPAT = System.getProperty("biome_test.compat", "");
	private static final String[] WATCHED_MODS = {"terrablender", "biolith", "biomesoplenty", "terralith", "lithostitched",
			"tectonic", "regions_unexplored", "c2me", "lithium", "sodium", "ferritecore", "chunky", "distanthorizons"};

	private final List<String> results = new ArrayList<>();
	private int failures;

	public static boolean active() {
		return !COMPAT.isEmpty();
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!active()) {
			return;
		}
		List<String> loaded = new ArrayList<>();
		for (String mod : WATCHED_MODS) {
			FabricLoader.getInstance().getModContainer(mod).ifPresent(m -> loaded.add(mod + " " + m.getMetadata().getVersion().getFriendlyString()));
		}
		info("compat group '" + COMPAT + "', mods: " + loaded);

		try {
			runChecks(context);
		} finally {
			String report = "[compat-test] ===== results for '" + COMPAT + "' =====\n" + String.join("\n", results) + "\n";
			JONE500BiomeAPI.LOGGER.info(report);
			// The game may exit before the log is flushed, so the results are also written to a file.
			try {
				java.nio.file.Files.writeString(FabricLoader.getInstance().getGameDir().resolve("compat-results-" + COMPAT.replace(',', '_') + ".txt"), report);
			} catch (java.io.IOException e) {
				JONE500BiomeAPI.LOGGER.warn("could not write compat results", e);
			}
		}
		if (failures > 0) {
			throw new AssertionError(failures + " compatibility check(s) failed for '" + COMPAT + "', see [compat-test] in the log");
		}
	}

	private void runChecks(ClientGameTestContext context) {
		// ---- singleplayer ----
		try (TestSingleplayerContext world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(s -> s.setSeed("424242"))
				.create()) {
			world.getConnection().waitForChunksRender();
			BlockPos red = world.getServer().computeOnServer(this::checkServer);
			if (red != null) {
				world.getServer().runCommand("gamemode spectator @a");
				world.getServer().runCommand("tp @a " + red.getX() + " " + (red.getY() + 30) + " " + red.getZ() + " 0 60");
				context.waitTicks(40);
				world.getConnection().waitForChunksRender();
				screenshot(context, "red_plains");
			}
			if (FabricLoader.getInstance().isModLoaded("chunky")) {
				runChunky(context, world);
			}
		}

		// ---- dedicated server ----
		if (eulaAccepted()) {
			try (TestDedicatedServerContext server = context.worldBuilder()
					.setUseConsistentSettings(false)
					.adjustSettings(s -> s.setSeed("424242"))
					.createServer()) {
				BlockPos red = server.computeOnServer(s -> surfaceOf(s.overworld(), BiomeTestMod.RED_PLAINS));
				try (TestDedicatedServerConnection connection = server.connect()) {
					server.runCommand("op @a");
					server.runCommand("gamemode spectator @a");
					List<ResourceKey<Biome>> compare = new ArrayList<>(List.of(BiomeTestMod.RED_PLAINS));
					if (FabricLoader.getInstance().isModLoaded("terrablender")) compare.add(BiomeTestMod.TB_MEADOW);
					if (FabricLoader.getInstance().isModLoaded("biolith")) compare.add(BiomeTestMod.BIOLITH_MEADOW);
					for (ResourceKey<Biome> biome : compare) {
						BlockPos pos = biome == BiomeTestMod.RED_PLAINS ? red : server.computeOnServer(s -> surfaceOf(s.overworld(), biome));
						if (pos == null) {
							fail("dedicated server: " + biome.identifier() + " not found");
							continue;
						}
						server.runCommand("tp @a " + pos.getX() + " " + (pos.getY() + 30) + " " + pos.getZ() + " 0 60");
						context.waitTicks(40);
						connection.waitForChunksRender();
						String onServer = server.computeOnServer(s -> s.overworld().getBiome(pos).getRegisteredName());
						String onClient = context.computeOnClient(mc -> mc.level.getBiome(pos).getRegisteredName());
						expect(onServer.equals(onClient) && onServer.equals(biome.identifier().toString()),
								"dedicated server: at " + pos.toShortString() + " server " + onServer + ", client " + onClient);
					}
					screenshot(context, "dedicated_server");
				}
			}
		} else {
			info("dedicated server skipped: EULA not accepted");
		}
	}

	/** Runs on the server thread. Returns where red plains is (for the screenshot), or null. */
	private BlockPos checkServer(MinecraftServer server) {
		long start = System.currentTimeMillis();
		ServerLevel overworld = server.overworld();
		BiomeSource source = overworld.getChunkSource().getGenerator().getBiomeSource();
		boolean hasRegions = BiomeServerGameTest.apiSource(source).jone500$getLookup() != null;
		expect(hasRegions, "overworld: Biome API regions active (biome source " + source.getClass().getSimpleName() + ")");

		// 1. How much of the world each mod's biomes get (12 x 12 km, surface height)
		Map<String, Integer> byNamespace = new TreeMap<>();
		Map<String, Integer> ours = new TreeMap<>();
		int samples = sample(overworld, 6000, 96, 64, byNamespace, ours);
		info("overworld share: " + percent(byNamespace, samples) + "; ours: " + percent(ours, samples));
		for (ResourceKey<Biome> biome : List.of(BiomeTestMod.RED_PLAINS, BiomeTestMod.BLUE_FOREST, BiomeTestMod.ORANGE_HILLS)) {
			expect(ours.getOrDefault(biome.identifier().getPath(), 0) > 0, "overworld: " + biome.identifier() + " appears");
		}
		if (FabricLoader.getInstance().isModLoaded("terrablender")) {
			expect(ours.getOrDefault("tb_meadow", 0) > 0, "overworld: TerraBlender biome tb_meadow appears");
		}
		if (FabricLoader.getInstance().isModLoaded("biolith")) {
			expect(ours.getOrDefault("biolith_meadow", 0) > 0, "overworld: Biolith biome biolith_meadow appears");
		}
		expect(byNamespace.getOrDefault("minecraft", 0) > 0, "overworld: vanilla biomes still appear");

		// 2. Surface rules: ours, TerraBlender's and vanilla's (desert sand) must all still work
		BlockPos red = surfaceOf(overworld, BiomeTestMod.RED_PLAINS);
		surfaceRule(overworld, red, BiomeTestMod.RED_PLAINS, Blocks.RED_SAND, "our surface rule (red plains -> red sand)");
		if (FabricLoader.getInstance().isModLoaded("terrablender")) {
			surfaceRule(overworld, surfaceOf(overworld, BiomeTestMod.TB_MEADOW), BiomeTestMod.TB_MEADOW, Blocks.DYED_TERRACOTTA.blue(),
					"TerraBlender surface rule (tb_meadow -> blue terracotta)");
		}
		BlockPos desert = surfaceOf(overworld, Biomes.DESERT);
		if (desert != null) {
			surfaceRule(overworld, desert, Biomes.DESERT, Blocks.SAND, "vanilla surface rule (desert -> sand)");
		} else {
			info("no desert within 6400 blocks (another mod may have replaced it)");
		}

		// 3. Nether
		ServerLevel nether = server.getLevel(Level.NETHER);
		Map<String, Integer> netherNs = new TreeMap<>();
		Map<String, Integer> netherOurs = new TreeMap<>();
		int netherSamples = sample(nether, 3000, 128, 64, netherNs, netherOurs);
		info("nether share: " + percent(netherNs, netherSamples) + "; ours: " + percent(netherOurs, netherSamples));
		expect(netherOurs.getOrDefault("ember_forest", 0) > 0, "nether: ember_forest appears");
		BlockPos ember = find(nether, BiomeTestMod.EMBER_FOREST, 64);
		if (ember != null) {
			int magma = 0;
			for (BlockPos pos : BlockPos.betweenClosed(ember.offset(-24, -32, -24), ember.offset(24, 32, 24))) {
				if (nether.getBlockState(pos).is(Blocks.MAGMA_BLOCK) && nether.getBiome(pos).is(BiomeTestMod.EMBER_FOREST)) {
					magma++;
				}
			}
			expect(magma > 10, "nether: ember_forest surface rule (" + magma + " magma blocks)");
		}

		// 4. End
		ServerLevel end = server.getLevel(Level.END);
		Map<String, Integer> endNs = new TreeMap<>();
		Map<String, Integer> endOurs = new TreeMap<>();
		int endSamples = sample(end, 4000, 128, 64, endNs, endOurs);
		info("end share: " + percent(endNs, endSamples) + "; ours: " + percent(endOurs, endSamples));
		expect(endOurs.getOrDefault("void_meadow", 0) > 0, "end: void_meadow appears");
		int smallIslands = 0;
		for (int x = -4000; x <= 4000; x += 128) {
			for (int z = -4000; z <= 4000; z += 128) {
				if (biomeAt(end, x, 64, z).is(Biomes.SMALL_END_ISLANDS)) smallIslands++;
			}
		}
		expect(smallIslands == 0, "end: small_end_islands fully replaced (" + smallIslands + " left)");
		BlockPos ash = find(end, BiomeTestMod.ASH_BARRENS, 64);
		if (ash != null) {
			int basalt = 0;
			for (BlockPos pos : BlockPos.betweenClosed(ash.offset(-24, -40, -24), ash.offset(24, 40, 24))) {
				if (end.getBlockState(pos).is(Blocks.SMOOTH_BASALT) && end.getBiome(pos).is(BiomeTestMod.ASH_BARRENS)) {
					basalt++;
				}
			}
			expect(basalt > 10, "end: ash_barrens surface rule (" + basalt + " smooth basalt blocks)");
		} else {
			info("end: no ash_barrens found near the sample point");
		}

		// 5. Generate some far-away chunks in every dimension (features, structures, surface)
		int chunks = 0;
		for (ServerLevel level : List.of(overworld, nether, end)) {
			for (int cx = 0; cx < 6; cx++) {
				for (int cz = 0; cz < 6; cz++) {
					level.getChunk(1200 + cx, -900 + cz);
					chunks++;
				}
			}
		}
		info("generated " + chunks + " extra chunks, server checks took " + (System.currentTimeMillis() - start) / 1000 + " s");
		return red;
	}

	private void surfaceRule(ServerLevel level, BlockPos center, ResourceKey<Biome> biome, Block expected, String what) {
		if (center == null) {
			fail(what + ": " + biome.identifier() + " not found");
			return;
		}
		int columns = 0;
		int hits = 0;
		Map<String, Integer> tops = new TreeMap<>();
		for (int dx = -40; dx <= 40; dx += 2) {
			for (int dz = -40; dz <= 40; dz += 2) {
				BlockPos column = center.offset(dx, 0, dz);
				level.getChunk(column.getX() >> 4, column.getZ() >> 4);
				BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();
				if (!level.getBiome(top).is(biome) || !level.getFluidState(top).isEmpty()) {
					continue;
				}
				columns++;
				Block block = level.getBlockState(top).getBlock();
				tops.merge(block.getDescriptionId().replace("block.minecraft.", ""), 1, Integer::sum);
				if (block == expected) hits++;
			}
		}
		expect(columns > 0 && hits * 2 > columns, what + ": " + hits + "/" + columns + " dry columns " + tops);
	}

	/** Samples biomes on a grid at height y; counts per namespace and, for biome_test, per biome. */
	private static int sample(ServerLevel level, int radius, int step, int y, Map<String, Integer> byNamespace, Map<String, Integer> ours) {
		int count = 0;
		for (int x = -radius; x <= radius; x += step) {
			for (int z = -radius; z <= radius; z += step) {
				Holder<Biome> biome = biomeAt(level, x, y, z);
				String id = biome.getRegisteredName();
				String ns = id.substring(0, id.indexOf(':'));
				byNamespace.merge(ns, 1, Integer::sum);
				if (ns.equals(BiomeTestMod.MOD_ID)) {
					ours.merge(id.substring(ns.length() + 1), 1, Integer::sum);
				}
				count++;
			}
		}
		return count;
	}

	private static Holder<Biome> biomeAt(ServerLevel level, int x, int y, int z) {
		Climate.Sampler sampler = TestWorldgen.sampler(level.getChunkSource().randomState());
		return TestWorldgen.biome(level.getChunkSource().getGenerator().getBiomeSource(), x >> 2, y >> 2, z >> 2, sampler);
	}

	private static BlockPos find(ServerLevel level, ResourceKey<Biome> biome, int y) {
		Predicate<Holder<Biome>> test = b -> b.is(biome);
		Pair<BlockPos, Holder<Biome>> found = level.getChunkSource().getGenerator().getBiomeSource().findClosestBiome3d(
				new BlockPos(0, y, 0), 6400, 32, 64, test, level.getChunkSource().randomState(), level);
		return found == null ? null : found.getFirst();
	}

	private static BlockPos surfaceOf(ServerLevel level, ResourceKey<Biome> biome) {
		BlockPos found = find(level, biome, 64);
		if (found == null) {
			return null;
		}
		level.getChunk(found.getX() >> 4, found.getZ() >> 4);
		return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found);
	}

	/** Lets Chunky pre-generate a 1000 x 1000 block area for 30 seconds (many chunks on many threads). */
	private void runChunky(ClientGameTestContext context, TestSingleplayerContext world) {
		int before = world.getServer().computeOnServer(s -> s.overworld().getChunkSource().getLoadedChunksCount());
		world.getServer().runCommand("chunky world minecraft:overworld");
		world.getServer().runCommand("chunky center 3000 3000");
		world.getServer().runCommand("chunky radius 500");
		world.getServer().runCommand("chunky start");
		context.waitTicks(600);
		world.getServer().runCommand("chunky cancel");
		context.waitTicks(20);
		boolean alive = world.getServer().computeOnServer(s -> s.isRunning());
		expect(alive, "chunky: server still running after 30 s of pre-generation (loaded chunks before: " + before + ")");
	}

	private static String percent(Map<String, Integer> counts, int total) {
		StringBuilder sb = new StringBuilder();
		counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).forEach(e -> {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(e.getKey()).append(' ').append(String.format("%.1f%%", 100.0 * e.getValue() / total));
		});
		return sb.toString();
	}

	private void expect(boolean ok, String message) {
		if (ok) {
			results.add("  PASS  " + message);
		} else {
			fail(message);
		}
	}

	private void fail(String message) {
		failures++;
		results.add("  FAIL  " + message);
	}

	/** Some renderer mods (Sodium, Distant Horizons) cannot render the test client's screenshot frame; that is not a world-gen problem. */
	private void screenshot(ClientGameTestContext context, String name) {
		try {
			context.takeScreenshot("compat_" + COMPAT.replace(',', '_') + "_" + name);
		} catch (RuntimeException e) {
			info("screenshot '" + name + "' skipped: " + e);
		}
	}

	private void info(String message) {
		results.add("  info  " + message);
	}

	private static boolean eulaAccepted() {
		try {
			return java.nio.file.Files.readString(java.nio.file.Path.of("eula.txt")).contains("eula=true");
		} catch (java.io.IOException e) {
			return false;
		}
	}
}
