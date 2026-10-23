# Janus anchor gate: plan

Wormhole anchors are the only way into a faction volume, and nothing stops them before the Galatia Academy story. A player can find a Hegemony anchor in the first week and open the Hegemony volume. Before anchors, every faction wormhole used the `wormhole` node type, which needs the vanilla flag `$playerCanUseGates`. The game sets that flag once the player has the Janus Device and can use the gate network. The `wormhole_exit` type that anchors open has no unlock condition, so that gate was lost.

This plan puts it back on the anchors. By default no anchor drops, stocks or opens a wormhole until the player has the Janus Device. A LunaLib toggle lets players use anchors from the start. The Exoship's Hyper-Nav Beacon is exempt, because Random Assortment of Things' own story already gates it.

Status: built and tested 2026-10-23, uncommitted on parallel-work. Not yet checked in game.

## Where the code lives

Paths are relative to `exiledsector-skill-tree/src/main/java/exiledsector/` unless they name another module.

The Janus check already exists in Star Charts: `WormholeGateway.JANUS_FLAG` and its `isOpen` read in `exiledsector-star-charts` (`charts/WormholeGateway.java`). The skill tree can't see Star Charts, so the check moves to core and both domains call it.

Anchors reach the player through:

| Source | Code |
|---|---|
| Battle drops | `effects/SkillTreeLootSource` rolls `FactionFleetDrops.roll(..., FactionFleetDrops::available)` |
| Market stock | `WormholeAnchorDefinitions.rollMarketStock`, which also filters with `FactionFleetDrops.available` |
| Showcase salvage | `SkillTreeLootSource.addShowcaseLoot` |
| Exoship recovery | `effects/HyperNavBeaconGrant.grantOnce`, run every economy tick until it grants once |
| Console | `console/GrantWormholeAnchorsCommand` |

A socketed anchor opens its exit in `skills/ShipTrees.openAnchorWormholes`, through `items/WormholeAnchorLinks`, which calls `WormholeAnchor.canOpen`, which is `inertness(sector) == null`.

## The toggle

A new boolean in `ui/SkillTreeSettings`, under the "Unlock Conditions" header: "Allow Wormhole Anchors Before the Janus Device", default off. It reads live, so turning it on or off mid-save applies at once. The existing "Disable Game State Unlock Conditions" toggle stays separate: it opens memory-flag nodes, not anchors.

The gate is open when the toggle is on or `$playerCanUseGates` is set.

## Step 1: a shared Janus check

- New `JanusDevice` in core (`exiledsector-core`, package `exiledsector.campaign` or next to the other sector-flag readers): `obtained(SectorAPI)` reads `$playerCanUseGates`, failing closed with a logged error like `WormholeGateway` does today.
- `WormholeGateway` calls it instead of reading the flag itself.
- The skill tree adds `AnchorJanusGate` (in `items/`). `open(SectorAPI)` is true when the toggle is on or `JanusDevice.obtained`. `applies(String definitionId)` is false for the ExoTech volume's anchor (the beacon, looked up through `WormholeAnchorDefinitions.definitionIdFor(SkillTags.EXOTECH_REGION)` as `HyperNavBeaconGrant` does), so every check below skips it.

## Step 2: withhold anchors

- `FactionFleetDrops.available` returns false for an anchor while the gate is closed. That covers battle drops and market stock.
- `SkillTreeLootSource.addShowcaseLoot` skips anchors the same way.
- The beacon is exempt: `HyperNavBeaconGrant.grantOnce` is unchanged, and the beacon still drops from ExoTech fleets and stocks in their markets before Janus.
- The console grant command is unchanged: it's a dev tool.
- NPC fleets are unchanged. `NpcItems.anchorFor` keeps reading only `areaActive`, so NPC trees still build into their faction's volume before Janus.

If the anchor gates plan (`anchor_gates.md`) is built first, the Janus check goes into its `obtainable` instead, and its factionless salvage drops follow it too. If this plan is built first, `obtainable` takes it over when it lands.

## Step 3: inert before Janus

- `WormholeAnchor.Inertness` gets `JANUS`, checked after `AREA_OFF` and before `LOCKED`. A held anchor (console grant, old save, an NPC item) can't open its wormhole until the gate is open. The beacon never reports `JANUS`.
- New string `socketable.anchor.janus`: "<bad>Inert</bad> until you can use the gate network." in `data/strings/exiledSector/skillTree/en.json`, with a zh_CN draft.
- Wormholes that are already open stay open. `openAnchorWormholes` only opens exits and nothing closes one when its anchor turns inert. Confirm that while building, and keep it that way.

## Step 4: strings and docs

- `settings.unlock.anchorsBeforeJanus` and its tooltip in `en.json` and `zh_CN.json`. Tooltip draft: "Wormhole anchors drop, sell and open wormholes before you have the Janus Device. Off, none but the Hyper-Nav Beacon appear until you can use the gate network."
- `docs/docs.md`: one line in the anchor section (or a new short "Wormhole anchors" section) that anchors wait for the Janus Device unless the toggle is on.
- README and `docs/forum_post.bbcode`: one line saying the same.

## Decided

- Saves that opened a volume before Janus keep it open. The rule only stops new wormholes from opening (2026-10-23).
- The Exoship's Hyper-Nav Beacon is exempt from the Janus gate everywhere: grant, drops, stock and inertness (2026-10-23).
- The three core-to-core `wormhole` shortcuts keep their own `$playerCanUseGates` unlock condition. The new toggle doesn't open them; "Disable Game State Unlock Conditions" still does (2026-10-23).

## Build order

1. Done: `JanusDevice` in core and `WormholeGateway` moved onto it (step 1).
2. Done: The toggle and `AnchorJanusGate` (step 1).
3. Done: Withheld drops, stock and showcase (step 2).
4. Done: `Inertness.JANUS` and its string (step 3).
5. Done: Strings and docs (step 4).

## Tests

Each test goes in the lowest module that sees every class it uses.

- `JanusDevice`: true with the flag, false without it, false and no throw when memory can't be read. `WormholeGateway`'s existing tests still pass.
- `AnchorJanusGate`: open with the flag, open with the toggle and no flag, closed with neither. `applies` is false for the beacon and true for every other anchor.
- `FactionFleetDrops.available` and `rollMarketStock` skip anchors while closed and keep them once open. The beacon and other item types are unaffected.
- `addShowcaseLoot` skips anchors while closed.
- `HyperNavBeaconGrant.grantOnce` still grants before Janus.
- `WormholeAnchor.inertness` returns `JANUS` before the gate opens and null after, and never returns it for the beacon. `openAnchorWormholes` doesn't open an exit before Janus and leaves an already-open one alone.
- `NpcItems.anchorFor` still returns the anchor before Janus.
- `TranslationKeyReferencesTest` covers the new keys.
