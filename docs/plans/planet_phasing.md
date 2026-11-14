# Planet Phasing

A late-game colony structure that uses p-space and wormhole technology to pull a planet out of the Outer Reaches and phase it into the system of the market that holds the structure. It is researched after the Gateway Nexus and costs a special item to research. The amount of power it needs means it only runs within reach of a working coronal hypershunt. Phased planets keep their own conditions and can carry new planetary traits that nothing else in the game grants.

Status: planned 2026-11-14. Nothing built. Apart from the structure name, every name, cost, duration and magnitude here is a placeholder.

## Facts checked

These were read from the 0.98a API source (`starfarer.api.zip`), our code and Vaults of Knowledge 5.0.7.

- Outer Reaches systems are temporary. `ChartRunCloser` removes the run's system with `removeStarSystemNextFrame`, so chart systems leave no trace in the save. A system's star, planets and terrain are a pure function of its cell seed (`docs/plans/atlas.md`, Generation), so a planet can't be fetched later from a closed system without building the system again. Planet conditions come from vanilla `PlanetConditionGenerator`, which draws from the static `StarSystemGenerator.random`.
- Chart planets keep condition-only markets. `ChartLayoutGenerator.prepareMarkets` sets survey level `SEEN`, marks conditions that need no survey as surveyed and strips the civilisation conditions unless the region keeps ruins. The player can survey chart planets during a run as usual.
- Run interactions already go through `ChartCampaignPlugin.pickInteractionDialogPlugin`, but that only replaces whole dialogs (see the step 1 check below on `FireAll`).
- The Wormhole Gateway is three industries (`exiledSector_wormhole_gateway`, `_advanced`, `_perfected`, the last one called Gateway Nexus), with `WormholeGateway.playerGatewayId` finding the player's highest one. With Vaults of Knowledge, our rows in `data/campaign/aotd_tech_options.csv` gate them. Gateway Nexus is a tier 5 research costing `research_databank:15,exiledSector_gateway_upgrade_perfected:1`. VoK uses tiers 0 to 5, 7 and 8, so tier 6 is free. VoK counts a special item cost only when the item has null data and is stored in a Research Facility (`docs/plans/completed/wormhole_tech_blueprint.md`). Without VoK, the Wormhole Tech Blueprint unlocks the Gateway with a right-click, there is no tier limit and the upgrades never drop.
- Vanilla finds a working hypershunt with `PopulationAndInfrastructure.getNearestCoronalTap(locInHyper, true)`. It walks every `HypershuntIntel`, so only hypershunts the player has discovered count, and it keeps only those whose entity memory has `$usable`, which the repair dialog sets. The Hypershunt Tap's requirement is that distance at most `ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS` (10), checked in `BaseInstallableItemEffect`. Our Outer Reaches Coronal Hypershunt anomaly can never be repaired (`completed/anomalies/coronal_hypershunt.md`), so only the Sector's own hypershunts can power this structure.
- `BaseThemeGenerator.findGaps(center, minPad, maxDist, minGap)` returns the free orbit bands around a body, and `Misc.initConditionMarket(planet)` gives a new planet its condition market. `StarSystemAPI.addPlanet` adds a planet to any system at runtime.
- Vaults of Knowledge adds Hypershunt Receivers (`aotd_hypershunt_receiver` entity, research `aotd_tech_hp_distribution`), described as drawing hypershunt power over a miniaturised wormhole, and turns the hypershunt into a megastructure (`CoronalHypershuntMegastructure`).
- Checked in step 1 (decompiled 0.98a core and VoK 5.0.7): vanilla saves only a market condition's id. `MarketCondition.writeReplace` drops the plugin when `isTransient()` holds, which `BaseMarketConditionPlugin` returns, so trait plugin classes can be renamed freely. A condition id with no spec throws "Market condition [id] not found" on load, so a removed trait still needs a stub row.
- Checked in step 1: once VoK's coronal collector section is restored, its `CoronalCollector` industry sets `$usable` to false on the hypershunt, so the vanilla lookup stops finding it. VoK then gives range through `CoronalHypershuntMegastructure.isWithinRangeOfAtLeastOneHypershunt(entity)` (10 light-years times its range upgrades, from player markets with `aotd_coronal_market_cond`). A Hypershunt Receiver must itself be within that range, so this check covers receivers too.
- Checked in step 1: Nexerelin 0.12.2d random sectors build only the core worlds in `ExerelinProcGen`; vanilla `MiscellaneousThemeGenerator.addCoronalTaps` still places its two hypershunts, and Nexerelin also sells their locations at bars. No fallback is needed.
- Checked in step 1: a planet added with `addPlanet` and `Misc.initConditionMarket` is colonisable when its condition market is surveyed and has no unexplored ruins (the vanilla colonise rules read `isPlanetConditionMarketOnly`, `isSurveyed` and `hasUnexploredRuins`). Hazard comes from `reapplyConditions()` after the conditions are added, as `PlanetConditionGenerator` does. `findGaps` ignores corona, magnetic field and pulsar terrain and starts at the centre's radius plus `minPad`, so `minPad` has to clear the corona.
- Checked in step 1: vanilla `FireAll` never calls the dialog plugin, so a dialog picked in `ChartCampaignPlugin` can't add an option to the vanilla planet menu without replacing it. The lock option is a `rules.csv` PopulateOptions row and a rule command instead, as `ExiledSectorAtlasOption` does for the atlas.
- Vaults of Knowledge's Bifrost megastructure icon (`graphics/icons/megastructures/bifrost.png`, 190x95) is ours to use, approved 2026-11-14. It shows a planet framed by a huge ring with a ship beside it, and it is already the size of a vanilla industry icon.

## Design

### The structure

Phase Displacement Gate, id `exiledSector_phase_displacement_gate`, in the star-charts domain. Its icon is the Bifrost image by Jeremy Cohen, copied to `graphics/icons/industry/exiledSector_phase_displacement_gate.png` and credited to him in `CREDITS.md`.

- Unlock. With VoK, it is a new research row: tier 6, after `exiledSector_tech_wormhole_gateway_perfected`, costing `research_databank:20` plus one of the new special item, rewarding the industry. Without VoK, right-clicking the special item unlocks it for good (a sector flag, like the blueprint's `$exiledSector_gatewayUnlocked`), and the player only needs a Wormhole Gateway of any tier, since the upgrades don't exist without VoK.
- Build conditions: the market holds the player's Gateway (Gateway Nexus with VoK), since the Displacement Gate runs on the Gateway's link to the Outer Reaches, and the market is within 10 light-years of a working hypershunt (the vanilla lookup above, or with VoK its own hypershunt range check, which also covers Hypershunt Receivers). One Gateway per faction means one Displacement Gate per faction. The build tooltip lists whichever condition is missing. A Gateway already built far from any hypershunt can't take a Displacement Gate, so the Gateway's own tooltip should mention the hypershunt range once the Displacement Gate research is done.
- Upkeep and demand. Idle, it costs only upkeep. While pulling, it demands a lot of transplutonics and volatiles (placeholders 10 and 8), and progress stops while either is short, the same rule as the Hypershunt Tap. If the hypershunt somehow stops counting (VoK changes, the mod that placed it is removed), the pull pauses rather than failing.
- Unraidable and a structure, like the Gateway (`structure, unraidable` tags).

### The special item

Placeholder name Displacement Theorem, id `exiledSector_displacement_theorem`, null data, real icon (VoK draws research costs with the item's icon), star-charts domain, flavour-only description. Without VoK it has a right-click unlock. With VoK it is a research cost stored in a Research Facility.

It drops only from Admiral officers (`atlas_officers.md`) beaten as the boss of a tier 15 or higher chart (corrupted tier 16 charts count), 30% of the time (placeholder chance), on top of their usual loot and carried item. The roll goes in `OfficerDefeat.defeated` next to the carried drop and reads the officer's rank before the defeat drops them a rank.

### Picking a planet: the phase lock

A planet is chosen inside a run, since that is the only time its system exists.

- While the player has a finished, idle Displacement Gate, every eligible planet in a run gets a "Phase lock" option in its interaction, added by a `rules.csv` row and rule command because vanilla planet menus never reach `ChartCampaignPlugin`. The option needs the planet fully surveyed, so the player sees the conditions they are taking.
- Eligible: any planet or moon that isn't a star and isn't owned by an anomaly (black hole and neutron star centres, chart site bodies). Gas giants and the Threat Expanse's deep-space centre world are allowed.
- Locking records the planet in a pending lock (see Save footprint) and shows a confirmation with what the record holds. One lock at a time: locking another planet replaces it.
- The lock holds only if the run closes with its objective done. `ChartRunCloser` turns a completed run's lock into a pull job and clears it on a failed run. This ties the reward to beating the system's boss.

The record holds the planet type id, radius, name, its conditions, its texture when Unknown Skies replaced it, the source system's region, its anomalies, and the chart tier. The traits below are rolled when the lock is made, using the run's seeded random, so a save reload doesn't reroll them.

### The pull

- There is no limit on phased planets per system or per save, and the Displacement Gate can start its next pull as soon as one ends, given a new phase lock from another completed run.
- When a pull job starts, the Displacement Gate picks the destination orbit in its own system: the widest gap from `BaseThemeGenerator.findGaps` around the primary star that clears the corona and keeps clear of rings, belts, accretion disks, jump points and stable locations. When no gap is wide enough, it uses an orbit past the outermost body, so a crowded system always has room.
- The pull takes 120 days (placeholder) of unshort demand. A wormhole visual (reusing `ChartWormholeVisual`) opens at the destination orbit and grows over the pull. Intel tracks progress.
- At the end the wormhole collapses and the planet is added with `addPlanet` at that orbit, given its condition market with `Misc.initConditionMarket`, its recorded conditions and traits, survey level `FULL` with every condition surveyed, and its recorded texture. A campaign message and intel announce it. Its name is the chart name. Whether the player can rename it on arrival is an open question.
- Conditions that only make sense inside a run are dropped. A gas giant gets no jump point of its own in the first build.

### Phased planet traits

Every phased planet gets Phase-Displaced, which names where the planet came from and adds a small hazard rating. It also rolls extra traits: the number grows with the chart tier (0 to 1 at tiers 1 to 5, 1 to 2 at 6 to 10, 2 to 3 at 11 and up, placeholders). They are picked by weight from a new `data/config/exiledSector/phased_planet_traits.csv` (id, condition id, weight, region multipliers, anomaly multipliers, excludes), so the source system decides what a planet can bring back. Each trait is a market condition with its own plugin, in its own string catalogue, worded by `docs/tooltip_wording.md`.

First draft, all to be decided:

| Trait | Leans towards | Effect idea |
|---|---|---|
| Phase-Displaced | every phased planet | Names its origin; +10% hazard |
| Time-Slipped Crust | `black_hole` | Structures here build and upgrade faster |
| Neutron-Forged Mantle | `neutron_star` | +1 ore and rare ore; better ground defences |
| Residual Wormhole Throat | `spatial_breach`, `relay_network` | Higher accessibility |
| Hypershunt-Bathed | `coronal_hypershunt` (very rare) | +1 maximum industries |
| Remnant Nanoforge Seams | `remnant_expanse`, `remnant_nexus_station` | Better ship quality from heavy industry here |
| Domain Vault Strata | `derelict_graveyard`, `domain_mothership`, `pre_collapse_facility` | Tech mining also finds hull items and Star Charts |
| Fabricator Husks | `threat_expanse` | Strong production bonus, much higher hazard |
| Abyssal Bloom | `abyssal_rift` (RAT only) | Alien biosphere: food and organics |
| Cryo-Locked Aquifers | `frozen_fringe` | +2 volatiles; counts as having water |
| Furnace Heart | `furnace` | Extra ore and rare ore; extreme heat |
| Smuggler Warrens | `pirate_badlands` | Free port style access without its stability cost |
| Storm-Scoured Sky | `storm_belt`, `magnetic_storm` | Hidden from raids and expeditions some of the time |

Traits stay on the planet for good and nothing else grants or removes them. While the star-charts domain is switched off, their plugins apply nothing but stay loadable.

## Save footprint

- The pending lock and the active pull job are encoded strings in the Displacement Gate market's memory, in the style of `AtlasAnomalyRecord.encode`, so no class of ours is saved by them.
- A phased planet is an ordinary vanilla planet with a condition market. Our trait condition ids are saved on it, so a removed trait needs a stub row and plugin, as for any removed spec (save compatibility rule since 2026-11-13). Step 1 checks whether vanilla saves condition plugin instances, which decides whether trait plugin classes can ever be renamed.
- Chart systems keep their zero-trace rule: the planet is rebuilt from the record in the colony system, never moved out of the run.

## Domains and code layout

Everything lives in `exiledsector-star-charts`. The structure, lock, pull and placement go in a new `exiledsector.charts.phasing` stage package (add it to the CLAUDE.md package list), and trait conditions in `charts.phasing.trait`. The lock option is offered through `ChartCampaignPlugin`, and the lock-to-job handoff is a call in `ChartRunCloser`. VoK research checks go through `VaultsOfKnowledgeCompat` (a new research id next to the Gateway ones, and the hide-while-unneeded list). Tests mirror the packages.

## Compatibility

- Vaults of Knowledge: the research row, the item cost, and how its hypershunt megastructure and Hypershunt Receivers affect `$usable` and `HypershuntIntel` (step 1). If a receiver brings hypershunt power to a far colony, a market with one should probably count as powered (open question).
- Nexerelin random sectors: confirm hypershunts still generate. If a sector has none, the Displacement Gate can never run, so it needs a fallback (open question).
- Unknown Skies: texture recorded from the chart planet and reapplied, as the chart generator already applies it from seeded randoms.
- Terraforming mods that add or remove conditions: check whether they strip unknown conditions, and protect our traits if needed.
- Foiler's Terrain FX reads vanilla planet specs, so a phased planet needs nothing extra.

## Build order

1. [x] Facts to confirm by decompiling: whether condition plugin instances are saved, VoK's hypershunt megastructure and receiver behaviour, hypershunts in Nexerelin random sectors, and that `addPlanet` plus `initConditionMarket` on a player-owned system leaves the planet colonisable with correct hazard. Recorded under Facts checked.
2. [x] Structure and unlock: the `industries.csv` row with the Bifrost icon and its `CREDITS.md` row (Jeremy Cohen), the special item (`special_items.csv`, icon, strings), its Admiral drop in `OfficerDefeat`, the VoK research row and its compat ids, the right-click unlock without VoK, the build conditions (Gateway market, hypershunt range). Tests for the build checks and the drop roll. Built in `charts.phasing`: `PhaseDisplacementGate` (industry), `PhaseGateRules` (unlock flag `$exiledSector_phaseGateUnlocked` and build blockers), `HypershuntPower` (vanilla lookup plus `VaultsOfKnowledgeCompat.isWithinHypershuntRange`), `DisplacementTheorem` (drop roll, called from `OfficerDefeat.bossDefeated`) and `DisplacementTheoremItemPlugin`. The research id is `VaultsOfKnowledgeCompat.PHASE_GATE_RESEARCH_ID`, hidden with the Gateway rows through `COLONY_RESEARCH_IDS`. The Gateway's tooltip says when no hypershunt is in range once the Displacement Gate is unlocked. The theorem's icon is a placeholder copy of the Gateway Nexus Upgrade icon.
3. [ ] Phase lock: the run option, eligibility, the encoded record, trait rolls at lock time, and the handoff in `ChartRunCloser` (kept on completion, cleared on failure). Tests for eligibility, encoding and the handoff.
4. [ ] Pull and placement: the job, the demand and pause rule, destination gap choice, intel, the wormhole visual and adding the planet. Tests for gap choice and placement from a record.
5. [ ] Traits: `phased_planet_traits.csv` (editor serialiser too), Phase-Displaced and the first set of traits, weights from region and anomalies, inert while the domain is off. Tests for weighting, tier counts and the domain switch.
6. [ ] Strings (en and zh_CN draft), the `docs/docs.md` section, README and forum post.

## Open questions

1. Names: the item and every trait.
2. Whether the Displacement Gate should also need the Hypershunt Tap installed or only the hypershunt in range.
3. Settled in step 1: VoK's hypershunt range (and so its receivers) counts as power, and Nexerelin random sectors keep vanilla hypershunts, so there is no fallback.
4. Whether the player picks the destination orbit and can rename the planet on arrival.
5. Whether moons keep their own moons (they don't in this draft) and whether gas giants get a jump point.
6. The trait list, effects and weights.

## Follow-up

Once this plan is built: new colony conditions that only ever appear on Outer Reaches planets (rolled by the chart generator, not by phasing), so a phased planet can bring back conditions the Sector never has. They get their own plan.
