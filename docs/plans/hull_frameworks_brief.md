# Hull Frameworks - design brief

Exiled Sector, Starsector mod at E:\Dev\ExiledSector.

## Goal

Add hull frameworks: installable items that give a ship extra, typed socket slots, plus new socketable kinds that only fit their own socket type.

## Hull frameworks

- One framework fits exactly one hull size (frigate, destroyer, cruiser, capital). It can only be installed on a ship of that size.
- Rarity: Common rolls 2 random socket types, Rare rolls 3, Unique rolls 4. The user once called the middle tier "magic"; confirm the name. Existing code uses SocketableRarity COMMON/RARE/UNIQUE.
- Socket types are rolled from the framework socket pool below. Some types are hull-restricted; a framework containing a restricted type cannot be installed on a hull that fails the restriction. Drops are restricted by hull size only; the hull-feature check happens at install time, and the install UI must say why a framework doesn't fit.
- Installing a framework shows its sockets; the player then sockets typed socketables into them.

## Framework socket types (8)

Universal (any hull of the right size):

- Bridge: officers (and possibly AI cores). Command, CR, crew and ship-system handling.
- Crew Quarters: support teams. Crew, logistics, salvage, sensors and EW, ground support.
- Engine Room: drives. Speed, maneuverability, zero-flux boost, burn, fuel, engine durability.
- Reactor: power plant. Vent rate, overload duration, hard-flux dissipation, system regen and charges. Must not duplicate the flux capacity and dissipation rolls that subroutines already carry.

Restricted:

- Weapon Mount: hulls with weapon slots. Weapon crews and fire control: damage, rate of fire, range, flux cost and ammo per weapon type.
- Shield Generator: shielded hulls only. Efficiency, upkeep, arc, raise and turn rate.
- Phase Coil: phase hulls only. Cloak cost, upkeep, time multiplier, cooldown.
- Flight Deck: hulls with fighter bays. Refit time, replacement rate and recovery, fighter damage, armour and range.

Restrictions should reuse the same hull checks as the tree's req_shields, req_phase and req_fighter_bays tags (NodeEligibility / NodeRequirements) so there is one rule for both.

## Subroutine sockets

- The existing tree socket node (type id modular_hull_socket, in game "Modular Hull Socket") becomes the "Subroutine Socket" in game. It accepts subroutines only; framework socketable kinds may not be inserted into it. Keep the node type id for save compatibility unless a migration is added.
- Subroutines never go into framework sockets.

## Existing uniques to convert

All current officer and team uniques move to framework kinds.

| Socket type | Uniques |
|---|---|
| Bridge | Alpha Site Survivor, Tri-Tachyon Morale Assurance Officer, Back-Alley Cyber-Surgeon |
| Crew Quarters | Auxiliary Conscripts, Olinadu Security Detachment, Asher Salvage Guild Crew, Salvager Crew, COMSEC Signals Detachment, Pather Cell |
| Engine Room | Sindrian Fuel Company Engineers |
| Weapon Mount | Coatl Bastion Gunnery Team, Gryphon Missile Crew, Deserter Gunners |
| Flight Deck or Weapon Mount (open question) | Converted Hangar Deck Crew. Flight Deck is thematic; Weapon Mount keeps its "adds a fighter bay" useful on non-carriers |

Optional moves of hardware-flavoured subroutine uniques: Gate Hauler Drive Coil to Engine Room, Synchrotron Cell to Reactor. Vambrace Plating and Fullerene Spool Integration stay subroutines.

Shield Generator, Phase Coil, Flight Deck and Reactor have no uniques yet; they need new content.

## UI

The skill tree already has a follow mode: clicking the ship card (bottom left) zooms all the way in on the viewed ship, locks the camera, hides other fleet sprites and hides the socket storage and template buttons (SkillTreeCanvasPlugin, TreeCamera, ShipCardClickTarget, ui/decoration/SkillTreeFleetRenderer). Framework selection and installation, and the framework's sockets, appear in this zoomed-in view.

## Open questions

Ask the user; don't guess.

1. Middle rarity name: Rare or Magic?
2. Socket-type roll weights (equal, or favour some), and whether a framework can roll the same type twice.
3. Does Bridge also accept AI cores, or do AI cores get no framework home for now?
4. Converted Hangar Deck Crew: Flight Deck or Weapon Mount?
5. Move Gate Hauler Drive Coil and Synchrotron Cell?
6. Where frameworks come from (salvage, Tech Mining, battles, crafting from Socketable Parts), costs, and whether and how they can be removed or swapped; what happens to socketed items when a framework is removed.
7. Do NPC ships get frameworks, and does the NPC bonus multiplier scale framework items?
8. How many frameworks per ship (presumably one).

## Project rules every session must follow

- No code comments except TODOs and a one-line justification above every @SuppressWarnings.
- All player-visible text goes through Translation with keys in both data/strings/exiledSector/en.json and zh_CN.json. Check new Chinese characters are in the CJK font atlas (graphics/fonts/exiledSector/notosanssc.fnt). LunaLib and addPara text must not contain a bare %.
- Persisted save classes (ShipSkillData, SocketableStore, Socketable and its subclasses Subroutine, Officer, Team, AiCore, RolledEffect, FrozenName) must not be renamed or removed without XStream aliases. Their fields can be renamed safely, since saves ignore unknown fields.
- New skill_types.json or socketable CSV fields must be added to tools/skill_tree_editor.html's serialisers, or saving from the editor will delete them.
- Reflection only through ui/refit/UiReflection (MethodHandles); ScriptSandboxRestrictionsTest enforces this.
- Minimise per-frame cost: build once, cache, no per-frame allocations in render loops.
- Name things descriptively (panelWidth, not width).
- Build: "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" -q test with JAVA_HOME "C:\Program Files\Java\jdk-17". Deploy with .\deploy.ps1 from the main checkout only, and only when no Starsector process runs from E:\Dev\Starsector.
- Never commit unless the user asks in that request.
- README.md and docs/forum_post.bbcode stay in sync; plain prose, no bold-label bullets.
