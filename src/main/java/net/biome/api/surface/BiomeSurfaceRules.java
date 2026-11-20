package net.biome.api.surface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

/**
 * Surface rules (which blocks make up the ground) for your Overworld, Nether and End biomes.
 *
 * <p>Since Minecraft 26.3 surface rules are <b>material rules</b> ({@code MaterialRules.ifTrue / sequence / state}).
 * They refer to biomes and named conditions (like "on floor") through registries that don't exist yet in
 * {@code onInitialize()}, so you give a function that gets a {@link SurfaceRuleContext} when a world loads:
 *
 * <pre>{@code
 * BiomeSurfaceRules.addOverworld(Identifier.fromNamespaceAndPath("mymod", "surface"), ctx ->
 *         MaterialRules.ifTrue(ctx.isBiome(ModBiomes.RED_PLAINS),
 *                 MaterialRules.ifTrue(ctx.onFloor(), MaterialRules.state(Blocks.RED_SAND.defaultBlockState()))));
 * }</pre>
 *
 * <p>Your rules run <b>before</b> the dimension's own rule; wrap them in {@code ctx.isBiome(...)} so they only
 * change your own biomes. Rules that need no registry can also be given directly ({@code addOverworld(id, rule)}).
 * Rules from different mods run sorted by their id.
 */
public final class BiomeSurfaceRules {
	private static final Map<Identifier, Function<SurfaceRuleContext, MaterialRule>> OVERWORLD = newMap();
	private static final Map<Identifier, Function<SurfaceRuleContext, MaterialRule>> NETHER = newMap();
	private static final Map<Identifier, Function<SurfaceRuleContext, MaterialRule>> END = newMap();

	private BiomeSurfaceRules() {
	}

	private static Map<Identifier, Function<SurfaceRuleContext, MaterialRule>> newMap() {
		return new TreeMap<>(Comparator.comparing(Identifier::toString));
	}

	/** Adds Overworld surface rules, made when a world loads. Call this in your mod's {@code onInitialize()}. */
	public static synchronized void addOverworld(Identifier id, Function<SurfaceRuleContext, MaterialRule> rule) {
		add(OVERWORLD, "Surface rules", id, rule);
	}

	/** Adds Overworld surface rules that need no registry (no {@code isBiome} or named conditions). */
	public static synchronized void addOverworld(Identifier id, MaterialRule rule) {
		Objects.requireNonNull(rule, "rule");
		add(OVERWORLD, "Surface rules", id, ctx -> rule);
	}

	/** All Overworld surface rules for this world, sorted by id. */
	public static synchronized List<MaterialRule> getOverworld(SurfaceRuleContext ctx) {
		return make(OVERWORLD, ctx);
	}

	/** Adds Nether surface rules (e.g. the floor of your Nether biome). Call this in your mod's {@code onInitialize()}. */
	public static synchronized void addNether(Identifier id, Function<SurfaceRuleContext, MaterialRule> rule) {
		add(NETHER, "Nether surface rules", id, rule);
	}

	/** Adds Nether surface rules that need no registry. */
	public static synchronized void addNether(Identifier id, MaterialRule rule) {
		Objects.requireNonNull(rule, "rule");
		add(NETHER, "Nether surface rules", id, ctx -> rule);
	}

	/** All Nether surface rules for this world, sorted by id. */
	public static synchronized List<MaterialRule> getNether(SurfaceRuleContext ctx) {
		return make(NETHER, ctx);
	}

	/**
	 * Adds End surface rules (e.g. the ground of your End biome instead of end stone). Call this in your mod's
	 * {@code onInitialize()}. Vanilla's End rule makes everything end stone, so wrap yours in
	 * {@code ctx.isBiome(...)}; {@code ctx.onFloor()} works like in the Overworld.
	 */
	public static synchronized void addEnd(Identifier id, Function<SurfaceRuleContext, MaterialRule> rule) {
		add(END, "End surface rules", id, rule);
	}

	/** Adds End surface rules that need no registry. */
	public static synchronized void addEnd(Identifier id, MaterialRule rule) {
		Objects.requireNonNull(rule, "rule");
		add(END, "End surface rules", id, ctx -> rule);
	}

	/** All End surface rules for this world, sorted by id. */
	public static synchronized List<MaterialRule> getEnd(SurfaceRuleContext ctx) {
		return make(END, ctx);
	}

	private static void add(Map<Identifier, Function<SurfaceRuleContext, MaterialRule>> map, String what, Identifier id,
			Function<SurfaceRuleContext, MaterialRule> rule) {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(rule, "rule");
		if (map.putIfAbsent(id, rule) != null) {
			throw new IllegalStateException(what + " with id " + id + " are already registered");
		}
	}

	private static List<MaterialRule> make(Map<Identifier, Function<SurfaceRuleContext, MaterialRule>> map, SurfaceRuleContext ctx) {
		List<MaterialRule> rules = new ArrayList<>(map.size());
		map.forEach((id, factory) -> {
			MaterialRule rule = factory.apply(ctx);
			if (rule == null) {
				throw new IllegalStateException("Surface rules " + id + " returned null");
			}
			rules.add(rule);
		});
		return rules;
	}
}
