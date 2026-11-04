# Pirate Badlands modifiers

Hull items dropped in Pirate Badlands systems can carry a Pirate Badlands modifier. The biome is pirate country, so its set is about ambush and kicking ships that are down: opening strikes, bonuses against crippled or overloaded targets, running away, ramming and smuggling. Like the pirate doctrine, one modifier carries a downside.

Status: built 2026-11-04. The framework is built (`../anomalies/abyssal_rift.md`, Biome modifiers): rows in `item_modifiers.csv` ending `_PIRATE_BADLANDS`, mapped in `biome_modifiers.csv`, reserved at item load and granted to 10% of hull items dropped here. Values are placeholders.

## Modifiers

| Item | Name | Effect | Placeholder | Theme |
|---|---|---|---|---|
| Command Suite | Ambusher's (prefix) | `OPENING_WEAPON_DAMAGE_PERCENT`: increased weapon damage for the first 30 s after the ship deploys | 10-15% | the opening volley |
| Specialist Crew | Press-ganged (prefix) | existing `LOW_TECH_AFFINITY_FLAT` | 30-40 | pressed spacers who only know old hulls |
| Drive Assembly | of the Getaway (suffix) | `LOW_HULL_TOP_SPEED_PERCENT`: increased top speed below 50% hull | 20-30% | knowing when to run |
| Reactor Regulator | Bootleg (prefix) | hybrid: `FLUX_DISSIPATION_PERCENT` and `PEAK_CR_DURATION_PERCENT` | 15-20%, and 5-10% reduced | a hot-wired reactor |
| Fire Control Suite | of Plunder (suffix) | `DAMAGE_TO_HELPLESS_SHIP_PERCENT`: increased damage to overloaded, venting or flamed-out ships | 10-20% | kicking them while down |
| Shield Emitter | Hit-and-run (prefix) | `SHIELD_DAMAGE_TAKEN_AT_SPEED_PERCENT`: reduced shield damage taken above 75% of top speed | 5-10% | never sitting still |
| Phase Coil Array | of the Smuggler (suffix) | `TRANSPONDER_OFF_SENSOR_PROFILE_PERCENT`, fleet-wide: reduced sensor profile with the transponder off | 10% per ship, up to 30% | running dark |
| Launch Assembly | Reaver (prefix) | `STRIKECRAFT_DAMAGE_TO_DAMAGED_SHIPS_PERCENT`: strikecraft deal increased damage to ships below 50% hull | 15-20% | finishing runs |
| Armor Plate | Ram-plated (prefix) | `RAM_PROW_COLLISION_DAMAGE_PERCENT`: collisions hurt enemies more and this ship less | 50% more dealt, 50% less taken | a ram prow |

Of the Smuggler is a percent on the fleet's detected-at range (its sensor profile) while the transponder is off, summed over active ships and synced per fleet like the Rift's `MOVE_SLOW_BURN_LEVEL_FLAT`; the player fleet is also resynced when the transponder is toggled. Built as a percent rather than the flat modifier first written here, since its value is a share of the profile.

Ram-plated: applied in a damage listener on collision damage only, both ways. The engine has no collision flag on a hit, so a collision is a hit with no projectile, weapon stats or EMP, of kinetic or energy type, between hulls in contact.

Reaver ignores strikecraft targets, Plunder counts any hostile ship, and Ambusher's 30 s run from each ship's own deployment.

## Build order

1. [x] The nine rows in `item_modifiers.csv` and `biome_modifiers.csv`, with strings (en and zh_CN). Built: the `_PIRATE_BADLANDS` rows; Bootleg is `HYBRID_FLUX_DISSIPATION_PEAK_CR_PIRATE_BADLANDS`, the crew row `LOW_TECH_AFFINITY_FLAT_PIRATE_BADLANDS`.
2. [x] The new effects, each in its ship part's `skilleffect` package with tests. Built: `weapon` (`OPENING_WEAPON_DAMAGE_PERCENT` with `OpeningVolley`, `DAMAGE_TO_HELPLESS_SHIP_PERCENT` with `HelplessShipDamageListener`), `defense` (`LOW_HULL_TOP_SPEED_PERCENT` with `LowHull`, `RAM_PROW_COLLISION_DAMAGE_PERCENT` with `CollisionDamageListener`), `shield` (`SHIELD_DAMAGE_TAKEN_AT_SPEED_PERCENT` with `HighSpeed`), `fighter` (`STRIKECRAFT_DAMAGE_TO_DAMAGED_SHIPS_PERCENT` with `DamagedShipDamageListener`), `logistics` (`TRANSPONDER_OFF_SENSOR_PROFILE_PERCENT` in `FleetWideEffects`).
3. [x] `docs/docs.md` for the collision damage. Built: the Pirate Badlands modifiers section.

## Decisions

Bootleg keeps its downside: a biome modifier may carry one, and Bootleg is the only one so far.
