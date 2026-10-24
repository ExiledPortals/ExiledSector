# Anchor gates: plan

This plan closes two holes in how wormhole anchors reach the player.

A wormhole anchor can drop, be stocked or be salvaged while its area is still locked behind its gate. The player then holds an anchor that stays inert until something like entering Pascal or defeating the Existential Threat. This plan gives each gate its own rule for how its anchor behaves, instead of one rule for every gate. NPC fleets keep using their faction's area whatever its gate.

The opposite also happens: an area can be open while its anchor can never be found, because the factions that drop and sell it aren't in the game. Those anchors move to salvage and ruins (see "Factionless volumes").

Status: built and tested 2026-10-23, uncommitted on parallel-work. Not yet checked in game.

## Where the code lives

Anchors, gates and unlock conditions all belong to the skill tree domain, in `exiledsector-skill-tree`. Core's campaign listeners (`ItemLootListener`, `ItemSalvageListener`, `MarketItemStock` in `exiledsector-core`, package `exiledsector.effects`) know nothing about anchors. They call every domain's `LootSource` (`exiledsector.loot`, in core), and the skill tree's is `SkillTreeLootSource` (`exiledsector.effects`, in `exiledsector-skill-tree`, registered by `SkillTreeDomain.lootSources`). So almost all of this plan's code goes into `exiledsector-skill-tree`. The one exception is recording a bought anchor (step 3), which needs a new hook in core.

Paths below are relative to `exiledsector-skill-tree/src/main/java/exiledsector/` unless they name another module.

## Today

`WormholeAnchor.inertness` (`items/WormholeAnchor.java`) reports why an anchor can't be used:

- `AREA_OFF`: the area is hidden by its LunaLib toggle or its mod is missing.
- `LOCKED`: the anchor's own `unlock` column in `data/config/exiledSector/wormhole_anchors.csv` isn't met (only the ExoTech beacon has one).
- `GATED`: the exit node carries a closed gate tag from `data/config/exiledSector/node_gates.json` (`skills/unlock/NodeGates.isLocked`).
- `EXIT_LOCKED`: the exit node's type has unmet unlock conditions.

Every anchor source checks only `AREA_OFF`, through `items/WormholeAnchorDefinitions.areaActive` or `items/FactionFleetDrops.available`:

| Source | Code |
|---|---|
| Battle drops | `SkillTreeLootSource.addEncounterLoot` and `deliverBattleLoot` roll `FactionFleetDrops.roll(..., FactionFleetDrops::available)` from the DP per faction that `recordEngagement` gathered |
| Market stock | `SkillTreeLootSource.stockSubmarket` calls `WormholeAnchorDefinitions.rollMarketStock` on the submarkets in `MarketItemStock.ANCHOR_SUBMARKETS` |
| Salvage | Salvage sites never drop anchors. The only salvage path is the console showcase (`ShowcaseLootCommand` in core), whose anchors come from `SkillTreeLootSource.addShowcaseLoot`, filtered with `areaActive` |
| NPC trees | `items/NpcItems.anchorFor`, used by `effects/NpcFleetLeveller` and `skills/npc/NpcTreeTag` |

NPC trees already ignore gates: `skills/npc/NpcSkillTreeBuilder` builds its `NodeEligibility.Context` without the `gated` predicate. A gate's conditions are OR'd (`skills/unlock/SkillTypeUnlockStatus.isLocked`): any one met opens it.

Battles that open a gate do so in `reportPlayerEngagement` (`compat/AbyssSingularity.recordBattle`, `skills/unlock/RemnantEncounter.recordBattle`), which fires before `ItemLootListener.reportEncounterLootGenerated` asks the loot sources for that battle's drops. A gate opened by a fight is already open when its loot is rolled.

## Factionless volumes

`skills/tags/AreaToggles` gives the LOST_SECTOR area (`enigma`, `kesteven`) and the Nightcross area (`nightcross`, `stargazer`) an `On` option that shows the area without its mod. Their gates then fail open (`UnlockConditionType.ModGate.isOpen` returns true when the mod is missing), which was the rule for a missing mod back when wormholes needed no item. Now that they need an anchor, the anchor has no source:

- Battle drops and market stock both need a faction from the anchor's `FactionFleetDrops.Source`: the volume's factions in `npc_faction_volumes.csv` plus the row's `drop_factions`. For these four volumes that is only `kesteven`, `enigma`, `nightcross` or `stargazer`, which exist only with their mod. No fleet or market of theirs ever appears.
- Salvage sites never drop anchors (the showcase is a console tool).

So with `On` and no mod, the four areas show their nodes but can never be entered. The RAT and Tania areas only offer `Auto` and `Off` (`REQUIRED_MOD_OPTIONS`), so their factions always exist when the area does. Vanilla factions always exist.

Nexerelin opens the same hole for any faction it eliminates. A faction that loses every market stays defined but stops fielding patrols and has nothing to sell, so a vanilla volume such as the Hegemony's can become unreachable mid-game.

### Fix: factionless anchors drop from salvage and ruins

A faction is gone when it doesn't exist in the sector (`sector.getFaction(id) == null`), or when Nexerelin is installed, counts the faction as playable and no longer has it alive. An anchor is factionless when every faction in its `Source` is gone. A factionless anchor that is `obtainable` (step 1) can drop from salvage sites and tech mining instead:

- The Nexerelin check goes in `compat/NexerelinCompat`, next to its alliance lookup. `exerelin.campaign.SectorManager.isFactionAlive` reads `liveFactionIds`, which only ever holds playable factions that hold markets (`NexConfig.getFactionConfig(id).playableFaction`). So the check counts a faction as eliminated only when its Nexerelin config is playable and it isn't alive. Remnant, Threat and the Abyssal factions can't be eliminated in Nexerelin and keep spawning, so as non-playable factions they never count as eliminated, whatever `isFactionAlive` says. If Nexerelin later revives the faction, its anchor goes back to battle drops and markets.

- New data file `data/config/exiledSector/anchor_salvage.csv` (`site,chance`), owned by the skill tree domain, keyed by the same site ids as `item_salvage.csv`. Placeholder rows: the rarer sites (`derelict_mothership`, `derelict_survey_ship`, `station_research`, `station_research_remnant`, `orbital_habitat`, `orbital_habitat_remnant`, `weapons_cache_high`, `techmining_first_find`, `techmining_monthly`), chance 0.1. The editor doesn't edit it, so it needs no round-trip change.
- `SkillTreeLootSource` overrides `addSalvageLoot` and `addTechMiningFinds`. Each rolls the site's chance once, and on a hit adds one anchor picked evenly from the factionless, obtainable ones. At most one anchor per site or tech mining find. Salvage seeds from the salvage seed with its own salt, tech mining uses `Find.random()`.
- Core only calls `addSalvageLoot` for sites with an `item_salvage.csv` rule, and passes the entity, not the site id. Make `ItemSalvageListener.siteId` public so the skill tree can read it. That is the only core change.
- Chart bonuses don't scale it, for the same reason they don't scale battle anchors.
- If the faction appears later (the mod is added to the save), the anchor stops being factionless and goes back to battle drops and markets only.
- Nightcross finds from salvage or ruins count as found for `ANCHOR_FOUND` (step 3), so they open the Nightcross gate when the mod is installed. Without the mod the gate fails open anyway.

## Why not one filter

The first draft of this plan withheld every gated anchor until its gate opened. That doesn't fit two areas:

- Nightcross should hand out anchors freely, and finding one should be what opens the area.
- The Abyss and Stargazer should hand out a guaranteed anchor for beating the Singularity and the Existential Threat, not leave it to a random roll afterwards.

So each gate says how its anchor behaves.

## Gate anchor rules

`node_gates.json` gets an optional `anchors` field per gate:

- `withheld` (the default): battle drops, market stock and the showcase skip the gated area's anchor until the gate opens. After that it drops normally.
- `free`: the anchor drops and stocks as if the area were ungated. Pair it with the `anchorFound` condition to make finding the anchor open the gate.

And an optional `rewardAnchor: true`: the battle that opens the gate always adds one of the area's anchors to its loot, once per save.

The rules only apply to gates on an area's wormhole exit, because only the exit decides whether an anchor works. A gate on an inner node (`gate_ancient_prototype_found`) is unaffected.

| Gate | Area | Rule | Change |
|---|---|---|---|
| `gate_pascal_visited` | Nightcross | `free` | Condition becomes `anchorFound` for the Nightcross volume, replacing `nightcrossPascalVisited` |
| `gate_singularity_defeated` | Abyss | `withheld`, `rewardAnchor` | The Singularity battle drops one Abyss anchor |
| `gate_existential_threat_defeated` | Stargazer | `withheld`, `rewardAnchor` | The Existential Threat battle drops one Stargazer anchor |
| `gate_frozen_heart_visited` | Enigma | `withheld` | |
| `gate_threat_fought` | Threat | `withheld` | Opens during the first fight, so that fight can already drop one |
| `gate_remnant_fought` | [REDACTED] | `withheld` | Same as Threat |
| `gate_celeste_met` | Iron Shell | `withheld` | Only on `iron_shell_volume` until that branch merges |

The ExoTech beacon's own `unlock` is a separate mechanism (`effects/HyperNavBeaconGrant` hands it out) and is withheld from random sources the same way until it is met.

## Decided

- NPC fleets keep socketing their faction's anchor and building into its area regardless of the gate. `NpcItems.anchorFor` and `NpcSkillTreeBuilder` don't change.
- Anchors the player already holds are left alone.
- Relocking areas with the console also withholds their anchors again, because the check reads the same gate state.
- A Nightcross anchor counts as found when it first appears in battle loot, salvage loot or a tech mining find, or when the player buys one. Seeing it in a market's stock doesn't count.
- Factionless anchors drop from salvage and ruins only, never from battles or markets.
- Factions eliminated in Nexerelin count as gone, so their anchors join the factionless ones (2026-10-23).

## Step 1: withheld anchors

Add `WormholeAnchorDefinitions.obtainable(String definitionId, SectorAPI sector)`: the area is active, the anchor's `unlock` is met, and the exit isn't held shut by a `withheld` gate or a locked exit type. Share the pieces with `WormholeAnchor.inertness` so the two can't disagree.

- `FactionFleetDrops.available` calls `obtainable` for anchors. That covers battle drops and market stock.
- `SkillTreeLootSource.addShowcaseLoot` filters with `obtainable`.
- `NpcItems.anchorFor` keeps `areaActive`.
- `NodeGates` parses `anchors` and `rewardAnchor` and exposes them per gate tag. An unknown `anchors` value logs an error and counts as `withheld`.

No core change.

## Step 2: boss rewards (Abyss and Stargazer)

A gate with `rewardAnchor` needs a per-battle check that says "this battle opened me". `SkillTreeLootSource.recordEngagement` asks it for each engagement and marks the battle in its own state on `BattleLoot` (`battle.state(this, ...)`, next to the DP per faction it already keeps). `addEncounterLoot` and `deliverBattleLoot` then add one of the area's anchors to a marked battle's drops, unless that battle's random roll already produced one. A sector flag per gate (`$exiledSector_rewardAnchorGranted_<gate>`) makes it once per save, even if the gate is relocked and the boss fought again.

- Singularity: `AbyssSingularity.recordBattle` already spots the Genesis kill in the engagement result. Reuse that check.
- Existential Threat: `compat/NightcrossUnlocks.isExistentialThreatDefeated` only reads the flags Nightcross raises (`$na_defeated_macula`, `$defeatedLunarFleet`), and when Nightcross raises them relative to the loot roll isn't known. Before building, read Nightcross's source for how it marks the Existential Threat (fleet memory key or flagship hull) and use that as the per-battle check, the same way the Singularity check uses the Genesis hull.
- Fallback for both: if the gate opens with its reward not yet granted and no marked battle to carry it (the other mod raised its flag after the loot screen, or some route other than the battle), add the anchor straight to the player's cargo with a campaign message the next time the gate is checked.
- Saves that beat the boss before this update get no reward: their gate is already open, so the anchor drops normally.

No core change: `ItemLootListener.holdLoot` already passes every engagement to the loot sources.

## Step 3: Nightcross opens on finding an anchor

- New mod-gate condition `ANCHOR_FOUND` in `skills/unlock/UnlockConditionType`, parameterised by volume (`{"type": "anchorFound", "volume": "nightcross"}`). It reads a sector flag `$exiledSector_anchorFound_<volume>`.
- Record the flag where an anchor reaches the player:
  - Battle loot: `SkillTreeLootSource.addEncounterLoot` and `deliverBattleLoot`.
  - Showcase loot: `SkillTreeLootSource.addShowcaseLoot`.
  - Salvage and tech mining: `SkillTreeLootSource.addSalvageLoot` and `addTechMiningFinds` (factionless anchors).
  - The console grant command, `console/GrantWormholeAnchorsCommand`.
  - Bought items: core's `MarketItemStock.unlistBought` turns bought listings into real items but can't see anchors. Add a default `LootSource` method (for example `itemsBought(List<ItemData>)`) that `unlistBought` calls with what it converted, and record the flag in `SkillTreeLootSource`'s override.
- Existing saves: on load, raise the Nightcross flag if the player already has a Nightcross anchor in cargo, storage or a socket, or if `$exiledSector_pascalVisited` is already set. Otherwise players who had the area open through Pascal would find it locked again. Run it from `SkillTreeDomain.onGameLoad`, next to `WormholeAnchorMigration.run()`.
- Remove `NIGHTCROSS_PASCAL_VISITED`, `NightcrossUnlocks.isPascalVisited` and its sector flag check once nothing uses them, and drop the `socketable.anchor.gate.gate_pascal_visited` string from `data/strings/exiledSector/skillTree/en.json` and `zh_CN.json`, since a found Nightcross anchor is never inert for this gate.
- The editor's unlock condition list (`tools/editor/src/pages/tree/model/types.ts`) and `tools/editor_vocabulary.json` learn `anchorFound`. `node_gates.json` isn't edited by the editor, so it needs no round-trip change.

## Step 4: docs

`docs/docs.md`: a short section on gate anchor rules (`withheld`, `free`, `rewardAnchor`), the Nightcross, Abyss and Stargazer behaviour, and factionless anchors dropping from salvage and ruins. README and `docs/forum_post.bbcode`: one line that story-locked areas' anchors only drop once the area can be opened, except Nightcross, whose anchor is what opens it, and one line that an area turned on without its mod, or whose faction Nexerelin has eliminated, has its anchor in salvage and ruins.

## Consequences to accept

- Abyss anchors stop dropping from Abyssal fleets until the Singularity is beaten, then the Singularity fight hands one out.
- Stargazer anchors stop dropping and stocking until the Existential Threat is beaten, then that fight hands one out.
- Enigma and Iron Shell markets stop stocking their anchors until their gates open.
- Market stock rolled before the update keeps any withheld anchor until that market's next refresh.

## Build order

1. Done: Gate rules in `NodeGates` and `obtainable`, routed through drops, markets and the showcase (step 1).
2. Done: Factionless anchors from salvage and tech mining ("Factionless volumes"). It needs `obtainable`, and step 3 records its finds.
3. Done: `ANCHOR_FOUND`, the find recording, the `LootSource` purchase hook, the save migration and the Nightcross gate change (step 3).
4. Done: The Abyss and Stargazer rewards (step 2).
5. Done: Docs (step 4).

## Tests

Each test goes in the lowest module that sees every class it uses. Most of these sit next to the existing ones in `exiledsector-skill-tree/src/test/java` (`NodeGatesTest`, `NodeGatesDataTest`, `AbyssSingularityTest`, `NightcrossUnlocksTest`, `GrantWormholeAnchorsCommandTest`, `WormholeAnchorMigrationTest`). Tests that drive the core listeners end to end go in `exiledsector-mod/src/test/java` with `ItemLootListenerTest`, `MarketItemStockTest`, `NpcItemsTest` and `WormholeAnchorTest`.

- `NodeGates`: parses `anchors` and `rewardAnchor`, defaults to `withheld`, and the real data gives each gate the rule in the table.
- `obtainable`: false for an area that is off, an unmet anchor `unlock`, a closed `withheld` gate and a locked exit type; true for a closed `free` gate.
- `FactionFleetDrops`, `rollMarketStock` and the showcase skip a withheld anchor and keep a free one.
- `NpcItems.anchorFor` still returns the anchor for a gated area.
- Factionless: an anchor is factionless only when every one of its volume's factions and `drop_factions` is missing or eliminated. A playable faction Nexerelin no longer has alive counts as eliminated. A non-playable one (Remnant, Abyssal) never does, and nothing counts as eliminated without Nexerelin. Salvage and tech mining drop at most one factionless, obtainable anchor at the site's chance, never one whose faction exists, and nothing at sites missing from `anchor_salvage.csv`.
- `ANCHOR_FOUND`: closed until the flag is raised; each recording point raises it, including a bought anchor through the new `LootSource` hook; the load migration raises it from a held anchor or the old Pascal flag.
- Boss rewards: a battle with the Genesis kill gets exactly one Abyss anchor, and a Existential Threat win exactly one Stargazer anchor, never two, and only once per save. The fallback grants the anchor to cargo when the gate opens without a marked battle.
