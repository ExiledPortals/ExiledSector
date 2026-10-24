# Granted node marker: plan

A Contraband Modspec Cache in an allocated socket allocates one to three notables with no path and no cost. On the tree a granted notable looks like any other allocated notable. Only its tooltip says "Allocated by ...", so a player can't see at a glance which notables the cache holds, or why a notable sits alone with no path to it.

This plan gives granted nodes a marker on the tree and ties them visibly to their socket.

Status: steps 1 to 4 built, uncommitted on the granted marker worktree. Not yet checked in game.

## Where the code lives

Paths are relative to `exiledsector-skill-tree/src/main/java/exiledsector/`.

- Granted nodes: `skills/ShipTrees.grantedNodesBySocket(data)` maps each socket to the nodes its item grants. It's cached per tree revision (`refreshGrantLinks`). `ShipTrees.grantingSocket` finds the socket for one node, and `isGrantHeld` says whether an allocated node is held by a grant.
- Grant state: `NodeAllocator.Snapshot.nodeGrantStatus(socketId, nodeId)` returns a `items/NodeGrantStatus` (`ACTIVE`, `READY`, `BY_PATH`, `DUPLICATE`, `INELIGIBLE`, `AREA_OFF`, `UNKNOWN_NODE`). Only `ACTIVE` means the cache is what holds the node.
- Tooltip line: `ui/node/SkillTreeNodeTooltipRenderer.withGrantLine` (`socketable.cache.grantedBy`).
- Drawing: `ui/node/SkillTreeNodeDrawer.renderNode` draws each node, and `ui/node/SkillTreeNodeRingRenderer.draw` draws its ring. The ring already takes a colour for doctrine-rewritten notables (`notableDoctrineColor`), and sockets holding a doctrine get a coloured belt (`drawDoctrineSocketBelt`). The marker should look related to those but stay distinct.

Only the Modspec Cache grants nodes today. The marker keys off the grant links, not the item kind, so any later granting item gets it too.

## The marker

Two parts:

1. A ring treatment on every node a grant holds (`ACTIVE`): a second thin ring just outside the normal notable ring, drawn in the granting item's frame colour (`Item.frameColor()`). It shows only while the node is allocated through the grant. A node that's also reached by path (`BY_PATH`) gets no marker, because the cache isn't what holds it.
2. A tether while hovering: hovering a socket that holds a granting item, or a granted node, draws a faint line from the socket to each node it grants, in the same colour. Granted nodes are often far from the socket, and the tether answers "where did this come from" without opening storage.

The socket itself gets no new marker: its tooltip already lists what the item allocates.

If a granted notable is also doctrine-rewritten, the doctrine colour stays on the main ring and the grant ring sits outside it, so both read.

### Plain-language rendering note

The tree is drawn with OpenGL. Each frame, every visible node is drawn as a few layers: a ring, the icon, and any extras such as the doctrine belt. The grant ring is one more circle outline in the same drawing pass, using the same blending setup the ring already turns on (`GL_BLEND` with normal alpha blending). The tether is a line drawn before the nodes, so the nodes cover its ends. Nothing new is loaded and no new textures are needed.

Technical details: the grant ring reuses the ring renderer's circle-outline helper at a slightly larger radius. The tether uses `GL_LINES` (or the existing connection-line helper, if it accepts a colour) with alpha around 0.35, drawn in the connection-line pass before the node pass. Everything stays in GL11 and on the render thread, with no `glGet` calls (Fast Rendering compatibility).

## Per-frame cost

`grantingSocket` loops over every grant link for one node, which is too slow to call for every visible node each frame. Instead, `renderNode`'s caller builds a `Map<String, Color>` (granted node id to colour) once per tree revision from `grantedNodesBySocket` and the snapshot's grant statuses. Each node then needs one map lookup. The hover tether reads the same map.

## Steps

1. (done, uncommitted on the granted marker worktree) A cached granted-node colour map, rebuilt when the tree revision or the socketed items change. Built as `ui/node/GrantMarkers`: `refresh(snapshot)` rebuilds only when the ship data, its revision or the grant lookup (or its revision) changes, and also keeps each socket's active granted nodes and colour for the tether. Tested in `GrantMarkersTest`.
2. (done, uncommitted on the granted marker worktree) The grant ring in `SkillTreeNodeRingRenderer`, called from `renderNode` for `ACTIVE` grants. `draw` takes a `grantColor` and draws one thin outline just outside the node's halo, inside the same blended pass as the donut.
3. (done, uncommitted on the granted marker worktree) The hover tether, for the hovered socket or granted node. `SkillTreeNodeConnectorRenderer.drawGrantTethers` draws it just before the node pass, with its ends cut at the node edges like connectors. The hover check only tests the sockets and nodes in the grant map, and is skipped while a dropdown is open or the root is moving.
4. (done, uncommitted on the granted marker worktree) No `docs/docs.md` change (it doesn't cover UI). The README doesn't describe the cache's look, so it's unchanged.

## Decided (provisional, recommended options)

These were built with the recommended option for each. The owner hasn't confirmed them yet, so any can still change.

- The colour: the granting item's frame colour (`Item.frameColor()`), not one fixed "granted" colour.
- The tether: on hover only, not always shown while the tree is open.
- The marker: only on `ACTIVE` grants, not on `READY` ones that aren't allocated yet.

## Tests

- The granted-node map holds exactly the `ACTIVE` grants, takes each colour from its item, and drops a node that's also reached by path.
- It's rebuilt after a socket change and reused when nothing changed (same instance between two frames with no revision change).
- Rendering itself isn't unit tested; check it in game.
