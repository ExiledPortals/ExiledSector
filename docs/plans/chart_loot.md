# Star Chart loot framework: plan

Today a chart system's loot works like this. One loot budget per tier buys points of interest. The drop rolls only change our own items at those points of interest, plus one 4% chart roll per won battle. A defeated chart fleet drops exactly what vanilla would drop for that fleet, and no chart modifier changes that. This plan replaces the single budget with categories and gives every kind of loot its own bonus. It also makes those bonuses apply to the fleets as well as to the sites. The vault is deleted, and its loot moves onto the warden fleet. Section 8 proposes how the hybrid prefixes should scale with tier, based on how Path of Exile scales its maps.

All numbers below are placeholders, like every other magnitude in the mod. `docs/analysis/balance.csv` is left alone.

## Three layers

1. Sites: what gets placed in the system. Four site budgets buy points of interest by category.
2. Sources: where loot comes out of. These are salvaging a site, beating a chart fleet (the warden above all), and finishing the objective (the completion reward).
3. Drop types: what the loot is (weapons, hull items, star charts, currency and so on).

The rule that holds the framework together: a drop bonus raises its drop type at every source in the system, and a site bonus only changes what gets placed. No drop bonus is limited to sites, so fleets are never left out again. The completion reward is the one exception. It stays scaled by Bounty and Quantity only, so stacking a single drop type can't make the guaranteed reward explode.

Every loot effect is an ordinary `ChartEffect`, and a modifier can carry any number of effect lines. So a modifier can be pure loot (a suffix such as "+30% currency") or a drawback bundled with loot (a hybrid such as "bigger fleets, +5% quantity"). Charts where every modifier is a drawback paying a targeted reward (like Path of Exile's tier 17 maps) are possible later as pure data, but they wait until there are many more drawbacks.

## 1. Delete the vault (done)

The vault objective, the vault point of interest and its salvage spec all go. The warden fleet becomes the system's jackpot (section 4). Two objectives remain: Clear and Boss.

What gets removed:
- `ChartObjective.VAULT`, `ChartPoi.Kind.VAULT` and `isVault`, `ChartPois.vaultsAt`, and `ChartIds.VAULT_TAG`.
- In the planner: `affordableVaults`, the vault branch in `plan` and `spendLoot`, and the vault preference in `bossTarget`. The warden now targets the most valuable site.
- In the generator: vault placement and naming.
- `ChartContent.vaultEntityId` and `ChartRun.vaultEntityId`. Starsector saves ignore unknown fields, so dropping a persisted field is safe.
- `ChartObjectives.lootShown` and the vault branch of target tracking. That leaves `ChartContentListener` with no `ShowLootListener` work, so the interface goes too.
- In `ChartGenContext`: `vaultRichnessPercent`, `enrichVault`, `vaultItemChanceMult`, `lootMultWithoutVault` and `lootBudgetWithoutVault`.
- `ChartTier.vaultWeight`, the `weight_vault` column in `star_chart_tiers.csv`, and its parsing in `ChartTiers`.
- Data rows: the `vault` row in `chart_poi.csv`, `exiledSector_chart_vault` in `item_salvage.csv`, and `data/campaign/procgen/salvage_entity_gen_data.csv`. The vault is that file's only row, so the whole file is deleted. `deploy.ps1` leaves removed files behind, so the deployed copy has to be deleted by hand once.
- Strings: `chart.poi.vault.name`, `chart.objective.vault.*` and the Hoard's effect text, in `en.json` and `zh_CN.json`.
- The editor's `weight_vault` column and its vocabulary entries in `tools/editor_vocabulary.json`, plus their tests.
- `ChartOpenCommand`'s vault text, and the "Sealed Vault" marker in `tools/check_chart_save.ps1`.
- `docs/plans/star_charts.md` gets a line saying the vault was replaced by warden spoils.

Tier 3 was the only tier where the vault was the sole alternative to Clear (no warden yet), so tier 3 becomes Clear only.

`ChartRun.objective` is stored as a string and read through `ChartObjective.parse`. A dev save with a live vault run therefore parses to null instead of crashing. Such a run counts as Clear, and its now-meaningless vault target is dropped from the target list. Star Charts haven't been released, so only dev saves can hit this.

`VaultsOfKnowledgeCompat` is about the Ashes of the Domain mod, not our vault, and stays as it is.

## 2. Site budgets (done)

"Loot budget" becomes "POI budget", split into four category budgets.

| Category | Members (`chart_poi.csv` id, cost, min tier) |
|---|---|
| Caches | `supply_small` 1 t1, `supply` 2 t1, `weapons_small` 2 t1, `weapons_small_high` 3 t5, `weapons` 3 t2, `weapons_high` 4 t6, `equipment_small` 2 t1, `equipment` 4 t3 |
| Domain | `probe` 3 t2, `survey_ship` 6 t5, `mothership` 14 t12 |
| Stations | `orbital_habitat` 5 t3, `mining_station` 6 t4, `research_station` 8 t6, `arsenal_station` (optional, see section 6) |
| Derelicts | `derelict_civilian` 2 t1, `derelict_small` 2 t1, `derelict_medium` 3 t3, `derelict_large` 5 t6 |

Data changes:
- `chart_poi.csv`: the `kind` column becomes the category. `probe` moves from `cache` to `domain`, and `survey_ship` and `mothership` move from `station` to `domain`. `ChartPoi.Kind` gains `DOMAIN`, which places like `STATION` (it takes an orbit focus). It is spawned through `addSalvageEntity`, not `addDerelict`.
- `star_chart_tiers.csv`: `loot_budget` is replaced by `cache_budget`, `domain_budget`, `station_budget` and `derelict_budget`. A category's budget is 0 below the lowest min tier of its members. The first fill splits today's budget roughly 35/30/20/15 (caches, derelicts, stations, domain), so totals stay about where they are. `domain_budget` must reach at least 14 by tier 12, or the mothership can never appear.
- `ChartTier` carries the four budgets. `ChartTier.withLootBudget` and the fallback tier get the same split.
- The editor's `chartTables.ts` gets the four columns, and the round-trip test is updated to pin them.

Spending (`ChartContentPlanner.spendLoot`) runs once per category with the existing cost-targeted pick. Two changes:
- Fractional leftover. When what's left is smaller than the cheapest member's cost, buy one more of the cheapest with chance `left / cost`. Without this, a +20% bonus on a budget of 5 usually buys nothing. With it, the expected number of sites rises in a straight line with the bonus.
- The 24-site cap (`MAX_POIS`) stays a total across all categories. Categories spend in the order domain, stations, derelicts, caches, so the cap cuts the cheap caches first.

Domain sites can bring vanilla Domain drone defenders (`probDefenders` in their salvage spec). That means a domain budget bonus also adds fights. This fits the theme and is accepted, and the modifier's tooltip says so.

## 3. Drop types

There are three ways to make "more" of something.

### Our items (done)

Our own code rolls these, and their bonuses are chance multipliers on those rolls.

| Drop type | Items | Rolled by |
|---|---|---|
| Hull items, one drop type per section | Bridge, Crew Quarters, Engine Room, Reactor, Armor Plating, Weapon Mount, Shield Generator, Phase Coil, Flight Deck items | `ItemDrops.roll`, `rollSectionItemLoot` |
| Subroutines | subroutines | `ItemDrops.roll` |
| Star Charts | charts, at the run tier with the usual tier-up chance | `ChartDrops.roll` |

Each of these is its own drop type: the nine hull sections, subroutines and star charts. A bonus to one of them leaves the general item rolls alone. Instead it adds a targeted roll for that type, at `base chance × bonus`. Hull sections pick through `ItemDrops.pickSectionItemBasic(ItemKind, random)`, and star charts through `ItemDrops.pickChartAtTier`. So "+40% Reactor items" literally means 40% more Reactor items, and "+40% star charts" means 40% more charts, without thinning out anything else. Quantity raises the general rolls and every targeted roll alike.

### Our materials (done)

| Drop type | Items | Rolled by |
|---|---|---|
| Currency | every nanoswarm (Augmentation to Replicator Nanoseed), the Inhibitors, and the ciphers (Reforging, Annulment, Cartographer's) | `ItemDrops.rollMaterials`, `ChartDrops` currency roll |
| Nanite Feedstock | `exiledSector_socketable_parts` | `ItemDrops.rollMaterials`, battle Parts |

Ciphers still only drop inside chart systems (the existing `ChartDrops` rule). The Currency bonus raises the nanoswarm rolls and the cipher roll together.

### Vanilla loot

The game rolls these from drop entries that we attach to sites and fleets (`addDropRandom` / `addDropValue`).

| Drop type | Vanilla drop groups |
|---|---|
| Weapons | `weapons1`, `weapons2`, `low_weapons*`, `high_weapons*`, plus fighter wing groups |
| Resources | `basic` value, `supply`, `machinery`, `goods`, `mining_bulk`, `hab_bulk` |
| Blueprints | `blueprints`, `blueprints_low`, `weapon_bp`, `ship_bp`, `fighter_bp`, `package_bp`, and the hull mod spec groups `any_hullmod_*` (by the time a player charts, most hull mod specs are known, so they don't need their own type) |
| AI cores | `ai_cores1`, `ai_cores2`, `ai_cores3` |

The game builds a site's salvage from its spec's drop entries plus any entries on the entity (`SalvageEntity`, lines 670-673). Battle loot reads every defeated fleet's drop entries (`FleetEncounterContext.generatePlayerLoot`). This route has three advantages: vanilla does the rolling, the player's Salvaging skills still apply, and the entries disappear with the system, so nothing is left in the save.

The groups belonging to each vanilla drop type are listed in data (`chart_loot.csv`, section 7), not in code. That way, modded drop groups can be added later.

### Colony items

Colony items (nanoforges, synchrotrons, fusion lamps and the rest of vanilla's `rare_tech` list) are kept very rare. No amount of bonus may guarantee one. So they don't use stacked drop entries, which would add up across 24 sites and a dozen fleets until one was nearly certain. Instead each run makes exactly one colony item roll, at most one item:

- The chance comes from a new `colony_item_chance` column in `star_chart_tiers.csv`, for example 0 below tier 6 and a few percent at tier 15.
- The bonus scales that chance, which then passes through a soft ceiling: `ceiling × (1 − e^(−chance / ceiling))`. While the chance is small this is almost exactly the raw chance, so the first bonuses count in full. As the chance grows it bends towards the ceiling and never reaches it. The ceiling is one data constant, for example 25%, so even a stacked chart finds a colony item at most about one run in four.
- The roll happens once when the system is built, from the run's seed. On a success, our code picks the item by vanilla's `rare_tech` weights and gives it to one holder: the warden if there is one, otherwise a random station or domain site. It is attached as extra salvage (`BaseSalvageSpecial.addExtraSalvage`). Vanilla pays extra salvage out both when a site is salvaged and in battle loot (`getCombinedExtraSalvage`, read in `generatePlayerLoot`), so the same call works for both kinds of holder.
- The colony items already in vanilla's own site specs are left alone. Bonuses never scale them, and the warden's spoils don't include `rare_tech_low` (section 4), so the ceiling holds.

## 4. Sources

### Site salvage (our drop types done; vanilla entries are step 5)

Vanilla drop types. When the system is built (`afterGeneration`), each site gets extra entries:
- Weapons, resources, blueprints and AI cores scale what the site already has. For each entry in the site's own spec whose group belongs to the drop type, a copy is added with `chances × bonus` or `value × bonus`. For example, +30% weapons on a weapons cache adds 30% of that cache's own weapon chances.
- Derelict wrecks (`Entities.WRECK`) have no spec drops, so derelicts get base entries from `chart_loot.csv`. Any site category can be given base entries for a drop type it would otherwise never have, for example AI cores on domain sites.

Our drop types. `ItemSalvageListener` stops reading the multiplier stamped on the entity (`ITEM_CHANCE_MEMORY_KEY`) and looks up the live run instead, the same way `ChartDrops.forSystem` already does. That removes `markSalvageChances`. `rollMaterials` takes one multiplier per material, so Currency and Feedstock scale separately. It also fixes today's bug where salvage materials ignore the chance multiplier altogether (`ItemSalvageListener.java:49`).

### Chart fleets (our drop types done; vanilla entries are step 5)

This is the piece that's missing today.

Vanilla drop types. When each fleet spawns (`ChartContentGenerator.spawnFleet`), it gets drop entries for weapons, resources, blueprints and AI cores. These are sized from `chart_loot.csv` per 100 fleet points and multiplied by the bonus. Vanilla pays them out at the first battle the player wins against that fleet, then clears them.

Our drop types. At spawn, the fleet's memory also records its starting deployment points. When the battle's loot is built (`ItemLootListener`), each defeated chart fleet from this run rolls our drop types once, using a new site rule `exiledSector_chart_fleet` in `item_salvage.csv`. The chance multiplier is `bonus × (DP defeated from this fleet / its starting DP)`. A fleet beaten in two engagements therefore pays out once in total, split across the two.

This replaces the single `system_battle` roll in `chart_drops.csv`. Its chart and cipher chances become rows of the fleet rule. The NPC-carried items and upgrade drops (`NpcItems`) are unchanged. They stack on top.

`ChartContentListener.reportBattleOccurred` and its TODO are removed, because the work lives in `ItemLootListener`, next to the existing battle loot.

### Warden spoils (our spoils done; vanilla spoils are step 5, the colony item step 6)

The warden takes over the vault's role as the big payout. It exists from tier 4 on, under both objectives. It carries everything a normal fleet does, plus spoils:
- Vanilla spoils. The vault's old drop list becomes `warden` rows in `chart_loot.csv`, attached to the warden at spawn: `basic` 20000 (resources), `ai_cores3` 6 (AI cores), `high_weapons2` 20 (weapons), and `any_hullmod_high` 8, `blueprints_low` 12 and `package_bp` 3 (blueprints). The vault's `rare_tech_low` entry is dropped, because colony items only come from the capped roll. Unlike the old flat vault, the spoils scale per 100 warden fleet points, so they grow with the warden's size from tier to tier.
- Our spoils. A new rule `exiledSector_chart_warden` in `item_salvage.csv` takes over the vault's old odds (`1;0.8;0.5;0.25;0.1`, unique chance 0.25). It also gets its own star chart and cipher chances, well above the fleet rule's.
- The colony item, when the run rolled one and there is a warden.
- The warden fights to the last (`FLEET_FIGHT_TO_THE_LAST`), so in practice a won battle against it is a destroyed warden. The DP-proportional split still covers the rare partial win for our spoils.
- Spoils announce themselves in the loot screen the same way battle drops do now. No new UI.

There are no spoils at tiers 1 to 3, because there is no warden there.

### Salvage defenders

Some sites spawn their own defenders when the player arrives, for example the Domain drones that guard probes and survey ships (`probDefenders` in the salvage spec). Since step 4 these fleets get nothing of ours: they don't carry the run key or a starting deployment point count, and the old `system_battle` roll is gone. They should count as chart fleets, so the loot bonuses reach them too.

Vanilla builds them in `SalvageGenFromSeed` and lets a `SalvageDefenderModificationPlugin` (a generic plugin) change the fleet in `modifyFleet(SDMParams, fleet, random, withOverride)`. `SDMParams.entity` is the site being defended. Our plugin checks that the entity belongs to the live run (`ChartIds.belongsToRun`) and then prepares the fleet the way `prepareFleet` does for loot: the run key, the starting deployment points, and the content tags, but not the hostility flags, the removed abilities or the warden name. From then on `ItemLootListener` treats it like any other chart fleet. Defenders roll the normal fleet rule, never the warden's spoils.

Two things to check before building it:
- The plugin manager picks one plugin by priority, so ours must not take over vanilla's (or another mod's) strength, size and quality choices. Either ours only claims chart system sites and passes every other method through to `SalvageDefenderModificationPluginImpl`, or the tagging moves somewhere that doesn't compete, for example a listener on the defender fleet's first engagement. Decompile `SalvageGenFromSeed` to see how the plugin is picked and whether `modifyFleet` runs for every defender spawn.
- Vanilla drop types (step 5) should reach defenders too. They get the per-100-fleet-point entries from `chart_loot.csv` in the same hook, scaled by the bonuses.

Defenders already have their own vanilla loot through the site, which is unchanged. They do not count towards the Clear objective.

### Completion reward

This is unchanged: Bounty plus Quantity, as now (`ChartRewards`).

## 5. Chart modifiers

New or renamed effects in `ChartEffect`, with modifiers in `star_chart_modifiers.csv`:

| Effect | Layer | Replaces |
|---|---|---|
| `CHART_POI_BUDGET_PERCENT` | all four site budgets | `CHART_LOOT_BUDGET_PERCENT` (Plenty) |
| `CHART_CACHE_BUDGET_PERCENT`, `CHART_DOMAIN_BUDGET_PERCENT`, `CHART_STATION_BUDGET_PERCENT`, `CHART_DERELICT_BUDGET_PERCENT` | one site budget | new |
| `CHART_ITEM_PERCENT` | every item type: hull sections, subroutines, star charts | `CHART_ITEM_DROP_CHANCE_PERCENT` (Salvage) |
| `CHART_BRIDGE_ITEM_PERCENT`, `CHART_CREW_QUARTERS_ITEM_PERCENT`, `CHART_ENGINE_ROOM_ITEM_PERCENT`, `CHART_REACTOR_ITEM_PERCENT`, `CHART_ARMOR_PLATING_ITEM_PERCENT`, `CHART_WEAPON_MOUNT_ITEM_PERCENT`, `CHART_SHIELD_GENERATOR_ITEM_PERCENT`, `CHART_PHASE_COIL_ITEM_PERCENT`, `CHART_FLIGHT_DECK_ITEM_PERCENT` | one hull section | new |
| `CHART_SUBROUTINE_PERCENT` | subroutines | new |
| `CHART_STAR_CHART_PERCENT` | star charts | new |
| `CHART_CURRENCY_PERCENT` | nanoswarms, Inhibitors and ciphers | new |
| `CHART_FEEDSTOCK_PERCENT` | Nanite Feedstock | new |
| `CHART_WEAPON_PERCENT` | weapons | new |
| `CHART_RESOURCE_PERCENT` | resources | new |
| `CHART_BLUEPRINT_PERCENT` | blueprints and hull mod specs | new |
| `CHART_AI_CORE_PERCENT` | AI cores | new |
| `CHART_COLONY_ITEM_PERCENT` | the run's one colony item roll, under the soft ceiling | new |
| `CHART_WARDEN_SPOILS_PERCENT` | everything in the warden's spoils | `CHART_VAULT_RICHNESS_PERCENT` (the Hoard) |
| `CHART_QUANTITY_PERCENT` | every drop type except colony items, and the completion reward | unchanged |
| `CHART_REWARD_PERCENT` | completion reward | unchanged |

Charts already roll up to six modifiers, three per side (`AffixLayout.STAR_CHART_LIMITS`). The prefixes are the difficulty hybrids, so almost all of the new loot modifiers are suffixes. Spread over three suffix slots, the per-type effects are specific enough to chase, and Salvage stays as the broad option. The eleven item-type suffixes (nine sections, subroutines, star charts) share one combined pool weight: each gets an eleventh of an ordinary suffix's weight in the `star_charts.csv` pools, so together they roll about as often as one ordinary suffix. This is data only. While some types are still locked, the family rolls a little less often, which is acceptable. Each item-type modifier is only rollable when that type can drop (unlock gating as for section items), so a chart never offers a bonus to something that can't drop. The Hoard is only rollable from tier 4 (`:t4+` in the pools), where the warden appears. Colony Items is only rollable from the first tier with a nonzero `colony_item_chance`.

Quantity leaves colony items out on purpose. Every hybrid prefix carries Quantity, so letting it reach colony items would push every hard chart towards the ceiling.

The Warden's Vigil unique (`unique_chart_wardens_vigil`) loses its vault suffix. `CHART_VAULT_RICHNESS_PERCENT_WARDENS_VIGIL` becomes `CHART_WARDEN_SPOILS_PERCENT_WARDENS_VIGIL` (100% at t1, 150% at t10), which pairs with its existing warden size prefix. The description drops the vault line in both languages and becomes flavour about what the warden is carrying.

Percent bonuses for the same drop type add before they multiply, so `Quantity + Weapons` behaves like `multiplier()` does today. Salvage counts towards every item type, as if it were added to each one. Chart quality keeps adding +1% Quantity per point.

`ChartGenContext` swaps its loose float fields for two maps: one keyed by site category and one by drop type. It also gets one accessor, `dropMult(DropType)`, which every source calls, plus `wardenSpoilsMult()` and `colonyItemChance(tier)`. `describe()` prints both maps.

The chart tooltip gets a totals block under the modifiers, like the summary at the top of a Path of Exile map: one line per drop type and site category that isn't at 100%, for example "Quantity +23%, Currency +47%, Reactor items +40%, Stations +30%". With six modifiers, two of them hybrids carrying Quantity, the player can't add these up in their head.

Star Charts have not been released (they arrived after the `0.8.0-alpha` tag), so the effects are renamed outright, with no aliases. Charts already sitting in dev saves log the existing "roll whose modifier is no longer defined" warning once and ignore that roll.

Strings: `chart.effect.<NAME>` in `en.json` and `zh_CN.json` for every new effect, plus modifier names and the totals block. The Quantity tooltip is reworded to say it covers everything except colony items.

## 6. Arsenal station (done)

The arsenal station comes from Industrial Evolution (`IndEvo_arsenalStation`, SirHartley, tested here with 4.1.b). Its salvage spec draws on IndEvo's own drop groups (`indEvo_weapons`, `indEvo_missiles`, `indEvo_consumables`), so it only works while IndEvo is loaded. We already ship `compat/salvage/IndEvo.csv` with hull item chances for it.

Plan: add a `requires_mod` column to `chart_poi.csv`. The arsenal row (cost about 7, min tier 5, stations) is only eligible when IndEvo is enabled and both of IndEvo's LunaLib settings for it are on: the master switch `IndEvo_Enable_Derelicts` and "Additional Station Types" (`IndEvo_enable_derelict_stations`). Both default to on. IndEvo itself only reads them at new game, so gating on them is our choice, made so that a player who turned the stations off doesn't meet them in chart systems. We read them with `LunaSettings.getBoolean("IndEvo", id)` and treat null as on. LunaLib is a hard IndEvo dependency, so no fallback to `Global.getSettings()` is needed. `ModSettings.booleanOr` only reads our own mod id, so this needs a small helper that takes another mod's id. We use IndEvo's entity type and art, so nothing is copied and no asset permission is needed. The station's row uses `IndEvo_arsenalStation` as both entity type and salvage spec, so `VanillaChartContentEngine` overrides nothing.

The compat side is mostly there already. `SalvageSiteCompat.SOURCES` registers IndEvo (tested 4.1.b) as a `CompatTarget` that checks the entity spec, and the README and forum post compat tables already list it for its arsenal stations and orbital laboratories. Step 7 only rewords that row to mention chart systems, in both files.

Checked against IndEvo 4.1.b (decompiled): a station placed through `BaseThemeGenerator.addSalvageEntity` behaves like one IndEvo placed itself.
- IndEvo's `DerelictStationPlacer` spawns it with its own static `addSalvageEntity`, a line-for-line copy of vanilla's, with faction neutral, as ours does. It adds no memory flags, tags or plugin. The entity type has no plugin class, and its tags (`has_interaction_dialog`, `salvageable`, `neutrino`, `salvor_orbital`) plus vanilla's salvage rules give all its behaviour. IndEvo's `rules.csv` has nothing for it, and its description row matches the id. IndEvo also spawns arsenal stations at runtime elsewhere (`PlanetoidSwarmSpawner`), so it doesn't assume its placer made every one.
- No IndEvo script keeps a list of arsenal stations or looks them up by id, so deleting the chart system leaves nothing dangling. The one listener that looks at them, `LocatorSystemRatingUpdater`, counts `salvageable` entities when the player jumps out and stores a rating in the system's own memory, which goes with the system. The only visible side effect is that IndEvo's locator shows a rating for chart systems.
- IndEvo sometimes adds a minefield to the planet an arsenal station orbits. We don't copy that, so arsenal stations in chart systems have no mines. No defenders or artillery attach to the entity.
- Our station gets no salvage special, because IndEvo assigns those only at new game. Specials are optional, so this is fine.

The salvage spec and its drop groups (`indEvo_weapons`, `indEvo_missiles`, `indEvo_missiles_always`, `indEvo_consumables`) are IndEvo data, and `SalvageEntityGeneratorOld.getSalvageSpec` throws without them. So the row must never be built while IndEvo is absent, and `VanillaChartContentEngine` keeps its existing path that removes the entity when spawning fails. `chart_loot.csv` lists `indEvo_weapons` and `indEvo_missiles` as optional weapon groups, so the Weapons bonus scales them too, and loot scaling must skip them when IndEvo isn't loaded. If a later IndEvo version renames the entity, the existing `CompatTarget` check for missing sites catches it.

## 7. New data file

`data/config/exiledSector/chart_loot.csv` is the framework table. Each row is a drop type, a source scope (a site category, `fleet` or `warden`), the vanilla group (for vanilla drop types), a base amount (chances, value, or chances per 100 fleet points) and a min tier. Our own drop types keep their base chances in `item_salvage.csv` and `chart_drops.csv`. `chart_loot.csv` only maps drop types to vanilla groups and holds the base entries for sources that have none, including the warden's spoils.

The colony item ceiling and the item weights (copied from vanilla's `rare_tech` group, so modded colony items can be appended) live in a small `chart_colony_items.csv`.

The editor gets a tab for both (or at least round-trip coverage), and an integrity check catches unknown drop types, scopes, groups and items.

## 8. Hybrid prefix scaling (proposal)

How Path of Exile scales maps by tier (poewiki Map page, and Path of Building's copy of the game's monster stat tables):

- Map tier only sets the area level: tiers 1 to 16 are levels 68 to 83, one level per tier. Every monster's base stats come from its level.
- Per level, monster life rises about 6.4% and damage about 5%. From tier 1 to tier 16, life grows 2.5x (6127 to 15319), damage 2.1x (374 to 783), armour 3.2x and evasion 1.7x. As a rough measure of threat, life times damage grows about 5x over 16 tiers, at a smooth, compounding ~11% per tier.
- Tier does not change how many monsters there are. Pack count and density come from the layout, and pack size only from modifiers.
- On top of that, the three modifier pools (tiers 1-5, 6-10, 11-16) raise each drawback's value and add nastier drawbacks, while each modifier's reward stays fixed. PoE needs this because a player's gear outgrows a 5x curve, so a fixed drawback would stop mattering.

How our tiers scale (`star_chart_tiers.csv`, with average fleet counts and 50 fleet points per 100% fleet size):

| Tier | Fleets x size | Warden | Total fleet points | NPC tree size |
|---|---|---|---|---|
| 1 | 2.5 x 100% | none | 125 | 50-80% |
| 4 | 3.5 x 160% | 300% | 430 | 80-110% |
| 8 | 5 x 270% | 520% | 935 | 120-150% |
| 12 | 6 x 410% | 820% | 1640 | 150-195% |
| 15 | 6.5 x 550% | 1100% | 2337 | 170-240% |

- Our tiers scale the amount of enemy far harder than PoE does: about 19x in fleet points from tier 1 to tier 15, before the per-ship gains from NPC tree size, officer levels and fleet quality. PoE holds the amount steady and scales each monster, at about 5x overall.
- Our curve is lumpy. Tier 3 to tier 4 more than doubles the fleet points (210 to 430), mostly because the warden appears, while most later steps are 10-20%.
- A player's fleet can't outgrow our curve the way PoE gear does. Fleet size and battle size cap it, and NPC trees grow with tier just as player trees do.

What the hybrids do today:
- Fleet size and warden size drawbacks are added as percentage points to the tier's base: Fortified adds 15-20 at tier 1 (on 100, so +15-20%), 50-70 at tier 6 (on 210, so +24-33%), and 150-200 at tier 11 (on 370, so +40-54%). So the drawback's real bite grows with tier, and so does its Quantity (4-6% up to 9-12%).
- Counted drawbacks (extra fleets, extra officers, officer levels) are absolute. "+1 fleet" is +40% at tier 1 (out of 2.5) but +15% at tier 15 (out of 6.5). The existing bands (1 / 2 from t8 / 3 from t13) already partly make up for that.

Suggestion:
1. Make the size drawbacks relative. Fleet size, warden size and sensor range multiply the tier's own value ("+30% fleet size" means 30% more than this tier's fleets) instead of adding percentage points. Each band then bites equally at every tier, which is what PoE gets for free by using relative drawbacks such as "more Monster Life". This is a code change in `ChartGenContext.adjust` (multiply instead of add) and a data change in the bands. Fleet quality stays additive, because it is already a bounded 0-150 scale.
2. Size counted drawbacks to a constant relative bite. Extra fleets is about +40% of the tier's average fleet count (1 at tiers 1-3, 2 at 4-9, 3 at 10-15). Extra officers and officer levels follow the tier's own officer bonuses in the same way.
3. Don't make the drawbacks harsher by tier on top of that. PoE does it because players outgrow a gentle curve; ours is steep and players can't outgrow it. Revisit only if playtesting shows high-tier players shrugging off the prefixes.
4. Fix each hybrid's Quantity by how punishing its drawback is, the way PoE does, in four steps (for example 4%, 6%, 8% and 10%), the same at every tier. Higher tiers already pay more through bigger site budgets, credits that grow with tier^1.5, warden spoils per fleet point and the colony item chance. So Quantity doesn't need to grow with tier as well.
5. Separately, smooth the tier curve towards a steady compounding step, so no single tier jumps the way tier 3 to tier 4 does. This is a `star_chart_tiers.csv` data pass, independent of the loot work.

Points 1 to 4 make one consistent rule: a modifier means the same thing at every tier, and the tier itself carries the progression. This is a balance change to existing data, so it waits for the user's go-ahead and is not in the build order.

## Build order

1. Delete the vault (section 1). It removes a branch from the planner before step 2 reshapes it. Done (d56abc58).
2. Site categories: `Kind.DOMAIN`, four tier budget columns, per-category spending with fractional leftover, editor columns and tests. Rename Plenty and add the four category effects. Done (merged in c3687c86).
3. Drop types and the context: the `DropType` enum (one entry per hull section, plus subroutines, star charts, currency, feedstock, the four vanilla types and colony items), `ChartGenContext` maps and `dropMult`, `ItemSalvageListener` live lookup, per-material multipliers, targeted per-type rolls, the tooltip totals block. Salvage, the nine section effects, Subroutine, Star Chart, Currency and Feedstock effects. Done (8692db2f, d0996373).
4. Fleet loot for our items: record starting DP at spawn, per-fleet rolls in `ItemLootListener`, the `exiledSector_chart_fleet` and `exiledSector_chart_warden` rules, the Hoard as warden spoils, the Warden's Vigil update. Remove `system_battle`. Done. The Hoard and the Warden's Vigil scale only our spoils until step 5 adds the vanilla ones.
5. Vanilla drop types: `chart_loot.csv`, entries on sites (scaled copies plus base) and on fleets (per fleet point), warden spoils rows. Weapon, Resource, Blueprint and AI Core effects.
6. Colony items: `colony_item_chance` tier column, `chart_colony_items.csv`, the single seeded roll with the soft ceiling, extra salvage on the warden or a site. Colony Item effect.
7. Arsenal station with IndEvo compat. Done (555e1e86, merged into main).
8. `docs/docs.md` chart loot section, `docs/plans/star_charts.md` cross-reference, strings in both languages.
9. Salvage defenders (section 4): tag defender fleets that spawn at the run's sites as chart fleets, so they roll fleet loot and, once step 5 is in, get the vanilla fleet entries. Needs steps 4 and 5. Update `docs/docs.md` with it.

Each step builds and passes tests on its own. Steps 4, 5 and 6 touch the same generator and listeners, so they run one after the other, not in parallel.

### Steps 2 and 3 in parallel

Steps 2 and 3 run at the same time, step 2 on its own branch and step 3 in the main checkout. This is how they divide the files they share.

Files only step 2 touches: `ChartPoi`, `ChartPois`, `chart_poi.csv`, `ChartTier`, `ChartTiers`, `star_chart_tiers.csv`, `ChartContentPlanner`, the placement code in `ChartContentGenerator`, the editor's `chartTables.ts` and `tools/editor_vocabulary.json`, and their tests.

Files only step 3 touches: the new `DropType`, `ItemDrops`, `ItemSalvageListener`, `ChartDrops`, `ChartIds.ITEM_CHANCE_MEMORY_KEY`, the chart tooltip, and their tests. Step 3 doesn't touch `ChartContentGenerator`.

`ChartGenContext` is split by field:
- Step 2 replaces `lootBudgetPercent`, `addLootBudgetPercent` and `lootMult()` with a map keyed by `ChartPoi.Kind`, `addSiteBudgetPercent(Kind, float)`, `addAllSiteBudgetsPercent(float)` and `siteBudgetMult(Kind)`, and makes `adjust()` scale the four budgets. Quantity stops raising the site budgets. It's a drop bonus, and by the framework's rule drop bonuses don't change what gets placed. Quantity reaches sites through step 3's `dropMult` instead.
- Step 3 replaces `itemChancePercent`, `addItemChancePercent`, `itemChanceMult()`, `chartDropMult()` and `markSalvageChances` with a map keyed by `DropType`, `addDropPercent(DropType, float)`, an all-items add for Salvage, and `dropMult(DropType)`. Step 3 also owns `quantityPercent` and `rewardMult()`.
- In `describe()`, step 2 replaces only the `loot x%.2f` part and step 3 only the `hull item chance x%.2f` part. That one statement will conflict, and the merge resolves it.
- In `ChartGenContextTest`, each step adds its own test methods and leaves the other's alone.

Shared data and strings are split by position:
- `ChartEffect`: step 2 renames `CHART_LOOT_BUDGET_PERCENT` and puts its four category effects right after it, above `CHART_EXTRA_POIS`. Step 3 renames `CHART_ITEM_DROP_CHANCE_PERCENT` and puts its new effects right after it, above `CHART_REWARD_PERCENT`.
- `en.json`, `zh_CN.json` and `star_chart_modifiers.csv` follow the same rule: each step's keys and rows go next to its own renamed entry.
- `star_charts.csv` can't be split this way, because both steps edit the suffix pool cell on every row. Each step changes only its own pool entries, and whichever step merges second puts the other step's entries back by hand.

The tooltip totals block starts with drop types and Quantity in step 3. Its site category lines are added after both steps are merged. Neither step edits `docs/docs.md` (that's step 8) or this plan.

## Tests

- Vault removal: no plan ever contains a vault or the Vault objective, tier 3 always plans Clear, and a run saved with objective `VAULT` loads as Clear without its vault target.
- Planner: each category spends only its own budget. Fractional leftover gives the right expected count over many seeds. The cap cuts caches first.
- Context: drop multipliers add within a type and stay independent across types. Quantity reaches every type except colony items, and Salvage reaches every item type.
- Targeted rolls: a Reactor bonus only ever adds Reactor items, and a star chart bonus only adds charts at the run tier (with tier-up). Neither changes the general roll's odds, and neither adds anything when that type can't drop yet.
- Currency: one bonus raises nanoswarm, Inhibitor and cipher rolls together, and ciphers still never drop outside chart systems.
- Site entries: copies scale the spec's own groups, base entries respect the min tier, and vanilla spec colony items are never scaled.
- Fleet loot: a fleet split across two battles pays out once in total, only the warden gets spoils, the Hoard scales only the spoils, and fleets from another run or outside a chart system roll nothing.
- Colony items: never more than one per run; the chance with any bonus stays below the ceiling; zero below the first tier with a chance; the item lands on the warden when there is one; the same seed gives the same result.
- Data: integrity tests on `chart_loot.csv` and `chart_colony_items.csv`. The editor round-trip covers the new tier and POI columns and the removed `weight_vault`.

## Open questions

1. Section 8: adopt relative drawbacks with fixed, severity-based Quantity for the hybrid prefixes? And smooth the tier curve?
2. The colony item ceiling and the tier chances: 25% and "a few percent at tier 15" are guesses to show the shape.
