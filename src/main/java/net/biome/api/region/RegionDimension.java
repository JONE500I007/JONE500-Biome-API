package net.biome.api.region;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/** Which dimension a {@link BiomeRegion} is in. (The End has its own system, see {@code BiomeMaker.end}.) */
public enum RegionDimension {
	OVERWORLD(LevelStem.OVERWORLD),
	NETHER(LevelStem.NETHER);

	private final ResourceKey<LevelStem> levelStem;

	RegionDimension(ResourceKey<LevelStem> levelStem) {
		this.levelStem = levelStem;
	}

	public ResourceKey<LevelStem> levelStem() {
		return levelStem;
	}
}
