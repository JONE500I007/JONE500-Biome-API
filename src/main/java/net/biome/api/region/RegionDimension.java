package net.biome.api.region;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Which dimension a {@link BiomeRegion} is in. In the {@link #END} regions can only replace biomes (the End has
 * no climate), see {@link BiomeRegion.Builder#end()}.
 */
public enum RegionDimension {
	OVERWORLD(LevelStem.OVERWORLD),
	NETHER(LevelStem.NETHER),
	END(LevelStem.END);

	private final ResourceKey<LevelStem> levelStem;

	RegionDimension(ResourceKey<LevelStem> levelStem) {
		this.levelStem = levelStem;
	}

	public ResourceKey<LevelStem> levelStem() {
		return levelStem;
	}
}
