# Subroutines rework: plan

Subroutines currently have no identity of their own. A tree socket accepts only a subroutine, but where the socket sits doesn't matter: the subroutine just adds flat stats to the whole ship. And 74 of the 86 effects subroutines can roll also exist on hull items. This rework makes every subroutine about the skill tree. Hull items keep the plain stat rolls; subroutines grant, rewrite, extend or bypass parts of the tree.

The first step is already done. Commit `49ebd8f6` turned all 14 tree sockets into `small_flux_optional` nodes, so sockets can be placed deliberately once the designs below are settled. The `modular_hull_socket` skill type is still in place.

The names below are working names. Where a design comes from a Path of Exile item, the PoE name is given so it's clear what's being emulated. Player text uses our own names.

## The six pieces

1. Delete the existing subroutines entirely.
2. Volume subroutines: one per tree volume, corrupted from the start, each granting a random notable from its volume.
3. Doctrine Archives (PoE: Timeless): one per vanilla faction. Each either transforms the nodes in its radius or adds modifiers to them.
4. Expansion subroutines (PoE: Cluster): add a new wheel to the tree, through a new kind of socket.
5. Bypass subroutine (PoE: Impossible Escape): corrupted, rolls a random core keystone, and lets nodes near that keystone be allocated without a path.
6. Unique subroutines: hand-made subroutines that cross the item identity line and carry small, themed sets of SkillEffects.

## 1. Delete the existing subroutines

All four definitions in `subroutines.csv` are removed: `domain_subroutine_military`, `_industrial`, `_consumer` and `_management`. So are every `item_modifiers.csv` row scoped only to `subroutine` (34 prefixes and 48 suffixes). The 8 rows scoped `subroutine;hull_item` lose their `subroutine` scope. Subroutines stop rolling explicit prefixes and suffixes altogether.

Old saves lose their subroutines with no compensation. Loading must not crash, though:
- The `Subroutine` class and its XStream alias `exiledSector.Subroutine` stay, because the new kinds reuse them. Renaming a persisted class breaks saves; adding fields is safe.
- On load, a subroutine whose definition no longer exists is deleted from item storage and from any socket, silently. It doesn't linger as an `UnresolvedItem`.
- The subroutine icons in `graphics/icons/socketables/` can go once nothing references them.
- `ItemDataIntegrity`, the editor's Subroutines page and the round-trip tests need updating to match.

Hull items are unaffected.

## Shared groundwork

Every new kind is still a `Subroutine` (`ItemType.SUBROUTINE`). A new `kind` column in `subroutines.csv` picks its behaviour: `VOLUME`, `DOCTRINE`, `EXPANSION`, `BYPASS` or `UNIQUE`. It maps to a `SubroutineKind` enum that is read from the definition, never persisted on the item.

Most kinds need the following:

- A frozen rolled node. Volume and Bypass subroutines each roll one tree node when they're created: a notable or a keystone. That node id is stored on the item and never rerolled, in line with the socketable-freezing rule. Doctrine Archives use the item's existing `seed` instead.
- Corruption from the start. Volume and Bypass subroutines are created already corrupted, which locks them from all currency. The current `CorruptionOutcome` values describe what a Corrupted Nanoswarm did to an item. Born-corrupted items need their own value (for example `INNATE`), so the tooltip can say "Corrupted" without claiming something was changed.
- A radius. Nothing measures distance between nodes yet. A radius is Euclidean distance on node `x`/`y`, in three sizes (small, medium, large) set in data. Which nodes fall inside each radius is computed once when the tree loads and cached per node. This keeps per-frame cost at zero. Moving nodes such as the Exoship are never in any radius.
- A radius ring on the tree, drawn when hovering a socket that holds a radius subroutine, or the subroutine itself in storage.
- Overrides per ship. Doctrine Archives change what a node does on one ship only. `SkillTree` is one global registry, so overrides have to be a per-ship layer applied when effects are gathered (`AllocatedSkillEffects`) and when tooltips and names are drawn, never a change to the shared tree.
- Socket kinds. The existing `modular_hull_socket` becomes the standard socket, which holds Volume, Doctrine, Bypass and Unique subroutines. Expansion subroutines need a second socket type (section 4). `Subroutine.canSocketInto` checks that the kind and socket type match.
- Socket placement. All sockets are placed by hand once this plan is settled, using the 14 flux nodes from `49ebd8f6` as candidate spots. Three of those still carry gate tags.
- NPCs. They never fill sockets today. That stays true for the first version.
- Data and editor. Every new column and file needs the editor's serialisers and `EditorRoundTripTest`, or editor saves will delete them. Player text goes through `Translation`, with en and zh_CN keys under the existing `socketable.*` prefix. `docs/docs.md` gets a section for each kind once it's built.

## 2. Volume subroutines

There is one base per tree volume. Each is corrupted when created and rolls a random notable from its volume. While socketed in an allocated standard socket, it allocates that notable on the ship without a path to it. Theme: a pirated modspec that cracks the DRM on one faction's technique, so the crew can install it without the path leading there.

Volumes and their current notable counts:

| Volume | Notables | Notes |
|---|---|---|
| core | 45 | large pool, so the most varied rolls |
| hegemony, tritachyon, pirate, REDACTED, kesteven | 4 each | |
| luddic, persean_league, enigma, nightcross, exotech, abyss | 3 each | |
| stargazer | 4 | |
| sindrian_dictat | 2 | |

The nightcross and stargazer bases exist only when RAT's areas are active, since those volumes need RAT.

The rules:
- The granted notable needs an unoccupied node slot under the allocated-node cap, and it costs OP as normal. Free allocations bank as they do now. Removing the subroutine releases the notable and refunds its cost.
- If the ship already has the notable allocated by path, the subroutine does nothing and its tooltip says so. Bonuses never stack.
- Exclusivity and hull requirements still apply through `NodeEligibility`. If the notable is ineligible on this ship, the subroutine stays inactive and its tooltip names the reason.
- The granted notable doesn't count as connected for pathing. Nodes beyond it still need a real path. Otherwise this would quietly become a Bypass subroutine.
- Drops: a volume's base drops from fleets of the factions mapped to it in `npc_faction_volumes.csv`. Core and non-faction volumes drop from general salvage. All of them are rare.

## 3. Doctrine Archives (PoE: Timeless)

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

## 4. Expansion subroutines (PoE: Cluster)

These add a whole new wheel to the tree: a notable plus its themed lead-in small nodes. They need a new socket type, the expansion socket, placed only on the tree's outer edge where there's room to draw a wheel.

- Each base rolls a theme (armor, shields, missiles, fighters and so on), a wheel size (small, medium or large), and from the theme's pool the notables in the wheel. Small nodes are the theme's standard small nodes.
- The added nodes exist only for that ship. Their ids are derived from the item id and position in the wheel (for example `exp:<itemId>:3`), so they survive saving and are unique even with several Expansion subroutines on one ship.
- The wheel connects only through its expansion socket. Allocating its nodes uses the normal cap and OP rules.
- Large wheels can contain one further expansion socket at their far end, like PoE's nested clusters. Leave this out of the first version.
- Removing the subroutine releases every node in its wheel and refunds them. The removal is refused (`STRANDS_NODES`) if anything allocated beyond the wheel depends on it, which can only happen with nested sockets.
- Layout is the hardest part. Wheel nodes are placed on a fixed arc pattern pointing outward from the socket. Each expansion socket's spot is chosen by hand so the largest wheel never overlaps existing nodes.

This needs the biggest engine change. Topology, `AllocationGate`, `TreeSearch`, effect gathering, the node drawer and the NPC tree builder all assume the global tree. They need to see a per-ship "tree plus extensions" view instead. Build it after the per-ship override layer from the Doctrine Archives work, since both need the same seam.

## 5. Bypass subroutine (PoE: Impossible Escape)

This is a single base, corrupted when created. It rolls one random keystone from the core volume (13 today, from `safety_overrides` to `operations_center`). Its radius is centred on that keystone, not on the socket it sits in. While socketed in an allocated standard socket, any node within the radius can be allocated without a connecting path. That includes the keystone.

- Reachability already has an alternative-path hook: `ShipSkillData.reachableAllocatedNodeIds` seeds the search from wormhole partners and from nodes with no connections. Nodes inside an active bypass radius become extra seeds in the same way. `AllocationGate.isConnected` accepts them too.
- Paths can grow outward from a node allocated in the radius as normal.
- Removing the subroutine strands the nodes that relied on it. As with any removed node, it's either refused (`STRANDS_NODES`) or, if the item was lost in battle, the stranded nodes are released and refunded. Pick one rule for both cases.
- The rolled keystone still costs a node slot and OP, and it still obeys exclusivity. The subroutine grants access, not the keystone itself.
- Only one per ship.

## 6. Unique subroutines

These are hand-made subroutines with fixed names, art and flavour text. They're the one place a subroutine may carry plain SkillEffects, the same kind of lines hull items roll, which would otherwise break the identity split. To keep that from blurring the split again:
- every unique is curated, never rolled from a pool;
- each has a clear theme;
- each carries a few small effects, often with a drawback.

Any of them may also include a tree mechanic from the kinds above (a small radius, a granted node).

- Data: rows in `subroutines.csv` with `kind = UNIQUE` and `unique = true`, listing their lines directly or as `@id` references to modifier rows scoped to a new `unique_subroutine` item type. Values can be fixed or a small range frozen at drop.
- They're not corrupted by default, but they don't roll explicits, so most currency has nothing to act on. Decide whether Tuning Nanoswarms can reroll their values.
- Drops: specific sources per unique (a named fleet, a derelict, a faction commission), so each one has a story. The faction learning vectors note is a good guide for choosing them.
- Start with 4 to 6 uniques, written alongside the Volume subroutines.

## Order of work

1. Delete the old subroutines and clean up loading (section 1). Add the `kind` column and `SubroutineKind`.
2. Place the standard sockets by hand.
3. Volume subroutines. They need born corruption and the frozen rolled node, but no radius.
4. Unique subroutines, a first handful.
5. Radius groundwork, then the Bypass subroutine. It reuses the reachability hook, so it's the cheapest radius kind.
6. The per-ship override layer, then Doctrine Archives: augment doctrines first, transform doctrines second.
7. Expansion sockets and Expansion subroutines, on top of the same per-ship layer.

## Open decisions

- Volume subroutines: roll from all of a volume's notables, or only from notables the player has unlocked (by blueprint or progression)? Rolling from all is simpler and keeps NPC drops uniform.
- Does a volume subroutine have to sit in a socket inside its own volume, or can it go in any socket?
- Is the Doctrine Archive split above (which factions transform and which augment) right?
- When a Bypass subroutine is removed, should removal be refused while nodes depend on it, or should it release those nodes?
- Can Tuning Nanoswarms reroll the values on unique subroutines?
- Should NPCs ever socket the new kinds, for example elite or named fleets with a Doctrine Archive of their own faction?
