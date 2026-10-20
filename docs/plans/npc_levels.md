# NPC levels: plan

Today every NPC tree mirrors the player. Each chosen NPC ship rolls 80% to 120% of the tree power of a random combat ship in the player's fleet (`NpcTreeSize.roll`). Chart fleets use the same mirror with a wider range per tier (50% to 80% at tier 1, up to 170% to 240% at tier 15). `NpcBonusScale` multiplies NPC bonuses by up to 3 from the fleet's average tree power. The enemy therefore grows exactly as fast as the player does, and levelling a ship feels futile.

This plan keeps the mirror in the open world but caps it by faction category. Charts stop mirroring and use fixed levels that start above every open-world cap. A ship levelled past the cap pulls ahead for good, and charts are the next step up.

All numbers below are placeholders, like every other magnitude in the mod. `docs/analysis/balance.csv` is left alone.

## Terms

An NPC ship's level is the number of nodes it allocates besides its root. That is the number `NpcTreeSize` already produces, and it lines up with `PlayerTreePower.shipPower` (level plus paid nodes). So a level 25 NPC is about as strong as a level 25 player ship.

Every level in this plan is clamped to the LunaLib max level (`ShipLevelConfig.maxLevel`, default 50) and to `maxAllocatedNodesBesidesRoot`. Otherwise a player who lowers the max level would meet chart fleets above it.

## 1. Faction categories (done)

| Category | Cap | Membership |
|---|---|---|
| Pirate | 15 | the faction's `pirateBehavior` custom flag (vanilla: `pirates`, `luddic_path`) |
| AI | 35 | listed faction ids (vanilla: `remnant`, `omega`, `derelict`, `threat`, `dweller`), or a fleet whose commander `isAICore()` |
| Faction | 25 | everything else, including the player's faction and its allies (allies can turn hostile quickly) |

The `pirateBehavior` flag picks up modded pirate factions for free, and the AI-core commander check picks up modded AI fleets. A new `data/config/exiledSector/npc_levels.csv` holds the three category caps and per-faction overrides (`faction_id,category` or `faction_id,cap`). Modded factions that get either check wrong can then be fixed as data.

As built, the columns are `id,category,cap`. A row whose id equals its category (`pirate,pirate,15`) sets that category's cap. Any other row is a faction: `remnant,ai,` moves it to a category, `some_faction,,20` gives it its own cap, and a row may do both. CSV rows win over the pirate flag and the AI-core commander.

## 2. Open-world fleets (done)

The roll stays as it is: 80% to 120% of a random player ship's tree power. The result is then capped at the fleet's category cap. `NpcBonusScale` keeps reading the fleet's average tree power, but that average is capped at the same category cap first, so the bonus stops growing once the player passes it.

Below the cap this is still a mirror, so going from level 5 to level 12 does not make pirates easier. That is intended: the roll does not shrink as the player nears the cap.

Flagship hull item counts used to follow player character level; since `docs/plans/npc_loot.md` NPC ships carry no items at all.

## 3. Bounties (done)

Bounty targets are mostly pirates or deserters. Under the pirate cap they would be trivial late, which is when they are meant to be the challenge, so they bridge the open world and charts.

A bounty fleet uses the same mirror roll, capped at the bounty cap instead of the category cap. The bounty cap runs from the category cap at the bounty's lowest difficulty to 25 at its highest. An AI bounty keeps its category cap of 35, since that is already above 25. The bounty's flagship gets 3 extra levels on top of its roll, and its cap is raised by 3 to match.

Difficulty comes from the bounty itself. Vanilla custom bounties have a difficulty that climbs with each one completed of that type, and person bounties have a level. So taking harder bounties is how the player works up to chart tier 1 strength.

First, decompile vanilla to find how a bounty fleet can be told apart from other fleets and where its difficulty can be read: `PersonBountyIntel`, the custom bounty missions (`BaseCustomBounty` and its creators), and Nexerelin's bounties from its bundled source. Don't guess memory flags.

As built (`NpcBounties`), difficulty is a 0 to 1 progress:
- Person bounties are found by their `PersonBountyIntel` fleet listener. Progress is `getLevel()` over 10, capped at 1, since bonus levels can reach 14.
- Custom bounties, vanilla and Nexerelin's `nex_remcb`, store their difficulty (0 to 10) in a protected `CustomBountyData`. Once accepted, the `BaseCustomBounty` is a fleet listener. Before accept, the offered fleets already exist in the world, so they are found through their `MissionFleetAutoDespawn` script's `mission` field. Both are read by reflection.
- MagicLib bounties are found by the `MagicLib_Bounty_target_fleet` tag or memory flag. They have no difficulty number, so they count as the hardest.

Nexerelin's faction and system bounties have no target fleet. A ship's cap is fixed when it is levelled, so a fleet that becomes a bounty later (a MagicLib bounty placed on an existing fleet) keeps its earlier cap, and `npc_levels.csv` edits only reach ships levelled afterwards. Person bounty fleets always use the pirate category, since vanilla keeps them neutral until the player enters their system.

## 4. Chart fleets (done)

Chart fleets stop mirroring. In `star_chart_tiers.csv`, the `fleet_strength_min` and `fleet_strength_max` percent columns become absolute `level_min` and `level_max` columns. Tier 1 starts at 26, one above the faction cap. Tier 15 ends at the max level. That is about 1.7 levels per tier: tier 1 is 26 to 29 and tier 15 is 47 to 50, with linear steps between them. Every ship in a chart fleet rolls a level in its tier's range, so the player's fleet no longer matters.

The Fleet Strength % chart modifier (`ChartIds.STRENGTH_BONUS_MEMORY_KEY`) is currently added to the percent range. It becomes a percent of the tier's levels instead: `level * (1 + bonus / 100)`, clamped to the max level. `ChartTiers.fleetStrengthRangeFor` returns a level range instead of a `NpcTreeSize.PercentRange`. `NpcFleetLeveller.decide` takes a fixed level range for chart fleets and the mirror roll for everything else.

Chart fleets ignore the faction category, since the chart sets their strength. Every ship in the warden fleet rolls one level higher than the tier's range, so the warden fleet is always one level above the rest of its system. This is the one exception to the max-level clamp: the warden can go to the max level plus one (still within `maxAllocatedNodesBesidesRoot`), so it stays a level higher at tier 15 too.

The editor's star chart tier table (`tools/editor/src/pages/items/model/chartTables.ts` and `chartModels.test.ts`) and the `star_charts.md` column list have to follow the column rename, or editor saves will drop the new columns.

## 5. Chart drops outside chart systems (done)

This ties back to `chart_loot.md`. Outside sources currently drop charts up to tier 6 (battles) and tier 5 (salvage and tech mining), from defeated deployment points or player level. Under this plan a chart's tier is its fleet level, so outside drops follow what the player actually fought.

A won battle's chart tier is the highest tier whose `level_min` is at or below the highest level among the defeated NPC ships, and never below tier 1. Open-world factions and pirates (caps 25 and 15) therefore always drop tier 1. AI fleets at 35 drop up to about tier 6, and the hardest bounty flagships (28) drop tier 2. Salvage and tech mining have no fleet, so they drop tier 1 only.

In `chart_drops.csv`, `min_strength` and `strength_per_tier` stay as the gate for whether a battle or salvage site qualifies at all, but no longer set the tier. `max_tier` becomes 1 for `salvage`, `techmining_first_find` and `techmining_monthly`. `ItemLootListener` records the highest defeated NPC level from the `NpcTreeRecords` it already reads, and passes it to `ChartDrops.forBattle`.

## 6. Settings and text (done)

- The LunaLib min and max percent settings (`NpcTreeSize.MIN_PERCENT_FIELD_ID`, `MAX_PERCENT_FIELD_ID`) keep driving the open-world roll. Their descriptions mention the category caps.
- `PlayerTreePower.logNpcScaling` logs the caps next to the roll.
- `docs/docs.md` gets the category rules, bounty rules and chart levels. README and `forum_post.bbcode` change together, if either describes NPC scaling.

## Build order

1. Categories and `npc_levels.csv`, with open-world caps and the capped `NpcBonusScale`. Done in c7a6f882 on `main`: `NpcLevelCategory`, `NpcLevelCaps`, the cap in `NpcFleetLeveller.decide`, `NpcBonusScale.forShip`. Each levelled ship carries its cap as a variant tag (`NpcLevelCaps.TAG_PREFIX`), because vanilla merges a battle side into one fleet with the primary fleet's faction and commander. `PlayerTreePower.logNpcScaling` already logs the caps (section 6).
2. Chart fleet levels: the column rename, `fleetStrengthRangeFor`, the Fleet Strength % conversion and the editor table. Done in 322df1bc, merged into `main`: `ChartLevelRange`, warden flag `ChartIds.WARDEN_MEMORY_KEY`.
3. Bounty detection (decompile first), then the bounty cap and the flagship bonus. Done, committed on `main`: `NpcBounties`, `NpcLevelLimit`, `NpcLevelCaps.bountyLimit`.
4. Outside chart drop tiers from defeated NPC levels. Done, committed on `main`: `ChartTiers.tierForLevel`, `ChartDrops.battleTier`. The defeated level comes from each lost ship's NPC tree tag (`NpcTreeTag.allocatedNodeCount` in `ItemLootListener.holdBattlePower` since the NPC loot merge). Battle `max_tier` stays 6, so an AI fleet at 35 (tier 7 by `level_min`) still drops tier 6.
5. Docs and settings text. Done, committed on `main`: the NPC Tree Size and NPC Bonus Scaling texts (en and zh_CN), the "NPC levels" section of `docs/docs.md`, and the NPC paragraphs of the README and forum post.

Steps 1 and 2 touch different files and can run in parallel. Step 3 needs step 1's category code. Step 4 needs step 2's `level_min` column.

## Tests

- Category resolution: the `pirateBehavior` flag, the AI-core commander, CSV overrides, and the faction fallback.
- The capped roll never exceeds the category cap and is unchanged below it. `NpcBonusScale` stops at the cap.
- Chart levels: tier 1 starts above the faction cap, tier 15 reaches the max level, and both clamp when the max level setting is lowered. The Fleet Strength % bonus scales the levels and clamps.
- Warden fleet ships roll exactly one level above the tier's range, including at tier 15.
- The bounty cap interpolates with difficulty, the AI bounty keeps 35, and the flagship bonus applies once.
- The battle chart tier comes from the highest defeated level: tier 1 for pirate and faction fleets, higher for AI fleets.
- The editor round trip keeps `level_min` and `level_max`.

## Decisions

- The player's faction and its allies use the faction cap, since allies can turn hostile quickly.
- The open-world roll does not shrink near the cap.
- Bounties have no minimum level. Below their cap they mirror like any other fleet.
- Every ship in the warden fleet is one level above its tier's range.
- The fleet bonus (`NpcBonusScale`) is revisited separately. With the default settings the capped bonus is x1.0 for pirates, x1.44 for factions and x1.89 for AI.
