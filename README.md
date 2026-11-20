# JONE500 Biome API

> **Beta (0.x).** The API may still change before 1.0.0.

A Fabric library for **Minecraft 26.1** that adds your Overworld biomes in **regions**:

- **Deterministic:** regions are sorted by id, so the same seed always gives the same world, no matter
  in which order mods load.
- **Per-patch rolls:** a replacement can happen in only part of your region's patches
  (e.g. 30%), so your biomes don't all appear at once.
- **Exclusive biomes:** "at most one of these biomes in the same patch".
- **Decimal weights:** very rare regions are possible (`weight(0.25F)`).
- **World-wide replacement:** "30% of all forests become my biome", or "in 50% of worlds, every cherry grove is my biome" (also caves and oceans), without a region.
- **Strict climate:** your inland biome never shows up in the sea.
- **Surface rules** for your biomes (which blocks make up the ground).

Works in the **Overworld** and the **Nether** (regions), and the **End** (replacements; placement with Fabric API).

## How it works (short)

The Overworld is split into big patches (about 512 blocks apart). Each patch is either vanilla or one of
the registered regions, picked by weight (vanilla = 10). Inside your region the terrain and climate are
exactly vanilla, but the vanilla biomes you replaced become your biomes.

## 1. Add it to your mod

```groovy
repositories {
    exclusiveContent {
        forRepository { maven { name = "Modrinth"; url = "https://api.modrinth.com/maven" } }
        filter { includeGroup "maven.modrinth" }
    }
}

dependencies {
    implementation "maven.modrinth:jone500-biome-api:0.1.0+26.1"
}
```

`fabric.mod.json`:

```json
"depends": {
    "jone500-biome-api": ">=0.1.0"
}
```

## 2. Make your biome

Your biome needs a normal biome JSON in `data/<your mod id>/worldgen/biome/<name>.json`, like any other
modded biome. First make a key for it:

```java
public static final ResourceKey<Biome> APPLE_GROVE =
        ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("mymod", "apple_grove"));
```

### Easiest: `BiomeMaker` in data generation

`BiomeMaker.overworld(context)` starts with everything vanilla puts **underground** in normal biomes
(caves, lava lakes, geodes, monster rooms, stone veins, springs, all ores, sand/clay/gravel disks). You
only add what is special about your biome:

```java
public static void bootstrap(BootstrapContext<Biome> context) {
    context.register(APPLE_GROVE, BiomeMaker.overworld(context)
            .temperature(0.55F)
            .downfall(0.4F)
            .waterColor(0x3F76E4)
            .grassColor(0x91BD59)
            .foliageColor(0x77AB2F)
            .fogColor(0xC0D8FF)
            .waterFogColor(0x050533)
            .music(SoundEvents.MUSIC_BIOME_MEADOW)
            .farmAnimals()                       // sheep, pigs, chickens, cows
            .defaultMonsters()                   // zombies, skeletons, creepers... + bats in caves
            .spawn(EntityType.FOX, 8, 2, 4)      // any mob: type, weight, min group, max group
            .features(BiomeDefaultFeatures::addPlainGrass)        // any vanilla helper
            .features(BiomeDefaultFeatures::addMeadowVegetation)
            .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.APPLE_TREE)  // your own
            .build());
}
```

| Method | What it does |
|---|---|
| `BiomeMaker.overworld(context)` / `BiomeMaker.empty(context)` | Start with vanilla's underground / with nothing |
| `.vanillaCaves()`, `.vanillaGeodes()`, `.vanillaMonsterRooms()`, `.vanillaStoneVeins()`, `.vanillaSprings()`, `.vanillaFreezing()`, `.vanillaOres()`, `.vanillaDisks()` | The parts of vanilla's underground one by one (all already in `overworld(...)`; use them with `empty(...)` to pick only some) |
| `.vanillaOresWithLargeCopper()`, `.extraGold()`, `.emeralds()`, `.infestedStone()` | Extra ores like dripstone caves / badlands / mountains |
| `.temperature(f)`, `.downfall(f)`, `.noRain()` | Climate (snow, plant colors) |
| `.waterColor`, `.grassColor`, `.foliageColor`, `.dryFoliageColor`, `.grassColorModifier` | Colors (`0xRRGGBB`) |
| `.skyColor`, `.fogColor`, `.waterFogColor` | Sky and fog (sky defaults to vanilla's color for the temperature) |
| `.music(sound)` | Background music |
| `.particles(particle, probability)` | Floating particles (can be called several times), e.g. `ParticleTypes.CHERRY_LEAVES`, `WHITE_ASH`, `CRIMSON_SPORE`. Vanilla uses 0.025 for crimson spores |
| `.ambientLoop(sound)`, `.caveMoodSounds()`, `.ambientSounds(...)` | Ambient sounds (loop, cave mood, full control) |
| `BiomeMaker.nether(context)` / `BiomeMaker.end(context)` | Start a Nether biome (nether caves, lava springs, fire, glowstone, magma, nether ores) / an End biome (endermen) |
| `.addFeature(step, feature)`, `.addCarver(carver)` | One feature or carver |
| `.features(BiomeDefaultFeatures::...)` | Any vanilla feature helper |
| `.farmAnimals()`, `.defaultMonsters()`, `.caveCreatures()`, `.plainsMobs()` | Vanilla mob sets |
| `.spawn(type, weight, min, max)`, `.spawn(category, type, ...)`, `.spawns(...)`, `.animalSpawnChance(f)` | Your own mob spawns |

### Read this before using `addFeature`: feature order and duplicates (warning)

This is a **Minecraft rule**, not a Biome API rule, but it is the most common reason a world with new
biomes does not load. Minecraft puts the features of **all biomes** (vanilla, yours and other mods') into
one order per generation step. If that is impossible, the world stops loading with
**"Feature order cycle found"**.

Two things break it (both checked by this API's own tests):

1. **The same feature twice in one biome.** `BiomeMaker.overworld(...)` already has every vanilla ore and
   the sand/clay/gravel disks, so adding `OrePlacements.ORE_DIAMOND` again breaks the world.
2. **Two biomes with the same features in opposite order.** Biome A has pumpkins then sugar cane,
   biome B has sugar cane then pumpkins: each is fine alone, both together break the world.

How to stay safe:
- For vanilla features use vanilla's `BiomeDefaultFeatures` helpers, or the `BiomeMaker` methods
  (`.vanillaOres()`, `.vanillaDisks()`, ...). They add features in vanilla's order.
- Add **your own** features (your own placed features) **after** the vanilla ones.
- If you add single vanilla features with `addFeature`, keep vanilla's order (e.g. stone veins, then ores
  from coal to diamond, then disks) and never add one that is already there.
- Use the same order for the same features in all of your biomes.

> **We can't check every combination for you.** There are hundreds of vanilla features and any mod can add
> more, so we do not know in advance which `addFeature(...)` combination conflicts with which biome, also
> biomes from other mods. The Biome API checks the order when a world loads and stops with a message that
> says which biomes conflict, so you find out right away. **Always create a new world to test after
> changing features**, with the other biome mods you want to support installed too.

### Structures and mob variants: `VanillaBiomeTags`

Vanilla decides **structures** (villages, mineshafts, strongholds, outposts, ruined portals, trial chambers,
trail ruins...) and **mob variants** (warm/cold cows, pigs, chickens and frogs, snow foxes, white rabbits,
some wolves) with **biome tags**. A new biome is in no tags, so it has **no villages, mineshafts or
strongholds** and only the normal mob variants.

Give your biome the same tags as a similar vanilla biome, in your biome tag provider (data generation):

```java
public class ModBiomeTagProvider extends FabricTagsProvider<Biome> {
    public ModBiomeTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.BIOME, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        // Everything plains has: mineshafts, plains villages, outposts, strongholds, ruined portals...
        VanillaBiomeTags.copy(Biomes.PLAINS, ModBiomes.APPLE_GROVE, (tag, biome) -> builder(tag).add(biome));

        // Like savanna (warm cows/pigs/chickens, spotted wolves), but without villages
        VanillaBiomeTags.copy(Biomes.SAVANNA, ModBiomes.DRY_ORCHARD, VanillaBiomeTags.NO_VILLAGES,
                (tag, biome) -> builder(tag).add(biome));

        // Or pick tags yourself
        builder(BiomeTags.HAS_MINESHAFT).add(ModBiomes.ANCIENT_WILDS);
    }
}
```

| Filter | Copies |
|---|---|
| `VanillaBiomeTags.ALL` (default) | Every tag of the vanilla biome |
| `VanillaBiomeTags.NO_VILLAGES` | Everything except villages |
| `VanillaBiomeTags.ONLY_STRUCTURES` | Only structures |
| `VanillaBiomeTags.NO_STRUCTURES` | Only mob variants and `is_overworld` / `is_forest` / ... |
| your own `tag -> ...` | Anything you want |

`VanillaBiomeTags.of(Biomes.PLAINS)` lists the tags, if you want to see them.

Good to know:
- If your biome JSON is **hand-written** (not made in data generation), use `builder(tag).addOptional(biome)`.
- A few wolf variants are tied to exact vanilla biomes, not tags (woods = forest, ashen = snowy taiga,
  black = old growth pine taiga, chestnut = old growth spruce taiga, snowy = grove). In your biome those
  wolves are the normal (pale) variant.

### Caves under your biome

Biomes are **3D**: every position has a biome, also underground.
- Vanilla **cave biomes** (lush caves, dripstone caves, deep dark) stay under your regions, like in vanilla.
  Your surface biome continues underground wherever vanilla's surface biome would.
- **Noise caves** (the big open caves) are part of the terrain and exist under every biome.
- **Carved caves and canyons** come from the biome: `BiomeMaker.overworld(...)` / `.vanillaCaves()` adds them.
- With `ClimatePoints` you choose: `depth(Depth.SURFACE)` (default) keeps cave biomes below,
  `depth(Depth.SURFACE_AND_UNDERGROUND)` makes your biome replace them too, `depth(Depth.UNDERGROUND)` makes
  a cave biome. To swap a vanilla cave biome in every world, use `BiomeReplacements.replaceWholeBiome`.

## 3. Register a region

In your mod's `onInitialize()`:

```java
BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "orchards"))
        .weight(2)                                                  // vanilla is 10
        .replace(Biomes.PLAINS, ModBiomes.APPLE_GROVE)              // in every patch of this region
        .replace(Biomes.FOREST, ModBiomes.GOLDEN_ORCHARD, 0.3F)     // only in 30% of the patches
        .register();
```

### All builder methods

| Method | What it does |
|---|---|
| `.overworld()` / `.nether()` | Which dimension the region is in. Default: Overworld |
| `.weight(float)` | How often this region's patches appear. Vanilla = 10. Decimals allowed. Default 1. |
| `.replace(vanilla, yours)` | Inside this region, `vanilla` becomes `yours` |
| `.replace(vanilla, yours, chance)` | Same, but only in `chance` (0-1) of this region's patches; elsewhere the vanilla biome stays |
| `.addBiome(ClimatePoints..., yours[, chance])` | Adds your biome at its **own climate** (see below), only inside this region |
| `.addBiome(point, yours[, chance])` | Same with a raw `Climate.ParameterPoint` |
| `.exclusive(a, b, ...)` | At most one of these biomes in the same patch; the others keep the vanilla biome there |

### Your biome with its own climate: `ClimatePoints`

Instead of replacing a vanilla biome, describe where your biome goes, with names instead of numbers:

```java
BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "orchards"))
        .weight(2)
        .addBiome(ClimatePoints.builder()
                .temperature(Temperature.NEUTRAL, Temperature.WARM)
                .humidity(Humidity.NEUTRAL, Humidity.WET)
                .continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_5, Erosion.EROSION_6)
                .terrain(Terrain.MID, Terrain.LOW), ModBiomes.APPLE_GROVE)
        .register();
```

Several values = "any of them". Anything you leave out = everything (depth defaults to `SURFACE`).
Inside the region your biome wins where its climate matches; elsewhere vanilla stays.

| Setting | Values (from → to) | Meaning |
|---|---|---|
| `temperature` | `FROZEN`, `COLD`, `NEUTRAL`, `WARM`, `HOT` | Snowy plains → desert |
| `humidity` | `ARID`, `DRY`, `NEUTRAL`, `WET`, `HUMID` | Desert → jungle |
| `continentalness` | `MUSHROOM_FIELDS`, `DEEP_OCEAN`, `OCEAN`, `COAST`, `NEAR_INLAND`, `MID_INLAND`, `FAR_INLAND` (+ `INLAND`, `ALL_LAND`) | Distance from the sea |
| `erosion` | `EROSION_0` … `EROSION_6` | `0` = mountains … `6` = very flat (swamps, flat plains) |
| `terrain` | `VALLEY`, `LOW`, `MID`, `HIGH`, `PEAK` | Land shape: rivers → normal land → mountain peaks |
| `depth` | see the table below | How high / deep |
| `offset` | a number, e.g. `0.375F` | Makes the biome smaller at its edges (vanilla uses this for rare biomes) |

**Depth** is about 0 at the ground surface, positive below it and **negative above it** (air, and sea water
above the sea floor):

| `Depth` | Range | Use it for |
|---|---|---|
| `SURFACE` (default) | 0 and 1 | Almost every biome: plains, forests, mountains, **also oceans and rivers** (vanilla oceans use this; the climate makes them oceans). Cave biomes stay below. |
| `UNDERGROUND` | 0.2 to 0.9 | Cave biomes (like lush caves, dripstone caves) |
| `DEEP_UNDERGROUND` | 1.1 | The very bottom (like the deep dark) |
| `SURFACE_AND_UNDERGROUND` | 0 to 1.1 | From the ground down to the bottom; also takes the place of cave biomes below |
| `ALL_HEIGHTS` | **-1** to 1.1 | Every height, **also above the ground** (air and water). For special biomes that must also own the water/air above lower ground nearby. Can show up in the water of nearby seas; add `.strict()` if you don't want that |

Each setting also takes your own numbers, so you are not limited to the named values:

```java
ClimatePoints.builder()
        .temperature(-0.2F, 0.4F)                              // numbers from -1 to 1
        .humidity(-0.5F, 0.5F)
        .continentalness(Continentalness.between(Continentalness.COAST, Continentalness.MID_INLAND))
        .erosion(Erosion.EROSION_4, Erosion.EROSION_5)          // each setting can use names or numbers
        .depth(0.0F, 1.0F)                                      // or .depth(-1.0F, 1.1F) for every height
```

`between(a, b)` makes one range from `a` to `b` (available on every named type). For advanced use,
`.weirdness(Weirdness...)` gives the 13 raw weirdness slices that `terrain` groups together.

### My biome appears in the sea! (`strict()` and depth)

Minecraft does not check "is this climate inside the ranges?". It picks the biome whose climate is
**closest**. Two things make an inland biome show up in oceans:

1. **Negative depth.** Depth is about 0 at the ground surface, positive below it and **negative above it**
   (in the air, and in sea water above the sea floor). Vanilla surface biomes only use depth 0 and 1. A biome
   with `depth(-1.0F, 1.0F)` (common in TerraBlender setups) matches the sea water "perfectly" on depth and
   wins there. Use `Depth.SURFACE` (the default) unless you really need `ALL_HEIGHTS` / negative depth.
2. **Closest, not inside.** Near the edge of your ranges your biome can still be the closest one a little
   outside them. Add `.strict()` and the biome is only used where temperature, humidity, continentalness,
   erosion and terrain are really inside your ranges; elsewhere the vanilla biome stays.

```java
.addBiome(ClimatePoints.builder()
        .continentalness(Continentalness.INLAND)
        .temperature(Temperature.NEUTRAL, Temperature.WARM)
        .strict(), ModBiomes.APPLE_GROVE)
```

Measured with the same inland biome over a large area (times it landed in ocean/coast climate):
`depth(-1, 1)` = 5779, `Depth.SURFACE` = 4, `Depth.SURFACE` + `strict()` = **0**.

### Example: biomes that should not appear together

```java
BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "meadows"))
        .weight(2)
        .replace(Biomes.MEADOW, ModBiomes.MINT_MEADOW)
        .replace(Biomes.CHERRY_GROVE, ModBiomes.AMBER_MEADOW)
        .exclusive(ModBiomes.MINT_MEADOW, ModBiomes.AMBER_MEADOW)  // never both in one patch
        .register();
```

## 3b. Replace a vanilla biome everywhere (no region needed)

There are two kinds. Both work for **any** Overworld biome: surface, ocean and cave biomes.

| | `replace(a, b, chance)` | `replaceWholeBiome(a, b, chance)` |
|---|---|---|
| What `chance` means | % of `a`'s **area**, in every world | % of **worlds** where **all** of `a` becomes `b` |
| In one world | A mix: some `a`, some `b` | All `a` **or** all `b`, never both |
| `chance = 1` | Every `a` is `b` | Every `a` is `b` (same) |
| Example | "30% of forests are apple forests" | "Half of all worlds have apple groves instead of cherry groves" |

### All or nothing per world: `replaceWholeBiome`

```java
// When a world is created, its seed decides once: 50% chance that EVERY cherry grove in it is an apple grove.
BiomeReplacements.replaceWholeBiome(Biomes.CHERRY_GROVE, ModBiomes.APPLE_GROVE, 0.5F);

// Every world: lush caves (a cave biome) are always crystal caves.
BiomeReplacements.replaceWholeBiome(Biomes.LUSH_CAVES, ModBiomes.CRYSTAL_CAVES, 1.0F);
```

Several biomes can compete for the same vanilla biome (at most one wins per world) if their chances add up
to 1 or less. The log says which swaps are on when a world loads:
`This world: every minecraft:cherry_grove is replaced by mymod:apple_grove`.
If both kinds are used on the same biome, the whole-biome swap wins in the worlds where it is on.

### Part of the area: `replace`

```java
BiomeReplacements.replace(Biomes.FOREST, ModBiomes.APPLE_FOREST, 0.3F);   // about 30% of all forest areas
BiomeReplacements.replace(Biomes.FOREST, ModBiomes.PEAR_FOREST, 0.2F);    // another 20% (total at most 100%)
```

The world is split into small patches (about 256 blocks apart); in each patch the seed decides whether the
forest there is replaced. The rest stays vanilla forest, so you get a mix instead of every forest being
gone. This works in the whole Overworld, also inside regions where they keep the vanilla biome.

| | `BiomeRegion...replace(a, b, chance)` | `BiomeReplacements.replace(a, b, chance)` |
|---|---|---|
| Where | Only inside your region's patches | Everywhere |
| `chance` means | % of your region's patches | % of the vanilla biome's area |
| Patch size | Region patches (~512 blocks) | Small patches (~256 blocks, `BiomeReplacements.setPatchSize`) |

### Only vanilla biomes: change how often they appear

You don't need a biome of your own: the replacement can be a vanilla biome too.

```java
BiomeReplacements.replace(Biomes.DESERT, Biomes.SAVANNA, 0.5F);          // half as much desert, more savanna
BiomeReplacements.replace(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, 0.3F); // more sunflower plains
BiomeReplacements.replaceWholeBiome(Biomes.MUSHROOM_FIELDS, Biomes.DEEP_OCEAN, 1.0F); // no mushroom islands
```

Pick a replacement that fits the place (similar temperature and terrain): desert to savanna is fine, desert
to snowy plains looks wrong. A biome can only get more common by taking area from other biomes.

## 4. Surface rules (optional)

```java
BiomeSurfaceRules.addOverworld(Identifier.fromNamespaceAndPath("mymod", "surface"),
        SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.APPLE_GROVE),
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                        SurfaceRules.state(Blocks.COARSE_DIRT.defaultBlockState()))));
```

Your rules run before the vanilla rules. Always wrap them in `SurfaceRules.isBiome(...)` so they only
change your own biomes. For the Nether use `BiomeSurfaceRules.addNether(...)` the same way.

## 4b. The Nether

The Nether works like the Overworld: same regions, chances, `exclusive`, `strict`, replacements. Add
`.nether()` to the region:

```java
BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "embers"))
        .nether()
        .weight(5)
        .replace(Biomes.CRIMSON_FOREST, ModBiomes.EMBER_FOREST)                 // in this region's patches
        .addBiome(ClimatePoints.netherPoint(0.25F, -0.3F), ModBiomes.EMBER_FOREST) // its own spot
        .register();
```

The Nether has only 5 vanilla biomes. Each one is a **point** in temperature × humidity, and the closest
point wins (so each biome owns the area around its point):

| Biome | `netherPoint(temperature, humidity)` |
|---|---|
| Nether wastes | `(0, 0)` |
| Soul sand valley | `(0, -0.5)` |
| Crimson forest | `(0.4, 0)` |
| Warped forest | `(0, 0.5)` (offset 0.375) |
| Basalt deltas | `(-0.5, 0)` (offset 0.175) |

Put your point between them; `netherPoint(t, h, offset)` makes it smaller with a higher offset. Make the
biome with `BiomeMaker.nether(context)` and copy tags (fortresses, bastions, `is_nether`) with
`VanillaBiomeTags.copy(Biomes.CRIMSON_FOREST, ...)`.

## 4c. The End

The End does not use climate: vanilla picks the main island, highlands, midlands, barrens or small islands
from the terrain. Place End biomes with **Fabric API** (already a dependency), and use this API for
replacements:

```java
TheEndBiomes.addHighlandsBiome(ModBiomes.VOID_MEADOW, 10.0);      // Fabric API: a share of the highlands
TheEndBiomes.addSmallIslandsBiome(ModBiomes.VOID_MEADOW, 5.0);    // ... or midlands / barrens / main island

BiomeReplacements.replace(Biomes.END_BARRENS, ModBiomes.ASH_BARRENS, 0.3F);              // this API: 30% of the area
BiomeReplacements.replaceWholeBiome(Biomes.SMALL_END_ISLANDS, ModBiomes.VOID_MEADOW, 1.0F); // or all of it
```

Make the biome with `BiomeMaker.end(context)` (add e.g. `EndPlacements.CHORUS_PLANT`) and copy tags (end
cities, `is_end`) with `VanillaBiomeTags.copy(Biomes.END_HIGHLANDS, ...)`.

## 5. Test it

Create a **new** world (regions only apply to worlds created with the mod) and press **F3**: the line
`Biome API region: ...` shows which region you are in. `/locate biome mymod:apple_grove` works too.

## 6. More examples

All examples below compile against this version of the API. `ModBiomes.*` are your own biome keys.

### Biomes: vanilla helpers and your own features together

`BiomeMaker.overworld(context)` already has vanilla's caves, ores and disks, so you only add what is
different. Vanilla's `BiomeDefaultFeatures` helpers and your own `addFeature(...)` can be mixed freely:

```java
// A forest: vanilla flowers, birch trees and grass + emeralds + pumpkins
context.register(ModBiomes.CLAY_FOREST, BiomeMaker.overworld(context)
        .temperature(0.7F)
        .downfall(0.8F)
        .farmAnimals()
        .defaultMonsters()
        .features(BiomeDefaultFeatures::addForestFlowers)
        .features(BiomeDefaultFeatures::addOtherBirchTrees)
        .features(BiomeDefaultFeatures::addDefaultFlowers)
        .features(BiomeDefaultFeatures::addForestGrass)
        .features(BiomeDefaultFeatures::addDefaultMushrooms)
        .emeralds()                                                                              // like mountains
        .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_PUMPKIN)
        .build());
```

Pick only some underground parts with `BiomeMaker.empty(context)` and add the ores you want yourself:

```java
// Caves full of diamonds and clay, but no coal, gold, redstone...
context.register(ModBiomes.DIAMOND_CAVES, BiomeMaker.empty(context)
        .temperature(0.8F)
        .downfall(0.4F)
        .vanillaCaves()                                     // caves, canyons, lava lakes
        .vanillaStoneVeins()                                // granite, diorite, andesite, tuff, dirt, gravel
        .addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, CavePlacements.AMETHYST_GEODE)
        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_IRON_UPPER)
        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_IRON_MIDDLE)
        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIAMOND)
        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIAMOND_LARGE)
        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIAMOND_BURIED)
        .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_CLAY)
        .caveCreatures()                                    // bats, glow squid
        .build());
```

> **Don't add a vanilla feature twice.** `overworld(...)` already contains every vanilla ore and the
> sand/clay/gravel disks, so `.addFeature(UNDERGROUND_ORES, OrePlacements.ORE_DIAMOND)` on top of it adds
> diamonds twice and the world does not load ("Feature order cycle found"). For more of a vanilla ore, start
> from `empty(...)` (like above) or make your own placed feature. See
> [Read this before using `addFeature`](#read-this-before-using-addfeature-feature-order-and-duplicates-warning).

### Mobs: vanilla helpers and your own spawns together

```java
context.register(ModBiomes.SLIME_MARSH, BiomeMaker.overworld(context)
        .temperature(0.8F)
        .downfall(0.9F)
        .spawns(BiomeDefaultFeatures::farmAnimals)                             // vanilla helper (= .farmAnimals())
        .spawns(BiomeDefaultFeatures::commonSpawns)                            // vanilla helper (= .defaultMonsters())
        .spawn(EntityType.SLIME, 100, 1, 4)                                    // monster (default category)
        .spawn(MobCategory.CREATURE, EntityType.FROG, 10, 2, 5)                // pick the category yourself
        .spawn(MobCategory.WATER_AMBIENT, EntityType.TROPICAL_FISH, 25, 8, 8)
        .animalSpawnChance(0.07F)                                              // more animals when chunks generate
        .features(BiomeDefaultFeatures::addSwampVegetation)
        .build());
```

| Vanilla spawn helper | What it adds |
|---|---|
| `BiomeDefaultFeatures::farmAnimals` | Sheep, pigs, chickens, cows |
| `BiomeDefaultFeatures::commonSpawns` | Zombies, skeletons, creepers, spiders, endermen, witches, slimes, drowned + bats, glow squid |
| `BiomeDefaultFeatures::plainsSpawns` | Farm animals + horses, donkeys + common monsters |
| `BiomeDefaultFeatures::desertSpawns` | Rabbits, camels, husks, parched + the usual monsters |
| `BiomeDefaultFeatures::caveSpawns` | Bats, glow squid |
| `BiomeDefaultFeatures::mooshroomSpawns` | Mooshrooms + bats, glow squid (no monsters) |

Several helpers in one call work with a lambda: `.spawns(s -> { BiomeDefaultFeatures.farmAnimals(s); BiomeDefaultFeatures.caveSpawns(s); })`.

### A dry biome and a Nether biome

```java
context.register(ModBiomes.DUNES, BiomeMaker.overworld(context)
        .temperature(2.0F)
        .downfall(0.0F)
        .noRain()
        .grassColor(0xBFB755)
        .foliageColor(0xAEA42A)
        .music(SoundEvents.MUSIC_BIOME_DESERT)
        .spawns(BiomeDefaultFeatures::desertSpawns)
        .features(BiomeDefaultFeatures::addDesertVegetation)
        .features(BiomeDefaultFeatures::addDesertExtraDecoration)  // desert wells
        .particles(ParticleTypes.WHITE_ASH, 0.01F)
        .build());

context.register(ModBiomes.EMBER_FOREST, BiomeMaker.nether(context)     // nether ores, fire, glowstone...
        .fogColor(0x6B1E00)
        .particles(ParticleTypes.CRIMSON_SPORE, 0.025F)
        .music(SoundEvents.MUSIC_BIOME_CRIMSON_FOREST)
        .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NetherPlacements.CRIMSON_FOREST_VEGETATION)
        .spawn(EntityType.STRIDER, 60, 1, 2)
        .build());
```

### Regions: named values and your own numbers together

Every `ClimatePoints` setting takes names (`Temperature.COLD`), a range of names (`between(a, b)`) or
plain numbers (`-0.35F, 0.3F`), and you can mix them in one biome:

```java
BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "highlands"))
        .overworld()
        .weight(1.5F)
        .addBiome(ClimatePoints.builder()
                .temperature(Temperature.COLD, Temperature.NEUTRAL)                  // names
                .humidity(-0.35F, 0.3F)                                              // your own numbers
                .continentalness(Continentalness.between(Continentalness.MID_INLAND, Continentalness.FAR_INLAND))
                .erosion(Erosion.EROSION_1, Erosion.EROSION_2)                       // hilly
                .terrain(Terrain.HIGH)
                .strict(), ModBiomes.HIGHLAND_MEADOW)                                // never outside these ranges
        .register();
```

Replacements, your own climate, chances and `exclusive` in one region:

```java
BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "orchards"))
        .weight(3)
        .replace(Biomes.PLAINS, ModBiomes.APPLE_GROVE, 0.6F)          // 60% of this region's patches
        .replace(Biomes.SUNFLOWER_PLAINS, ModBiomes.APPLE_GROVE)       // every patch
        .addBiome(ClimatePoints.builder()
                .temperature(Temperature.WARM)
                .humidity(Humidity.NEUTRAL)
                .continentalness(Continentalness.INLAND)
                .offset(0.2F), ModBiomes.GOLDEN_ORCHARD, 0.5F)          // own climate, in 50% of the patches
        .exclusive(ModBiomes.APPLE_GROVE, ModBiomes.GOLDEN_ORCHARD)    // never both in one patch
        .register();
```

A rare cave biome (only underground, vanilla stays on the surface):

```java
BiomeRegion.builder(Identifier.fromNamespaceAndPath("mymod", "crystals"))
        .weight(0.25F)                                                 // 1 patch for every 40 vanilla patches
        .addBiome(ClimatePoints.builder()
                .humidity(Humidity.WET, Humidity.HUMID)
                .depth(Depth.UNDERGROUND), ModBiomes.CRYSTAL_CAVES)
        .register();
```

### Replacing biomes and changing how often they appear

```java
// Your biomes: part of the area / some worlds
BiomeReplacements.replace(Biomes.FOREST, ModBiomes.APPLE_FOREST, 0.25F);          // 25% of all forests
BiomeReplacements.replace(Biomes.FLOWER_FOREST, ModBiomes.APPLE_FOREST, 0.1F);    // same biome, other source
BiomeReplacements.replaceWholeBiome(Biomes.LUSH_CAVES, ModBiomes.CRYSTAL_CAVES, 0.3F); // 30% of worlds

// Only vanilla biomes (no biome of your own needed)
BiomeReplacements.replace(Biomes.DESERT, Biomes.SAVANNA, 0.3F);                   // desert: 30% savanna,
BiomeReplacements.replace(Biomes.DESERT, Biomes.BADLANDS, 0.1F);                  //   10% badlands, 60% desert
BiomeReplacements.replace(Biomes.FOREST, Biomes.BIRCH_FOREST, 0.2F);              // more birch forest
BiomeReplacements.replace(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, 0.3F);          // more sunflowers
BiomeReplacements.replaceWholeBiome(Biomes.MUSHROOM_FIELDS, Biomes.DEEP_OCEAN, 1.0F); // never mushroom islands
BiomeReplacements.replaceWholeBiome(Biomes.CHERRY_GROVE, Biomes.MEADOW, 0.5F);    // half of worlds: no cherry
```

The chances for one biome can be split between several replacements, as long as they add up to 1 or less.

### Modpack settings (before a world loads)

```java
BiomeRegions.setVanillaWeight(6.0F);    // vanilla patches weight (default 10): lower = modded regions more often
BiomeRegions.setRegionSize(8);          // 4 to 10 (default 7 = about 512 blocks); 8 = about twice as big
BiomeReplacements.setPatchSize(5);      // 3 to 10 (default 6 = about 256 blocks); smaller = smaller pieces
```

## Good to know

- Adding or removing a mod with regions changes where regions are in **new chunks** of existing worlds,
  like with any biome mod.
- For modpacks: `BiomeRegions.setVanillaWeight(...)` and `BiomeRegions.setRegionSize(...)` (before a world loads).

## Supported versions

Minecraft **26.1** (26.1, 26.1.1, 26.1.2) · Fabric · Requires **Fabric API** · Java 25

## License

MIT

Working on the Biome API itself? See [DEVELOPING.md](DEVELOPING.md).
