# Attributes: plan

The 108 "Caps, Vents, Hull or Logistics" nodes become affinity nodes. Each one offers 5 Low Tech, 5 High Tech or 5 Midline affinity. Affinity adds up across the ship and grants stat bonuses in steps. The three roots grant 10 affinity of their own type instead of their current bonuses. Each root is redrawn as a glowing processor core whose circuit traces pulse outward, with the root's affinity total written in its centre.

Status: planned, not built.

Reference image for the root art: `docs/plans/images/attributes_root_circuit_reference.jpg`.

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
- `small_flux_optional` keeps its id (saves and `released_skill_nodes.json` refer to it). Its `optionalOptions` become the three affinity types, its name becomes "Affinity", and its own icon (shown before a choice is made) gets new art combining the three root icons.
- The four old option types stay in `skill_types.json` only while anything still refers to them (the migration below). Delete them afterwards if nothing else uses them.
- The roots' `effects` and `hullSizeEffects` become a single `*_AFFINITY_FLAT` of 10.

### Tooltips

Following `docs/tooltip_wording.md`:

- An affinity node: "+5 to Low Tech Affinity".
- A root: "+10 to Low Tech Affinity", then the ship's three affinity totals and the bonuses they currently grant: "2% more hull for every 5 Low Tech Affinity", and so on, each with its current total in brackets.
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

The three roots become processor cores, following the reference image. For a novice: the tree is drawn with OpenGL, and every frame each layer is drawn in order, back to front. The root adds two layers below its icon: a still picture of the circuit, and small glowing dots ("sparks") that move along the circuit lines. Additive blending means a spark's colour is added to what's under it, so it brightens the line instead of covering it.

### Shape

A dark disc with a thin bright rim, its middle left empty so the root's icon shows through. Pin stubs ring the disc's edge at even angles. From each pin a thin cyan circuit trace runs outward, bending at 45° or 90° once or twice, and ends in a small solder dot. The traces fan out in every direction.

The trace shapes are data: a list of polylines in `data/config/exiledSector/root_circuit.json`, in units of the root's radius, so the drawing and the spark paths always match. The circuit must fit inside the gap around the roots. Measure the space to the nearest node and connectors in `ship_skill_tree.json` and keep the traces inside it. The culling reach in `SkillTreeNodeRingRenderer.reach` grows to cover the traces, so a root at the screen edge still draws them.

### Static layer

One sprite, `graphics/ui/exiledSector/root_circuit.png`: the rim, the pins and every trace as crisp, dim lines in neutral white, with a faint constant halo baked in so the lines look live between pulses. It's never animated. Python isn't available, so the sprite is made by a PowerShell script (`tools/make-root-circuit.ps1`) that reads `root_circuit.json` and draws it with `System.Drawing` (anti-aliased lines, plus a blurred copy for the halo), so changing a trace only means rerunning the script. Each root draws it tinted with its own colour (see "Root colours").

### Root colours

Each root has its own colour for its circuit, sparks and affinity number, so the three roots read apart while choosing. The placeholder colours are amber for Low Tech, cyan for High Tech and green for Midline. The final colours come from each root icon's main colour. They sit in one place in code next to the circuit renderer, and the affinity node tooltips and summary lines use the same colours.

### Sparks

At regular intervals a bright glow runs down each trace:

- A small soft spark leaves the pin and travels along the trace, following every bend, to the solder dot. It has a hot head, near white with a touch of the root's colour, and a short tail that fades behind it. The line under it briefly brightens as it passes, and it fades out at the solder dot.
- The spark is one small glow sprite (`graphics/ui/exiledSector/circuit_spark.png`, a soft radial gradient) drawn with additive blending and tinted with the root's colour. The tail is three or four more copies of the same sprite behind the head along the path, each smaller and fainter. The brightening comes from the additive head and tail themselves, with no extra line drawing.
- Every trace fires on the same fixed interval (a placeholder of 2.5 seconds), and each has its own phase offset: its angle around the core divided by a full turn, times the interval. So sparks leave the core in a steady rotating rhythm rather than all at once.
- Every spark moves at the same speed (in root radii per second), so a longer trace just takes longer. If a trip takes longer than the interval, a trace can have two sparks in flight at once. The position comes from `(time - phase) mod interval` for each spark in flight, so nothing is stored per spark.
- Positions along a trace come from cumulative segment lengths, computed once when `root_circuit.json` loads.

### Drawing order and performance

Circuit sprite, then sparks, then the root's ring, icon and number. Connectors and other nodes stay above the circuit. A visible root draws one sprite plus about four spark sprites per trace each frame. Sparks are skipped when the root is off screen, or so zoomed out that the root is only a few pixels across. That keeps the per-frame cost small.

Technical details: `Sprite.render` sets its own blend function and turns `GL_BLEND` off after drawing. Set additive blending on the spark sprite with `setAdditiveBlend()`, and re-enable `GL_BLEND` before any raw GL that follows. Everything stays in GL11 on the render thread, with no `glGet` calls (Fast Rendering compatibility). The animation clock is the tree panel's own accumulated `advance` time, not wall-clock time, so it pauses with the panel.

### Root icon and affinity number

- The root icon is redrawn to sit inside the core's empty middle: smaller and dimmer than now, as a backdrop for the number. The three icon files keep their paths, and the affinity nodes use the same art.
- The ship's affinity total for that root's type is drawn in the centre with `SkillTreePanelStyle.font()`, centred, sized so three digits fit inside the core's inner disc. It has a minimum on-screen size so it stays readable when zoomed out, even if it then spills slightly past the disc.
- While the player is still choosing a root, each root shows the affinity the ship would start with (10).
- The number's `DrawableString` is cached and rebuilt only when the total or the font size changes, not every frame.

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

1. The three affinity effects and the stepped conversion in Exiled Lib, with tests.
2. The option types, `small_flux_optional`'s new options, and the new root effects.
3. Migration of saved selections, templates and NPC tags.
4. NPC option picking.
5. Tooltips and the summary line.
6. `root_circuit.json`, the PowerShell sprite script and the static circuit layer.
7. Sparks.
8. Root icon redraw and the affinity number.
9. Strings and docs.

## Tests

Each test goes in the lowest module that sees every class it uses.

- The conversion: 0, 4, 5, 9, 10, 14 and 100 affinity of each type give the right stepped MULT amounts. Totals from several sources are summed before stepping (two nodes give the same as one source of 10).
- A ship with each root and no nodes has exactly 10 of that affinity and none of the others.
- `small_flux_optional` offers exactly the three affinity types. Each option gives 5 of its affinity.
- The migration maps each old option once, leaves new selections alone, and covers templates and NPC tags.
- NPC builds pick their root's affinity.
- `root_circuit.json` loads: every trace starts on the rim, has one or two bends at 45° or 90°, and stays inside the measured space. Phase offsets are evenly spread over the interval.
- Spark positions: at the phase offset a spark sits on the pin, after `length / speed` seconds it sits on the solder dot, and with a trip longer than the interval two sparks are in flight.
- `EditorRoundTripTest` and `TranslationKeyReferencesTest` still pass with the new types and keys.
