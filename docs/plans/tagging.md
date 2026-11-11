# Theme tags for modifiers and nodes

Status: steps 1 to 4, 6 and 7 built; step 5 applied and waiting on the user's review.

Item modifiers, star chart modifiers and skill tree nodes share one vocabulary of theme tags that say what they do, so crafting, drops and tree mechanics can work from tags the way Path of Exile's fossils, bench meta-crafts and cluster jewels do. A modifier or node can carry any number of tags.

## Two tag families

Placement tags decide where something can appear: item type scopes, section and base pools with their weights, biomes, tree volumes, and the `req_*`, gate and rule tags. They already exist and don't change.

Theme tags describe what something does. They are new for modifiers and replace today's theme tags on skill types (`SkillTags.THEME`).

## Vocabulary

The vocabulary lives in `data/config/exiledSector/theme_tags.csv` (`tag,parent,scopes`), read by a core class in `exiledsector.skills.tags`, with names in the core string catalogue (`tag.<id>`). The editor mirrors it. A tag counts as its parent too, so a `weapon` bias also hits `ballistic` modifiers. Damage types have no parent, so kinetic damage taken stays out of weapon biases. `scopes` says what may carry the tag: `ship` (item modifiers and nodes), `star_chart`, or both.

| Group | Tags | Scopes |
|---|---|---|
| Weapons | `weapon` > `ballistic`, `energy`, `missile`, `beam`, `point_defense`, `damage`, `range`, `rate_of_fire`, `ammo` | both |
| Damage types | `kinetic`, `high_explosive`, `fragmentation`, `energy_damage`, `emp` | both |
| Defence | `defence` > `armour`, `hull`, `shield`, `shieldless`, `damage_taken`; `shield` > `front_shield`, `omni_shield` | both |
| Core systems | `flux`, `phase`, `ship_system`, `mobility`, `combat_readiness`, `repair`, `d_mods` | both |
| Fleet | `strikecraft`, `fleet_support`, `sensors`, `electronic_warfare` | both |
| Logistics | `logistics` > `cargo`, `fuel`, `crew`, `supplies`, `salvage` | ship |
| Tech | `low_tech`, `midline`, `high_tech` | ship |
| Conditions | `conditional` > `while_phased`, `flux_level`, `low_hull`, `on_hit`, `on_kill`, `nearby_ships` | both |
| Chart encounters | `enemy`, `encounter` > `fleet_size`, `officers`, `boss`; `battle_rules` | star_chart |
| Chart rewards | `loot` > `hull_item`, `subroutine`, `star_chart`, `crafting_item`, `vanilla_loot` | star_chart |

Renamed from today's tags: `dmgtype*` to the damage type names, `weapons` to `weapon`, `speed` to `mobility`.

Plain `shield` means any shield; only effects that care about the shield's type get `front_shield` or `omni_shield`. A Makeshift Shield Generator counts as a front shield, as `FittedShipProfile` already treats it. New requirement tags `req_front_shield` and `req_omni_shield` sit beside `req_shields` and `req_no_shields`. Theme and requirement tags stay separate, since a node can be about omni shields while needing a front shield.

Condition tags mark effects that only work in a situation, alongside their stat tags: an armour bonus below half hull is `armour` and `low_hull`.

## Star chart modifiers

Chart modifiers use the same vocabulary as ship modifiers wherever they touch the same thing: a shield arc drawback is `shield`, and enemy kinetic damage is `enemy`, `kinetic` and `damage`. `enemy` marks modifiers that strengthen the enemy rather than weaken the player. Fleet size, officers and bosses are `encounter`. Deployment, command points, battle size and autopilot are `battle_rules`. Reward suffixes carry `loot` and what they drop. Chart and ship modifiers never share a pool, so a tag's meaning only has to match between them.

Atlas tree node types (`atlas_tree_types.json`) carry `star_chart` scope tags in a `tags` list, so atlas nodes and chart modifiers can be matched by tag. They are internal only and never shown to the player.

## Where tags are written

Tags are handwritten: a `tags` column on `item_modifiers.csv` and `star_chart_modifiers.csv`, and the existing `tags` list on each type in `skill_types.json`. A first pass drafts tags for every modifier and type from what they already do (effect part, effect name, today's theme and requirement tags, vanilla hull mod equivalents), for review.

A modifier with `none` is tagless. Biases never reach it: nothing boosts or blocks it, guaranteed-tag crafts never produce it, and only its pool weight sets how rare it is. Biases that boost other tags dilute its odds, as fossils do in Path of Exile. Tagless is for very powerful modifiers only, so finding one stays exciting.

## Weighting

A shared core `TagWeights` sets every tag-driven roll. The final weight is the pool weight times, for each active bias source (a crafting item, a biome, an atlas node), that source's multiplier for the modifier's tags. Within one source any matching x0 blocks and otherwise the strongest match applies. Separate sources multiply. NPC builds' focus (`NpcFocus`, `NpcRelevance`) moves onto it, and `NpcRelevance` also favours the ship's own shield type.

## Display

The F1 expanded tooltip (`TooltipExpansion`) shows each modifier's tags while crafting, including on star charts, and each node's tags in the ship skill tree. Atlas tree tags are never shown. The crafting UI gets reworked later.

## What this enables later

Each gets its own plan:
- Biased nanoswarms and ciphers (more of a tag, none of another).
- Crafts that guarantee a tag.
- Bench blocks ("cannot roll logistics modifiers").
- Tuning that strengthens tagged modifiers.
- Atlas, biome and chart drop biases.
- Modifiers scaling with allocated nodes of a tag.
- Subroutines affecting tagged nodes in radius.
- Tag-picked node pools.

## Steps

1. [x] Vocabulary: `theme_tags.csv`, the core tag class, names in en and zh_CN, the editor mirror, and the renames across `skill_types.json`, `npc_faction_focus.csv`, `npc_ship_system_focus.csv` and the editor. Built: core `skills/tags/ThemeTags` and `ThemeTagScope`, loaded in `ExiledSectorModPlugin.onApplicationLoad`; `SkillTags.all()`.
2. [x] Shield requirement tags `req_front_shield` and `req_omni_shield` in `NodeRequirements`.
3. [x] `tags` columns on `item_modifiers.csv` and `star_chart_modifiers.csv`, parsed onto the modifiers, and a `tags` list on atlas tree types, all kept by the editor's serialisers (round-trip tests). Built: `Modifier.tags()`/`tagless()` parsed in `ItemModifiers`; atlas types already read `tags` in `AtlasTreeData`; editor `items/model/modifiers.ts` and `ModifierForm`.
4. [x] Checks (built: `ItemDataIntegrity` and `ItemDataIntegrityTest`, `SkillTypesDataConsistencyTest`, `AtlasTreeDataTest`, `ThemeTags.ancestorsListedBeside`; skill types also exempt roots and the structural `wormhole`, `wormhole_exit`, `exoship`, `subroutine_socket`; editor `shared/themeTags.ts`):
   - every tag is in the vocabulary;
   - its scopes allow where it is used;
   - every modifier has tags or `none`;
   - every skill type and atlas tree type has a theme tag (the atlas root excepted);
   - a tag written next to its own parent is warned about.
5. [ ] First tagging pass over every item modifier, chart modifier, skill type and atlas tree type, reviewed by the user, including which modifiers are tagless. Drafted and applied to the data files (drafts with tagless candidates and notes in `docs/plans/tagging_drafts/`); only the Extended Warranty is `none` so far. Waiting on the user's review.
6. [x] `TagWeights` in core, NPC focus moved onto it, and item and chart rolls routed through it with no sources active (rolls unchanged, pinned by a test). Built: core `skills/tags/TagWeights`; rolls and shown odds through `PoolEntry.biasedWeight` with ancestors (`UnbiasedRollsTest`, `ItemRollOdds`, `DoctrineArchive.lineChances`); `NpcFocus` gives one source per faction and ship system, combined in `NpcRelevance`, which also favours the ship's shield type.
7. [x] Tags in the F1 tooltip for modifiers while crafting and for skill tree nodes. Built: core `skills/tags/ThemeTagLine`, added under each modifier in `Item.addModifierLines` and to node details in `SkillTreeNodeTooltipRenderer`.
