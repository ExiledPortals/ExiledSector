# Attributes: plan

The 108 "Caps, Vents, Hull or Logistics" nodes become affinity nodes. Each one offers 5 Low Tech, 5 High Tech or 5 Midline affinity. Affinity adds up across the ship and grants stat bonuses in steps. The three roots grant 10 affinity of their own type instead of their current bonuses. Each root becomes a glowing processor core around its existing icon whose circuit traces pulse outward, with the root's affinity total written in its centre.

Status: built and tested 2026-10-23, committed on parallel-work. Not yet checked in game.

Reference image for the root art: `docs/plans/completed/images/attributes_root_circuit_reference.jpg`.

## Today

Paths are relative to `exiledsector-skill-tree/src/main/java/exiledsector/` unless they name another module.

- The highway nodes are the node type `small_flux_optional` in `data/skilltrees/skill_types.json`, placed 108 times in `ship_skill_tree.json`. Its `optionalOptions` are `flux_capacity` (2% more flux capacity), `flux_dissipation` (2% more flux dissipation), `hull` (2% more hull plus flat armor by hull size) and `fuel_crew_cargo` (1% more fuel, crew and cargo capacity).
- The only other optional type is `shield_turn_raise_rate_optional` (one node), which this plan leaves alone.
- A ship's choice for an optional node is stored as its optional selection (`ShipSkillData.getOptionalSelection`), checked by `NodeEligibility.validOption`, and picked from `ui/node/SkillTreeNodeDropdownRenderer`. NPC trees pick options in `skills/npc/NpcSkillTreeBuilder`.
- The roots are `root_low_tech` (10% hull, flat armor), `root_midline` (5% hull, 5% flux capacity) and `root_high_tech` (10% flux dissipation, flat flux capacity). The player picks one root per ship, permanently (`ui/node/StartingRootChoice`). Roots are drawn in `ui/node/SkillTreeNodeDrawer.renderRootNode`: a ring from `SkillTreeNodeRingRenderer.draw`, then the icon (`graphics/icons/root_*.png`, 256x256).
- Tree MULT effects (`*_MULT`) are summed with each other and applied as one multiplier on top of outside sources, so their lines read "more" (`docs/tooltip_wording.md`). `EffectTotals` (Exiled Lib) applies diminishing returns to tree bonuses to armor, hull, flux capacity and flux dissipation.

## Affinity

Three affinity types: Low Tech, High Tech and Midline. A ship's affinity of each type is 10 from its root if the root matches, plus 5 for each allocated affinity node set to that type. Each type grants these bonuses, counted in whole steps (14 affinity is two steps of 5 and one step of 10):

| Affinity | For every 5 | For every 10 |
|---|---|---|
| Low Tech | 2% more hull; 1% more cargo, fuel and crew capacity | 2% more armor |
| High Tech | 2% more flux capacity; 1% more cargo, fuel and crew capacity | 2% more flux dissipation |
| Midline | 2% more flux capacity; 2% more hull; 1% more cargo, fuel and crew capacity | 1% more armor; 1% more flux dissipation |

The bonuses feed the existing tree MULT effects (`HULL_MULT`, `FLUX_CAPACITY_MULT`, `FLUX_DISSIPATION_MULT`, `CARGO_CAPACITY_MULT`, `FUEL_CAPACITY_MULT`, `CREW_CAPACITY_MULT`). Armor has no MULT effect yet (only `ARMOR_PERCENT` and `ARMOR_FLAT`), so add `ARMOR_MULT` in the `defense` part alongside the others. The bonuses are summed with every other tree MULT source of the same stat and get the same diminishing returns as other tree bonuses. All magnitudes are placeholders pending balance.

### Effects

Affinity is a reusable stepped bonus, so it lives in Exiled Lib, in the `special` part (it spans hull, flux and logistics):

- Three new effects in `SpecialSkillEffect`: `LOW_TECH_AFFINITY_FLAT`, `HIGH_TECH_AFFINITY_FLAT` and `MIDLINE_AFFINITY_FLAT`. Their magnitude is the affinity they add.
- A conversion step turns each affinity total into its stepped MULT contributions before the totals are applied. Read how `EffectTotals` sums effects per ship first, and add the conversion there so diminishing returns and the summary see the stat bonuses, not the raw affinity. The step sizes and per-step amounts sit in one table in that class, not spread across effects.
- Register the effects in `SkillEffectRegistry`, add their keys to `exiled-lib/mod/data/strings/exiledLib/`, and add them to `tools/editor_vocabulary.json` so the editor knows them.

### Node types

- Three new option types: `affinity_low_tech`, `affinity_high_tech` and `affinity_midline`, each giving 5 of its affinity. Each uses its root's icon (`graphics/icons/root_low_tech.png` and so on). Icons must be circular copies: check the root icons are, and make circular copies with the editor's "Make circular copy" if not.
- `small_flux_optional` keeps its id (saves and `released_skill_nodes.json` refer to it). Its `optionalOptions` become the three affinity types, and its name becomes "Affinity". Before a choice is made the tree draws it as the usual optional-node pie split of its three options' icons, which are the root icons.
- The four old option types stay in `skill_types.json` only while anything still refers to them (the migration below). Delete them afterwards if nothing else uses them.
- The roots' `effects` and `hullSizeEffects` become a single `*_AFFINITY_FLAT` of 10.

### Tooltips

Following `docs/tooltip_wording.md`:

- An affinity node: "+5 to Low Tech Affinity".
- A root: "+10 to Low Tech Affinity", then, once the ship has a root, the ship's total of that root's affinity and the bonuses it currently grants ("4% more hull points."). F1 adds what each step gives: "2% more hull for every 5 Low Tech Affinity", and so on (2026-10-23).
- The ship summary gets one line per affinity type the ship has, with its total.

## Migration

Existing ships have old option selections on these nodes. On load, each one is mapped once:

| Old option | New option |
|---|---|
| `hull` | `affinity_low_tech` |
| `flux_capacity`, `flux_dissipation` | `affinity_high_tech` |
| `fuel_crew_cargo` | `affinity_midline` |

Saved tree templates that name the old options are mapped the same way (`skills/template/`). NPC tree tags store option ids too, so `NpcTreeTag.decode` maps them as it reads.

## NPC trees

`NpcSkillTreeBuilder` picks the affinity that matches the NPC ship's root for every affinity node.

## Root art and animation

The three roots become round glowing processor cores, following the reference image. For a novice: the tree is drawn with OpenGL, and every frame each layer is drawn in order, back to front. The circuit is drawn live the way vanilla beams are: each trace segment is a thin bright strip with a wider soft glow strip under it, all with additive blending, which adds light to what's underneath instead of covering it.

### Shape

A round core with a glowing rim. Four bundles of nine parallel traces leave the core at the top, right, bottom and left. Each bundle splays out with 45° bends (outer traces bend first so traces never cross), then most bend back to run parallel and end at varied lengths in a bright solder dot.

The trace shapes are data: polylines in `data/config/exiledSector/root_circuit.json`, in units of the root's radius, so the drawing and the spark paths always match. The circuit stays inside the gap around the roots (2.4 radii; the nearest unrelated connector is about 2.45 radii away), and the culling reach covers it.

### Layers

All textures are vanilla (`starsector-core/graphics/fx`), tinted with the root's colour: `star_halo` (a wide faint halo), `owner_glow` (the core's glow), `beamfringeb` and `beamcorec` (each trace's glow and its near-white line), `hit_glow_small` and `particlealpha32sq` (the solder dots), `shields256ringc` (the rim) and `particlealpha64sq` (the sparks). Each layer is one batch with one texture bind (`TexturedQuadBatch`), in `RootCircuitRenderer`.

### Reveal

While the player is choosing a root, and while the chosen root flies to its place, only the core and its rim show. Once the chosen root arrives, the traces grow out from the rim at a fixed speed with a spark riding each tip, each solder dot fades in as its trace completes, and the halo brightens with the growth (`RootCircuitGrowth`). Sparks start once it's complete. Opening the tree on a ship that already has a root shows the whole circuit at once. Unchosen roots show only their core.

### Root colours

Each root has its own colour, taken from its icon's main colour: orange for Low Tech, gold for Midline, blue for High Tech (`RootColours`).

### Sparks

- Every trace fires on the same interval (a placeholder of 5 seconds). Phase offsets are scattered across traces by the golden ratio, so only a few sparks are in flight at a time rather than a bundle firing at once.
- A spark has a hot near-white head and a short tail of smaller, fainter copies along the path behind it, so it follows bends. It fades out at the solder dot.
- Every spark moves at the same speed, so a longer trace takes longer. The position comes from `(time - phase) mod interval`, so nothing is stored per spark.

### Performance

A visible root draws about 200 textured quads per frame in nine batches. Sparks are skipped when the root is under 24 px across, and nothing is drawn when it's off screen. Everything stays in GL11 on the render thread, with no `glGet` calls (Fast Rendering compatibility). The clock is the tree panel's own accumulated `advance` time, so it pauses with the panel.

### Root icon and affinity number

- The root icons are vanilla's own Low Tech, High Tech and Midline art and are never redrawn. The core only enhances them: the icon draws smaller and dimmer inside the core, as a backdrop for the number.
- The ship's affinity total for that root's type is drawn in the centre in white with a dark outline, so it reads on any icon or glow. It has a minimum size so it stays readable zoomed out.
- Until a chosen root is in place (choosing, flying, or returning after a reset), each root shows the affinity the ship would start with (10).
- The number's labels are rebuilt only when the total changes or the zoom crosses a 2 px font step.

## Strings and docs

- New keys in `data/strings/exiledSector/skillTree/en.json` and `zh_CN.json` for the node name, option names, root and summary lines. Effect lines go in Exiled Lib's catalogue.
- `docs/docs.md`: a short "Affinity" section on how totals and steps work and where the code lives. No UI or animation details (they aren't mechanics).
- README and `docs/forum_post.bbcode`: one line that small highway nodes now grant affinity.

## Decided

- Each root uses its own colour (2026-10-23).
- Only the 108 `small_flux_optional` nodes change. The `shield_turn_raise_rate_optional` node stays as it is (2026-10-23).
- Existing choices are converted with the migration map, not reset (2026-10-23).
- Other items will grant affinity later, but not in this plan. Keep the affinity effects free of any node-only assumption so an item can carry them (2026-10-23).

## Build order

1. Done: The three affinity effects and the stepped conversion in Exiled Lib, with tests.
2. Done: The option types, `small_flux_optional`'s new options, and the new root effects.
3. Done: Migration of saved selections, templates and NPC tags.
4. Done: NPC option picking.
5. Done: Tooltips and the summary line.
6. Done: `root_circuit.json` and the live circuit layers drawn from vanilla fx textures, with the reveal on choosing a root.
7. Done: Sparks.
8. Done: Root icon placement and the affinity number.
9. Done: Strings and docs.

## Tests

Each test goes in the lowest module that sees every class it uses.

- The conversion: 0, 4, 5, 9, 10, 14 and 100 affinity of each type give the right stepped MULT amounts. Totals from several sources are summed before stepping (two nodes give the same as one source of 10).
- A ship with each root and no nodes has exactly 10 of that affinity and none of the others.
- `small_flux_optional` offers exactly the three affinity types. Each option gives 5 of its affinity.
- The migration maps each old option once, leaves new selections alone, and covers templates and NPC tags.
- NPC builds pick their root's affinity.
- `root_circuit.json` loads: every trace starts on the rim, has at most two bends at 45° or 90°, and stays inside the measured space. Phase offsets are scattered over the whole interval.
- The reveal: nothing grows while choosing or flying, growth starts when the chosen root arrives, other roots stay bare, and a ship that already has a root shows the whole circuit.
- Spark positions: at the phase offset a spark sits on the pin, after `length / speed` seconds it sits on the solder dot, and with a trip longer than the interval two sparks are in flight.
- `EditorRoundTripTest` and `TranslationKeyReferencesTest` still pass with the new types and keys.
