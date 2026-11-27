package net.biome.api.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.world.level.biome.Climate;

/**
 * The "rest" boxes for overlay points ({@code ClimatePoints.Builder.overlay()}).
 *
 * <p>An overlay point does not compete with the dimension's real points. It competes with boxes that cover the
 * climate outside it: for every climate value, the parts of the -1..1 range that the overlay points don't use.
 * Where a spot is closer to an overlay point than to every rest box, the overlay biome is placed.
 *
 * <p>This gives the same placement as TerraBlender's overlay builder (an independent implementation of the same
 * rules, no code taken from it):
 * <ul>
 *   <li>Points that differ from another point in exactly one value are grouped by that value; the rest boxes of a
 *       group are made from all its points. A point with no such partner gets rest boxes of its own.</li>
 *   <li>Per value, the ranges are sorted by their start; a gap is added before a range that starts after the end
 *       of the range before it, and after the last range up to 1. (The "end" is always the end of the range just
 *       before, also when an earlier range reached further: this matches TerraBlender.) No gap at all = the whole
 *       -1..1 range.</li>
 *   <li>The boxes are every combination of the gaps of the six values, with offset 0.</li>
 * </ul>
 *
 * <p>Internal class, not part of the API.
 */
final class OverlayRest {
	private static final long MIN = Climate.quantizeCoord(-1.0F);
	private static final long MAX = Climate.quantizeCoord(1.0F);

	/** The six climate values of a point, in a fixed order. */
	private static final List<Function<Climate.ParameterPoint, Climate.Parameter>> VALUES = List.of(
			Climate.ParameterPoint::temperature, Climate.ParameterPoint::humidity, Climate.ParameterPoint::continentalness,
			Climate.ParameterPoint::erosion, Climate.ParameterPoint::depth, Climate.ParameterPoint::weirdness);

	private OverlayRest() {
	}

	/** The rest boxes for all overlay points of one region. */
	static List<Climate.ParameterPoint> boxes(List<Climate.ParameterPoint> points) {
		Set<Climate.ParameterPoint> alone = new LinkedHashSet<>(points);
		Map<Integer, Set<Climate.ParameterPoint>> groups = new LinkedHashMap<>();
		List<Climate.ParameterPoint> distinct = new ArrayList<>(alone);
		for (int i = 0; i < distinct.size(); i++) {
			for (int j = i + 1; j < distinct.size(); j++) {
				int value = onlyDifference(distinct.get(i), distinct.get(j));
				if (value >= 0) {
					Set<Climate.ParameterPoint> group = groups.computeIfAbsent(value, v -> new LinkedHashSet<>());
					group.add(distinct.get(i));
					group.add(distinct.get(j));
					alone.remove(distinct.get(i));
					alone.remove(distinct.get(j));
				}
			}
		}
		List<Climate.ParameterPoint> boxes = new ArrayList<>();
		for (Climate.ParameterPoint point : alone) {
			boxes.addAll(restOf(List.of(point)));
		}
		for (Set<Climate.ParameterPoint> group : groups.values()) {
			boxes.addAll(restOf(List.copyOf(group)));
		}
		return boxes;
	}

	/** The index of the only value in which the two points differ, or -1 (same point, or more than one difference). */
	private static int onlyDifference(Climate.ParameterPoint a, Climate.ParameterPoint b) {
		int found = -1;
		for (int v = 0; v < VALUES.size(); v++) {
			if (!VALUES.get(v).apply(a).equals(VALUES.get(v).apply(b))) {
				if (found >= 0) {
					return -1;
				}
				found = v;
			}
		}
		return found;
	}

	/** Every combination of the gaps of the six values. */
	private static List<Climate.ParameterPoint> restOf(List<Climate.ParameterPoint> points) {
		List<List<Climate.Parameter>> gaps = new ArrayList<>(VALUES.size());
		for (Function<Climate.ParameterPoint, Climate.Parameter> value : VALUES) {
			gaps.add(gaps(points.stream().map(value).sorted(Comparator.comparingLong(Climate.Parameter::min)).toList()));
		}
		List<Climate.ParameterPoint> boxes = new ArrayList<>();
		for (Climate.Parameter t : gaps.get(0)) {
			for (Climate.Parameter h : gaps.get(1)) {
				for (Climate.Parameter c : gaps.get(2)) {
					for (Climate.Parameter e : gaps.get(3)) {
						for (Climate.Parameter d : gaps.get(4)) {
							for (Climate.Parameter w : gaps.get(5)) {
								boxes.add(new Climate.ParameterPoint(t, h, c, e, d, w, 0L));
							}
						}
					}
				}
			}
		}
		return boxes;
	}

	/** The parts of -1..1 between the (sorted) ranges; the whole -1..1 if there are none. */
	private static List<Climate.Parameter> gaps(List<Climate.Parameter> sortedRanges) {
		List<Climate.Parameter> gaps = new ArrayList<>();
		long end = MIN;
		for (Climate.Parameter range : sortedRanges) {
			if (range.min() > end) {
				gaps.add(new Climate.Parameter(end, range.min()));
			}
			end = range.max();
		}
		if (end < MAX) {
			gaps.add(new Climate.Parameter(end, MAX));
		}
		if (gaps.isEmpty()) {
			gaps.add(new Climate.Parameter(MIN, MAX));
		}
		return gaps;
	}
}
