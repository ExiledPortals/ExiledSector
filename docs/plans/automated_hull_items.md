# Hull items on automated ships

Status: steps 1 to 7 built, except the art and the optional Reactor unique.

Player feedback: automated ships shouldn't take hull items that are people. Besides the lore, crew-based drawbacks vanish on a ship with no crew, so the Tri-Tachyon Morale Assurance Officer is a free peak CR boost there. The feedback also suggested hull items only automated ships can take, focused on CR and repairs after battle.

A ship is automated when vanilla's `Misc.isAutomated(variant)` is true: the `automated` hull mod, or the `automated` tag on the variant or its hull spec. Using it rather than the hull mod alone also catches modded AI hulls.

NPCs get no hull upgrade points, so everything here only affects the player's ships.

## Rules

1. Crew Quarters is a crewed-only section.
2. A hull item can be crewed-only or automated-only. Items that are people, or whose effects need crew, are crewed-only.
3. Generic hull items are installations, never people, so every section an automated ship can open has a generic item.
4. Unique hull items that are people but whose effect is what they left behind (armor riveted, drive tuned, optics set) stay usable on every ship; their descriptions say so.
5. Automated-only uniques give automated hulls an equivalent where rules 1 and 2 thin a section out.

## Crew Quarters

`ShipSection.CREW_QUARTERS` gets the requirement tag `req_crewed`, checked in `NodeRequirements` against a new automated flag on `ShipProfile`. This uses the same path as Shield Generator, Phase Coil and Flight Deck: the section shows as restricted and can't be unlocked. On an automated ship that already unlocked it, the section goes inactive and the player can lock it to get the point back. `ShipSections.forVariant` caches on hull mod ids, which covers the `automated` hull mod. A variant or hull tag change isn't in the cache key, but those don't change during play.

Automated ships keep their slot count. A ship has at most 5 hull upgrade points, and an automated hull with no shields and no bays still has 5 other sections (Bridge, Engine Room, Reactor, Armor Plating, Weapon Mount).

What automated ships lose:
- Already dead on them: the implicit (minimum crew), the crew capacity, crew loss and Boarders rolls, Auxiliary Conscripts entirely and Olinadu Security's crew loss.
- Only rolled in Crew Quarters: post-battle salvage, survey cost, ship recovery chance, d-mod effect reduction.
- Also rolled on the Bridge: supplies per month, repair rate. The Bridge implicit is CR recovery.
- Uniques that work in full on automated hulls today: Pather Cell, COMSEC Signals Detachment, Kanta's Den Scrapdogs.

Fleet-wide stats (salvage, survey, recovery) can still go on any crewed ship, so only fully automated fleets lose them. Crew Quarters drops are worth less to a player flying mostly automated ships.

## Bridge

| Item | Rule |
|---|---|
| Alpha Site Survivor | Crewed-only |
| Tri-Tachyon Morale Assurance Officer | Crewed-only (crew loss drawback is free on automated hulls) |
| Back-Alley Cyber-Surgeon | Crewed-only (crew loss half is dead; max CR half is the problem below) |
| Bridge Officer | Becomes Command Suite, usable everywhere |
| Extended Warranty Contract | Everywhere |
| Heritage Fire Control | Everywhere |
| Tesseract Stillwell | Everywhere |
| Lion's Guard Blueprint Vault | Everywhere; description reworded so the vault, not the crew, does the learning |

Automated ships keep 4 of the 7 Bridge uniques.

## Generic items become installations

Bridge Officer and Deck Crew are the only generic items for their sections, so they can't become crewed-only. They become installations instead. Ids stay (`bridge_officer`, `flight_deck_crew`) and only display text and art change, so saved items keep working. Their modifier pools have no crew stats except Deck Crew's two noted below.

| Item | New name (working) | Changes |
|---|---|---|
| `bridge_officer` | Command Suite | Name, description, new icon, `item_names.json` codename words (today Admiral, Commodore, Captain...), `socketable.noun.bridge` ("officer"), `framework.socketDescription.bridge` |
| `flight_deck_crew` | Launch Assembly | Name, description, new icon, codename words, `socketable.noun.flight_deck` ("deck crew"), `framework.socketDescription.flight_deck` |
| `weapon_mount_fire_control` | Fire Control Suite | Description only: drop the gunnery officer |
| `crew_quarters_specialists` | Specialist Crew | No change; the section is crewed-only |

Both new icons go through the usual item art process: an option sheet built from vanilla art, and the user picks.

Deck Crew's `FIGHTER_CREW_LOSS_PERCENT` and `MIN_CREW_PERCENT_PER_FIGHTER_BAY` rolls do nothing on automated carriers. Leave them for now; a per-modifier requirement is a separate change if it matters.

## Uniques in other sections

None of these has a crew stat, so this is lore, not exploits.

| Section | Item | Rule |
|---|---|---|
| Weapon Mount | Coatl Bastion Gunnery Team | Crewed-only |
| Weapon Mount | Gryphon Missile Crew | Crewed-only |
| Weapon Mount | Deserter Gunners | Crewed-only |
| Weapon Mount | Old Guard Gun Captains | Crewed-only |
| Weapon Mount | Line Yard Ordnance Crew | Everywhere; reworded so they rebuilt the missile feeds the way the yards build them |
| Weapon Mount | Tachyon Lens Technicians | Everywhere; reworded so the optics stay tuned after they leave |
| Engine Room | Sindrian Fuel Company Engineers | Everywhere; reworded so the settings are locked in |
| Engine Room | Galatia Hyperspace Physics Group | Everywhere; reworded so the drive field stays tuned |
| Armor Plating | Bulwark Riveters | Everywhere; the plate stays when they leave |
| Flight Deck | Converted Hangar Deck Crew | Crewed-only |
| Flight Deck | Mixed Doctrine Flight Instructors | Crewed-only (automated fighters have no pilots) |
| Flight Deck | Pather Wing Zealots | Crewed-only |
| Flight Deck | Deck Armorers' Guild | Everywhere; reworded so the craft come off the line already refitted |

Tachyon Lens Technicians staying open matters most: Remnant hulls are high-tech energy ships. Weapon Mount keeps 12 uniques on automated hulls. Flight Deck keeps only Deck Armorers' Guild and the generic, so it gets an automated-only unique.

Descriptions stay flavour with no numbers and follow `docs/tooltip_wording.md`.

## Max CR on automated ships

Vanilla's Automated Ships skill gives automated ships up to +100% max CR and shrinks that once the fleet's automated deployment points pass 120. Flat max CR on an automated hull offsets that shrinkage, letting the player field more automated ships than the skill allows. Back-Alley Cyber-Surgeon does this today and goes crewed-only; Command Suite's "Drilled" and "Veteran" rolls still do, in small amounts. Automated-only items avoid flat max CR and use CR recovery, peak CR duration, repair rate and d-mod mitigation instead.

## Automated-only uniques

Working names and directions; numbers go in `docs/analysis/balance.csv` later. Sources are Remnant, Omega or derelict drones. Each needs art through the item art process.

| Section | Concept | Effects |
|---|---|---|
| Bridge | A Remnant battle relay | CR recovery rate, peak CR duration |
| Armor Plating | Repair nanite lattice from a derelict | Repair rate per day, reduced d-mod effect |
| Flight Deck | A drone fabricator bay | Fighter replacement rate, replacement recovery |
| Reactor (optional) | A core that restores the ship after battle | Hull or CR restored after combat |

A machine counterpart to Crew Quarters (an automated-only section for after-battle items) was considered. It needs a tenth section with its own art and anchors, and automated ships lose no slots without it, so it waits until these uniques prove too few.

## Build order

1. Done. `ShipProfile` automated flag from `Misc.isAutomated`; `req_crewed` and `req_automated` in `NodeRequirements`; `req_crewed` on `CREW_QUARTERS`. Restricted tooltip line and translation keys (en and zh_CN). The flag is `ShipProfile.automated(variant)`, which mirrors `Misc.isAutomated` without loading `Misc`; texts are `framework.requirement.req_crewed` and `req_automated` in the hullUpgrades catalogues.
2. A `crew` column in `hull_items.csv` (`crewed`, `automated` or blank), read by `HullItem`, checked in `canSocketInto` against the ship and in `ShipSections` so a socketed item on the wrong kind of ship goes inactive like an unmet section. The item tooltip names the restriction. Add the column to the tools/editor serialiser so saves from the editor keep it. Done: `HullItem.requirementTag` and `canSocketInto(section, fit)`, `HullUpgradeRules.canSocket` takes the current fit, `HullItemDataRules` rejects unknown values, the tooltip line is `HullItem.limitLine` (`hullItem.crew.*`). The editor reads, writes and validates `crew` (`definitions.ts`, `hullItemCrew` in the vocabulary) and has a Crew field on the hull item panel.
3. Done. Mark the crewed-only items from the two tables above.
4. Done. Reword the "everywhere" descriptions and Lion's Guard Blueprint Vault, plus Fire Control Suite's gunnery officer. The zh_CN texts are drafts.
5. Done except art. Rename Bridge Officer and Deck Crew: names, descriptions, codename words, socket nouns and section descriptions. Art option sheets for both icons. Command Suite's codename words swap Admiral, Commodore, Captain, Orders and Mandate for Plot, Signal, Beacon, Console and Relay; Launch Assembly's words had no people in them and are unchanged. Both still use their old icons until the option sheets are picked.
6. Done except art and the Reactor concept. Automated-only uniques: concepts, effects, art, `item_modifiers.csv` rows. Built as Remnant Battle Relay (`unique_remnant_battle_relay`), Derelict Nanite Lattice (`unique_derelict_nanite_lattice`) and Drone Fabricator Bay (`unique_drone_fabricator_bay`), working names with placeholder magnitudes and placeholder icons (`bridge_officer.png`, `damage_control.png`, `flight_deck_crew.png`). The Reactor unique needs a new after-combat restore effect and isn't built.
7. Done. `docs/docs.md`: a short section on crewed and automated hull items ("Crewed and automated ships").

Tests: section restriction and item restriction for a crewed and an automated profile, an already-socketed crewed item on an automated ship going inactive, and the editor round-trip for the new column. Done: `HullUpgradeTest`, `ShipSectionsTest`, `NodeRequirementsTest`, `ShipProfileTest` and the "hull item crew" block in the editor's `models.test.ts`.

Pather Cell's rebalance is a separate change.
