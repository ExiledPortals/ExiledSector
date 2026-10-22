# Anchor gates: plan

A wormhole anchor can drop, be stocked or be salvaged while its area is still locked behind its gate. The player then holds an anchor that stays inert until something like entering Pascal or defeating the Lunar Fleet. This plan gives each gate its own rule for how its anchor behaves, instead of one rule for every gate. NPC fleets keep using their faction's area whatever its gate.

Status: planned, not built.

## Today

`WormholeAnchor.inertness` reports why an anchor can't be used:

- `AREA_OFF`: the area is hidden by its LunaLib toggle or its mod is missing.
- `LOCKED`: the anchor's own `unlock` column in `wormhole_anchors.csv` isn't met (only the ExoTech beacon has one).
- `GATED`: the exit node carries a closed gate tag from `node_gates.json` (`NodeGates.isLocked`).
- `EXIT_LOCKED`: the exit node's type has unmet unlock conditions.

Every anchor source checks only `AREA_OFF`, through `WormholeAnchorDefinitions.areaActive` or `FactionFleetDrops.available`:

| Source | Code |
|---|---|
| Battle drops | `ItemLootListener.reportEncounterLootGenerated` rolls `FactionFleetDrops.roll(..., FactionFleetDrops::available)` |
| Market stock | `MarketItemStock` calls `WormholeAnchorDefinitions.rollMarketStock` |
| Salvage | `ItemSalvageListener.reportAboutToShowLootToPlayer` filters with `areaActive` |
| NPC trees | `NpcItems.anchorFor`, used by `NpcFleetLeveller` and `NpcTreeTag` |

NPC trees already ignore gates: `NpcSkillTreeBuilder` builds its `NodeEligibility.Context` without the `gated` predicate. A gate's conditions are OR'd (`SkillTypeUnlockStatus.isLocked`): any one met opens it.

Battles that open a gate do so in `reportPlayerEngagement` (`AbyssSingularity.recordBattle`, `RemnantEncounter`), which fires before `reportEncounterLootGenerated` rolls that battle's drops. A gate opened by a fight is already open when its loot is rolled.

## Why not one filter

The first draft of this plan withheld every gated anchor until its gate opened. That doesn't fit two areas:

- Nightcross should hand out anchors freely, and finding one should be what opens the area.
- The Abyss and Stargazer should hand out a guaranteed anchor for beating the Singularity and the Lunar Fleet, not leave it to a random roll afterwards.

So each gate says how its anchor behaves.

## Gate anchor rules

`node_gates.json` gets an optional `anchors` field per gate:

- `withheld` (the default): battle drops, market stock and salvage skip the gated area's anchor until the gate opens. After that it drops normally.
- `free`: the anchor drops, stocks and salvages as if the area were ungated. Pair it with the `anchorFound` condition to make finding the anchor open the gate.

And an optional `rewardAnchor: true`: the battle that opens the gate always adds one of the area's anchors to its loot, once per save.

The rules only apply to gates on an area's wormhole exit, because only the exit decides whether an anchor works. A gate on an inner node (`gate_ancient_prototype_found`) is unaffected.

| Gate | Area | Rule | Change |
|---|---|---|---|
| `gate_pascal_visited` | Nightcross | `free` | Condition becomes `anchorFound` for the Nightcross volume, replacing `nightcrossPascalVisited` |
| `gate_singularity_defeated` | Abyss | `withheld`, `rewardAnchor` | The Singularity battle drops one Abyss anchor |
| `gate_lunar_fleet_defeated` | Stargazer | `withheld`, `rewardAnchor` | The Lunar Fleet battle drops one Stargazer anchor |
| `gate_frozen_heart_visited` | Enigma | `withheld` | |
| `gate_threat_fought` | Threat | `withheld` | Opens during the first fight, so that fight can already drop one |
| `gate_remnant_fought` | [REDACTED] | `withheld` | Same as Threat |
| `gate_celeste_met` | Iron Shell | `withheld` | Only on `iron_shell_volume` until that branch merges |

The ExoTech beacon's own `unlock` is a separate mechanism (`HyperNavBeaconGrant` hands it out) and is withheld from random sources the same way until it is met.

## Decided

- NPC fleets keep socketing their faction's anchor and building into its area regardless of the gate. `NpcItems.anchorFor` and `NpcSkillTreeBuilder` don't change.
- Anchors the player already holds are left alone.
- Relocking areas with the console also withholds their anchors again, because the check reads the same gate state.
- A Nightcross anchor counts as found when it first appears in battle loot or salvage loot, or when the player buys one. Seeing it in a market's stock doesn't count.

## Step 1: withheld anchors

Add `WormholeAnchorDefinitions.obtainable(String definitionId, SectorAPI sector)`: the area is active, the anchor's `unlock` is met, and the exit isn't held shut by a `withheld` gate or a locked exit type. Share the pieces with `WormholeAnchor.inertness` so the two can't disagree.

- `FactionFleetDrops.available` calls `obtainable` for anchors. That covers battle drops and market stock.
- `ItemSalvageListener` filters with `obtainable`.
- `NpcItems.anchorFor` keeps `areaActive`.
- `NodeGates` parses `anchors` and `rewardAnchor` and exposes them per gate tag. An unknown `anchors` value logs an error and counts as `withheld`.

## Step 2: boss rewards (Abyss and Stargazer)

A gate with `rewardAnchor` needs a per-battle check that says "this battle opened me". `ItemLootListener.holdLoot` asks it for the battle it holds and marks the pending loot. `reportEncounterLootGenerated` then adds one of the area's anchors to a marked battle's loot, unless that battle's random roll already produced one. A sector flag per gate (`$exiledSector_rewardAnchorGranted_<gate>`) makes it once per save, even if the gate is relocked and the boss fought again.

- Singularity: `AbyssSingularity.recordBattle` already spots the Genesis kill in the engagement result. Reuse that check.
- Lunar Fleet: `NightcrossUnlocks.isLunarFleetDefeated` only reads the flags Nightcross raises (`$na_defeated_macula`, `$defeatedLunarFleet`), and when Nightcross raises them relative to the loot roll isn't known. Before building, read Nightcross's source for how it marks the Lunar Fleet (fleet memory key or flagship hull) and use that as the per-battle check, the same way the Singularity check uses the Genesis hull.
- Fallback for both: if the gate opens with its reward not yet granted and no marked battle to carry it (the other mod raised its flag after the loot screen, or some route other than the battle), add the anchor straight to the player's cargo with a campaign message the next time the gate is checked.
- Saves that beat the boss before this update get no reward: their gate is already open, so the anchor drops normally.

## Step 3: Nightcross opens on finding an anchor

- New mod-gate condition `ANCHOR_FOUND`, parameterised by volume (`{"type": "anchorFound", "volume": "nightcross"}`). It reads a sector flag `$exiledSector_anchorFound_<volume>`.
- Record the flag where an anchor reaches the player: `ItemLootListener.reportEncounterLootGenerated` (battle loot), `ItemSalvageListener` (salvage loot), `MarketItemStock.reportPlayerMarketTransaction` (bought items) and the console grant command.
- Existing saves: on load, raise the Nightcross flag if the player already has a Nightcross anchor in cargo, storage or a socket, or if `$exiledSector_pascalVisited` is already set. Otherwise players who had the area open through Pascal would find it locked again.
- Remove `nightcrossPascalVisited` and its sector flag check once nothing uses them, and drop the `socketable.anchor.gate.gate_pascal_visited` string, since a found Nightcross anchor is never inert for this gate.
- The editor's unlock condition list (`tools/editor/src/pages/tree/model/types.ts`) and `editor_vocabulary.json` learn `anchorFound`. `node_gates.json` isn't edited by the editor, so it needs no round-trip change.

## Step 4: docs

`docs/docs.md`: a short section on gate anchor rules (`withheld`, `free`, `rewardAnchor`) and the Nightcross, Abyss and Stargazer behaviour. README: one line that story-locked areas' anchors only drop once the area can be opened, except Nightcross, whose anchor is what opens it.

## Consequences to accept

- Abyss anchors stop dropping from Abyssal fleets until the Singularity is beaten, then the Singularity fight hands one out.
- Stargazer anchors stop dropping and stocking until the Lunar Fleet is beaten, then that fight hands one out.
- Enigma and Iron Shell markets stop stocking their anchors until their gates open.
- Market stock rolled before the update keeps any withheld anchor until that market's next refresh.

## Build order

1. Gate rules in `NodeGates` and `obtainable`, routed through drops, markets and salvage (step 1).
2. `ANCHOR_FOUND`, the find recording, the save migration and the Nightcross gate change (step 3).
3. The Abyss and Stargazer rewards (step 2).
4. Docs (step 4).

## Tests

- `NodeGates`: parses `anchors` and `rewardAnchor`, defaults to `withheld`, and the real data gives each gate the rule in the table.
- `obtainable`: false for an area that is off, an unmet anchor `unlock`, a closed `withheld` gate and a locked exit type; true for a closed `free` gate.
- `FactionFleetDrops`, `rollMarketStock` and salvage skip a withheld anchor and keep a free one.
- `NpcItems.anchorFor` still returns the anchor for a gated area.
- `ANCHOR_FOUND`: closed until the flag is raised; each recording point raises it; the load migration raises it from a held anchor or the old Pascal flag.
- Boss rewards: a battle with the Genesis kill gets exactly one Abyss anchor, and a Lunar Fleet win exactly one Stargazer anchor, never two, and only once per save. The fallback grants the anchor to cargo when the gate opens without a marked battle.
