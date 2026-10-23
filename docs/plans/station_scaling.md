# Station scaling: plan

Player ships and NPC ships both get much stronger through their skill trees, but combat stations get nothing. Market stations (orbital stations, battlestations, star fortresses, including the player's own colonies) and the Remnant Nexus have no tree and no bonus. A station that was a fair fight in vanilla falls further behind as fleets level up.

This plan gives stations generated skill trees, scaled like NPC fleets, and shares them with the station's modules.

Status: planned, not built.

## Where the code lives

Paths are relative to `exiledsector-skill-tree/src/main/java/exiledsector/` unless they name another module.

Stations are dropped at four points:

1. `effects/NpcFleetLeveller.isLevellable` rejects any fleet in station mode (`!fleet.isStationMode()`). That covers every market station fleet (vanilla `OrbitalStation` calls `setStationMode(true)`) and the Remnant Nexus (`RemnantThemeGenerator.addBattlestations`).
2. `skills/npc/NpcShipSelector` rejects any member where `member.isStation()`, so a station hull is skipped even outside a station fleet.
3. With no tree tag, `effects/SkillTreeEffectSource.bonusScale` gives no `NpcBonusScale`, and core's `SkillTreeHullMod` applies nothing.
4. `effects/ModuleTreeShare.attach` returns early for `ship.isStation()`, so a station's modules would get no share even if the station had a tree.

Nothing else scales stations. Station fleets do reach the levelling entry points (`NpcFleetInflationListener`, `NpcFleetSweepScript`, `NpcFleetDialogListener`), so lifting the filters is enough to start levelling them.

How NPC trees are sized today: `skills/npc/NpcTreeSize.roll` takes a random player combat ship's tree power times 80 to 120% (LunaLib settings), clamped by the fleet's level cap from `skills/npc/NpcLevelCaps.limitFor`. Market stations fall in the `faction` category (25) or `pirate` (15). The Nexus falls in `ai` (35), through its alpha core commander or the `remnant` row in `npc_levels.csv`.

## Design

A station is levelled like any other NPC ship, with three differences:

- It's always chosen. `NpcShipSelector.isChosen` only picks officered ships, flagships and a 30% roll of the rest. A station fleet's only member is the station, so it should always get a tree, whatever those settings say.
- It gets a station profile. Movement, phase and ship-system nodes do nothing on an immobile station. The builder's `ShipProfile` and `NpcFocus` should mark a station as immobile, and `NpcSkillTreeBuilder` should skip nodes tagged for movement, phase and ship systems, and fighter nodes when it has no bays. Read how `NpcFocus` weights tags before building, and add a station weight there rather than a separate builder.
- Its modules get the share. Most of a station's guns sit on its modules (`isStationModule()`), so `ModuleTreeShare` must let station parents through. A station's modules are its main body, not add-ons, so they share at a higher rate than mobile modular ships: a separate "Station Module Share" setting, default 100%.

`NpcBonusScale` then applies to stations through the tree tag, with no extra code.

A new LunaLib toggle on the NPC tab, "Skill Trees for Stations", default on. Turning it off leaves stations as they are today. Trees already given stay until the fleet is rebuilt, the same as the other NPC toggles.

## Steps

1. Lift the filters behind the toggle: `isLevellable` lets station-mode fleets through, `NpcShipSelector` accepts station members, and `isChosen` always picks a station.
2. Add the station profile to `ShipProfile` / `NpcFocus` and have the builder skip the useless parts.
3. Let `ModuleTreeShare.attach` take station parents, with the station share setting. Check that the existing `ModuleShares` classification of each effect holds for station modules too (for example hull and armor rescaling on modules that never move).
4. Check rebuilds: a market station that's destroyed and rebuilt, or upgraded (orbital station to battlestation to star fortress), gets new members. `decide` keys records by member, so the new hull should be levelled on the next sweep. Confirm, and test it.
5. Docs: a short "Stations" paragraph in `docs/docs.md` under "Generated NPC trees", and update "Module effects" for station parents. README and `docs/forum_post.bbcode`: one line that stations now get skill trees.

## Decided

- Stations get generated trees, not a flat bonus (2026-10-23).
- Station modules share through their own "Station Module Share" setting, default 100%, separate from Module Share (2026-10-23).
- The player's own colony stations get trees too, under the same rules and sizing (2026-10-23).
- Stations use their faction's level cap category (faction 25, pirate 15, AI 35), with no separate station category (2026-10-23).

## Tests

Each test goes in the lowest module that sees every class it uses (most are in `exiledsector-mod/src/test/java` next to `NpcFleetLevellerTest`).

- `NpcFleetLevellerTest.playerStationAndLocationlessFleetsAreNeverLevelled` changes: a station-mode fleet is levelled when the toggle is on and skipped when it's off. Player fleets and fleets with no location are still never levelled.
- `NpcShipSelector` accepts a station member and always picks it, with officered, flagship and the other-ship chance all off.
- The station profile never picks movement, phase or ship-system nodes, and no fighter nodes without bays.
- `ModuleTreeShare.attach` attaches to a station parent with a tree and uses the station share.
- A rebuilt or upgraded station's new member gets a tree.
- The Nexus lands in the `ai` cap category, and a market station in its faction's.
