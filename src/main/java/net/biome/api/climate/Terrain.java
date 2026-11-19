package net.biome.api.climate;

import java.util.List;

import net.minecraft.world.level.biome.Climate;

/**
 * The shape of the land (vanilla calls this "weirdness" / "peaks and valleys"). Easier to use than the 13
 * raw {@link Weirdness} slices: each value here covers all slices with that shape.
 *
 * <ul>
 *   <li>{@link #VALLEY} - the lowest land, where vanilla puts <b>rivers</b>.</li>
 *   <li>{@link #LOW} - low land next to valleys (plains, beaches, swamps).</li>
 *   <li>{@link #MID} - normal land (most forests, plains, hills).</li>
 *   <li>{@link #HIGH} - high land and slopes.</li>
 *   <li>{@link #PEAK} - mountain peaks.</li>
 * </ul>
 */
public enum Terrain {
	VALLEY(Weirdness.VALLEY),
	LOW(Weirdness.LOW_SLICE_NORMAL_DESCENDING, Weirdness.LOW_SLICE_VARIANT_ASCENDING),
	MID(Weirdness.MID_SLICE_NORMAL_ASCENDING, Weirdness.MID_SLICE_NORMAL_DESCENDING,
			Weirdness.MID_SLICE_VARIANT_ASCENDING, Weirdness.MID_SLICE_VARIANT_DESCENDING),
	HIGH(Weirdness.HIGH_SLICE_NORMAL_ASCENDING, Weirdness.HIGH_SLICE_NORMAL_DESCENDING,
			Weirdness.HIGH_SLICE_VARIANT_ASCENDING, Weirdness.HIGH_SLICE_VARIANT_DESCENDING),
	PEAK(Weirdness.PEAK_NORMAL, Weirdness.PEAK_VARIANT);

	private final List<Weirdness> slices;

	Terrain(Weirdness... slices) {
		this.slices = List.of(slices);
	}

	/** The raw weirdness slices this shape covers. */
	public List<Weirdness> slices() {
		return slices;
	}

	public List<Climate.Parameter> parameters() {
		return slices.stream().map(Weirdness::parameter).toList();
	}
}
