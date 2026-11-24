package net.biome.tutorial.test;

import net.biome.tutorial.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.datafixers.util.Pair;

import net.biome.api.JONE500BiomeAPI;
import net.biome.api.impl.BiomeApiBiomeSource;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Real world generation tests (full chunks with features, structures and surface rules):
 * <ol>
 *   <li>Overworld, Nether and End chunks generate without errors; surface rules and End swaps are visible</li>
 *   <li>save, close, reopen: the same biomes everywhere</li>
 *   <li>other world types: large biomes, amplified, superflat, single biome</li>
 *   <li>dedicated server: the client sees the same biome as the server</li>
 * </ol>
 * Run with: gradlew runClientGameTest
 */
public class BiomeWorldGenClientGameTest implements FabricClientGameTest {
	private static final int AREA = 3; // chunks in each direction -> 7 x 7 chunks per area

	@Override
	public void runTest(ClientGameTestContext context) {
		if (BiomeCompatClientGameTest.active()) {
			return; // compatibility run: only BiomeCompatClientGameTest
		}
		TestWorldSave save;
		Map<BlockPos, String> remembered;

		// ---- 1. generate real chunks in all three dimensions ----
		try (TestSingleplayerContext world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(s -> s.setSeed("424242"))
				.create()) {
			world.getClientLevel().waitForChunksRender();
			remembered = world.getServer().computeOnServer(BiomeWorldGenClientGameTest::generateEverywhere);
			save = world.getWorldSave();
		}

		// ---- 2. reopen the same world: same biomes ----
		try (TestSingleplayerContext world = save.open()) {
			world.getClientLevel().waitForChunksRender();
			world.getServer().runOnServer(server -> {
				for (Map.Entry<BlockPos, String> entry : remembered.entrySet()) {
					ServerLevel level = levelFor(server, entry.getKey());
					BlockPos pos = unpack(entry.getKey());
					String now = level.getBiome(pos).getRegisteredName();
					check(now.equals(entry.getValue()), "after reopening, " + pos + " is " + now + " instead of " + entry.getValue());
					// Also freshly computed (not only what the chunk saved):
					String computed = level.getChunkSource().getGenerator().getBiomeSource()
							.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2, level.getChunkSource().randomState().sampler())
							.getRegisteredName();
					check(computed.equals(entry.getValue()), "after reopening, " + pos + " computes " + computed + " instead of " + entry.getValue());
				}
				JONE500BiomeAPI.LOGGER.info("[worldgen-test] reopened world: {} positions have the same biome", remembered.size());
			});
		}

		// ---- 3. other world types ----
		for (ResourceKey<WorldPreset> preset : List.of(WorldPresets.LARGE_BIOMES, WorldPresets.AMPLIFIED, WorldPresets.FLAT,
				WorldPresets.SINGLE_BIOME_SURFACE)) {
			try (TestSingleplayerContext world = context.worldBuilder()
					.setUseConsistentSettings(false)
					.adjustSettings(s -> {
						s.setSeed("777");
						s.setWorldType(findPreset(s, preset));
					})
					.create()) {
				world.getClientLevel().waitForChunksRender();
				world.getServer().runOnServer(server -> {
					ServerLevel overworld = server.overworld();
					BiomeSource source = overworld.getChunkSource().getGenerator().getBiomeSource();
					boolean hasRegions = source instanceof BiomeApiBiomeSource api && api.jone500$getLookup() != null;
					boolean multiNoise = preset.equals(WorldPresets.LARGE_BIOMES) || preset.equals(WorldPresets.AMPLIFIED);
					check(hasRegions == multiNoise, preset.identifier() + ": regions applied = " + hasRegions + ", expected " + multiNoise);
					generate(overworld, new BlockPos(2000, 0, 2000));
					String found = "-";
					if (multiNoise) {
						Pair<BlockPos, Holder<Biome>> red = source.findClosestBiome3d(BlockPos.ZERO.atY(80), 6400, 64, 64,
								b -> b.is(TutorialBiomes.RED_PLAINS), overworld.getChunkSource().randomState().sampler(), overworld);
						found = red == null ? "not within 6400 blocks" : red.getFirst().toShortString();
					}
					JONE500BiomeAPI.LOGGER.info("[worldgen-test] {}: OK (regions {}, red plains at {})", preset.identifier(), hasRegions, found);
				});
			}
		}

		// ---- 4. dedicated server: client and server agree ----
		// A dedicated server only starts when the Minecraft EULA is accepted (eula.txt in build/run/clientGameTest).
		if (!eulaAccepted()) {
			JONE500BiomeAPI.LOGGER.warn("[worldgen-test] dedicated server part skipped: Minecraft EULA not accepted in build/run/clientGameTest/eula.txt");
			return;
		}
		try (TestDedicatedServerContext server = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(s -> s.setSeed("424242"))
				.createServer()) {
			BlockPos red = server.computeOnServer(s -> findSurface(s.overworld(), TutorialBiomes.RED_PLAINS));
			try (TestServerConnection connection = server.connect()) {
				server.runCommand("op @a");
				server.runCommand("gamemode spectator @a");
				server.runCommand("tp @a " + red.getX() + " " + (red.getY() + 30) + " " + red.getZ() + " 0 60");
				context.waitTicks(40);
				connection.getClientLevel().waitForChunksRender();
				String onServer = server.computeOnServer(s -> s.overworld().getBiome(red).getRegisteredName());
				String onClient = context.computeOnClient(mc -> mc.level.getBiome(red).getRegisteredName());
				JONE500BiomeAPI.LOGGER.info("[worldgen-test] dedicated server: biome at {} is {} on the server and {} on the client",
						red.toShortString(), onServer, onClient);
				check(onServer.equals(onClient), "client sees " + onClient + " but server has " + onServer);
				check(onServer.equals(TutorialBiomes.RED_PLAINS.identifier().toString()), "server biome is " + onServer);
				context.takeScreenshot("dedicated_server_red_plains");
			}
		}
	}

	/** Generates areas in all three dimensions, checks them, and returns positions + biomes to compare later. */
	private static Map<BlockPos, String> generateEverywhere(MinecraftServer server) {
		Map<BlockPos, String> remembered = new LinkedHashMap<>();
		long start = System.currentTimeMillis();

		// Overworld: red plains (surface rule red sand) and a random far area
		ServerLevel overworld = server.overworld();
		BlockPos red = findSurface(overworld, TutorialBiomes.RED_PLAINS);
		List<ChunkAccess> redChunks = generate(overworld, red);
		int redSand = 0;
		Map<String, Integer> topBlocks = new java.util.TreeMap<>();
		int redColumns = 0;
		for (ChunkAccess chunk : redChunks) {
			for (int x = 0; x < 16; x += 2) {
				for (int z = 0; z < 16; z += 2) {
					BlockPos top = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
							chunk.getPos().getWorldPosition().offset(x, 0, z)).below();
					if (overworld.getBiome(top).is(TutorialBiomes.RED_PLAINS)) {
						redColumns++;
						topBlocks.merge(overworld.getBlockState(top).getBlock().getDescriptionId(), 1, Integer::sum);
						if (overworld.getBlockState(top).is(Blocks.RED_SAND)) {
							redSand++;
						}
					}
				}
			}
		}
		check(redColumns > 0 && redSand > redColumns / 4,
				"red plains surface rule: only " + redSand + " of " + redColumns + " red plains columns have red sand (top blocks: " + topBlocks + ")");
		generate(overworld, new BlockPos(-30000, 0, 25000));
		remember(remembered, overworld, red, new BlockPos(-30000, 70, 25000), new BlockPos(123, -30, -456));

		// Nether: ember forest with its magma floor
		ServerLevel nether = server.getLevel(Level.NETHER);
		BlockPos ember = findAny(nether, TutorialBiomes.EMBER_FOREST, 64);
		List<ChunkAccess> emberChunks = generate(nether, ember);
		int magma = 0;
		for (ChunkAccess chunk : emberChunks) {
			BlockPos base = chunk.getPos().getWorldPosition();
			for (int x = 0; x < 16; x += 2) {
				for (int z = 0; z < 16; z += 2) {
					for (int y = 32; y < 110; y++) {
						BlockPos pos = base.offset(x, y, z);
						if (nether.getBlockState(pos).is(Blocks.MAGMA_BLOCK) && nether.getBiome(pos).is(TutorialBiomes.EMBER_FOREST)) {
							magma++;
						}
					}
				}
			}
		}
		check(magma > 20, "ember forest Nether surface rule: only " + magma + " magma blocks found");
		remember(remembered, nether, ember, new BlockPos(500, 64, -800));

		// End: islands far from the center must have no small end islands (replaceWholeBiome 1.0)
		ServerLevel end = server.getLevel(Level.END);
		BlockPos islands = new BlockPos(1500, 64, 300);
		List<ChunkAccess> endChunks = generate(end, islands);
		int smallIslands = 0;
		int voidMeadow = 0;
		for (ChunkAccess chunk : endChunks) {
			BlockPos base = chunk.getPos().getWorldPosition();
			for (int x = 0; x < 16; x += 4) {
				for (int z = 0; z < 16; z += 4) {
					Holder<Biome> biome = end.getBiome(base.offset(x, 64, z));
					if (biome.is(Biomes.SMALL_END_ISLANDS)) smallIslands++;
					if (biome.is(TutorialBiomes.VOID_MEADOW)) voidMeadow++;
				}
			}
		}
		check(smallIslands == 0, "small end islands still in generated End chunks: " + smallIslands);
		remember(remembered, end, islands, new BlockPos(0, 64, 0));

		JONE500BiomeAPI.LOGGER.info("[worldgen-test] generated {} chunks in 3 dimensions in {} s: red plains columns {} (red sand {}), "
						+ "ember magma {}, end void meadow {} / small islands {}",
				redChunks.size() * 2 + emberChunks.size() + endChunks.size(), (System.currentTimeMillis() - start) / 1000,
				redColumns, redSand, magma, voidMeadow, smallIslands);
		return remembered;
	}

	/** Fully generates (features, structures, surface) the chunks around pos. */
	private static List<ChunkAccess> generate(ServerLevel level, BlockPos pos) {
		List<ChunkAccess> chunks = new ArrayList<>();
		int cx = pos.getX() >> 4;
		int cz = pos.getZ() >> 4;
		for (int dx = -AREA; dx <= AREA; dx++) {
			for (int dz = -AREA; dz <= AREA; dz++) {
				chunks.add(level.getChunk(cx + dx, cz + dz));
			}
		}
		return chunks;
	}

	private static BlockPos findSurface(ServerLevel level, ResourceKey<Biome> biome) {
		BlockPos found = findAny(level, biome, 64);
		level.getChunk(found.getX() >> 4, found.getZ() >> 4);
		return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found);
	}

	private static BlockPos findAny(ServerLevel level, ResourceKey<Biome> biome, int y) {
		BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
		Pair<BlockPos, Holder<Biome>> found = source.findClosestBiome3d(new BlockPos(0, y, 0), 6400, 32, 64, b -> b.is(biome),
				level.getChunkSource().randomState().sampler(), level);
		check(found != null, "could not find " + biome.identifier() + " in " + level.dimension().identifier());
		return found.getFirst();
	}

	private static void remember(Map<BlockPos, String> map, ServerLevel level, BlockPos... positions) {
		int dim = level.dimension() == Level.OVERWORLD ? 0 : level.dimension() == Level.NETHER ? 1 : 2;
		for (BlockPos pos : positions) {
			level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
			map.put(pack(pos, dim), level.getBiome(pos).getRegisteredName());
		}
	}

	/** Keeps the dimension in the key: x stays, z stays, y gets dim * 10000 added. */
	private static BlockPos pack(BlockPos pos, int dim) {
		return new BlockPos(pos.getX(), pos.getY() + dim * 10000, pos.getZ());
	}

	private static BlockPos unpack(BlockPos packed) {
		return new BlockPos(packed.getX(), Math.floorMod(packed.getY() + 5000, 10000) - 5000, packed.getZ());
	}

	private static ServerLevel levelFor(MinecraftServer server, BlockPos packed) {
		int dim = Math.floorDiv(packed.getY() + 5000, 10000);
		return switch (dim) {
			case 0 -> server.overworld();
			case 1 -> server.getLevel(Level.NETHER);
			default -> server.getLevel(Level.END);
		};
	}

	private static WorldCreationUiState.WorldTypeEntry findPreset(WorldCreationUiState state, ResourceKey<WorldPreset> preset) {
		List<WorldCreationUiState.WorldTypeEntry> all = new ArrayList<>(state.getNormalPresetList());
		all.addAll(state.getAltPresetList());
		return all.stream().filter(e -> e.preset() != null && e.preset().is(preset)).findFirst()
				.orElseThrow(() -> new AssertionError("world type " + preset.identifier() + " not found"));
	}

	private static boolean eulaAccepted() {
		try {
			return java.nio.file.Files.readString(java.nio.file.Path.of("eula.txt")).contains("eula=true");
		} catch (java.io.IOException e) {
			return false;
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
