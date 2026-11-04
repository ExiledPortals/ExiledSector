# Abyssal Rift content and biome modifiers

The Abyssal Rift (Random Assortment of Things only) gets its own content built from RAT's Abyss: abyssal salvage sites, the Abyssal Shipyard, the Abyssal Research Complex and the Abyssal Storm. Every hull item dropped in a Rift system carries an abyssal modifier, the first case of biome modifiers, which every atlas biome will get.

Status: proposed 2026-11-04. See `overview.md` for the shared anomaly framework. RAT's Abyss assets are approved for use.

## Rebuild, don't spawn RAT's entities

RAT picks the dialog for its abyss stations by custom entity id in any location (`RATCampaignPlugin`), and the dialog looks up RAT's own abyss grid at the entity's coordinates. That lookup gives the wrong biome's illustration and loot multiplier outside RAT's system and crashes when the player has turned RAT's Abyss off (`rat_abyssEnabled`). So all of this content uses our own entity ids, dialogs and salvage specs with RAT's art and RAT's drop groups. The Rift already requires RAT, so its drop groups are always loaded. Nothing here writes RAT memory, intel or abilities.

## Biome modifiers

Each atlas biome can own one modifier per base hull item type, kept in a new table `data/config/exiledSector/biome_modifiers.csv` (`biome,item,modifier,note`). Every hull item dropped in that biome's systems gets its type's modifier: chart fleets, the boss, salvage sites and anomaly drops alike. Uniques keep their fixed modifiers and get none.

The modifiers are `item_modifiers.csv` rows ending in the biome's suffix (`_ABYSSAL_RIFT` here), reserved at item load the way `StormLoot.reserveModifiers` reserves the storm ones, so no other roll ever draws them. They are added through `GrantedModifiers.grant`, which already handles items with no free slot. A nanoswarm reroll can remove one like any other modifier; only the biome, or a program like the Research Complex's, adds it again.

### Abyssal Rift modifiers

One per base hull item type except Specialist Crew, which gets none: the abyss is all AI, so it has nothing to teach a crew. Values are placeholders, and the effect lines are settled when built, after checking how each effect stacks (`docs/tooltip_wording.md`).

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Chronal (prefix) | `TIMEFLOW_PERCENT` | 3-5% time flow | Chronos cores |
| Drive Assembly | of the Depths (suffix) | new, fleet-wide: moving-slowly burn level | +1 per ship, up to +6 | RAT's fog hides slow fleets |
| Reactor Regulator | Lightless (prefix) | `SENSOR_PROFILE_PERCENT` | 20-30% reduced sensor profile | abyssal darkness |
| Fire Control Suite | of the Abyss (suffix) | `BEAM_WEAPON_HARD_FLUX_PERCENT` | 10-20% of beam damage as hard flux | abyssal beam drones |
| Shield Emitter | Plasmatic (prefix) | `SHIELD_UPKEEP_MULT` | 15-20% reduced shield upkeep | Plasmatic Shield |
| Phase Coil Array | of the Seraph (suffix) | new: armor repair on entering phase | 10% per cell, up to 50% of armor per combat | Seraph hulls |
| Launch Assembly | Fabricator-fed (prefix) | new: damage to drone fighters | 5-10% increased | abyssal drone wings |
| Armor Plate | Graced (prefix) | Borrowed Grace stacks, one per 150 armor and hull damage taken | up to 10 stacks | Seraph's Grace |

Drive Assembly: every ship carrying it adds 1 to the burn level at or below which the whole fleet counts as moving slowly, up to 6 for the fleet. Vanilla reads that level as the fleet's minimum burn times its sneak multiplier plus the fleet stat `move_slow_speed_bonus_mod` (`Misc.getGoSlowBurnLevel`), so the effect is a flat modifier on that stat, synced per fleet like our other fleet-wide effects.

Phase Coil Array: each time the ship enters phase, every armor cell repairs 10% of its maximum, up to full. Repairs stop for the rest of the combat once they total 50% of the ship's maximum armor (the sum of its cells' maximums). Only armor actually restored counts toward that cap, so entering phase with undamaged armor costs nothing, and the last repair is cut short to land exactly on the cap.

Launch Assembly: strikecraft launched from the ship deal increased damage to drone fighters, meaning strikecraft with no crew (automated wings, as the Abyssals and Remnant fly). The ship's own weapons don't benefit. A new effect beside `DAMAGE_TO_AUTOMATED_SHIP_PERCENT`.

Armor Plate: the item grants Borrowed Grace stacks on its own, earned every 150 armor and hull damage the ship takes instead of the node's 100, up to 10 stacks (placeholder), lasting 15 seconds and pausing while phased as the node's do. Each stack has the full Borrowed Grace effect whether or not the ship has the node. That means moving the per-stack effect out of the node: `BorrowedGraceListener` gives every stack a fixed 1% increased top speed, flux dissipation and weapon rate of fire, and 1% reduced weapon flux cost and damage taken, and `BORROWED_GRACE_PERCENT_PER_STACK` leaves the node. A ship with both the node and the item has one stack pool: maximum stacks add up, and stacks come every 100 damage, the better of the two.

## Abyssal salvage sites

Three Rift-only `chart_poi.csv` rows of the station kind, each a custom entity of ours with RAT's sprite and illustration, and a salvage spec of ours listing RAT's drop groups:

| Site | Sprite | Illustration | Drop groups (from RAT) |
|---|---|---|---|
| Abyssal Fabricator | `graphics/stations/rat_abyss_fabricator.png` | `rat_abyss_wreckage*.jpg` | `rat_abyss_alterations_rare`, `rat_abyss_weapons`, `rat_abyss_artifact_rare` |
| Abyssal Accumulator | `graphics/stations/rat_abyss_accumalator.png` | `rat_abyss_abandoned2.jpg` | as the fabricator, with more basic salvage |
| Abyssal Research Station | `graphics/stations/rat_abyss_research.png` | `rat_abyss_abandoned.jpg` | `rat_abyss_weapons`, `rat_abyss_alteration_common`, `rat_abyss_transmitter` |

`chart_poi.csv` gets a `biomes` column: a row with biomes appears only in those biomes, and a row without appears everywhere but in biomes that turn ordinary sites off. The Rift keeps vanilla caches and derelicts out and places only these. The Rift layout has no orbits, so the sites sit on clear spots around the photospheres. Guarded sites get `rat_abyssals` defenders through the usual defended-site adoption.

## Abyssal Shipyard

A Rift landmark: a `biomes=abyssal_rift` row in `chart_anomalies.csv`, so exactly one system in each Rift pocket holds it (as the Remnant Nexus, `../completed/anomalies/biome_stations.md`). It is a fight and nothing else. The shipyard is a salvage entity guarded by a heavy abyssal fleet in its `$defenderFleet`, fought through vanilla's defender interaction as the Derelict Forge's guards are. RAT's lineup is the model: a Morkoth flagship, Aboleths, Chuul, Makara and Merrow under Chronos and Cosmos cores, scaled to the chart tier. Beating it opens the shipyard's salvage (RAT's shipyard drop groups) plus chart fleet items. It never launches fleets, and RAT's Abyssal Burn grant is left out.

## Abyssal Research Complex

A Rift-only anomaly that works like the Derelict Forge (`../completed/anomalies/derelict_forge.md`). It is guarded by a Seraph fleet modelled on RAT's: a Gabriel with Sariels and Raguels under Seraph cores, scaled to the tier. Once the guards are beaten, its one program adds the Abyssal Rift modifier for a hull item's type to a hull item from anywhere, through the forge's bench mode with the forge's refusal rules: no uniques, Subroutines, Star Charts, corrupted or mirrored items, and only an item with room for the modifier. It runs once per run (placeholder) and costs metals, rare metals and Nanite Feedstock scaling with tier (placeholders). It drops no items of its own. RAT's lore logs, the Primordial Catalyst reveal and survey data are left out.

## Abyssal Storm

A Rift-only anomaly after RAT's Sea of Solitude storms. Every 27 to 33 seconds a storm lasts 10 to 16 seconds, and fleets outside abyssal fog have their detected range multiplied by 2.5 and raised by 150 su, with a full-screen flash (`graphics/fx/rat_solitude_thunder.png`) and a thunder sound (RAT's numbers, placeholders). It is campaign-only: battles are untouched. Fleets that stay in fog are hidden from it, so the Rift's fog becomes cover. It runs from a run-bound system script that `ChartRunCloser` removes. It adds item quantity found in the system through its `effects` column (placeholder `CHART_QUANTITY_PERCENT:30`).

## Rift-only anomalies

The Rift weighs every anomaly it doesn't list at 0 (`*:0`). The Research Complex and the Abyssal Storm are rolled normally inside the Rift and must never roll or be inserted anywhere else. Their `fitsAt` refuses every node outside a biome that lists them, so `AtlasAnomalies.canInsert` refuses them there too. Only the Shipyard is a `biomes` landmark.

## Build order

1. [ ] Biome modifiers: `biome_modifiers.csv` and its loader, reserving the modifiers at item load; every hull item dropped in a biome's systems granted its type's modifier, through one hook for fleet, boss, site and anomaly drops; the eight Rift rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN).
1a. [ ] The new effects: the fleet-wide moving-slowly burn level (capped at +6 per fleet), the phase-entry armor repair with its per-combat cap, and damage to drone fighters, each in its ship part's `skilleffect` package with tests.
1b. [ ] Borrowed Grace: the per-stack effect moves into the stack, the node drops `BORROWED_GRACE_PERCENT_PER_STACK`, and the item's stack source with its 150 damage threshold shares the node's stack pool; update the node's lines, `docs/docs.md` and the Borrowed Grace tests.
2. [ ] Abyssal salvage sites: the `biomes` column on `chart_poi.csv`, our three custom entities and salvage specs, placement in the orbitless Rift layout.
3. [ ] Abyssal Shipyard: its landmark row, entity, guard fleet and salvage.
4. [ ] Abyssal Research Complex: Rift-only `fitsAt`, entity, Seraph guards and the abyssal graft program on the forge's bench mode.
5. [ ] Abyssal Storm: the storm script, overlay and sound, its fog check and its `effects` line.
6. [ ] Atlas marker icons for the three anomalies, catalysts if wanted, the `docs/docs.md` sections and tests for each step.

## Not pursued

Decided 2026-11-04: no Abyssal Graveyard of recoverable abyssal wrecks, no Abyssal Fracture, no droneship, transmitter or alternative light sources, and none of RAT's unique or story content (Sariel outpost, the Primordial set, Ethereal Shores, the warning beacon, the sensor arrays).

## Open questions

- Whether the Research Complex is a rolled Rift anomaly (as above) or a landmark like the Shipyard.
- Whether "boss fight" means the Shipyard's guards are the chart's boss in its system. As written they are a separate fight, and the system still has its own boss.
- Whether biome modifiers scale with tier group like chart modifier lines, and whether the Research Complex should cost something abyssal instead of metals and Nanite Feedstock.
- Whether the Abyssal Storm should also reward fights fought during a storm, as the Storm Front does.