package net.biome.api.biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.NetherPlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Makes a {@link Biome} with less code. Use it in data generation (biome bootstrap).
 *
 * <pre>{@code
 * public static void bootstrap(BootstrapContext<Biome> context) {
 *     context.register(APPLE_GROVE, BiomeMaker.overworld(context)
 *             .temperature(0.55F)
 *             .downfall(0.4F)
 *             .waterColor(0x3F76E4)
 *             .grassColor(0x91BD59)
 *             .foliageColor(0x77AB2F)
 *             .music(SoundEvents.MUSIC_BIOME_MEADOW)
 *             .farmAnimals()
 *             .defaultMonsters()
 *             .spawn(EntityTypes.FOX, 8, 2, 4)
 *             .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.APPLE_TREE)
 *             .features(BiomeDefaultFeatures::addMeadowVegetation)
 *             .build());
 * }
 * }</pre>
 */
public final class BiomeMaker {
	private final BiomeGenerationSettings.Builder generation;
	private final MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
	private final BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder().waterColor(0x3F76E4);
	private float temperature = 0.8F;
	private float downfall = 0.4F;
	private boolean precipitation = true;
	private Integer skyColor;
	private Integer fogColor;
	private Integer waterFogColor;
	private Holder<SoundEvent> music;
	private AmbientSounds ambientSounds;
	private final List<AmbientParticle> particles = new ArrayList<>();
	/** Overworld biomes get vanilla's sky color for their temperature; Nether/End skies come from the dimension. */
	private boolean skyFromTemperature = true;

	private BiomeMaker(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		this.generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
	}

	// ------------------------------------------------------------------------------------------
	// Nether and End
	// ------------------------------------------------------------------------------------------

	/**
	 * A <b>Nether</b> biome with what all vanilla Nether biomes have: nether caves, lava springs (open and
	 * closed), fire patches, glowstone, magma, and the nether ores (gravel, blackstone, gold, quartz, ancient
	 * debris). No rain, temperature 2.0. Add plants, fog color, particles and mobs yourself.
	 *
	 * <p>Place it with a region: {@code BiomeRegion.builder(id).nether().replace(Biomes.CRIMSON_FOREST, ...)}
	 * or {@code .addBiome(ClimatePoints.netherPoint(0.2F, -0.3F), ...)}.
	 */
	public static BiomeMaker nether(BootstrapContext<Biome> context) {
		BiomeMaker maker = empty(context);
		maker.temperature = 2.0F;
		maker.downfall = 0.0F;
		maker.precipitation = false;
		maker.skyFromTemperature = false;
		// Same order as vanilla Nether biomes (a different order causes "Feature order cycle").
		maker.generation.addCarver(Carvers.NETHER_CAVE);
		maker.generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, MiscOverworldPlacements.SPRING_LAVA);
		maker.generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, NetherPlacements.SPRING_OPEN);
		maker.generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, NetherPlacements.PATCH_FIRE);
		maker.generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, NetherPlacements.GLOWSTONE_EXTRA);
		maker.generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, NetherPlacements.GLOWSTONE);
		maker.generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, OrePlacements.ORE_MAGMA);
		maker.generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, NetherPlacements.SPRING_CLOSED);
		BiomeDefaultFeatures.addNetherDefaultOres(maker.generation);
		return maker;
	}

	/**
	 * An <b>End</b> biome like vanilla's: endermen, no rain, temperature 0.5. Add features yourself, e.g.
	 * chorus plants: {@code .addFeature(VEGETAL_DECORATION, EndPlacements.CHORUS_PLANT)}.
	 *
	 * <p>Place it with Fabric API: {@code TheEndBiomes.addHighlandsBiome(MY_BIOME, 10)} (also
	 * {@code addMidlandsBiome}, {@code addBarrensBiome}, {@code addSmallIslandsBiome}), or with
	 * {@code BiomeReplacements.replace / replaceWholeBiome}.
	 */
	public static BiomeMaker end(BootstrapContext<Biome> context) {
		BiomeMaker maker = empty(context);
		maker.temperature = 0.5F;
		maker.downfall = 0.5F;
		maker.precipitation = false;
		maker.skyFromTemperature = false;
		BiomeDefaultFeatures.endSpawns(maker.spawns);
		return maker;
	}

	/**
	 * An Overworld biome that already has everything vanilla puts <b>underground</b> in normal biomes like
	 * plains:
	 * caves and canyons, lava lakes, amethyst geodes, monster rooms, dirt/gravel/granite/diorite/andesite/tuff
	 * veins, water and lava springs, all normal ores, sand/clay/gravel disks and freezing of water.
	 * Add the surface plants, trees and mobs yourself.
	 */
	public static BiomeMaker overworld(BootstrapContext<Biome> context) {
		return overworld(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));
	}

	/** Same as {@link #overworld(BootstrapContext)}, with the lookups given directly. */
	public static BiomeMaker overworld(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		return new BiomeMaker(placedFeatures, carvers)
				.vanillaCaves()
				.vanillaGeodes()
				.vanillaMonsterRooms()
				.vanillaStoneVeins()
				.vanillaSprings()
				.vanillaFreezing()
				.vanillaOres()
				.vanillaDisks();
	}

	/**
	 * A biome with no features, carvers or mobs at all. Add only the parts you want, for example:
	 * <pre>{@code
	 * BiomeMaker.empty(context)
	 *         .vanillaCaves()          // but no geodes, ores, ...
	 *         .vanillaStoneVeins()
	 *         .vanillaOres()
	 * }</pre>
	 */
	public static BiomeMaker empty(BootstrapContext<Biome> context) {
		return new BiomeMaker(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));
	}

	/** Same as {@link #empty(BootstrapContext)}, with the lookups given directly. */
	public static BiomeMaker empty(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		return new BiomeMaker(placedFeatures, carvers);
	}

	// ------------------------------------------------------------------------------------------
	// Vanilla underground parts (all of them are already in overworld(...); use them with empty(...))
	// ------------------------------------------------------------------------------------------

	/** Caves and canyons (carvers) and lava lakes. Noise caves (cheese / spaghetti) exist in every biome anyway. */
	public BiomeMaker vanillaCaves() {
		BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
		return this;
	}

	/** Amethyst geodes. */
	public BiomeMaker vanillaGeodes() {
		BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
		return this;
	}

	/** Monster rooms (dungeons with spawners). */
	public BiomeMaker vanillaMonsterRooms() {
		BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
		return this;
	}

	/** Dirt, gravel, granite, diorite, andesite and tuff veins. */
	public BiomeMaker vanillaStoneVeins() {
		BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
		return this;
	}

	/** Water and lava springs. */
	public BiomeMaker vanillaSprings() {
		BiomeDefaultFeatures.addDefaultSprings(generation);
		return this;
	}

	/** Water freezes and snow falls when the biome is cold enough. */
	public BiomeMaker vanillaFreezing() {
		BiomeDefaultFeatures.addSurfaceFreezing(generation);
		return this;
	}

	/** Coal, iron, gold, redstone, diamond, lapis and copper, like most vanilla biomes. */
	public BiomeMaker vanillaOres() {
		BiomeDefaultFeatures.addDefaultOres(generation);
		return this;
	}

	/** Like {@link #vanillaOres()} but with the big copper veins of dripstone caves. */
	public BiomeMaker vanillaOresWithLargeCopper() {
		BiomeDefaultFeatures.addDefaultOres(generation, true);
		return this;
	}

	/** Sand, clay and gravel disks in water. */
	public BiomeMaker vanillaDisks() {
		BiomeDefaultFeatures.addDefaultSoftDisks(generation);
		return this;
	}

	/** Extra gold, like badlands. */
	public BiomeMaker extraGold() {
		BiomeDefaultFeatures.addExtraGold(generation);
		return this;
	}

	/** Emerald ore, like mountains. */
	public BiomeMaker emeralds() {
		BiomeDefaultFeatures.addExtraEmeralds(generation);
		return this;
	}

	/** Infested stone (silverfish), like mountains. */
	public BiomeMaker infestedStone() {
		BiomeDefaultFeatures.addInfestedStone(generation);
		return this;
	}

	// ------------------------------------------------------------------------------------------
	// Climate
	// ------------------------------------------------------------------------------------------

	/** Temperature of the biome (snow, plant colors). Plains 0.8, forest 0.7, taiga 0.25, desert 2.0. Default 0.8. */
	public BiomeMaker temperature(float temperature) {
		this.temperature = temperature;
		return this;
	}

	/** How wet the biome is (plant colors). Plains 0.4, forest 0.8, desert 0. Default 0.4. */
	public BiomeMaker downfall(float downfall) {
		this.downfall = downfall;
		return this;
	}

	/** No rain or snow, like a desert. */
	public BiomeMaker noRain() {
		this.precipitation = false;
		return this;
	}

	// ------------------------------------------------------------------------------------------
	// Colors and sound
	// ------------------------------------------------------------------------------------------

	/** Water color, e.g. {@code 0x3F76E4} (vanilla default). */
	public BiomeMaker waterColor(int rgb) {
		effects.waterColor(rgb);
		return this;
	}

	/** Grass color. Leave it out to use the color from temperature and downfall. */
	public BiomeMaker grassColor(int rgb) {
		effects.grassColorOverride(rgb);
		return this;
	}

	/** Leaves color. Leave it out to use the color from temperature and downfall. */
	public BiomeMaker foliageColor(int rgb) {
		effects.foliageColorOverride(rgb);
		return this;
	}

	/** Color of dry leaves (leaf litter). */
	public BiomeMaker dryFoliageColor(int rgb) {
		effects.dryFoliageColorOverride(rgb);
		return this;
	}

	/** Special grass coloring, e.g. {@code DARK_FOREST} or {@code SWAMP}. */
	public BiomeMaker grassColorModifier(BiomeSpecialEffects.GrassColorModifier modifier) {
		effects.grassColorModifier(modifier);
		return this;
	}

	/** Sky color. Leave it out to use vanilla's color for the biome's temperature. */
	public BiomeMaker skyColor(int rgb) {
		this.skyColor = rgb;
		return this;
	}

	/** Fog color. */
	public BiomeMaker fogColor(int rgb) {
		this.fogColor = rgb;
		return this;
	}

	/** Fog color under water. */
	public BiomeMaker waterFogColor(int rgb) {
		this.waterFogColor = rgb;
		return this;
	}

	/** Background music, e.g. {@code SoundEvents.MUSIC_BIOME_MEADOW}. */
	public BiomeMaker music(Holder<SoundEvent> music) {
		this.music = music;
		return this;
	}

	/**
	 * Particles floating in the air, like the spores of crimson forests or the ash of basalt deltas.
	 * Can be called several times for several particles.
	 *
	 * @param particle    e.g. {@code ParticleTypes.CRIMSON_SPORE}, {@code ParticleTypes.WHITE_ASH},
	 *                    {@code ParticleTypes.CHERRY_LEAVES}, {@code ParticleTypes.SPORE_BLOSSOM_AIR}
	 * @param probability how many: vanilla uses 0.025 (crimson spores), 0.118 (basalt ash), 0.00625 (soul sand
	 *                    valley ash). Try values between 0.001 and 0.1.
	 */
	public BiomeMaker particles(ParticleOptions particle, float probability) {
		particles.add(new AmbientParticle(particle, probability));
		return this;
	}

	/** A sound that loops while you are in the biome, e.g. {@code SoundEvents.AMBIENT_CRIMSON_FOREST_LOOP}. */
	public BiomeMaker ambientLoop(Holder<SoundEvent> sound) {
		AmbientSounds current = ambientSounds != null ? ambientSounds : AmbientSounds.EMPTY;
		this.ambientSounds = new AmbientSounds(Optional.of(sound), current.mood(), current.additions());
		return this;
	}

	/** The spooky cave sounds of vanilla Overworld biomes. */
	public BiomeMaker caveMoodSounds() {
		AmbientSounds current = ambientSounds != null ? ambientSounds : AmbientSounds.EMPTY;
		this.ambientSounds = new AmbientSounds(current.loop(), Optional.of(AmbientMoodSettings.LEGACY_CAVE_SETTINGS), current.additions());
		return this;
	}

	/** Full control over ambient sounds (loop, mood and random additions). */
	public BiomeMaker ambientSounds(AmbientSounds sounds) {
		this.ambientSounds = sounds;
		return this;
	}

	// ------------------------------------------------------------------------------------------
	// Features (trees, plants, ores...)
	// ------------------------------------------------------------------------------------------

	/**
	 * Adds one placed feature, e.g. your tree.
	 *
	 * <p><b>Order matters.</b> If two biomes have the same features, they must be in the same order in both
	 * (for example vanilla grass before vanilla flowers), or Minecraft stops with "Feature order cycle".
	 * Use vanilla's {@code BiomeDefaultFeatures} helpers through {@link #features} to keep vanilla's order,
	 * and add your own features after them.
	 */
	public BiomeMaker addFeature(GenerationStep.Decoration step, ResourceKey<PlacedFeature> feature) {
		generation.addFeature(step, feature);
		return this;
	}

	/** Adds a carver (caves, canyons). */
	public BiomeMaker addCarver(ResourceKey<ConfiguredWorldCarver<?>> carver) {
		generation.addCarver(carver);
		return this;
	}

	/**
	 * Runs any vanilla helper (or your own) on the generation settings, e.g.
	 * {@code .features(BiomeDefaultFeatures::addMeadowVegetation)} or
	 * {@code .features(BiomeDefaultFeatures::addForestFlowers)}.
	 */
	public BiomeMaker features(Consumer<BiomeGenerationSettings.Builder> vanillaHelper) {
		vanillaHelper.accept(generation);
		return this;
	}

	// ------------------------------------------------------------------------------------------
	// Mobs
	// ------------------------------------------------------------------------------------------

	/**
	 * Adds a mob spawn. The category (animal, monster, water...) is taken from the entity type.
	 *
	 * @param weight how often compared to other mobs of the same category (vanilla cow = 8, zombie = 95)
	 */
	public BiomeMaker spawn(EntityType<?> type, int weight, int minGroup, int maxGroup) {
		spawns.addSpawn(type.getCategory(), weight, new MobSpawnSettings.SpawnerData(type, minGroup, maxGroup));
		return this;
	}

	/** Adds a mob spawn with an explicit category. */
	public BiomeMaker spawn(MobCategory category, EntityType<?> type, int weight, int minGroup, int maxGroup) {
		spawns.addSpawn(category, weight, new MobSpawnSettings.SpawnerData(type, minGroup, maxGroup));
		return this;
	}

	/** Sheep, pigs, chickens and cows, like vanilla plains/forests. */
	public BiomeMaker farmAnimals() {
		BiomeDefaultFeatures.farmAnimals(spawns);
		return this;
	}

	/**
	 * The normal Overworld night mobs (zombies, skeletons, creepers, spiders, endermen, witches, slimes...)
	 * plus bats and glow squids in caves, like most vanilla biomes.
	 */
	public BiomeMaker defaultMonsters() {
		BiomeDefaultFeatures.commonSpawns(spawns);
		return this;
	}

	/** How often animals spawn when chunks generate (vanilla default 0.1, snowy biomes 0.07). */
	public BiomeMaker animalSpawnChance(float chance) {
		spawns.creatureGenerationProbability(chance);
		return this;
	}

	/** Bats in caves and glow squids in underground water (already part of {@link #defaultMonsters()}). */
	public BiomeMaker caveCreatures() {
		BiomeDefaultFeatures.caveSpawns(spawns);
		return this;
	}

	/**
	 * Exactly the mobs of vanilla plains: farm animals, horses, donkeys and the normal monsters (with zombie
	 * horses). Don't combine it with {@link #farmAnimals()} or {@link #defaultMonsters()}, it already has them.
	 */
	public BiomeMaker plainsMobs() {
		BiomeDefaultFeatures.plainsSpawns(spawns);
		return this;
	}

	/** Runs any vanilla helper (or your own) on the spawn settings, e.g. {@code .spawns(BiomeDefaultFeatures::plainsSpawns)}. */
	public BiomeMaker spawns(Consumer<MobSpawnSettings.Builder> vanillaHelper) {
		vanillaHelper.accept(spawns);
		return this;
	}

	// ------------------------------------------------------------------------------------------

	public Biome build() {
		Biome.BiomeBuilder builder = new Biome.BiomeBuilder()
				.hasPrecipitation(precipitation)
				.temperature(temperature)
				.downfall(downfall)
				.specialEffects(effects.build())
				.mobSpawnSettings(spawns.build())
				.generationSettings(generation.build());
		if (skyColor != null) {
			builder.setAttribute(EnvironmentAttributes.SKY_COLOR, skyColor);
		} else if (skyFromTemperature) {
			builder.setAttribute(EnvironmentAttributes.SKY_COLOR, OverworldBiomes.calculateSkyColor(temperature));
		}
		if (!particles.isEmpty()) {
			builder.setAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, List.copyOf(particles));
		}
		if (ambientSounds != null) {
			builder.setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, ambientSounds);
		}
		if (fogColor != null) {
			builder.setAttribute(EnvironmentAttributes.FOG_COLOR, fogColor);
		}
		if (waterFogColor != null) {
			builder.setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, waterFogColor);
		}
		if (music != null) {
			builder.setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(music));
		}
		return builder.build();
	}
}
