package net.biome.api.surface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Surface rules (which blocks make up the ground) for your Overworld, Nether and End biomes.
 *
 * <p>Your rules run <b>before</b> the vanilla rules. Wrap them in a biome condition so they only
 * affect your own biomes:
 *
 * <pre>{@code
 * BiomeSurfaceRules.addOverworld(Identifier.fromNamespaceAndPath("mymod", "surface"),
 *         SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.RED_PLAINS),
 *                 SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.RED_SAND.defaultBlockState()))));
 * }</pre>
 *
 * <p>Rules from different mods run sorted by their id.
 */
public final class BiomeSurfaceRules {
	private static final Map<Identifier, SurfaceRules.RuleSource> OVERWORLD = new TreeMap<>(Comparator.comparing(Identifier::toString));
	private static final Map<Identifier, SurfaceRules.RuleSource> NETHER = new TreeMap<>(Comparator.comparing(Identifier::toString));
	private static final Map<Identifier, SurfaceRules.RuleSource> END = new TreeMap<>(Comparator.comparing(Identifier::toString));

	private BiomeSurfaceRules() {
	}

	/** Adds Overworld surface rules. Call this in your mod's {@code onInitialize()}. */
	public static synchronized void addOverworld(Identifier id, SurfaceRules.RuleSource rule) {
		if (OVERWORLD.putIfAbsent(id, rule) != null) {
			throw new IllegalStateException("Surface rules with id " + id + " are already registered");
		}
	}

	/** All Overworld surface rules, sorted by id. */
	public static synchronized List<SurfaceRules.RuleSource> getOverworld() {
		return new ArrayList<>(OVERWORLD.values());
	}

	/** Adds Nether surface rules (e.g. the floor of your Nether biome). Call this in your mod's {@code onInitialize()}. */
	public static synchronized void addNether(Identifier id, SurfaceRules.RuleSource rule) {
		if (NETHER.putIfAbsent(id, rule) != null) {
			throw new IllegalStateException("Nether surface rules with id " + id + " are already registered");
		}
	}

	/** All Nether surface rules, sorted by id. */
	public static synchronized List<SurfaceRules.RuleSource> getNether() {
		return new ArrayList<>(NETHER.values());
	}

	/**
	 * Adds End surface rules (e.g. the ground of your End biome instead of end stone). Call this in your mod's
	 * {@code onInitialize()}. Vanilla's End rule makes everything end stone, so wrap yours in
	 * {@code SurfaceRules.isBiome(...)}; {@code SurfaceRules.ON_FLOOR} works like in the Overworld.
	 */
	public static synchronized void addEnd(Identifier id, SurfaceRules.RuleSource rule) {
		if (END.putIfAbsent(id, rule) != null) {
			throw new IllegalStateException("End surface rules with id " + id + " are already registered");
		}
	}

	/** All End surface rules, sorted by id. */
	public static synchronized List<SurfaceRules.RuleSource> getEnd() {
		return new ArrayList<>(END.values());
	}
}
