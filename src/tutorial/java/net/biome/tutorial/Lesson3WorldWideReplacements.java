package net.biome.tutorial;

import net.biome.api.region.BiomeReplacements;
import net.minecraft.world.level.biome.Biomes;

/**
 * <h2>Lesson 3: replacing a vanilla biome everywhere (no region)</h2>
 *
 * A region only changes biomes inside its own patches. {@link BiomeReplacements} works on the <b>whole</b> world,
 * vanilla patches included. There are two kinds:
 * <ul>
 *   <li>{@code replace(original, yourBiome, chance)}: the world is cut into smaller pieces (about 256 blocks,
 *       change it with {@code BiomeReplacements.setPatchSize}), and in that share of the pieces the original
 *       biome becomes yours. Here: 40% of birch forest areas become blue forest.</li>
 *   <li>{@code replaceWholeBiome(original, yourBiome, chanceOfWorld)}: decided <b>once per world</b> (by the
 *       seed). With {@code 0.5F}, in half of all worlds <b>every</b> dark forest is a blue forest, and in the other
 *       half none is. With {@code 1.0F} it happens in every world.</li>
 * </ul>
 *
 * <p>Cave biomes and ocean biomes can be replaced too: here lush caves become mint meadow.
 *
 * <p>Want several biomes by weight instead of one? See {@code WeightedBiomes} in {@link Lesson5Nether}; it works
 * with {@code BiomeReplacements.replace(original, weightedBiomes)} too.
 */
public final class Lesson3WorldWideReplacements {
	private Lesson3WorldWideReplacements() {
	}

	static void init() {
		BiomeReplacements.replace(Biomes.BIRCH_FOREST, TutorialBiomes.BLUE_FOREST, 0.4F);
		BiomeReplacements.replaceWholeBiome(Biomes.DARK_FOREST, TutorialBiomes.BLUE_FOREST, 0.5F);
		BiomeReplacements.replaceWholeBiome(Biomes.LUSH_CAVES, TutorialBiomes.MINT_MEADOW, 1.0F);
	}
}
