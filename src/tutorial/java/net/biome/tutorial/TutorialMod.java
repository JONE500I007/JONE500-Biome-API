package net.biome.tutorial;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/**
 * <h2>JONE500 Biome API tutorial</h2>
 *
 * A small mod that uses Biome API, with one class per lesson. Read them in this order:
 * <ol>
 *   <li>{@link TutorialBiomes}: biome keys, and where each biome's JSON file comes from</li>
 *   <li>{@link Lesson1FirstRegion}: a region that swaps vanilla biomes for yours, and a biome with its own climate</li>
 *   <li>{@link Lesson2ExclusiveBiomes}: two biomes that never appear in the same patch</li>
 *   <li>{@link Lesson3WorldWideReplacements}: replacing a vanilla biome everywhere, without a region</li>
 *   <li>{@link Lesson4SurfaceRules}: which blocks make up the ground of your biome</li>
 *   <li>{@link Lesson5Nether}: Nether regions, a Nether biome at its own point, several biomes by weight</li>
 *   <li>{@link Lesson6End}: End regions and End surface rules</li>
 *   <li>{@link TutorialDataGen}: making biome JSON with {@code BiomeMaker}, and vanilla tags (structures, mob
 *       variants) with {@code VanillaBiomeTags}</li>
 * </ol>
 *
 * <p><b>Try it:</b> run the Gradle task {@code runTutorialClient} and make a <b>new</b> world (biomes only change in
 * new chunks). Find a biome with {@code /locate biome biome_tutorial:red_plains}. In the F3 screen, the line
 * {@code Biome API region} shows which region you are standing in.
 *
 * <p>The package {@code net.biome.tutorial.test} holds the automated tests of Biome API. They use these lessons'
 * biomes, but you don't need to read them to learn the API.
 *
 * <p>Everything here runs in {@code onInitialize}: regions and replacements must be registered before a world loads.
 */
public class TutorialMod implements ModInitializer {
	public static final String MOD_ID = "biome_tutorial";

	@Override
	public void onInitialize() {
		Lesson1FirstRegion.init();
		Lesson2ExclusiveBiomes.init();
		Lesson3WorldWideReplacements.init();
		Lesson4SurfaceRules.init();
		Lesson5Nether.init();
		Lesson6End.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
