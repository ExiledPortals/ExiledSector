# Mixed damage hits

Status: planned. Nothing is built yet.

The goal is for a partial damage conversion to be one hit that carries several damage types at once. For example, a kinetic shot with 40% converted to high explosive becomes one hit that is 60% kinetic and 40% high explosive. Today, a conversion either retypes the whole hit, or (Ion Spill, Ion Cascade) leaves the hit alone and adds a separate hit on the next frame.

The game has no mixed-type hit. Every hit has exactly one `DamageType`. However, the engine reads that type's shield, armor and hull multipliers live on every hit, and the enum exposes public setters for them. So for the length of one hit, a carrier type's multipliers can be set to a blend of the real types' multipliers, and set back afterwards. Nothing is overridden or patched.

## Engine facts this relies on

Verified by decompiling `Ship`, `ship.new` (armor) and `EmpArcEntity` from 0.98a-RC8. Also recorded in the `starsector_damage_listener_facts` memory.

- One hit is one synchronous call. `Ship.applyDamage` runs, in order: the engine-wide `DamageDealtModifier`s, the attacker ship's `DamageDealtModifier`s, the engine-wide `DamageTakenModifier`s, the target ship's `DamageTakenModifier`s, then `applyDamageInner`, then `reportDamageApplied`. Last, it unmodifies the returned ids. Modifiers only run when `getMultiplier() > 0` and `getBaseDamage() > 0`, so EMP-only hits skip them.
- `DamageType` has private `shieldMult`, `armorMult` and `hullMult` fields with public `setShieldMult`, `setArmorMult` and `setHullMult`. The defaults are KINETIC 2 / 0.5 / 1, HIGH_EXPLOSIVE 0.5 / 2 / 1, FRAGMENTATION 0.25 / 0.25 / 1, ENERGY 1 / 1 / 1 and OTHER 1 / 1 / 1. The values are global and shared by every hit of that type.
- Shields use `type.getShieldMult()` times the shield damage taken stats (Ship around line 4531).
- Armor (`ship.new` around lines 279 to 345) uses one factor for both armor damage and hit strength. That factor is `armorMult` times ArmorDamageTaken, and also times KineticArmorDamageTaken when the type is KINETIC. Damage that gets through armor to hull is rescaled by `hullMult / that factor` times HullDamageTaken.
- The per-type stats (Kinetic/HighExplosive/Fragmentation/Energy DamageTaken and the matching ShieldDamageTaken) are applied in switches on the hit's type. OTHER has no per-type stats at all.
- The AI, tooltips and the codex also read these getters, but never during a hit.

## Design

### Carrier type: OTHER

A mixed hit is retyped to OTHER for its length. OTHER's three multipliers are set to the blend, and the hit's own type and the multipliers go back as soon as the hit reports.

OTHER is the right carrier because nothing in vanilla or the installed mods deals OTHER damage from inside a hit (see Mod conflicts). Because OTHER has no per-type stats, the engine adds nothing on top of the blend. So a blended OTHER changes only our hit. If the carrier were KINETIC, HIGH_EXPLOSIVE or ENERGY instead, every other hit of that type dealt during the bracket would also take the blend. Liminal's Mirror Skin, for one, deals energy damage from inside the hit. And the target's per-type resistance for the carrier would have to be divided back out of the blend.

The cost is that other mods' type checks see OTHER on a mixed hit. These are listed below. The rejected alternative was to use the largest share's type as the carrier. That keeps type checks roughly right, and Detailed Combat Results keeps counting the hit, but it brings back the cross-hit leaks just described.

### Where the mix is decided and where it is applied

1. The attacker side records the mix and changes nothing else. A new `HitMix` in the skilleffect root holds the type shares for one `DamageAPI` (by identity). Conversions write to it from the attacker's dealt modifiers, as `DamageConversionListener` does today. A hit with no entry has 100% of its own type. `HitMix` is cleared together with the `HitDamageRestorer` entry.
2. The carrier and blend are applied in an engine-wide `DamageTakenModifier`, `MixedHitBlender`. It registers at combat start through `SkillEffectSupport.ensureEngineListener`. Doing it there rather than on the attacker means every attacker-side dealt modifier, ours or another mod's, still sees the weapon's real type. Only target-side taken modifiers see OTHER.
3. The blender acts only when the mix has two or more types with a nonzero share and the target is a `ShipAPI`. For any other target, such as missiles or asteroids, it retypes to the largest share through `HitDamageRestorer.retypeForThisHit` and does no blending, because a report for a non-ship target is not guaranteed.
4. `reportDamageApplied` sets the multipliers back and pops the bracket. The type itself is restored by the existing `HitDamageRestorer` entry.

### Blend formula

For target stats S and shares s_t over KINETIC, HIGH_EXPLOSIVE, FRAGMENTATION and ENERGY that sum to 1, let d_t be the target's per-type DamageTaken for t:

- shield = sum of s_t x shieldMult_t x d_t x (t's ShieldDamageTaken)
- armor = sum of s_t x armorMult_t x d_t, with KineticArmorDamageTaken also multiplied into the kinetic term
- hull = sum of s_t x hullMult_t x d_t

The type-independent ShieldDamageTaken, ArmorDamageTaken and HullDamageTaken stay in the engine's own maths and are not folded in. The multipliers are read from the live enum values recorded at load, not from constants. For a 60% kinetic and 40% high explosive hit with no resistances, that gives shield 1.4, armor 1.1 and hull 1.0.

As part of the build, check each per-type switch's place in `applyDamageInner` and confirm that d_t comes before the layer split. If a switch scales only one layer, fold it into that layer only.

A mixed hit is stronger against armor than two split hits of the same total. Armor reduction works on hit strength, and one hit keeps the full strength. That is intended. The conversion magnitudes stay placeholders.

### Nested hits and safety nets

- Nested hits. Another mod's target-side modifier can deal damage synchronously with `damage.getType()`, which is OTHER during the bracket. Arthr's Close Support Wings redirect is one. That nested hit has no `HitMix` entry. The blender gives it the outer hit's mix, blended for the nested target, and puts the outer blend back when the nested hit reports. The blends sit on a stack, so the depth doesn't matter. Nested hits of any type other than OTHER aren't affected.
- Our own side damage. `HitDamageRestorer.whileDealingSideDamage` also puts OTHER's defaults in place for its duration, and takes the blend back afterwards.
- Defaults. These are snapshotted at application load from the live enum. They are put back at the start of every combat frame, when combat ends, on game load, and at the start of any hit whose stack entry belongs to a hit that has already finished. A stuck blend therefore lasts one hit at most.
- Check, as part of the build, that every path through `Ship.applyDamage` that gets past the taken modifiers reaches `reportDamageApplied`.

### Conversions that use it

- `DamageConversion` gains a share. At 100%, which covers all three current mount conversions, the hit is still a plain retype and nothing is blended. Below 100%, the conversion writes a `HitMix` entry. The first-match rule in `DamageConversion.first` stays.
- EMP conversions don't need this. EMP isn't one of the three multipliers, and `HitDamageRestorer.moveToEmpForThisHit` can already move a share of a hit into EMP within that hit.
- Ion Spill (shield EMP to energy) and Ion Cascade (EMP to high explosive) change on any hit that carries damage. They move their share of the hit's EMP into its damage, as the converted type, in the same hit, and record the new shares. If the hit is already the converted type, only the damage is added and no mix is recorded. EMP-only hits, such as pure EMP arcs, skip every modifier, so `EmpOnlyHits` and `DeferredHits` still deal those as a separate hit.
- The Atlas scar high explosive conversion and the adaptive retype are full retypes. They read the mix through `HitMix`, and when they fire they replace it with a single type.
- Arc weapons get the same treatment once arcs can be traced back to their weapons (the separate arc attribution task).

### Exiled Sector readers to switch to `HitMix`

Any of our listeners that reads `damage.getType()` during a hit must read the shares instead, and weight per-type effects by them:

- `weapon/DamageTypeListener`: per-type damage bonus percents, plus the fragmentation check through `originalHitType`.
- `charts/effect/ChartEnemyDamageListener`: per-type multipliers for chart enemies.
- `charts/officer/scar/ScarDamageTally`: splits its tally by share.
- `onhit/DamageConversionListener`, which covers both the conversion match and the adaptive retype, and `scar/HighExplosiveConversionListener`.
- `HitDamageRestorer.originalHitType` must never return OTHER for a mixed hit. Callers that need a single type get the largest share.
- `fighter/BomberPayloadDetonation` reads the type when the payload detonates. Confirm that this happens outside any bracket.

`HitMix` reads the attacker's conversions lazily and caches the result, so all of our readers agree, in whatever order they run.

## Tooltip wording

This plan affects the pending arc conversion task, whose brief (not yet handed to a session) asks for Ion Spill and Ion Cascade to be reworded as an extra hit. With mixed hits, an extra hit only happens on EMP-only hits. On every other hit, the conversion is part of the same hit, so the current "is dealt as ... instead" wording becomes correct again. The user needs to decide which wording to use before either task changes these strings. Partial mount conversions can use the existing "is dealt as ... instead" pattern, following `docs/tooltip_wording.md`.

## Tests

The tests go in core, mirroring `skilleffect` and `skilleffect/onhit`. A JUnit extension snapshots the `DamageType` multipliers before each test and puts them back after it, so that no test leaks into another.

- The blend formula against a reference that computes each layer the way the engine does. Cover single types, 60/40 kinetic and high explosive, a fragmentation mix, KineticArmorDamageTaken, per-type and shield-specific resistances, and a zero resistance.
- The bracket: the blend is set at the taken stage and restored on report; non-ship targets fall back to the largest share; a 100% conversion doesn't blend.
- Nested hits: an inner OTHER hit inherits the mix and the outer blend comes back afterwards; `whileDealingSideDamage` deals at the defaults.
- Safety nets: the reset at frame start, at combat end and on a stale hit.
- `HitMix` arithmetic: shares from a partial conversion, EMP moved into damage for Ion Spill and Ion Cascade, a full retype replacing a mix.
- Each reader in the list above weights its effect by share.

## Mod conflicts

All 84 enabled mods (`enabled_mods.json` in the C: install) were checked in two ways. A bytecode string scan of every jar looked for calls to the `DamageType` setters, uses of OTHER, damage listeners that read the type or the multipliers, and listeners that deal damage. Every flagged class was then disassembled with `javap`, and the loose scripts were grepped (Emergent Threats compiles its own `.java` files). No mod calls the `DamageType` setters. No mod replaces the `DamageType` class: Theory of Toolbox ships a copy of `starfarer.api.jar` under `0.98a/`, but only to build against, and it isn't on its jar list.

Attacker-side dealt modifiers always see the weapon's real type, because the carrier is set after them. So nothing in that group conflicts.

### Affected: target-side checks that see OTHER

| Mod | Class | What happens on a mixed hit |
| --- | --- | --- |
| Arthr's Ships | `ass_StealthStats$stealtApply` | It checks for fragmentation and deals damage again with the hit's type. A mixed hit never counts as fragmentation, and the damage it deals again is a nested OTHER hit that inherits the mix. |
| Arthr's Ships | `ass_DodgeStats$doStuffWhenBeam` | It switches on the type, so a mixed hit takes its default branch. |
| Arthr's Ships | `ass_CloseSupportWings$getDownMrPresident` | It redirects damage with the hit's type through `applyDamage`. The redirected damage is a nested OTHER hit that inherits the mix. That is correct. |
| Arthr's Ships | `ass_fighterShielderStats$fighterDamageNullifyer` | It spawns an arc with the hit's type. The arc lands on a later frame as plain OTHER (1 / 1 / 1), so the reflected damage loses the mix. |
| LOST_SECTOR | `nskr_absorption$absorptionCounter` | It branches on fragmentation, high explosive and kinetic, so a mixed hit gets the fallback (energy-like) handling. |
| Secrets of the Frontier | `SotfInvokeHerBlessingPlugin$SotfHullSiphonDamageTakenListener` | It checks for fragmentation, so a mixed hit never counts as one. |
| Knights of Ludd | `KnightRefit$ExplosionOcclusionRaycast` through `StarficzAIUtils.damageAfterArmor` | It estimates armor damage with its own switch on the type, so a mixed hit takes the typeless estimate. |
| Detailed Combat Results | `DamageDetectorListener.reportDamageApplied` | It returns immediately for OTHER results, so mixed hits are left out of the after-battle damage tables. This is the most visible effect. It can't be fixed without changing that mod. |

### Correct as they are

| Mod | Class | Why |
| --- | --- | --- |
| Random Assortment of Things | `PhaseshiftShield$PhaseshiftShieldDamageModifier` | It reads `damage.getType().getShieldMult()`, so it sees the blend. |
| Secrets of the Frontier | `SpiritWardDamageModifier` | Same: it reads the shield multiplier and sees the blend. |
| Liminal | `MirrorSkinInst$MirrorSkinListener` | It deals energy damage synchronously. Energy's multipliers are never touched. |
| Knights of Ludd | `ShipExplosionListener` | It checks high explosive on ship explosions, which are never mixed. |
| Arthr's Ships | `ass_BreachRounds`, `ass_IncendRounds`, `ass_BlyezeeysDamageDealtMod` | Attacker-side, so they see the real type. |
| Ship Mastery System | `WarpedPseudocoreInterface`, `LargeBallisticFragDamage`, `BeamPartialHardFlux` | Attacker-side, so they see the real type. `BeamPartialHardFlux` deals its side damage before the carrier is set. |
| Second-in-Command | `FocusedLensesListener` | Attacker-side. |
| Random Assortment of Things | `MoonlightLanceEffect`, `SeabreamEffect` | Attacker-side. |
| Unthemed Weapons Collection | `MorphCannonEffect`, `SpikerEffect` (dealt), `EnergyBallLauncherEffect` (every frame) | Attacker-side or outside hits. |
| Emergent Threats (Vice and IX Revival) | the `*HardFlux` dealt modifiers and the weapon effect scripts | The modifiers match weapon ids only. The scripts read `getDamage().getType()` outside hits or in `onHit`, which the engine calls after the hit returns, with the real type already restored. |
| AoTD Vaults of Knowledge | `DisgustingResilience` listeners | Weapon type checks only. |
| Nightcross Armory | Second-in-Command skill modifiers | Weapon type checks only. |
| Vanilla | `EnergyWeaponMastery$EWMDamageDealtMod` | Weapon type check only. |
| Industrial Evolution | `IndEvo_targetingBeamEffect` | It spawns OTHER arcs, but from its every-frame plugin and never inside a hit. |
| tahlan Shipworks | `CrusherDriveBeamEffect`, `EthniuBeamEffect` | They deal OTHER from beam plugins, outside hits. |
| Random Assortment of Things | `SaintShipSystem$SaintConstructionScript` | Energy damage from a script, outside hits. |
| Secrets of the Frontier | `SotfHatredBeyondDeath` and `SotfElegyOfOpis` Spite explosions | High explosive damage from advance, outside hits. |
| Console Commands | `Kill` | OTHER damage from a command, outside hits. |
| GraphicsLib | `ArcEffectOnOverload` | Visual arcs. |
| Galactic Constellate | `gr_FluxReversal` | A ship system script, outside hits. |
| Ashlib | `WeaponInfoGenerator` | Tooltip text only. |

Out-of-hit OTHER damage still goes through `Ship.applyDamage`, so the stale-hit check resets any stuck blend before it lands.

### Installed but not enabled

- ExoticaTechnologies: `DaemonCore`'s dealt and taken listeners and `EqualizerCore` check the type, and `NanotechArmor` and `PenanceEngine` use OTHER. If it is enabled later, its taken listeners would join the affected table.
- UAF: `uaf_reflectiveArmor` (both versions) and `uaf_shieldedTorpedo` use OTHER. Check them again if it is enabled.
- Leftover folders without a `mod_info.json` are never loaded and were skipped: an older Detailed Combat Results, Console Commands, Second-in-Command 2.1.0 and a second Vaults of Knowledge.

## Steps

1. `HitMix` and the default snapshot, with the JUnit extension that restores the multipliers.
2. The blend formula as a pure function, with its tests.
3. `MixedHitBlender`: the engine-wide taken modifier and the report restore, the stack, the non-ship fallback and the safety nets, plus the `whileDealingSideDamage` change.
4. A share on `DamageConversion` and partial conversions through `HitMix`.
5. Ion Spill and Ion Cascade move EMP into the same hit on damage-carrying hits, and keep the deferred hit for EMP-only hits.
6. The Exiled Sector readers switch to `HitMix`.
7. The wording, once the user has chosen.
