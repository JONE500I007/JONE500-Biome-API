package net.biome.api.climate;

import java.util.List;

import net.minecraft.world.level.biome.Climate;

/**
 * Where (how high / deep) the biome is.
 *
 * <p><b>How depth works:</b> depth is about 0 at the ground surface, grows going down, and is <b>negative
 * above the ground</b> (in the air, and in sea water above the sea floor).
 *
 * <table>
 *   <caption>Depth values</caption>
 *   <tr><th>Value</th><th>Range</th><th>Use it for</th></tr>
 *   <tr><td>{@link #SURFACE}</td><td>0 and 1</td><td>Almost every biome: plains, forests, mountains,
 *       <b>also oceans and rivers</b> (vanilla oceans use this too, the climate makes them oceans).
 *       Cave biomes stay below it.</td></tr>
 *   <tr><td>{@link #UNDERGROUND}</td><td>0.2 to 0.9</td><td>Cave biomes, like lush caves / dripstone caves.</td></tr>
 *   <tr><td>{@link #DEEP_UNDERGROUND}</td><td>1.1</td><td>The very bottom, like the deep dark.</td></tr>
 *   <tr><td>{@link #SURFACE_AND_UNDERGROUND}</td><td>0 to 1.1</td><td>Your biome from the ground down to the
 *       bottom; it also takes the place of cave biomes below it.</td></tr>
 *   <tr><td>{@link #ALL_HEIGHTS}</td><td>-1 to 1.1</td><td>Every height, <b>also above the ground</b> (air and
 *       water). Only for special biomes that must also own the water/air above lower ground nearby. Such a
 *       biome can show up in the water of nearby seas and lakes; combine with
 *       {@link ClimatePoints.Builder#strict()} if that is not wanted.</td></tr>
 * </table>
 */
public enum Depth {
	SURFACE(List.of(Climate.Parameter.point(0.0F), Climate.Parameter.point(1.0F))),
	UNDERGROUND(List.of(Climate.Parameter.span(0.2F, 0.9F))),
	DEEP_UNDERGROUND(List.of(Climate.Parameter.point(1.1F))),
	SURFACE_AND_UNDERGROUND(List.of(Climate.Parameter.span(0.0F, 1.1F))),
	ALL_HEIGHTS(List.of(Climate.Parameter.span(-1.0F, 1.1F)));

	private final List<Climate.Parameter> parameters;

	Depth(List<Climate.Parameter> parameters) {
		this.parameters = parameters;
	}

	/** Vanilla uses two depth values for surface biomes, so this is a list. */
	public List<Climate.Parameter> parameters() {
		return parameters;
	}
}
