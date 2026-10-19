# Bypass subroutine: plan

Status: parked. The owner left it out of the subroutines rework on 2026-10-19, so it isn't being built. This file keeps the design and records the code that already exists for it, so it can be picked up later or removed cleanly. It was section 4 of `subroutines_rework.md`.

The name is a working name. The design comes from Path of Exile's Impossible Escape.

## Design

This is a single base, corrupted when created. It rolls one random keystone from the core volume (13 today, from `safety_overrides` to `operations_center`). Its radius is centred on that keystone, not on the socket it sits in. While socketed in an allocated standard socket, any node within the radius can be allocated without a connecting path. That includes the keystone.

- Reachability already has an alternative-path hook: `ShipSkillData.reachableAllocatedNodeIds` seeds the search from wormhole partners and from nodes with no connections. Nodes inside an active bypass radius become extra seeds in the same way. `AllocationGate.isConnected` accepts them too.
- Paths can grow outward from a node allocated in the radius as normal.
- The rolled keystone still costs a node slot and OP, and it still obeys exclusivity. The subroutine grants access, not the keystone itself.
- Only one per ship.
- Player only. NPC socket picks exclude it (decided 2026-10-19 for all new kinds).
- It composes with wormhole anchors without special cases: closing an anchor releases only nodes that are no longer reachable, and nodes inside an active bypass radius still are.

## Decided

- Removal releases the nodes that relied on it, with the same confirmation and refunds as closing an anchor wormhole, for player removal and battle loss alike (2026-10-19). That means A's `releaseNewlyStranded` / `previewRelease` and the arm-and-repeat `ReleaseConfirmation`.

## Code that already exists for Bypass

All of it is on `parallel-work`. None of it does anything in game, because `subroutines.csv` has no `BYPASS` row.

Bypass-specific:
- `SubroutineKind.BYPASS` (`src/main/java/exiledsector/items/SubroutineKind.java:11`): born corrupted, rolls a node, one per ship, and left out of the generic pools.
- `SubroutineRadius` (`src/main/java/exiledsector/items/SubroutineRadius.java:22` and `:32`): `BYPASS` returns no radius size and no centre, so no ring is drawn yet. The keystone centre goes here.
- The editor's kind list: `tools/editor_vocabulary.json:376` is generated from `SubroutineKind` by the `EditorVocabulary` test tool, so the editor already offers `BYPASS` in the Subroutines kind field and validates it.
- Tests that use the kind:
  - `ItemDefinitionsTest` (lines 82 to 90): parsing the kind.
  - `ItemDataIntegrityTest` (153 and 154): the expected kind list in error text.
  - `SubroutineCreationRollsTest`: born corruption, the frozen rolled node and the pool hook, with a Bypass row as one of the fixtures.
  - `SubroutineKindPoolsTest`: kept out of NPC socket picks and generic drops.
  - `SubroutineSocketLimitsTest`: the one-per-ship cap.
  - `SocketPlacementRefusalTest`: uses "Second Bypass" only as refusal text.
  - Editor tests `models.test.ts` and `modifiers.test.ts`: the kind list.
- `docs/docs.md` mentions it in "Subroutines" (which points here), the born-corruption and rolled-node paragraphs, the socket-limit paragraph, the Volume grant's pathing rule, and "Node radii".

Shared groundwork it would build on, built for other kinds and generic:
- Born corruption: `CorruptionOutcome.INNATE`, set from `SubroutineKind.bornCorrupted()` in the `Subroutine` constructors and `readResolve`.
- The frozen rolled node: `Subroutine.rolledNodeId`, picked by `SubroutineNodePools` from the seed. Bypass would register its core-keystone pool with `SubroutineNodePools.use(SubroutineKind.BYPASS, ...)`; no pool is registered today, so a Bypass item would roll nothing.
- The one-per-ship cap: `SubroutineSocketLimits`, checked when socketing (`SocketPlacement`, `TreeAllocationSession`), on transposition (`SocketCustody.unsocketIfMisfit`), for NPC rolls (`NpcFleetLeveller`) and in the load sweep (`SocketTypeMigration`).
- Kept out of drops and NPC sockets: `SubroutineKind.inGenericPools` via `ItemDrops.isDroppable` and `NpcItems.pickDefinition`. A drop path of its own is still needed.
- Radii: `exiledsector.skills.radius` (`NodeRadii`, `NodeRadiusCache`, `RadiusSize`) with sizes in `data/config/exiledSector/node_radii.json`. The centre node is never in its own radius, so Bypass must add the keystone itself.
- The ring: `NodeRadiusRingRenderer`, reached through `SkillTreeCanvasPlugin.setHighlightedRadius`, with hover hooks on the socket and in item storage that ask `SubroutineRadius` for the centre and size.
- Reachability and release: `ShipSkillData.reachableAllocatedNodeIds`, `AllocationGate.isConnected`, `releaseNewlyStranded` / `previewRelease` and `ReleaseConfirmation`, from the wormhole anchor work. Volume subroutines also add granted notables to that search, after the path search and never as a source (`ShipSkillData.grantOnlyNodeIds`). Bypass seeds would be real sources, so they must not reuse that late-join path.

## Not built

- A `subroutines.csv` row, with name, flavour text and zh_CN text.
- The core-keystone pool registration.
- Radius seeds in `reachableAllocatedNodeIds` and `AllocationGate.isConnected`, also in the drawn snapshot and the planning copy.
- The keystone centre and size in `SubroutineRadius`, so the ring draws around the keystone.
- Release on removal and on battle loss through `releaseNewlyStranded` and `ReleaseConfirmation`, and the matching `RespecPlan` handling.
- A drop path of its own.
- A console grant command, `docs/docs.md` section, and tests.

## Removing it instead

If Bypass is dropped for good, these go:
- the `BYPASS` value;
- its `SubroutineRadius` cases;
- the Bypass fixtures in the tests listed above;
- the docs mentions.

Then regenerate `tools/editor_vocabulary.json`. `SubroutineKind` isn't persisted on items, so removing the value is save-safe.
