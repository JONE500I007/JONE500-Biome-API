package net.biome.api.impl;

import java.util.List;
import java.util.Map;

/**
 * Which vanilla biome tags list each vanilla biome <b>directly</b> (Minecraft 26.1 data).
 * Tags that include other tags (e.g. {@code #minecraft:is_forest} inside {@code has_structure/mineshaft})
 * are not repeated: adding a biome to the direct tags is enough for those.
 *
 * <p>Generated from {@code data/minecraft/tags/worldgen/biome} of the Minecraft jar. Regenerate this table for
 * every Minecraft version branch. Internal class, not part of the API.
 */
final class VanillaBiomeTagData {
	static final Map<String, List<String>> DIRECT_TAGS = Map.ofEntries(
			Map.entry("minecraft:badlands", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_badlands", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:bamboo_jungle", List.of("minecraft:has_structure/jungle_temple", "minecraft:has_structure/trial_chambers", "minecraft:is_jungle", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:basalt_deltas", List.of("minecraft:is_nether")),
			Map.entry("minecraft:beach", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_beach", "minecraft:is_overworld")),
			Map.entry("minecraft:birch_forest", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_forest", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:cherry_grove", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_mountain", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:cold_ocean", List.of("minecraft:has_structure/ocean_ruin_cold", "minecraft:has_structure/trial_chambers", "minecraft:is_ocean", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals")),
			Map.entry("minecraft:crimson_forest", List.of("minecraft:has_structure/bastion_remnant", "minecraft:is_nether")),
			Map.entry("minecraft:dark_forest", List.of("minecraft:has_structure/trial_chambers", "minecraft:has_structure/woodland_mansion", "minecraft:is_forest", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:deep_cold_ocean", List.of("minecraft:has_structure/ocean_ruin_cold", "minecraft:has_structure/trial_chambers", "minecraft:is_deep_ocean", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals")),
			Map.entry("minecraft:deep_dark", List.of("minecraft:has_structure/ancient_city", "minecraft:is_overworld", "minecraft:mineshaft_blocking", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs")),
			Map.entry("minecraft:deep_frozen_ocean", List.of("minecraft:has_structure/ocean_ruin_cold", "minecraft:has_structure/trial_chambers", "minecraft:is_deep_ocean", "minecraft:is_overworld", "minecraft:polar_bears_spawn_on_alternate_blocks", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs")),
			Map.entry("minecraft:deep_lukewarm_ocean", List.of("minecraft:has_structure/ocean_ruin_warm", "minecraft:has_structure/trial_chambers", "minecraft:is_deep_ocean", "minecraft:is_overworld", "minecraft:spawns_warm_variant_farm_animals")),
			Map.entry("minecraft:deep_ocean", List.of("minecraft:has_structure/ocean_ruin_cold", "minecraft:has_structure/trial_chambers", "minecraft:is_deep_ocean", "minecraft:is_overworld")),
			Map.entry("minecraft:desert", List.of("minecraft:has_structure/desert_pyramid", "minecraft:has_structure/mineshaft", "minecraft:has_structure/pillager_outpost", "minecraft:has_structure/ruined_portal_desert", "minecraft:has_structure/trial_chambers", "minecraft:has_structure/village_desert", "minecraft:is_overworld", "minecraft:spawns_gold_rabbits", "minecraft:spawns_warm_variant_farm_animals", "minecraft:spawns_warm_variant_frogs", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:dripstone_caves", List.of("minecraft:has_structure/mineshaft", "minecraft:has_structure/ruined_portal_standard", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:end_barrens", List.of("minecraft:is_end")),
			Map.entry("minecraft:end_highlands", List.of("minecraft:has_structure/end_city", "minecraft:is_end")),
			Map.entry("minecraft:end_midlands", List.of("minecraft:has_structure/end_city", "minecraft:is_end")),
			Map.entry("minecraft:eroded_badlands", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_badlands", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:flower_forest", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_forest", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:forest", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_forest", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:frozen_ocean", List.of("minecraft:has_structure/ocean_ruin_cold", "minecraft:has_structure/trial_chambers", "minecraft:is_ocean", "minecraft:is_overworld", "minecraft:polar_bears_spawn_on_alternate_blocks", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs", "minecraft:spawns_snow_foxes", "minecraft:spawns_white_rabbits")),
			Map.entry("minecraft:frozen_peaks", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_mountain", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs", "minecraft:spawns_snow_foxes", "minecraft:spawns_white_rabbits", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:frozen_river", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:is_river", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs", "minecraft:spawns_snow_foxes", "minecraft:spawns_white_rabbits")),
			Map.entry("minecraft:grove", List.of("minecraft:has_structure/pillager_outpost", "minecraft:has_structure/trial_chambers", "minecraft:is_forest", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs", "minecraft:spawns_snow_foxes", "minecraft:spawns_white_rabbits", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:ice_spikes", List.of("minecraft:has_structure/mineshaft", "minecraft:has_structure/ruined_portal_standard", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs", "minecraft:spawns_snow_foxes", "minecraft:spawns_white_rabbits", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:jagged_peaks", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_mountain", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs", "minecraft:spawns_snow_foxes", "minecraft:spawns_white_rabbits", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:jungle", List.of("minecraft:has_structure/jungle_temple", "minecraft:has_structure/trail_ruins", "minecraft:has_structure/trial_chambers", "minecraft:is_jungle", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:lukewarm_ocean", List.of("minecraft:has_structure/ocean_ruin_warm", "minecraft:has_structure/trial_chambers", "minecraft:is_ocean", "minecraft:is_overworld", "minecraft:spawns_warm_variant_farm_animals")),
			Map.entry("minecraft:lush_caves", List.of("minecraft:allows_tropical_fish_spawns_at_any_height", "minecraft:has_structure/mineshaft", "minecraft:has_structure/ruined_portal_standard", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:mangrove_swamp", List.of("minecraft:allows_surface_slime_spawns", "minecraft:has_structure/mineshaft", "minecraft:has_structure/ruined_portal_swamp", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:spawns_warm_variant_farm_animals", "minecraft:spawns_warm_variant_frogs", "minecraft:water_on_map_outlines")),
			Map.entry("minecraft:meadow", List.of("minecraft:has_structure/trial_chambers", "minecraft:has_structure/village_plains", "minecraft:is_mountain", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:mushroom_fields", List.of("minecraft:has_structure/mineshaft", "minecraft:has_structure/ruined_portal_standard", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:stronghold_biased_to", "minecraft:without_zombie_sieges")),
			Map.entry("minecraft:nether_wastes", List.of("minecraft:has_structure/bastion_remnant", "minecraft:is_nether")),
			Map.entry("minecraft:ocean", List.of("minecraft:has_structure/ocean_ruin_cold", "minecraft:has_structure/trial_chambers", "minecraft:is_ocean", "minecraft:is_overworld")),
			Map.entry("minecraft:old_growth_birch_forest", List.of("minecraft:has_structure/trail_ruins", "minecraft:has_structure/trial_chambers", "minecraft:is_forest", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:old_growth_pine_taiga", List.of("minecraft:has_structure/trail_ruins", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:is_taiga", "minecraft:spawns_cold_variant_farm_animals", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:old_growth_spruce_taiga", List.of("minecraft:has_structure/trail_ruins", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:is_taiga", "minecraft:spawns_cold_variant_farm_animals", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:pale_garden", List.of("minecraft:has_structure/trial_chambers", "minecraft:has_structure/woodland_mansion", "minecraft:is_forest", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:plains", List.of("minecraft:has_structure/mineshaft", "minecraft:has_structure/pillager_outpost", "minecraft:has_structure/ruined_portal_standard", "minecraft:has_structure/trial_chambers", "minecraft:has_structure/village_plains", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:river", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:is_river")),
			Map.entry("minecraft:savanna", List.of("minecraft:has_structure/mineshaft", "minecraft:has_structure/pillager_outpost", "minecraft:has_structure/ruined_portal_standard", "minecraft:has_structure/trial_chambers", "minecraft:has_structure/village_savanna", "minecraft:is_overworld", "minecraft:is_savanna", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:savanna_plateau", List.of("minecraft:has_structure/mineshaft", "minecraft:has_structure/ruined_portal_mountain", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:is_savanna", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:small_end_islands", List.of("minecraft:is_end")),
			Map.entry("minecraft:snowy_beach", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_beach", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs", "minecraft:spawns_snow_foxes", "minecraft:spawns_white_rabbits")),
			Map.entry("minecraft:snowy_plains", List.of("minecraft:has_structure/igloo", "minecraft:has_structure/mineshaft", "minecraft:has_structure/pillager_outpost", "minecraft:has_structure/ruined_portal_standard", "minecraft:has_structure/trial_chambers", "minecraft:has_structure/village_snowy", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs", "minecraft:spawns_snow_foxes", "minecraft:spawns_white_rabbits", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:snowy_slopes", List.of("minecraft:has_structure/igloo", "minecraft:has_structure/trial_chambers", "minecraft:is_mountain", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs", "minecraft:spawns_snow_foxes", "minecraft:spawns_white_rabbits", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:snowy_taiga", List.of("minecraft:has_structure/igloo", "minecraft:has_structure/trail_ruins", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:is_taiga", "minecraft:spawns_cold_variant_farm_animals", "minecraft:spawns_cold_variant_frogs", "minecraft:spawns_snow_foxes", "minecraft:spawns_white_rabbits", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:soul_sand_valley", List.of("minecraft:has_structure/bastion_remnant", "minecraft:has_structure/nether_fossil", "minecraft:is_nether")),
			Map.entry("minecraft:sparse_jungle", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_jungle", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:stony_peaks", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_mountain", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:stony_shore", List.of("minecraft:has_structure/mineshaft", "minecraft:has_structure/ruined_portal_mountain", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld")),
			Map.entry("minecraft:sunflower_plains", List.of("minecraft:has_structure/mineshaft", "minecraft:has_structure/ruined_portal_standard", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:swamp", List.of("minecraft:allows_surface_slime_spawns", "minecraft:has_structure/mineshaft", "minecraft:has_structure/ruined_portal_swamp", "minecraft:has_structure/swamp_hut", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:water_on_map_outlines")),
			Map.entry("minecraft:taiga", List.of("minecraft:has_structure/pillager_outpost", "minecraft:has_structure/trail_ruins", "minecraft:has_structure/trial_chambers", "minecraft:has_structure/village_taiga", "minecraft:is_overworld", "minecraft:is_taiga", "minecraft:spawns_cold_variant_farm_animals", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:the_end", List.of("minecraft:is_end")),
			Map.entry("minecraft:the_void", List.of("minecraft:without_wandering_trader_spawns")),
			Map.entry("minecraft:warm_ocean", List.of("minecraft:has_structure/ocean_ruin_warm", "minecraft:has_structure/trial_chambers", "minecraft:is_ocean", "minecraft:is_overworld", "minecraft:produces_corals_from_bonemeal", "minecraft:spawns_coral_variant_zombie_nautilus", "minecraft:spawns_warm_variant_farm_animals", "minecraft:spawns_warm_variant_frogs")),
			Map.entry("minecraft:warped_forest", List.of("minecraft:has_structure/bastion_remnant", "minecraft:is_nether")),
			Map.entry("minecraft:windswept_forest", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_hill", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:windswept_gravelly_hills", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_hill", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:windswept_hills", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_hill", "minecraft:is_overworld", "minecraft:spawns_cold_variant_farm_animals", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:windswept_savanna", List.of("minecraft:has_structure/mineshaft", "minecraft:has_structure/ruined_portal_mountain", "minecraft:has_structure/trial_chambers", "minecraft:is_overworld", "minecraft:is_savanna", "minecraft:stronghold_biased_to")),
			Map.entry("minecraft:wooded_badlands", List.of("minecraft:has_structure/trial_chambers", "minecraft:is_badlands", "minecraft:is_overworld", "minecraft:stronghold_biased_to"))
	);

	private VanillaBiomeTagData() {
	}
}
