# Hull Frameworks - implementation plan

Companion to `hull_frameworks_brief.md`. No code has been written yet.

## Decisions (asked 2026-10-07)

| Question | Answer |
|---|---|
| Rarities | Common rolls 2 sockets. Rare rolls 3 or 4. Unique frameworks come later |
| Socket-type rolls | Equal weights, no duplicate types on one framework. Every type can roll on every hull size |
| Frameworks per ship | One. Installing another replaces it |
| Hull restrictions | Current fit, not base hull: Shield Shunt, Makeshift Shield Generator, Converted Hangar and tree nodes all count. Only Shield Generator, Phase Coil and Flight Deck are restricted; Weapon Mount is universal |
| Framework socket contents | Rolled basics plus uniques. Every socket type gets non-unique definitions like the domain subroutines |
| Converted Hangar Deck Crew | Flight Deck |
| Ko Combine Shipbreakers | Crew Quarters (missing from the brief's table) |
| Galatia Hyperspace Physics Group | Engine Room (missing from the brief's table) |
| Subroutine uniques that move | Gate Hauler Drive Coil to Engine Room, Synchrotron Cell to Reactor, Nanoforge Seed to Weapon Mount, Fullerene Spool Integration and Vambrace Plating to Armor Plating (a socket type added for them), Planetkiller Resonance Circuit to Weapon Mount. No subroutine uniques remain |
| AI cores | Delete the AI core kind. A socketable's kind becomes its socket type; each converted unique's kind changes to the socket type it belongs to |
| Transposition | Crosses socket types. If the result no longer fits the socket it was in, it returns to storage |
| Framework sources | Salvage sites, Tech Mining, battle drops. No crafting |
| Drop gate | Frameworks and framework socketables only drop from player level 15. They then roll in parallel with subroutines at the same chance, so subroutine drop rates don't change |
| Removal | Free swap: removing returns the framework and its socketed items to storage. A destroyed ship loses both, the same as tree sockets today |
| NPC frameworks | From player level 15, each NPC fleet's flagship has a 50% chance of a framework: Common 70%, Rare with 3 sockets 20%, Rare with 4 sockets 10% |
| NPC framework sockets | Each socket rolls the existing NPC socketable chance (5% + 1% per player level) for a rolled basic of its type |
| NPC scaling | The NPC bonus multiplier scales framework items, as it scales tree socket items |

Defaults I chose without asking, each easy to change:

- Player drops use the NPC rarity split: Common 70%, Rare 3 sockets 20%, Rare 4 sockets 10%.
- Unique socketables keep their existing gate (player level 15 plus per-item unlock conditions), whatever their socket type.
- Tree-socketed items that no longer fit a Subroutine Socket after conversion are returned to storage on load, with one campaign message listing them.
- The workbench's Common synthesis keeps making subroutines only.

## What the code does today

- `SocketableKind` (SUBROUTINE, OFFICER, TEAM, AI_CORE) is an enum with a factory. Each kind has a persisted subclass of the abstract `Socketable` (`Subroutine`, `Officer`, `Team`, `AiCore`), and `kind()` comes from the class, not the definition. `SocketableItemData.create` and `SocketableCrafting.transpose` build instances through `definition.kind().create(...)`.
- All four subclasses are empty; every field lives in `Socketable`. XStream aliases (`SocketableSaveAliases`) have existed since the model was added in 92dcaab, so every save refers to them as `exiledSector.Officer` and so on, never by Java class name.
- `Socketable.canSocketInto(SkillNode)` accepts any node whose tier is `SOCKET`. There is one socket node type, `modular_hull_socket`, used 11 times in `ship_skill_tree.json`.
- `ShipSkillData.socketedItems` maps tree node id to socketable id. `release`, `forgetUnknownNodes` and node replacement unsocket items as nodes go away.
- Socketed effects reach the ship only through allocated nodes: `AllocatedSkillEffects.appliedEffects(data, allocatedNode, hullSize)` appends the socketed item's effects to the node's own. `ResolvedTree` (combat and campaign stats, one mod id per node), `EffectTotals` (summaries and the stat panel) and `NpcBonusScaling` all read that one method.
- `SocketCustody` derives installations from `socketedItems`, records ships lost in combat, and on reconcile unsockets items from ships the player no longer owns, deleting them when the ship was destroyed.
- `SocketableDrops.pickBasic` only picks non-unique subroutines. `pickUnique` picks any unique, gated by `SocketableUnlock` (player level 15 and per-item conditions).
- NPCs use the same pickers for tree sockets (`NpcSocketables`: 5% + 1% per player level for a first item, a further 5% from level 15). `NpcTreeTag` restores NPC socket entries from the variant tag. `SocketableLootListener` loots the items NPC ships carried when they're destroyed or disabled and not recovered.
- The workbench can load installed items, so crafting currency can be used on a socketed item.
- Follow mode (`CanvasMode.FLEET_FOLLOW`) zooms to `SmoothZoom.MAX_ZOOM` (2.5) on the viewed ship, hides the storage and template buttons, and closes the storage panel (`closesStorage`). At that zoom a fleet sprite is drawn at 0.07 to 0.11 scale times 2.5, so even a capital is only about 70 px on screen. Sockets can't be drawn on the sprite itself.
- The editor's kind dropdown comes from `tools/editor_vocabulary.json` (`socketableKinds`), generated by the test-side `EditorVocabulary`.

## Data model

### Socket types replace kinds

`SocketableKind` becomes `SocketType`. It is safe to rename because no save stores the enum: the kind came from the class. It has ten values:

| Id | Framework socket | Requirement tag |
|---|---|---|
| `subroutine` | no (tree only) | - |
| `bridge` | yes | - |
| `crew_quarters` | yes | - |
| `engine_room` | yes | - |
| `reactor` | yes | - |
| `armor_plating` | yes | - |
| `weapon_mount` | yes | - |
| `shield_generator` | yes | `req_shields` |
| `phase_coil` | yes | `req_phase` |
| `flight_deck` | yes | `req_fighter_bays` |

- Each value carries its id, its requirement tag (nullable), whether it is a framework socket, and a display name key. The keys `socketable.kind.<id>` become `socket.type.<id>` (en and zh).
- `socketables.csv` keeps its `kind` column, now holding socket type ids. A row with `officer`, `team` or `ai_core` is rejected with an error naming the new ids, through the existing "Skipping row" path.

### Socketable instances

- `Socketable` becomes a concrete class. `kind()` returns `definition().kind()`, or null when the definition isn't loaded (an item with no loaded definition fits no socket, as it effectively does today).
- Converting a unique is then a CSV edit. Existing instances in saves pick up their new socket type with no per-instance migration.
- `Subroutine`, `Officer`, `Team` and `AiCore` are deleted. `SocketableSaveAliases` maps all four legacy alias names to `Socketable.class`, then registers `exiledSector.Socketable` last, so new saves write the new name and old saves still load. XStream's `ClassAliasingMapper` keeps every name for reading, and the last alias registered for a class is the one written.
- A test deserialises hand-written XML using each legacy element name and checks the written name. If Starsector's bundled XStream behaves differently, the fallback is to keep the four classes as empty subclasses.
- `canSocketInto(SkillNode)` accepts `SOCKET`-tier nodes only for `SocketType.SUBROUTINE`. A new `canSocketInto(SocketType frameworkSocket)` requires an exact type match.
- AI cores are removed: `AI_CORE`, `AiCore`, the `socketable.kind.ai_core` strings and the vocabulary entry. No content ever used the kind, so no save holds an `AiCore`, but its alias stays mapped to `Socketable` anyway.

### Transposition across socket types

- `SocketableCrafting.transpose` picks any other allowed unique, regardless of socket type. It is built as a plain `Socketable` with the same instance id, replacing the old one in the store as today.
- If the transposed item is installed and its new type doesn't fit where it sits (tree socket or framework slot), the item is unsocketed and stays in storage. The workbench result line says so ("Moved to storage: it now fits Bridge sockets").
- The unsocketing goes through the same `ShipSkillData` methods the UI uses, so the revision bump refreshes stats.

### Framework item

New persisted class `HullFramework`, aliased `exiledSector.HullFramework`:

```
String id                  "framework_<n>", from the store's counter
HullSize hullSize
SocketableRarity rarity    COMMON or RARE for now; UNIQUE reserved for hand-made frameworks later
List<String> socketTypeIds 2, 3 or 4 ids, no duplicates, in rolled order
long seed
```

- Socket types are stored as id strings, so a type removed later only drops that slot on load instead of breaking deserialisation. `socketTypes()` skips unknown ids.
- The display name is generated: "Rare Cruiser Framework" (en) and its zh form. Rarity colour comes from `SocketableRarity`.
- `HullFrameworkRoller` has two entry points:
  - `rollSocketCount(Random)` gives 2 (Common, 70%), 3 (Rare, 20%) or 4 (Rare, 10%).
  - `roll(hullSize, socketCount, allowedTypes, seed)` shuffles the allowed framework types with the seed and keeps the first `socketCount`, giving equal weights with no duplicates.
  - Player drops pass every framework type. NPC flagships pass only the types their current fit allows.

As cargo it is a special item `exiledSector_hull_framework` with a `HullFrameworkItemPlugin`. Its data is `size|rarity|type,type,...|seed`, encoded and decoded by a small codec beside `SocketableCodec`. It is absorbed into storage on the same hook that calls `SocketableStore.absorbFrom`, the same as socketables.

### Storage and per-ship state

- `SocketableStore` gains a lazily created `List<HullFramework> frameworks` and its own id counter. Old saves load with the field null, so no new persistent key or store class is needed. API: `addFramework`, `removeFramework`, `findFramework`, `frameworks()`.
- `ShipSkillData` gains two fields:
  - `String installedFrameworkId`: a store id for player ships; an `npc:` encoded framework for NPC ships, like NPC socketable ids.
  - `Map<String, String> frameworkSocketedItems`: slot index ("0" to "3") to socketable id. It is separate from `socketedItems`, so none of the tree-node code (`release`, `forgetUnknownNodes`, node replacement) can touch it.
- New methods: `installFramework(id)`, `removeFramework()` (clears the slot items and returns the old framework id plus the freed item ids), `socketFrameworkItem(slot, id)`, `unsocketFrameworkItem(slot)` and getters. Every change bumps `revision`.
- Respec, `reset`, templates and starting-root choice leave the framework alone.
- An installed framework stays in the store's list, and "installed" is derived from ship data, the same as socketables. Storage UI filters installed frameworks out of the picker.

### Custody

- `SocketCustody.Installation` gets a location: tree node id or framework slot index. `installations()`, `isInstalled(Socketable)` and `shipNames` read both maps. New `isInstalled(HullFramework)` and `frameworkInstallations()`.
- `reconcile` handles framework slots like tree sockets. For a ship the player no longer owns, framework items are unsocketed and the framework uninstalled. If the ship was destroyed in combat, the framework and its items are deleted from the store; otherwise both return to storage.
- `SocketableDisassembly` refuses items in framework sockets, as it refuses tree-socketed ones.

### Effects pipeline

- A new `FrameworkSlots.of(shipData, hullSize, fit)` returns one record per framework slot: `(slotIndex, SocketType type, Socketable item, boolean active, String inactiveReason)`. A slot is active when its type's requirement passes against the current fit (see Hull restrictions). For NPC ships the framework and items resolve through `NpcSocketables`, as NPC tree items do.
- The three consumers of `AllocatedSkillEffects.appliedEffects` each add framework slots as extra effect sources, using `socketed.skillEffects(hullSize)` with the same `appliesToNpcShips` filter:
  - `ResolvedTree` gives each slot its own mod id, `exiledSector_framework_<slot>`, beside the per-node ids. The cache already keys on the ship data revision.
  - `EffectTotals` adds them to the permanent totals, so summaries, the stat panel and `DamageTakenCaps` see them.
  - `NpcBonusScaling` adds framework slot effects to its bonus totals and scales them through a slot overload of `scaled`. The existing filters (scalable stat mode, beneficial magnitude, `UNSCALED_EFFECTS`) apply; the node-tag filter doesn't, because slots have no tags.
- `AllocatedNode.of(data)` does not grow synthetic nodes, so node counts, eligibility contexts and exclusivity checks are unaffected.

## Hull restrictions

The brief asks for one rule shared with the tree. `req_shields`, `req_phase` and `req_fighter_bays` are already `ShipProfile` requirements in `NodeRequirements.isMet`, so frameworks call `NodeRequirements.firstUnmet(List.of(type.requirementTag()), profile)` directly. No new tags are needed.

Restrictions follow the current fit, so the profile must describe the ship as fitted now. `ShipProfile.of(member)` doesn't fully do that: shield type comes from the hull spec, and bays come from live stats, which would include framework items' own effects. A new factory, `ShipProfile.currentFit(member, shipData)`, builds:

| Profile field | Source |
|---|---|
| Shield type | Hull spec, then Makeshift Shield Generator gives FRONT and Shield Shunt gives NONE, then the tree's shield effects via `NodeEligibility.currentShieldType` |
| Fighter bays | Hull spec bays + Converted Hangar hull mod + the tree's `FIGHTER_BAYS_FLAT` node effects. Never `getNumFlightDecks()`, which includes framework items, so Converted Hangar Deck Crew can't qualify its own Flight Deck socket |
| Phase | `hullSpec.isPhase()` |

Framework socket effects are never inputs to the fit, which avoids circularity.

- **Installing** requires `framework.hullSize() == ship hull size` and every restricted type passing now. The UI shows the first failure, e.g. "Phase Coil needs a phase hull" or "Flight Deck needs fighter bays".
- **After a refit** that breaks a requirement, the framework stays installed. The failing slot goes inactive: its item's effects stop and the slot is drawn dim, with the reason in its tooltip. The item stays socketed until the player removes it or the requirement is met again.
- **Drops** are restricted by hull size only, as the brief says.
- **Performance:** `FrameworkSlots` is computed when the ship data revision or the variant changes, never per frame. `ResolvedTree` already rebuilds on those.

## Converting existing uniques

| Unique (id) | From | To |
|---|---|---|
| Alpha Site Survivor (`unique_alpha_site_survivor`) | officer | bridge |
| Tri-Tachyon Morale Assurance Officer (`unique_morale_assurance_officer`) | officer | bridge |
| Back-Alley Cyber-Surgeon (`unique_back_alley_surgeon`) | officer | bridge |
| Auxiliary Conscripts (`unique_auxiliary_conscripts`) | team | crew_quarters |
| Olinadu Security Detachment (`unique_olinadu_security`) | team | crew_quarters |
| Asher Salvage Guild Crew (`unique_asher_salvage_guild`) | team | crew_quarters |
| Salvager Crew (`unique_salvager_crew`) | team | crew_quarters |
| COMSEC Signals Detachment (`unique_comsec_detachment`) | team | crew_quarters |
| Pather Cell (`unique_pather_cell`) | team | crew_quarters |
| Ko Combine Shipbreakers (`unique_ko_shipbreakers`) | team | crew_quarters |
| Sindrian Fuel Company Engineers (`unique_sindrian_fuel_engineers`) | team | engine_room |
| Galatia Hyperspace Physics Group (`unique_hyperspace_physics_group`) | team | engine_room |
| Gate Hauler Drive Coil (`unique_gate_hauler_coil`) | subroutine | engine_room |
| Synchrotron Cell (`unique_synchrotron_cell`) | subroutine | reactor |
| Fullerene Spool Integration (`unique_fullerene_spool`) | subroutine | armor_plating |
| Vambrace Plating (`unique_vambrace_plating`) | subroutine | armor_plating |
| Planetkiller Resonance Circuit (`unique_planetkiller_circuit`) | subroutine | weapon_mount |
| Coatl Bastion Gunnery Team (`unique_coatl_gunnery_team`) | team | weapon_mount |
| Gryphon Missile Crew (`unique_gryphon_missile_crew`) | team | weapon_mount |
| Deserter Gunners (`unique_deserter_gunners`) | team | weapon_mount |
| Nanoforge Seed (`unique_nanoforge_seed`) | subroutine | weapon_mount |
| Converted Hangar Deck Crew (`unique_converted_hangar_crew`) | team | flight_deck |

After the move, no subroutine uniques remain. Shield Generator and Phase Coil have no uniques yet.

Save migration (`SocketTypeMigration`, run from `onGameLoad`, latched by a persistent-data key):

- **Player ships:** every tree-socketed item whose kind is no longer `subroutine` is unsocketed. It stays in `SocketableStore`, now free. One campaign message lists the returned items and says they now go into hull framework sockets.
- **NPC ships:** nothing to migrate. `AllocatedSkillEffects` already skips items that fail `canSocketInto`, so a misfit unique in an old NPC tree simply does nothing. `NpcTreeTag.restoreSockets` also skips them from now on. On capture, `claimForPlayer` still hands them to the player, and they go to storage because they no longer fit. NPC tree sockets only pick subroutines (basic and unique) from now on.
- **Socket storage:** the list keeps every item. Filtering and search use the socket type name instead of the old kind name.

## Drops (player)

Every framework drop and every framework-socketable drop needs player level 15 or more. Below that, nothing new drops and subroutine drops are untouched. From level 15 each source rolls in parallel with its subroutine roll, at the same chance:

- **Salvage:** for each subroutine chance in the site's `chances` list, a second roll at the same chance may give a framework socketable. One extra roll at the site's first chance may give a framework. No new CSV column, so other mods' salvage rows get frameworks automatically.
- **Tech Mining:** first find and monthly each get one framework-socketable roll and one framework roll, at the subroutine chances.
- **Battles:** NPC flagships' frameworks and their socketed items join the existing NPC loot in `SocketableLootListener`. They drop when the flagship is destroyed or disabled and not recovered, as carried tree-socket items do now. A recovered flagship keeps them through `claimForPlayer`.
- **Picks:**
  - **Framework socketables:** the type is picked with equal weight from the eight framework types, then a basic of that type is drawn by the `rarity` column.
  - **Uniques:** keep their existing roll and gate. That roll now draws from all uniques, so converted uniques keep dropping at the rate they had.
  - **Frameworks:** the hull size is equal across the four sizes, and the socket count uses the 70/20/10 split.
- **Code:** `SocketableDrops` gains `pickFrameworkBasic(Random)` and `rollFramework(Random)`, with the level gate in one helper (`FrameworkUnlock.isOpen(sector)`), reused by NPC spawning. `pickBasic` stays subroutine-only.

## NPC flagships

- **Where:** in `NpcFleetLeveller.ensure`, after the trees are built, when the player is level 15 or more. The flagship of each NPC fleet gets a seeded 50% roll, using the same seeding and record flow as the tree, so the result is stable per sector seed, fleet and member.
- **What:**
  - **Socket count:** 70/20/10 as above. Types come from those the flagship's current fit allows (`ShipProfile.currentFit`), so an NPC never carries an inactive slot.
  - **Sockets:** each socket independently rolls `NpcSocketables.firstChance(playerLevel)` (5% + 1% per player level) and, on success, a basic of its type. No NPC uniques in framework sockets until uniques are added for NPCs.
- **Persistence:** `NpcTreeTag` gains a framework field: the encoded framework, plus `slot:npcSocketableId` entries. `restore` rebuilds `installedFrameworkId` and `frameworkSocketedItems`. Old tags have no field and decode as before.
- **Capture:** `NpcSocketables.claimForPlayer` and `storeForPlayer` also move the framework and its items into the player's store. A claimed ship keeps them installed.
- **Inspector:** the X-key inspector, Codex and refit tooltips show the framework line for NPC ships too.
- **Scaling:** framework items are scaled by the bonus multiplier (see Effects pipeline).

## UI (follow mode)

- Follow mode stays the entry point: click the ship card to zoom in on the viewed ship. Once the camera settles (`TreeCamera` finishes `FOLLOW_SETTLE_SECONDS`), a `FrameworkPanel` fades in. It is a screen-space overlay centred on the screen, because the followed ship is held at screen centre and is too small to draw sockets on.
- **No framework:** the panel shows an "Install framework" button. It opens a picker listing owned, uninstalled frameworks of this hull size. Frameworks that don't fit the current ship are dimmed, with the reason in the tooltip. Other hull sizes are hidden, with a count line ("3 frameworks for other hull sizes").
- **Installed:**
  - **Header:** name, rarity colour and hull size, with Swap and Remove buttons. Both are free; items return to storage.
  - **Sockets:** 2 to 4 socket rings sit on an arc around the ship. Each ring uses the existing socket ring look from `SkillTreeSocketRenderer`, with a per-type colour and icon (placeholder icons until art exists).
- **Socketing:**
  - **Fill:** clicking an empty framework socket opens the socket storage panel, filtered to that type. `closesStorage` no longer closes it in `FLEET_FOLLOW` when it was opened from a framework socket.
  - **Placing:** `SocketPlacement`'s target generalises from `SkillNode` to a small sealed `SocketTarget` (`TreeSocket(node)` or `FrameworkSocket(slot)`), so place, Ctrl-click to empty and escape behave the same as on tree sockets.
- **Tooltips:** hovering a filled framework socket shows `SocketableHoverTooltip` with the ship's hull size. An inactive socket also shows why.
- **Storage panel:**
  - `SocketStorageFilter` gains a socket type filter: chips in the header, plus the preset used above.
  - Rows show the socket type name.
  - A Frameworks tab lists owned frameworks with their install location, in place of a separate storage screen.
- **Ship summary:** the summary and inspector tooltips gain a framework line ("Rare Cruiser Framework: Bridge, Reactor, Shield Generator"), and their bonus lists include framework items through `EffectTotals`.
- **Per frame:** the panel is built on entering follow mode and when the data revision changes; rendering only draws cached geometry.

## Stages

Each stage builds, passes the full suite and is reviewable on its own. Stages 1 to 6 should ship in one release: after stage 1 the converted uniques have no socket until stage 5 lands.

1. **Socket types and the Socketable model.**
   - **Model:** `SocketableKind` becomes `SocketType` (10 values, no AI core). `Socketable` becomes concrete, with kind from the definition. Delete the four subclasses and map the legacy aliases.
   - **CSV:** the 19 kind changes from the conversion table. They belong here because `officer` and `team` stop existing.
   - **Rules:** CSV parsing rejects legacy kinds. Tree sockets accept subroutines only. Transposition crosses types and returns misfitting installed items to storage. Until stage 2, an old save's tree-socketed misfits do nothing, because `canSocketInto` already filters them.
   - **Strings, vocabulary, editor:** strings become `socket.type.*`. Regenerate the editor vocabulary; the editor dropdown follows automatically.
   - **Tests:** XStream legacy-alias round trip; kind follows definition; transposition crosses types and unsockets misfits; tree socket rejects non-subroutines.
2. **Migration and the Subroutine Socket rename.**
   - **Rename:** `modular_hull_socket` keeps its id. Its name becomes "Subroutine Socket" and its description is rewritten (en and zh, skill_types.json and the Translation keys). Update the README and forum post terms in stage 7.
   - **Migration:** `SocketTypeMigration` with its message. NPC tree-socket picks are limited to subroutines, and `NpcTreeTag.restoreSockets` filters misfits.
   - **Tests:** migration ejects misfits once and only once; NPC tag restore skips them; `DescriptionGoldenTest` regenerated.
3. **Framework items, storage, install state and effects.**
   - **Items and storage:** `HullFramework`, roller, codec, special item and item plugin; `SocketableStore` frameworks; `ShipSkillData` fields and methods; custody and disassembly changes.
   - **Fit and effects:** `ShipProfile.currentFit`; `FrameworkSlots`; framework effects in `ResolvedTree`, `EffectTotals` and `NpcBonusScaling`. Each step is headless and test-covered.
   - **Tests:**
     - The roll is 70/20/10, equal across types and duplicate-free, and can include every type on every hull size.
     - Installing is blocked by hull size and by each restriction.
     - A refit deactivates a slot and reactivating the fit restores it.
     - Converted Hangar Deck Crew can't satisfy its own socket.
     - Reconcile deletes on destruction and returns to storage on sale.
     - Effects appear once, with their own mod id, and NPC slots scale.
     - Respec leaves the framework alone.
   - **Review:** run an adversarial review here (complex src/main change).
4. **Drops and NPC flagships.**
   - **Player drops:** the level-15 gate, parallel salvage and Tech Mining rolls, and cargo absorption.
   - **NPC flagships:** the 50% flagship roll, per-socket items, the `NpcTreeTag` field, battle loot and capture.
   - **Tests:** with seeded Randoms, nothing drops below level 15; subroutine drop counts are unchanged by the parallel rolls; the flagship roll is deterministic; tags round-trip; loot and capture move frameworks and items.
   - **Review:** adversarial review of the NPC side.
5. **Install and socket UI.**
   - **Panels:** `FrameworkPanel`, framework picker, `SocketTarget` generalisation, storage filter and Frameworks tab.
   - **Integration:** follow-mode chrome changes in `CanvasMode`, and summary and tooltip lines.
   - **Checks:** tests for the canvas-mode table and the placement state machine, plus an in-game check, since UI can't be verified headless.
   - **Review:** adversarial review.
6. **New content.**
   - **Basics:** at least one non-unique definition per framework socket type (8), with prefix and suffix pools drawn from the brief's themes. Reactor must not reuse the subroutines' flux capacity and dissipation flats. Uses fixed hull-size values where the effect is a flat.
   - **Uniques:** for Shield Generator and Phase Coil.
   - **Naming:** affix words in `socketable_affixes.csv` and rare-name word lists in en and zh, checked against the CJK atlas.
   - **Art:** icons, including socket type icons for the framework panel.
   - **Balance:** a pass comparing a fully socketed 4-socket Rare framework, player and NPC flagship (scaled), against the tree's existing 1.5x-style totals.
7. **Docs.**
   - **Public docs:** README and forum post (kept in sync, plain prose) cover Subroutine Sockets, hull frameworks, socket types, restrictions, sources, the level 15 gate and NPC flagships. Add a short frameworks section to `docs.md`.
   - **Memory:** update the Modular Hull Sockets memory (in-game term and kinds).
   - **Changelog:** only when asked.

## Risks

- **XStream aliases:** if several aliases on one class misbehave in Starsector's XStream, use the empty-subclass fallback in stage 1. The stage 1 test catches it before any save is written.
- **Effect double counting:** a framework item must contribute exactly once, never through both a tree node and a framework slot. Custody keeps one installation per item id, and a test asserts it.
- **Circular fit:** handled by building the fit without framework effects. A test covers Converted Hangar Deck Crew.
- **Cross-type transposition:** a transposed installed item can lose its slot. It always lands in storage, never deleted, and the workbench says so.
- **NPC flagship strength:** a scaled 4-socket flagship at high levels could spike difficulty. Stage 6's balance pass covers it, and the 50% chance could become a LunaLib setting if needed.
- **Empty sockets between stages:** the converted uniques have no home until stage 5, so ship stages 1 to 6 together.
