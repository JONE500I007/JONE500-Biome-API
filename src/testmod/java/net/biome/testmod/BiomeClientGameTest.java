package net.biome.testmod;

import com.mojang.datafixers.util.Pair;

import net.biome.api.impl.BiomeApiBiomeSource;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.cow.CowVariants;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

/**
 * End-to-end check in a real (normal, not flat) singleplayer world:
 * <ol>
 *   <li>the Overworld biome source got the regions,</li>
 *   <li>/locate-style search finds the region biomes (so they are part of the possible biomes),</li>
 *   <li>screenshots from above red plains (red grass + red sand surface rule) and orange hills (made with
 *       BiomeMaker, placed with ClimatePoints).</li>
 * </ol>
 * Screenshots are saved in build/run/clientGameTest/screenshots. Run with: gradlew runClientGameTest
 */
public class BiomeClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (BiomeCompatClientGameTest.active()) {
			return; // compatibility run: only BiomeCompatClientGameTest
		}
		try (TestSingleplayerContext world = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(settings -> settings.setSeed("12345"))
				.create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("gamemode spectator @a");

			// Biome tags copied with VanillaBiomeTags (structures + mob variants).
			world.getServer().runOnServer(server -> {
				var biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
				Holder<Biome> red = biomes.getOrThrow(BiomeTestMod.RED_PLAINS);
				Holder<Biome> blue = biomes.getOrThrow(BiomeTestMod.BLUE_FOREST);
				Holder<Biome> orange = biomes.getOrThrow(BiomeTestMod.ORANGE_HILLS);
				check(red.is(BiomeTags.HAS_MINESHAFT), "red plains should have mineshafts");
				check(red.is(BiomeTags.HAS_VILLAGE_PLAINS), "red plains should have plains villages");
				check(red.is(BiomeTags.STRONGHOLD_BIASED_TO), "strongholds should be able to start in red plains");
				check(blue.is(BiomeTags.HAS_MINESHAFT), "blue forest should have mineshafts (through #is_forest)");
				check(orange.is(BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS), "orange hills should spawn warm farm animals (through #is_savanna)");
				check(!orange.is(BiomeTags.HAS_VILLAGE_SAVANNA), "orange hills were copied with NO_VILLAGES");
			});

			BlockPos redPlains = world.getServer().computeOnServer(server -> find(server, BiomeTestMod.RED_PLAINS));
			lookFromAbove(context, world, redPlains, "red_plains_from_above");
			world.getServer().runOnServer(server -> checkCowVariant(server, redPlains, CowVariants.TEMPERATE));

			BlockPos orangeHills = world.getServer().computeOnServer(server -> find(server, BiomeTestMod.ORANGE_HILLS));
			lookFromAbove(context, world, orangeHills, "orange_hills_from_above");
			world.getServer().runOnServer(server -> checkCowVariant(server, orangeHills, CowVariants.WARM));
		}
	}

	/** Spawns a cow on the ground at pos (chunk must be loaded) and checks the variant the game picked. */
	private static void checkCowVariant(MinecraftServer server, BlockPos pos, ResourceKey<CowVariant> expected) {
		ServerLevel level = server.overworld();
		BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
		Cow cow = EntityTypes.COW.spawn(level, ground, EntitySpawnReason.NATURAL);
		check(cow != null, "could not spawn a cow at " + ground);
		check(cow.getVariant().is(expected), "cow at " + ground + " in " + level.getBiome(ground).getRegisteredName()
				+ " is " + cow.getVariant().getRegisteredName() + ", expected " + expected.identifier());
		cow.discard();
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	private static BlockPos find(MinecraftServer server, ResourceKey<Biome> biome) {
		ServerLevel overworld = server.overworld();
		BiomeSource source = overworld.getChunkSource().getGenerator().getBiomeSource();
		if (!(source instanceof BiomeApiBiomeSource apiSource) || apiSource.jone500$getLookup() == null) {
			throw new AssertionError("Overworld biome source has no regions");
		}
		if (source.possibleBiomes().stream().noneMatch(b -> b.is(biome))) {
			throw new AssertionError(biome.identifier() + " is not in the possible biomes");
		}
		Pair<BlockPos, Holder<Biome>> found = source.findClosestBiome3d(BlockPos.ZERO.atY(80), 6400, 32, 64,
				b -> b.is(biome), overworld.getChunkSource().randomState().sampler(), overworld);
		if (found == null) {
			throw new AssertionError("could not find " + biome.identifier() + " within 6400 blocks");
		}
		return found.getFirst();
	}

	private static void lookFromAbove(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos, String screenshot) {
		world.getServer().runCommand("tp @a " + pos.getX() + " 160 " + pos.getZ() + " 0 55");
		context.waitTicks(20);
		world.getConnection().waitForChunksRender();
		context.waitTicks(40);
		context.takeScreenshot(screenshot);
	}
}
