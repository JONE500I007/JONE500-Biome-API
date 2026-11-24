package net.biome.tutorial;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * <h2>Before the lessons: your biomes</h2>
 *
 * Biome API does not make biomes, it <b>places</b> them. Each biome is a normal biome JSON file in
 * {@code data/<mod id>/worldgen/biome/<name>.json}, like any modded biome. In code you use a
 * {@code ResourceKey<Biome>} with the same id.
 *
 * <p>This tutorial has both ways to make the JSON:
 * <ul>
 *   <li><b>Written by hand:</b> red plains, blue forest, mint meadow and amber meadow are recolored copies of vanilla
 *       biomes, in {@code src/tutorial/resources/data/biome_tutorial/worldgen/biome/}.</li>
 *   <li><b>Data generation:</b> orange hills, highland meadow, crystal caves and the Nether and End biomes are made in Java with
 *       {@code BiomeMaker} (see {@link TutorialDataGen}). The Gradle task {@code runDatagen} writes their JSON to
 *       {@code src/tutorial/generated/}.</li>
 * </ul>
 */
public final class TutorialBiomes {
	// Overworld, hand-written JSON
	public static final ResourceKey<Biome> RED_PLAINS = key("red_plains");
	public static final ResourceKey<Biome> BLUE_FOREST = key("blue_forest");
	public static final ResourceKey<Biome> MINT_MEADOW = key("mint_meadow");
	public static final ResourceKey<Biome> AMBER_MEADOW = key("amber_meadow");
	// Overworld, made with BiomeMaker
	public static final ResourceKey<Biome> ORANGE_HILLS = key("orange_hills");
	public static final ResourceKey<Biome> HIGHLAND_MEADOW = key("highland_meadow");
	public static final ResourceKey<Biome> CRYSTAL_CAVES = key("crystal_caves");
	// Nether, made with BiomeMaker
	public static final ResourceKey<Biome> EMBER_FOREST = key("ember_forest");
	public static final ResourceKey<Biome> SULFUR_WASTES = key("sulfur_wastes");
	public static final ResourceKey<Biome> BONE_FIELDS = key("bone_fields");
	// End, made with BiomeMaker
	public static final ResourceKey<Biome> VOID_MEADOW = key("void_meadow");
	public static final ResourceKey<Biome> ASH_BARRENS = key("ash_barrens");

	private TutorialBiomes() {
	}

	public static ResourceKey<Biome> key(String path) {
		return ResourceKey.create(Registries.BIOME, TutorialMod.id(path));
	}
}
