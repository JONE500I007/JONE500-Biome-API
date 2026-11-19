package net.biome.api.impl;

import java.util.List;

/** Public door to {@link VanillaBiomeTagData}. Internal, not part of the API. */
public final class VanillaBiomeTagAccess {
	private VanillaBiomeTagAccess() {
	}

	public static List<String> directTags(String biomeId) {
		return VanillaBiomeTagData.DIRECT_TAGS.getOrDefault(biomeId, List.of());
	}
}
