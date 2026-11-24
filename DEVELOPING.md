# Developing JONE500 Biome API

Notes for working on the Biome API itself. To **use** it in a mod, see [README.md](README.md).

## Project layout

| Folder | What it is |
|---|---|
| `src/main/java/net/biome/api/region`, `.../surface`, `.../climate`, `.../biome` | **Public API** (keep stable, see versioning) |
| `src/main/java/net/biome/api/impl` | Internal code, can change any time |
| `src/main/java/net/biome/api/mixin` | Hooks into Minecraft |
| `src/tutorial/java/net/biome/tutorial` | Tutorial mod (`biome_tutorial`) for users of the API: one lesson per file (regions, own climate, exclusive biomes, replacements, surface rules, Nether, End, data generation) |
| `src/tutorial/java/net/biome/tutorial/test` | Automated tests (game tests, client game tests, compat tests, `ReadmeExamples` = the README code must compile). They use the tutorial's biomes and regions, so change a lesson only together with its tests |
| `docs/research-terrablender-biolith.md` | How TerraBlender and Biolith work (reference for design decisions) |

Reference clones of TerraBlender and Biolith are in `D:\minecraft modding\biome-api-reference\`
(outside this project on purpose: both are **LGPL-3.0**, read them, never copy code from them).

## How it works

1. `BiomeRegions` keeps the regions **sorted by id**.
2. On `SERVER_STARTING` (before any level exists) `BiomeApiWorldSetup` takes the Overworld's and the
   Nether's `MultiNoiseBiomeSource` and builds a `RegionBiomeLookup` for each (Nether seed salted, so its
   map differs from the Overworld):
   - `RegionLayout` splits the world into patches (jittered grid + wavy borders) and picks a region per
     patch by weight. Pure function of seed + cell, nothing depends on load order.
   - Every region gets a copy of the Overworld's climate points, **relabeled** (replaced biome or original
     biome) plus its extra points. So a lookup is one tree search, like vanilla.
   - Per-patch chances and exclusive groups are rolled from (seed, patch, region id, biome id).
3. It then **recomputes** the biome source's possible biomes and the generator's feature order (the world
   creation screen already computed them without our biomes). Vanilla's `validate()` stays on.
   - World-wide replacements (`replace`, per small patch) and whole-biome swaps (`replaceWholeBiome`, one
     roll per **world seed**, same in every dimension) are applied after that. Each dimension only gets the
     replacements whose original biome it has.
3b. The **End** (`TheEndBiomeSource`) gets a lookup with replacements only. Its hook runs at `RETURN` with
   priority 900, before Fabric API's `TheEndBiomes` hook (which returns early); if we don't replace, Fabric
   still places its modded End biomes.
4. Mixins: `MultiNoiseBiomeSource.getNoiseBiome(...)` (lookup), `collectPossibleBiomes` (add biomes),
   `addDebugInfo` (F3 line), `TheEndBiomeSource` (replacements), `NoiseBasedChunkGenerator` (surface rules).

## Gradle tasks

| Task | What it does |
|---|---|
| `runTutorialClient` | Minecraft with the API + the tutorial mod |
| `runDatagen` | Writes the tutorial's generated biome JSON and tags to `src/tutorial/generated` |
| `runGameTest` | Region logic tests on a dedicated server (biomes appear, sorting, same seed = same world, weights, chances, patch size) |
| `runClientGameTest` | Real normal world: checks the Overworld got the regions, finds red plains, screenshot in `build/run/clientGameTest/screenshots` |
| `build` | `build/libs/jone500-biome-api-<version>.jar` |

## Minecraft versions

Branches 26.1, 26.2 and 26.3 (one git branch each, like Wooden API). What differs:

| | 26.1 | 26.2 | 26.3 |
|---|---|---|---|
| Biome lookup (main mixin) | `@WrapMethod getNoiseBiome(x, y, z, sampler)` | same | `@WrapMethod createResolver` / `createResolverForChunk`; the returned `BiomeResolver` is wrapped |
| Surface rules | `SurfaceRules`, `addOverworld(id, rule)` | `isBiome` needs the biome registry: `addOverworld(id, biomes -> rule)` | `MaterialRules` (data-driven): `addOverworld(id, ctx -> rule)` with `SurfaceRuleContext` |
| Surface mixin target | `buildSurface(..., Registry, Blender)` + `applyCarvers` | `buildSurface(..., Blender, Set)` + `applyCarvers` | `@ModifyVariable` on the `MaterialRule` argument of `buildSurface` + `generateCarvers` |
| Mobs / colors / carvers | `EntityType.X`, int colors, `ConfiguredWorldCarver` | `EntityTypes.X` | + `IntProvider` group sizes, `Vector3fc` colors, `WorldCarver`, animal chance is an attribute |
| Game test server | Nether and End are real | same | **every** dimension uses a fixed biome: Nether/End tests skip there and the client game test covers them |

Regions Unexplored has no 26.2/26.3 build yet, so it is only in the 26.1 compat tests.

### Vanilla data copied into the code (redo for every Minecraft branch)

- `impl/VanillaBiomeTagData.java`: which vanilla biome tags list each vanilla biome **directly**. Generated
  from `data/minecraft/tags/worldgen/biome/*.json` in the Minecraft jar (entries starting with `#` are
  skipped). Check that the structure / variant tags didn't change.
- `climate/*` enums: the climate ranges from vanilla's `OverworldBiomeBuilder`.
- `BiomeMaker`: names of `BiomeDefaultFeatures` methods, entity types and attribute value types (colors are
  `int` in 26.1; 26.3 uses `ARGB.vector3fFromRGB24`).

## Versioning

`mod_version` in `gradle.properties`; full version `<mod_version>+<minecraft_version>`.
While on `0.x` the API may change. From `1.0.0`: bug fix = `1.0.1`, new method = `1.1.0`,
removed/renamed public method = `2.0.0`.
