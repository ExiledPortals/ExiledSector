# Subroutines rework: plan

Subroutines currently have no identity of their own. A tree socket accepts only a subroutine, but where the socket sits doesn't matter: the subroutine just adds flat stats to the whole ship. And 74 of the 86 effects subroutines can roll also exist on hull items. This rework makes every subroutine about the skill tree. Hull items keep the plain stat rolls; subroutines grant, rewrite or bypass parts of the tree.

Alongside the rework comes a new socketable type, the wormhole anchor (section 6). It isn't a subroutine, but it shares the tree sockets and it changes how the faction volumes are reached, so it's planned here too.

The first step is already done. Commit `49ebd8f6` turned all 14 tree sockets into `small_flux_optional` nodes, so sockets can be placed deliberately once the designs below are settled. The `modular_hull_socket` skill type is still in place.

Finished work is marked with a "Status" line, or "(done)" on a single bullet or step. Done means built and tested on the `parallel-work` branch.

The names below are working names. Where a design comes from a Path of Exile item, the PoE name is given so it's clear what's being emulated. Player text uses our own names.

## The pieces

1. Delete the existing subroutines entirely.
2. Contraband Modspec Cache (replaced the Volume subroutines on 2026-10-19): one unique subroutine, corrupted from the start, granting 1 to 3 random notables from anywhere in the tree for free.
3. Doctrine Archives (PoE: Timeless): one per vanilla faction. Each either transforms the nodes in its radius or adds modifiers to them.
4. Bypass subroutine (PoE: Impossible Escape): corrupted, rolls a random core keystone, and lets nodes near that keystone be allocated without a path.
5. Unique subroutines: hand-made subroutines that cross the item identity line and carry small, themed sets of SkillEffects.
6. Wormhole anchors: a new socketable type, one per faction volume. The faction wormholes stop being part of the tree; a socketed anchor opens the wormhole to its volume instead.

Expansion subroutines (PoE: Cluster Jewels) are dropped from this plan. The tree has no clusters to build them around yet.

The Bypass subroutine (item 4) was left out on 2026-10-19 by owner decision and isn't being built. Its design and the code already built for it are in `bypass_subroutine.md`.

## 1. Delete the existing subroutines

Status: done. The sweep is `DeletedSubroutines`, run from `onGameLoad` and described in `docs/docs.md` under "Subroutines".

All four definitions in `subroutines.csv` are removed: `domain_subroutine_military`, `_industrial`, `_consumer` and `_management`. So are every `item_modifiers.csv` row scoped only to `subroutine` (34 prefixes and 48 suffixes). The 8 rows scoped `subroutine;hull_item` lose their `subroutine` scope. Subroutines stop rolling explicit prefixes and suffixes altogether.

Old saves lose their subroutines with no compensation. Loading must not crash, though:
- The `Subroutine` class and its XStream alias `exiledSector.Subroutine` stay, because the new kinds reuse them. Renaming a persisted class breaks saves; adding fields is safe.
- On load, the four retired subroutines are deleted silently: from item storage, from player and NPC sockets, from fleet cargo and from every market's storage. This includes 0.8.0 saves, where they load as `UnresolvedItem` under the old `exiledSector.Socketable` alias. Deletion is keyed on a fixed list of retired ids (`DeletedSubroutines.DELETED_DEFINITION_IDS`), not on "definition missing". A subroutine whose definition is only temporarily missing (a failed load, a bad row) stays an `UnresolvedItem` and comes back. Retiring another subroutine later means adding its id to that list.
- The subroutine icons in `graphics/icons/socketables/` can go once nothing references them.
- `ItemDataIntegrity`, the editor's Subroutines page and the round-trip tests need updating to match.

Hull items are unaffected.

## Shared groundwork

Every new kind is still a `Subroutine` (`ItemType.SUBROUTINE`). A new `kind` column in `subroutines.csv` picks its behaviour: `VOLUME`, `DOCTRINE`, `BYPASS` or `UNIQUE`. It maps to a `SubroutineKind` enum that is read from the definition, never persisted on the item. (done)

Most kinds need the following:

- A frozen rolled node. Volume and Bypass subroutines each roll one tree node when they're created: a notable or a keystone. That node id is stored on the item and never rerolled, in line with the socketable-freezing rule. Doctrine Archives use the item's existing `seed` instead. (done: `Subroutine.rolledNodeId`, picked by `SubroutineNodePools` from the seed. Volume and Bypass register their candidate pools with `SubroutineNodePools.use` when they're built. Cargo stacks still carry only the seed, so the node is fixed when the item is absorbed into storage.)
- Corruption from the start. Volume and Bypass subroutines are created already corrupted, which locks them from all currency. The current `CorruptionOutcome` values describe what a Corrupted Nanoswarm did to an item. Born-corrupted items need their own value (for example `INNATE`), so the tooltip can say "Corrupted" without claiming something was changed. (done: `CorruptionOutcome.INNATE`, set from `SubroutineKind.bornCorrupted()`. Nanoswarms draw only from `NANOSWARM_OUTCOMES`.)
- A radius. Nothing measures distance between nodes yet. A radius is Euclidean distance on node `x`/`y`, in three sizes (small, medium, large) set in data. Which nodes fall inside each radius is computed once when the tree loads and cached per node. This keeps per-frame cost at zero. Moving nodes such as the Exoship are never in any radius. (done: `exiledsector.skills.radius`, sizes in `node_radii.json`, read through `SkillTree.radii()`)
- A radius ring on the tree, drawn when hovering a socket that holds a radius subroutine, or the subroutine itself in storage. (done: `NodeRadiusRingRenderer`, reached through `SkillTreeCanvasPlugin.setHighlightedRadius`. Hover hooks on the socket and in item storage pick the centre and size per kind through `SubroutineRadius`; Bypass still has to add its keystone centre.)
- Overrides per ship. Doctrine Archives change what a node does on one ship only. `SkillTree` is one global registry, so overrides have to be a per-ship layer applied when effects are gathered (`AllocatedSkillEffects`) and when tooltips and names are drawn, never a change to the shared tree. (done: `DoctrineOverrides`, resolved through `SkillNode.resolveEffectiveType`)
- Socket kinds. The existing `modular_hull_socket` becomes the standard socket, which holds Volume, Doctrine, Bypass and Unique subroutines, and wormhole anchors. `Subroutine.canSocketInto` and the anchor's own override check that the item fits the socket. (subroutine side done: a kind is required, and `SubroutineSocketLimits` caps Doctrine and Bypass at one per ship when socketing, on transposition, for NPC rolls, and in a load sweep.)
- Socket placement. All sockets are placed by hand once this plan is settled, using the 14 flux nodes from `49ebd8f6` as candidate spots. Three of those still carry gate tags. The core ends of the faction wormholes (section 6) free up more spots in places the tree already expects travel to start. (done in `f9a5898a`: six standard sockets in the core, `modular_hull_socket_12` and `_16` to `_20`, none gated. The wormhole core ends get no sockets (decided 2026-10-19).)
- NPCs. NPC trees already fill sockets. `NpcSkillTreeBuilder` claims sockets, and `NpcFleetLeveller.decide` fills each one with `NpcItems.pickDefinition`, which takes any non-unique subroutine through `ItemDrops.pickBasic`. That finds nothing only because `subroutines.csv` has no non-unique rows. In the first version NPCs socket none of the new kinds, so the pick must be restricted by kind before the first Volume subroutine row lands. Otherwise NPC trees would socket Volume subroutines immediately. Wormhole anchors are the exception (section 6), because NPC trees already take one notable from their faction's volume. (done: `SubroutineKind.inGenericPools` keeps MODSPEC_CACHE, DOCTRINE and BYPASS out of NPC socket picks and every generic drop picker, so each kind needs its own drop path)
- Data and editor. Every new column and file needs the editor's serialisers and `EditorRoundTripTest`, or editor saves will delete them. Player text goes through `Translation`, with en and zh_CN keys under the existing `socketable.*` prefix. `docs/docs.md` gets a section for each kind once it's built.

## 2. Contraband Modspec Cache (was Volume subroutines)

Status: done (uncommitted on `parallel-work`, 2026-10-19). The 14 per-volume subroutines from wave 3 were merged into one unique-rarity subroutine, the Contraband Modspec Cache (kind `MODSPEC_CACHE`), on the owner's request. They were never released, so there is no save migration. What is built:
- One row in `subroutines.csv` with `excluded_nodes`, `node_count_chances` (`0.9;0.09;0.01`), `drop_chance` and `salvage_chance`. The `volume` column is gone. The columns go through the editor and are pinned in both round-trip tests.
- The pool is `ModspecCaches`: every active non-optional notable in the tree, from any volume, minus `excluded_nodes` (empty for now). The item rolls 1, 2 or 3 distinct notables, frozen in `Subroutine.rolledNodeIds`.
- The grant goes through `NodeAllocator.grantSocketedNodes` and `AllocationGate.grant`. Granted nodes are free: no node slot, no OP, no banked free allocation and no item cost (`ShipSkillData.grantFreeNodeIds`, `usedNodeSlots`). Eligibility still applies.
- A granted notable joins reachability only after the path search, so it never acts as a path source. That includes waking dormant nodes.
- Removal releases every free node the cache granted, even one a path also reaches, and whatever only it held up. Only the release of paid nodes asks for confirmation. A ship destroyed in battle gets no refund.
- Drops: fleets of any faction (`FactionFleetDrops`, group `modspec_cache`), plus salvage, tech mining and chart loot.
- The item and socket tooltips show one "Allocates X" line per node, with a short bracketed problem when there is one. Console command `ExiledSectorGrantModspecCache [nodes]`.

Differences from the old Volume plan:
- No per-volume bases and no volume restriction on the roll or the socket.
- Granted nodes cost nothing, where the old plan charged a slot and OP like any node.
- A path built to a granted notable no longer takes it over: it stays free while the cache is socketed and is released with the cache.
- An item whose nodes are in an area that is off can still be socketed; those nodes read inert.

Still to do:
- Owner review of the name, the flavour text, the zh draft and the drop chances.
- Whether the cache should get the level 15 gate other uniques have.
- A real icon; the current one is a placeholder from `graphics/icons/socketables/`.
- A visual marker on the tree for a granted node.

Known leftover: a few rare unsocket paths leave a granted notable allocated with nothing holding it until the tree next refreshes, which now releases it. These are the deleted-subroutine sweep, the socket-limit, misfit and type migrations. A dormant socket keeps its grant.

## 3. Doctrine Archives (PoE: Timeless)

Status: done (wave 3, uncommitted on `parallel-work`): the per-ship override layer (`DoctrineOverrides`), `doctrine_archives.csv` with the `doctrine` modifier scope, all seven doctrines, tree and tooltip display, the hover ring, and the console command `ExiledSectorGrantDoctrineArchives`. 2026-10-19 (uncommitted on `main`): every doctrine is unique rarity and large radius, the tooltip shows "Limited to 1 doctrine archive" and "Radius: large" instead of the behaviour text, a socketed doctrine draws a constant rotating radius ring, and NPC tag restores respect the one-per-ship limit. 2026-10-20 (uncommitted on `parallel-work`): real icons, the hyperspace data tablet with an animated faction screen per doctrine (`DoctrineIconFx`, baked by `tools/DoctrineArchiveIcons.java`). Still to do: drop sources (none yet; reuse `FactionFleetDrops`), and owner review of the curated nodes, keystones, augment pools and zh drafts. Known limits: allocation still checks a node's original type, so an ineligible original can't be allocated even when its replacement could, and a transformed optional node still asks for an option. Removing a doctrine reverts nodes whose originals may strip conflicting hull mods; the socket tooltip warns but doesn't confirm.

These are recovered faction combat firmware, one per vanilla faction volume to start with. When socketed in a standard socket, the crew re-learn every node within its radius the way that faction does it. Like the PoE original:
- You still have to allocate the affected nodes to benefit.
- Only one Doctrine Archive can be socketed per ship.
- A firmware revision number (the item's `seed`, shown as "Rev. 4471") together with each node's id decides each rewrite, so two copies of the same doctrine can differ.

There are two behaviours:
- Transform: notables in range become different notables, and keystones in range become a faction keystone. Small nodes in range may also change. Replacement nodes come from a per-doctrine pool: that faction's own volume nodes, plus curated nodes written for the doctrine.
- Augment: every node in range keeps what it does and gains one or more extra modifier lines, drawn from a per-doctrine modifier pool and decided by the seed.

A suggested starting split, to be confirmed:

| Doctrine | Behaviour | What it does in range |
|---|---|---|
| Hegemony | augment | armor, hull and ballistic lines |
| Tri-Tachyon | transform | notables become corporate and AI notables |
| Luddic | transform | small nodes give nothing; notables become strong faith-tech notables |
| Sindrian Diktat | augment | Lion's Guard discipline: CR, range and energy lines |
| Pirate | augment | cheap, unstable lines: speed and damage, with a d-mod or CR drawback |
| Persean League | augment | fleet support, fighter and logistics lines |
| Remnant (REDACTED volume) | transform | notables become AI-fleet notables |

Implementation notes:
- Data: `doctrine_archives.csv` lists each doctrine's behaviour, radius size, replacement node pool and augment modifier pool. Augment pools reference `item_modifiers.csv` rows scoped to a new `doctrine` item type.
- The rewrite for each node is a pure function of (seed, node id). It's computed when the subroutine is socketed and cached on the ship's override layer, never per frame.
- The tree shows rewritten nodes with their new name, icon tint and tooltip, and their original in the tooltip ("Originally: Heavy Armor").
- If a transformed notable is allocated and the subroutine is removed, the node reverts to its original and stays allocated. A small node that gave nothing gives its normal bonus again.
- Transform pools must respect hull requirements and exclusivity. If a replacement is ineligible on a ship, the rewrite skips to the next candidate in seed order.

## 4. Bypass subroutine (PoE: Impossible Escape)

Status: left out (owner decision 2026-10-19). Moved to its own plan, `bypass_subroutine.md`. It holds the design, the decided removal rule, and every piece of code already built for it.

## 5. Unique subroutines

Status: the framework and a first draft of five uniques are done.
- Data: `lines` and `source` columns in `subroutines.csv`, and a `unique_subroutine` modifier scope.
- Effects apply only in an allocated socket.
- Every currency refuses a unique, including Tuning Nanoswarms (decided: no rerolls).
- Drops reuse the faction flagship and salvage-site unique paths.
- The five drafts await review: Halcyon Fire-Control Lattice, Cracked Drive Governor, KR-9 Lamplighter Modspec, Domain Autorepair Kernel, Warden Threat Matrix.
- A per-definition `socket_limit` column: each current unique is limited to 1 per ship, and blank or 0 means unlimited.
- Load warnings for sources that can never drop (`UniqueSourceCheck`).
- Still missing: named-fleet and commission sources, and showing the source in the tooltip.

These are hand-made subroutines with fixed names, art and flavour text. They're the one place a subroutine may carry plain SkillEffects, the same kind of lines hull items roll, which would otherwise break the identity split. To keep that from blurring the split again:
- every unique is curated, never rolled from a pool;
- each has a clear theme;
- each carries a few small effects, often with a drawback.

Any of them may also include a tree mechanic from the kinds above (a small radius, a granted node).

- Data: rows in `subroutines.csv` with `kind = UNIQUE` and `unique = true`, listing their lines directly or as `@id` references to modifier rows scoped to a new `unique_subroutine` item type. Values can be fixed or a small range frozen at drop.
- They're not corrupted by default, but they don't roll explicits, so most currency has nothing to act on. Decide whether Tuning Nanoswarms can reroll their values.
- Drops: specific sources per unique (a named fleet, a derelict, a faction commission), so each one has a story. The faction learning vectors note is a good guide for choosing them.
- Start with 4 to 6 uniques, written alongside the Volume subroutines.

## 6. Wormhole anchors

Today every faction volume is reached through a fixed wormhole pair in the tree. Any ship can walk to the core end, allocate it, get the far end free, and ctrl-click to jump across. That changes. The faction wormholes stop being inherent to the tree. Instead, each volume has a lootable wormhole anchor. Socketed into a tree socket, the anchor opens a wormhole from that socket to its volume. From then on it behaves like today's wormholes: ctrl-click the socket to travel to the far end, then allocate nodes outward from there.

Theme: a recovered navigation lock for one faction's tech corridor. The crew can't reach the techniques on the other side until they've got a fix on where they are.

Status: done (wave 3, uncommitted on `parallel-work`). The design differs from the text below in four places. A socket and exit stay linked while an inert anchor merely can't open, so a false reading never strands a volume. Releases happen only for nodes the player's action strands. The close confirmation is arm-and-repeat, not a dialog. `SocketCustody.reconcile` releases nothing, because of Nexerelin's task groups. Still to do: owner review of the drop rates, exit and Exoship descriptions and zh drafts; unit tests for `HyperNavBeaconGrant` and the confirmation click flow; an in-game check of the UI flows. Faction icons are done too (uncommitted on `parallel-work`): a baked recolour per volume, plus the `AnchorIconFx` frame, crest badge and wormhole animation in cargo, for every anchor but the Hyper-Nav Beacon, which keeps its pulsing buoy (docs.md "Anchor icons"). Still to do for them: an in-game look at every faction, and a simplified glyph for crests that read poorly at badge size (Nightcross, Stargazer, Persean League).

### Which wormholes become anchors

The tree has four kinds of wormhole link today:

| Link | Pairs | Under anchors |
|---|---|---|
| Core to a faction volume | hegemony, tritachyon, luddic, pirate, persean_league, sindrian_dictat, REDACTED | replaced by anchors |
| Faction volume to faction volume | kesteven (via persean_league), enigma (via REDACTED), nightcross (via sindrian_dictat), stargazer (via REDACTED), abyss (via hegemony) | replaced by anchors |
| Core to core shortcuts | `hightech_wormhole_1`/`midline_wormhole_1`, `hightech_wormhole_2`/`lowtech_wormhole_2`, `lowtech_wormhole_1`/`wormhole_6` | stay as they are |
| Exoship and ExoTech beacon | `exoship`/`exotech_beacon` | replaced by an anchor (see "ExoTech and the Exoship" below) |

Each anchor leads straight from its socket to its own volume. The faction-to-faction legs go away, so a Kesteven anchor doesn't need Persean League access first. That makes all thirteen volumes equal: one anchor, one socket, one volume.

The core-to-core shortcuts stay inherent because they're travel aids inside the core, not access to new content.

### ExoTech and the Exoship

The ExoTech volume moves to an anchor like every other volume. The Exoship itself stays: it's the volume's exit, and the voyage, escorts and boarding view keep working as they do now.

- The ExoTech anchor is the Hyper-Nav Beacon. Today the beacon is a fixed core node that guides the Exoship's warp drive. As an item it's a portable beacon, and it fits the anchor role without new flavour. The `exotech_beacon` node is removed from the tree, and its spot at (-947, -1635) is a socket candidate like the other core ends.
- The `exoship` node becomes the ExoTech exit. It drops `pairedWith`, moves to the exit tier, and stays carried, so its branches still travel with the ship.
- Ctrl-clicking the socket that holds the beacon boards the Exoship. `SkillTreeCanvasPlugin` already boards when the jump target is the Exoship, so once `wormholeJumpTarget` uses the per-ship partner, this needs no change. Clicking the Exoship wherever it is still boards it. Ctrl-clicking the Exoship node returns to the socket instead of the old beacon.
- The `ratPlayerExoship` unlock condition moves from the two node types to the anchor. The anchor stays inert until RAT reports the Exoship recovered or owned. The tooltip names the condition, the same as gated exits elsewhere.
- Beacon-specific drawing goes away or moves to the socket. `SkillTreeNodeDrawer` and `SkillTreeNodeConnectorRenderer` special-case `ExoshipNodes.isBeacon`. A socket holding the beacon anchor can reuse that look, or drop it. The ghost-flight and connector rules for carried nodes (`ExoshipNodes.isCarried`) stay as they are.
- The Hyper-Nav Beacon anchor comes from RAT's ExoTech content rather than fleet drops, since no NPC faction maps to the exotech volume. One source is a guaranteed beacon when the player first recovers the Exoship. A second is a rare drop from ExoTech encounters, for extra ships.
- `node_replacements.csv` maps `exotech_wormhole_2` to `exotech_beacon` and `exotech_wormhole_1` to `exoship`. The first row now points at a removed node, so the save migration must treat it the same as `exotech_beacon` itself. The second row stays.
- The `exoship` and `exotech_beacon` type descriptions in `skill_types.json` and both catalogues need rewriting. They describe the beacon as a node to allocate.

### Tree changes

- The entry end of each replaced pair is removed. The core-side ends (`hegemony_1`, `tritach_1` and so on) are good candidate spots for standard sockets, since the tree's paths already lead there. The faction-side entries of the faction-to-faction legs (`kesteven_wormhole2`, `lost_sector_wormhole2` and so on) go away completely.
- The far end of each replaced pair stays as the volume's exit node, for example `hegemony_2`. It loses its `pairedWith`. It needs a way to say "I'm an anchor exit". A new `wormhole_exit` tier, or a flag on the wormhole tier, both work. `WormholePairValidator` must accept exits without a partner, and must check that every anchor definition names an exit that exists.
- An exit node is only ever allocated through an anchor, never by a path reaching it from inside its volume.
- The ship tree JSON already has a layout orbit anchor with the id `wormhole_anchor` (the 5200-radius ring at the centre). It's unrelated. Consider renaming it, for example to `core_ring_anchor`, before anchor code lands, so searches for "wormhole_anchor" only find the item.

### How a socketed anchor behaves

- The anchor goes in a standard socket, the same sockets that hold subroutines. Sockets are scarce, so every anchor competes with a subroutine for its slot.
- The wormhole is open only while the anchor is socketed and its socket node is allocated. The socket then acts as the entry end and the volume's exit node as the far end.
- As today, the far end comes free. Opening the wormhole allocates the exit node at no OP cost, but it still takes a node slot under the cap. If the ship has no free slot, the anchor can't open and its tooltip says so.
- Ctrl-clicking the socket jumps the view to the exit node, and ctrl-clicking the exit node jumps back. This is the same `wormholeJumpTarget` behaviour as today, with the partner looked up per ship.
- The hyperspace overlay draws a route for each open anchor wormhole, from socket to exit, alongside the fixed core shortcuts.
- One anchor per volume per ship. A second anchor for the same volume is refused when socketing. Different volumes stack freely, limited only by sockets.
- An anchor can sit in any standard socket, wherever it is on the tree. The exit is what matters.
- Gate tags carry over. Nightcross, Stargazer and Abyss exits keep their `gate_*` tag. An anchor whose exit is gated stays inert until the gate is met, and its tooltip names the condition. Anchors for RAT volumes only exist while RAT's areas are active, like the Volume subroutines.

### Closing the wormhole

Unallocating the anchor's socket, or removing the anchor from it, closes the wormhole. Every allocated node that depended on it is unallocated in the same step:

- "Depended on it" means no longer reachable from the root once this anchor's link is gone. That's the existing `reachableAllocatedNodeIds` search, run without the anchor's partner. Nodes still reachable another way stay allocated. Examples are another anchor's path or the Bypass subroutine's radius. A notable granted by a Volume subroutine doesn't depend on the anchor at all, since it never needed a path.
- Released nodes are refunded the way an ordinary unallocation is: OP back, item charges returned through `deallocateReleasingCharges`, and free allocations back in the bank.
- This is a release, not a refusal. Unlike the `STRANDS_NODES` rule for ordinary nodes, closing a wormhole is always allowed. The UI must warn first, though. A confirmation names how many nodes will be released and the OP they return, because one click can undo a whole volume's worth of allocation.
- The same release happens when the anchor is lost without the player choosing it. That includes the socket losing custody of the item in battle (`SocketLossListener`), and the anchor's definition disappearing on load, for example RAT being removed.
- Planned respecs (`RespecPlan`) treat the socket and its exit as a pair, the way they treat wormhole pairs today.

### Engine seam

Status: done. Anchor links are cached in `ShipSkillData` per data revision plus `WormholeAnchorLinks`' lookup revision, and the live data, drawn snapshot and planning copy all read the same socketed anchors.

The only change to topology is the wormhole partner, which now depends on the ship. A per-ship view is needed only for partners, not for the whole tree, so this is far smaller than a per-ship tree.

- Add a single per-ship lookup, `ShipSkillData.wormholePartner(nodeId)`. It returns the static `pairedWith` for the core shortcuts, and the socket-to-exit or exit-to-socket link for each socketed anchor. It's cached per data revision. Socketing or removing an anchor bumps the revision.
- Every caller of `getPairedNodeId` switches to it: `ShipSkillData.allocate`, `deallocate`, `allocatedWormholePartner` and the free-pair logic, `AllocationGate.placement` and `isConnected`, `RespecPlan`, `TreeAllocationSession.wormholeJumpTarget`, the node snapshot's hidden checks and `HyperspaceMode`'s routes. `SkillNode.getPairedNodeId` stays as the static data, but nothing outside topology reads it for allocation any more.
- The Bypass subroutine's reachability seeds (section 4) go through the same search, so the two features compose without special cases.
- The seam (`WormholePartners`, `ShipSkillData.wormholePartner` and `wormholePairs`) exists since wave 1 and returns the static pairs, with TODOs where anchor links go. Its callers read different copies of the ship's data. The node drawer's deallocation feedback and `wormholeJumpTarget` read the live data, while the ghost flights read the drawn snapshot and `TemplatePlanner` its planning copy. Once anchor links are added, all of them must see the same socketed anchors, or a planned socket change will draw and jump differently from what the plan shows.

### Item and data

Status: done.
- `ItemType.WORMHOLE_ANCHOR`, `WormholeAnchor`, `wormhole_anchors.csv` with 13 rows, integrity checks, the editor panel and the `ExiledSectorGrantWormholeAnchors` console command.
- Area gating through the active tree, and the `rat_player_exoship` unlock, which fails closed. The unlock now sits on the anchor only, not on the `exoship` type.
- Drops (done): `FactionFleetDrops`, keyed by definition id, with `drop_factions` and `drop_chance` columns. ExoTech also gets a guaranteed first beacon (`HyperNavBeaconGrant`). Rates await owner review.
- The one-per-volume socket rule (`WormholeAnchorSocketLimits`), the wormhole links, the one-off save migration (`WormholeAnchorMigration`) and NPC implicit anchors are done.

- A new `ItemType.WORMHOLE_ANCHOR` and a `WormholeAnchor` item class next to `Subroutine`, with its own XStream alias. It's a socketable, not a subroutine, so it never takes subroutine currency or modifiers.
- No rolls in the first version. An anchor is defined entirely by its volume. It isn't corrupted, and currency has nothing to act on.
- Data: `wormhole_anchors.csv` with `id`, `volume`, `exit_node`, `icon`, `requires` (for example RAT) and drop weights. It goes through the editor's serialisers and `EditorRoundTripTest` like every other new file, and `ItemDataIntegrity` checks that every `exit_node` is an exit.
- Drops: from fleets of the factions mapped to the volume in `npc_faction_volumes.csv`. Kesteven, Enigma, Nightcross, Stargazer and Abyss have no vanilla fleet faction, so they need their own sources. ExoTech's source is covered above. Use derelicts, named fleets or the RAT encounters that gate them today, and pick the source per volume with the faction learning vectors note. Anchors should be uncommon, not rare. Without one, a player can't reach a volume at all.
- Player text uses `socketable.anchor.*` keys in en and zh_CN.

### NPCs

NPC trees currently cross the one wormhole joining the core to their faction's volume, take its far end free, and spend toward one notable or keystone there. Keep that behaviour by giving each NPC with a faction volume an implicit anchor for it. `NpcSkillTreeBuilder` treats the nearest standard socket it can afford as holding that anchor. That costs the socket's path and slot the way a player would pay, and the exit then comes free as before. No item is created, and nothing is dropped from the ship. Factions without a volume get no anchor. `wormholeAllowed` becomes "the socket holds my faction's implicit anchor".

### Old saves

Ships in existing saves may have nodes allocated in faction volumes through wormholes that no longer exist. On load:

- Any allocated entry node that was removed from the tree is unallocated and refunded.
- The volume's nodes are released by the closing rule above, since nothing anchors them yet, and refunded. No anchor is given: the player has to find one, like any other anchor (decided 2026-10-19).
- NPC trees are regenerated from their seed and need nothing.

## Order of work

1. Delete the old subroutines and clean up loading (section 1). Add the `kind` column and `SubroutineKind`. (done)
2. Place the standard sockets by hand, using the core ends of the faction wormholes as candidate spots. (done)
3. Wormhole anchors (section 6): the per-ship partner lookup first (done), then the item and data (done), tree changes, save migration and NPC implicit anchors together, since the tree changes cut off every volume until anchors exist. (done in wave 3, uncommitted on `parallel-work`; owner review of drops and text remains)
4. Volume subroutines. They need born corruption and the frozen rolled node, but no radius. (done in wave 3; merged into the Contraband Modspec Cache on 2026-10-19, uncommitted on `parallel-work`; owner content review remains)
5. Unique subroutines, a first handful. (done, drafts awaiting review)
6. Radius groundwork (done), then the Bypass subroutine. It reuses the reachability hook, so it's the cheapest radius kind. (Bypass left out 2026-10-19 by owner decision; only the radius groundwork stands)
7. The per-ship override layer, then Doctrine Archives: augment doctrines first, transform doctrines second. (done in wave 3, uncommitted on `parallel-work`; drops and owner content review remain)

## Open decisions

- Volume subroutines: roll from all of a volume's notables, or only from notables the player has unlocked (by blueprint or progression)? Decided 2026-10-19: all of the volume's notables, with a data-driven way to exclude specific nodes from the pool (empty to start).
- Does a volume subroutine have to sit in a socket inside its own volume, or can it go in any socket? Decided 2026-10-19: any standard socket.
- Is the Doctrine Archive split above (which factions transform and which augment) right? Decided 2026-10-19: keep the split.
- When a Bypass subroutine is removed, should removal be refused while nodes depend on it, or should it release those nodes? Decided 2026-10-19: release them, with the same confirmation and refunds as closing an anchor wormhole, for player removal and battle loss alike. (Bypass is now parked in `bypass_subroutine.md`.)
- Can Tuning Nanoswarms reroll the values on unique subroutines? Decided: no.
- Should NPCs ever socket the new kinds, for example elite or named fleets with a Doctrine Archive of their own faction? Decided 2026-10-19: NPC ships get their faction volume's implicit anchor (section 6) so they can reach its nodes. Volume, Doctrine and Bypass subroutines are player only for now, so NPC socket picks exclude them.
- Wormhole anchors: should they share the standard sockets with subroutines, as planned above, or get dedicated anchor sockets? Decided 2026-10-19: shared standard sockets.
- Should the removed core-side wormhole entries become standard sockets? Decided 2026-10-19: no. The entries are removed and the six existing standard sockets stay the only ones.
- Should anchors ever roll anything, for example a discount on the far side's OP or an extra free node?
- Old saves: is putting a matching anchor in storage enough, or should the migration re-socket it and keep the nodes when there's a free allocated socket? Decided 2026-10-19: neither. The migration releases and refunds the volume nodes and gives no anchor; anchors must be found. This replaces the storage step in "Old saves" above.
