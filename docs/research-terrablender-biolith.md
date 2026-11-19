# Research: how TerraBlender and Biolith place biomes

Notes from reading the source of both libraries, to decide how JONE500 Biome API should work.

- TerraBlender: branch `26.3`, commit `7d7a778` (2026-10-02)
- Biolith: branch `main`, commit `2852bd2` (2026-11-10, already on 26.4 snapshots; branches `26.1`, `26.2`, `26.3` exist)
- Both clones are in `D:\minecraft modding\biome-api-reference\` (outside this project on purpose).

> **License:** both are **LGPL-3.0**. We read them to understand *what* has to be done, then write our
> own code. Do not copy code from them into this project (it would have to become LGPL).

---

## 1. The vanilla part both of them change

Overworld and Nether use `MultiNoiseBiomeSource`. For every biome lookup (quart position x/y/z):

1. `Climate.Sampler.sample(x, y, z)` gives a `Climate.TargetPoint`
   (temperature, humidity/vegetation, continentalness, erosion, depth, weirdness).
2. `Climate.ParameterList` holds a list of `(ParameterPoint, Biome)` pairs, stored in a `Climate.RTree`.
3. The R-tree returns the biome whose `ParameterPoint` is **closest** to the target point.

So there is only **one** list of points for the whole dimension. Adding a biome means adding points to
that list, and every new point takes space from whatever biome was closest before.

This lookup runs **millions of times** while chunks generate, so any hook here must be very cheap.

---

## 2. TerraBlender: "regions" (several R-trees, picked by a 2D layer)

### Idea
- Every mod registers one or more `Region`s with a `weight`. Each region has its **own full list of
  parameter points** (often a copy of vanilla with some biomes swapped).
- An old-style layer generator (like Minecraft before 1.18: weighted random start + zoom layers)
  produces a 2D map: "this x/z belongs to region N". Region size = number of zoom steps (config).
- Biome lookup = pick the region for this x/z, then search **that region's** R-tree.
- A region can use the placeholder biome `DEFERRED_PLACEHOLDER` in a spot; the lookup then falls back to
  region 0 (vanilla, or whatever the datapack defines).

### Where it hooks in (mixins, simplified)
| Hook | What it does |
|---|---|
| `Climate.ParameterList` | Adds the "uniqueness" layer and one R-tree per region; new method `findValuePositional(target, x, y, z)` |
| `MultiNoiseBiomeSource` (the two resolver lambdas) | Replaces `getNoiseBiome(target)` with the positional lookup above; adds "Region: ..." to F3 |
| `MinecraftServer` start (Fabric) | `LevelUtils.initializeOnServerStart`: for every level stem, initializes regions with the world seed and appends the modded biomes to the biome source's biome list |
| `NoiseGeneratorSettings` | Wraps the surface (material) rules so each mod's rules apply to its own namespace |
| `ChunkGenerator.validate` | **Cancelled completely**, because it would resolve feature order before modded biomes are added |
| `TheEndBiomeSource` | Separate weighted system for End biomes |

### Strengths
- Simple mental model for modders: "my region = my copy of the climate table".
- Modded biomes don't take space from vanilla points directly: they live in their own regions.

### Weak points (matches the open issues we read)
- **Region index = registration order** (`LinkedHashMap` filled when mods initialize). If mods load in a
  different order, the region map changes, so the same seed can give different biomes
  (issues #243/#244).
- **Weights are integers, minimum 1**, vanilla is 10 by default: hard to make a biome rare (#226).
- **A whole region appears at once**: all biomes in a region show up together; no per-instance
  conditions (#226).
- Cancelling `ChunkGenerator.validate` removes a vanilla safety check for everyone.
- Many crash reports happen at world creation or with other dimension mods (JAMD, End mods).

---

## 3. Biolith: edit the one list + local replacement noise

### Idea
- Keeps vanilla's single climate table, but **edits it once** the first time it is requested:
  remove points, add points, and add points for replacements/sub-biomes.
- **Replacement "in part"**: an extra seeded OpenSimplex noise (`getLocalNoise`, several octaves) decides
  at each position whether a biome is replaced, so e.g. 30% of a vanilla biome's area can become yours.
- **Sub-biomes with criteria**: inside a parent biome, choose a sub-biome by conditions on the climate
  values, the original biome, neighbors, ratios, deviation, all-of/any-of/not
  (`impl/biome/sub/*Criterion`). This is very close to what issue #226 asks TerraBlender for.
- Placements and surface rules can also come from **datapacks** (JSON).
- Includes `/biolith` commands to describe what is placed where.

### Where it hooks in (simplified)
| Hook | What it does |
|---|---|
| `MultiNoiseBiomeSource.parameters()` | Builds the edited parameter list once (removals, additions, replacements, sub-biomes) |
| `MultiNoiseBiomeSource` resolver lambdas | Re-does vanilla's sampling and runs Biolith's selection (replacement noise + sub-biome criteria) |
| `Climate.RTree` / `Parameter` / `ParameterPoint` | Extra search info needed for "fittest nodes" and criteria |
| `FeatureSorter` | Prevents "feature order cycle" crashes instead of disabling validation |
| `MinecraftServer`, `WorldStem`, `ServerLevel`, `LevelStem` | Gets the seed and registries at the right time |
| TerraBlender / Moderner Beta / Fabric End API | Explicit compatibility code; its biome source mixin has **priority 900 so it runs before TerraBlender** |

### Strengths
- Fine control: partial replacement, rare biomes, conditions; no "whole region at once".
- Placement is independent of mod load order (driven by noise + the climate points).
- Built-in compatibility with TerraBlender.

### Weak points
- Much more complex (about 1000 lines only in `impl/biome`, plus criteria, compat, data loaders).
- README still says "somewhat experimental": selection strategy may change and move biomes in existing
  worlds.

---

## 4. What this means for JONE500 Biome API

### Things we must do no matter which design
1. Hook `MultiNoiseBiomeSource` lookup (both libraries do it the same way: wrap the two resolver
   lambdas). Keep it **fast**.
2. Initialize with the **world seed and registries at server start**, for each level stem.
3. Add our biomes to the biome source's biome list (otherwise features/structures don't know them).
4. Surface rules per biome (TerraBlender: namespaced rule wrapper; Biolith: rule collector).
5. Don't break feature ordering: prefer Biolith's approach (fix `FeatureSorter`) over cancelling
   `validate`.
6. Make results **deterministic**: depend only on the seed and on sorted ids, never on mod load order.
7. Plan for TerraBlender and Biolith being installed at the same time as us.

### Design choice (to decide next)
| | Region layer (TerraBlender style) | Edit list + local noise (Biolith style) |
|---|---|---|
| Easy for modders | Yes | Medium |
| Rare / conditional biomes (#226) | Needs extra work | Natural |
| Whole set appears together | Yes (problem) | No |
| Code size | Smaller | Larger |
| Determinism | Must sort regions ourselves | Easier |

A possible middle way for our first version: **regions, but sorted by id and with a per-region-instance
roll** (seeded by region position) that can swap or drop individual biomes. That solves #226 and
#243/#244 for More Than Apples without building all of Biolith.

### Scope for version 1
- Minecraft **26.1 - 26.3** only.
- **Overworld only** first. Nether and End later.
- More Than Apples is the first user (5 biomes, currently one TerraBlender region each).
