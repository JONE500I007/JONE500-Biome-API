package net.biome.tutorial;

import net.biome.api.region.BiomeRegion;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biomes;

/**
 * <h2>Lesson 3: biomes that should not appear together</h2>
 *
 * Meadows and cherry groves often touch each other. This region turns meadows into mint meadows and cherry
 * groves into amber meadows, but two light-green and orange meadows side by side would look odd.
 *
 * <p>{@code exclusive(a, b)} means "at most one of these biomes in the same patch": in each patch the API picks
 * one of them, and the other one's vanilla biome stays vanilla there. Over many patches both appear.
 *
 * <p>No {@code .overworld()} here: the Overworld is the default.
 */
public final class Lesson3ExclusiveBiomes {
	public static final Identifier MEADOWS = TutorialMod.id("meadows");

	private Lesson3ExclusiveBiomes() {
	}

	static void init() {
		BiomeRegion.builder(MEADOWS)
				.weight(2)
				.replace(Biomes.MEADOW, TutorialBiomes.MINT_MEADOW)
				.replace(Biomes.CHERRY_GROVE, TutorialBiomes.AMBER_MEADOW)
				.exclusive(TutorialBiomes.MINT_MEADOW, TutorialBiomes.AMBER_MEADOW)
				.register();
	}
}
