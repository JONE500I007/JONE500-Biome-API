package net.biome.api.biome;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import net.biome.api.impl.VanillaBiomeTagAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Gives your biome the same vanilla <b>biome tags</b> as a vanilla biome.
 *
 * <p>Biome tags decide a lot in vanilla:
 * <ul>
 *   <li><b>Structures</b>: mineshafts, strongholds, villages, pillager outposts, ruined portals, trial
 *       chambers, trail ruins... ({@code #minecraft:has_structure/...}, {@code stronghold_biased_to})</li>
 *   <li><b>Mob variants</b>: warm/cold cows, pigs and chickens, warm/cold frogs, snow foxes, white and gold
 *       rabbits, some wolf variants ({@code is_jungle}, {@code is_savanna}, {@code is_badlands})</li>
 *   <li>Other things like {@code is_overworld}, {@code is_forest}, {@code is_mountain}, {@code is_river}...</li>
 * </ul>
 * Without tags, your biome has <b>no villages, mineshafts or strongholds</b> and always gets the
 * temperate mob variants.
 *
 * <p>Use it in your biome tag provider (data generation):
 * <pre>{@code
 * public class ModBiomeTagProvider extends FabricTagsProvider<Biome> {
 *     protected void addTags(HolderLookup.Provider lookup) {
 *         // like plains: mineshafts, plains villages, outposts, strongholds, ruined portals...
 *         VanillaBiomeTags.copy(Biomes.PLAINS, ModBiomes.APPLE_GROVE, (tag, biome) -> builder(tag).add(biome));
 *         // like savanna, but without villages
 *         VanillaBiomeTags.copy(Biomes.SAVANNA, ModBiomes.DRY_ORCHARD, VanillaBiomeTags.NO_VILLAGES,
 *                 (tag, biome) -> builder(tag).add(biome));
 *     }
 * }
 * }</pre>
 *
 * <p>Some wolf variants are tied to exact vanilla biomes, not tags (woods wolf = forest, ashen = snowy
 * taiga, black = old growth pine taiga, chestnut = old growth spruce taiga, snowy = grove). Those can't be
 * copied with tags; in your biome these wolves are the normal (pale) variant.
 */
public final class VanillaBiomeTags {
	/** Keeps every tag. */
	public static final Predicate<TagKey<Biome>> ALL = tag -> true;
	/** Everything except villages (and zombie sieges / wandering traders stay as in the copied biome). */
	public static final Predicate<TagKey<Biome>> NO_VILLAGES = tag -> !tag.location().getPath().startsWith("has_structure/village_");
	/** Only the structure tags ({@code has_structure/...} and {@code stronghold_biased_to}). */
	public static final Predicate<TagKey<Biome>> ONLY_STRUCTURES = tag -> tag.location().getPath().startsWith("has_structure/")
			|| tag.location().getPath().equals("stronghold_biased_to");
	/** Everything except structure tags (mob variants, is_overworld, is_forest...). */
	public static final Predicate<TagKey<Biome>> NO_STRUCTURES = ONLY_STRUCTURES.negate();

	private VanillaBiomeTags() {
	}

	/** The vanilla tags that list {@code vanillaBiome} directly (empty for non-vanilla biomes). */
	public static List<TagKey<Biome>> of(ResourceKey<Biome> vanillaBiome) {
		return VanillaBiomeTagAccess.directTags(vanillaBiome.identifier().toString()).stream()
				.map(id -> TagKey.create(Registries.BIOME, Identifier.parse(id)))
				.toList();
	}

	/** Adds {@code yourBiome} to every vanilla tag that {@code vanillaBiome} is in. */
	public static void copy(ResourceKey<Biome> vanillaBiome, ResourceKey<Biome> yourBiome,
			BiConsumer<TagKey<Biome>, ResourceKey<Biome>> addToTag) {
		copy(vanillaBiome, yourBiome, ALL, addToTag);
	}

	/**
	 * Like {@link #copy(ResourceKey, ResourceKey, BiConsumer)}, but only the tags {@code filter} accepts,
	 * e.g. {@link #NO_VILLAGES} or your own {@code tag -> !tag.equals(BiomeTags.HAS_PILLAGER_OUTPOST)}.
	 */
	public static void copy(ResourceKey<Biome> vanillaBiome, ResourceKey<Biome> yourBiome, Predicate<TagKey<Biome>> filter,
			BiConsumer<TagKey<Biome>, ResourceKey<Biome>> addToTag) {
		List<TagKey<Biome>> tags = of(vanillaBiome);
		if (tags.isEmpty()) {
			throw new IllegalArgumentException(vanillaBiome.identifier() + " is not a vanilla biome with tags");
		}
		for (TagKey<Biome> tag : tags) {
			if (filter.test(tag)) {
				addToTag.accept(tag, yourBiome);
			}
		}
	}
}
