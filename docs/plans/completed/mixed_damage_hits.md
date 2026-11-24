# Mixed damage hits

Status: all steps are built and tested.

The goal is for a partial damage conversion to be one hit that carries several damage types at once. For example, a kinetic shot with 40% converted to high explosive becomes one hit that is 60% kinetic and 40% high explosive. Today, a conversion either retypes the whole hit, or (Ion Spill, Ion Cascade) leaves the hit alone and adds a separate hit on the next frame.

The game has no mixed-type hit. Every hit has exactly one `DamageType`. However, the engine reads that type's shield, armor and hull multipliers live on every hit, and the enum exposes public setters for them. So for the length of one hit, a carrier type's multipliers can be set to a blend of the real types' multipliers, and set back afterwards. Nothing is overridden or patched.

## Engine facts this relies on

Verified by decompiling `Ship`, `ship.new` (armor) and `EmpArcEntity` from 0.98a-RC8. Also recorded in the `starsector_damage_listener_facts` memory.

- One hit is one synchronous call. `Ship.applyDamage` runs, in order: the engine-wide `DamageDealtModifier`s, the attacker ship's `DamageDealtModifier`s, the engine-wide `DamageTakenModifier`s, the target ship's `DamageTakenModifier`s, then `applyDamageInner`, then `reportDamageApplied`. Last, it unmodifies the returned ids. Modifiers only run when `getMultiplier() > 0` and `getBaseDamage() > 0`, so EMP-only hits skip them.
- `DamageType` has private `shieldMult`, `armorMult` and `hullMult` fields with public `setShieldMult`, `setArmorMult` and `setHullMult`. The defaults are KINETIC 2 / 0.5 / 1, HIGH_EXPLOSIVE 0.5 / 2 / 1, FRAGMENTATION 0.25 / 0.25 / 1, ENERGY 1 / 1 / 1 and OTHER 1 / 1 / 1. The values are global and shared by every hit of that type.
- Shields use `type.getShieldMult()` times the shield damage taken stats (Ship around line 4531). On a shield hit only the per-type ShieldDamageTaken applies; the general per-type DamageTaken does not.
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

- shield = sum of s_t x shieldMult_t x (t's ShieldDamageTaken), with no d_t, because shield hits skip the general per-type DamageTaken
- armor = sum of s_t x armorMult_t x d_t, with KineticArmorDamageTaken also multiplied into the kinetic term
- hull = sum of s_t x hullMult_t x d_t

The type-independent ShieldDamageTaken, ArmorDamageTaken and HullDamageTaken stay in the engine's own maths and are not folded in. The multipliers are read from the live enum values recorded at load (`DamageTypeMults`), not from constants. For a 60% kinetic and 40% high explosive hit with no resistances, that gives shield 1.4, armor 1.1 and hull 1.0.

Checked: on non-shield hits the per-type DamageTaken is applied before the layer split, so d_t scales armor and hull alike.

A mixed hit is stronger against armor than two split hits of the same total. Armor reduction works on hit strength, and one hit keeps the full strength. That is intended. The conversion magnitudes stay placeholders.

### Nested hits and safety nets

- Nested hits. Another mod's target-side modifier can deal damage synchronously with `damage.getType()`, which is OTHER during the bracket. Arthr's Close Support Wings redirect is one. That nested hit has no `HitMix` entry. The blender gives it the outer hit's mix, blended for the nested target, and puts the outer blend back when the nested hit reports. The blends sit on a stack, so the depth doesn't matter. Nested hits of any type other than OTHER aren't affected.
- Our own side damage. `HitDamageRestorer.whileDealingSideDamage` also puts OTHER's defaults in place for its duration, and takes the blend back afterwards.
- Defaults. These are snapshotted at application load from the live enum. They are put back at the start of every combat frame, when combat ends, on game load, and at the start of any hit whose stack entry belongs to a hit that has already finished. A stuck blend therefore lasts one hit at most.
- Checked: every path through `Ship.applyDamage` that gets past the taken modifiers reaches `reportDamageApplied`, so only an exception can skip a report. The stale-hit check is therefore per frame: an open hit from an earlier frame is undone before the next hit lands.

### Conversions that use it

- `DamageConversion` gains a share. At 100%, which covers all three current mount conversions, the hit is still a plain retype and nothing is blended. Below 100%, the conversion writes a `HitMix` entry. The first-match rule in `DamageConversion.first` stays.
- EMP conversions don't need this. EMP isn't one of the three multipliers, and `HitDamageRestorer.moveToEmpForThisHit` can already move a share of a hit into EMP within that hit.
- Ion Spill (shield EMP to energy) and Ion Cascade (EMP to high explosive) change on any hit that carries damage. They move their share of the hit's EMP into its damage, as the converted type, in the same hit, and record the new shares. If the hit is already the converted type, only the damage is added and no mix is recorded. EMP-only hits skip every modifier. `EmpArcLandings` lands EMP-only engine arcs itself before the engine does (see step 10). Any other EMP-only hit still gets its bonus and conversion from `EmpOnlyHits` and `DeferredHits`, as a separate hit on the next frame.
- The Atlas scar high explosive conversion and the adaptive retype are full retypes. They read the mix through `HitMix`, and when they fire they replace it with a single type.
- Arc weapons get the same treatment: `ArcWeapons` traces an arc back to its weapon inside the conversion stages, so an arc that carries damage is converted like any other hit.

### Exiled Sector readers to switch to `HitMix`

Any of our listeners that reads `damage.getType()` during a hit must read the shares instead, and weight per-type effects by them:

- `weapon/DamageTypeListener`: per-type damage bonus percents, plus the fragmentation check through `originalHitType`.
- `charts/effect/ChartEnemyDamageListener`: per-type multipliers for chart enemies.
- `charts/officer/scar/ScarDamageTally`: splits its tally by share.
- `onhit/DamageConversionListener`, which covers both the conversion match and the adaptive retype, and `scar/HighExplosiveConversionListener`.
- `HitDamageRestorer.originalHitType` must never return OTHER for a mixed hit. Callers that need a single type get the largest share.
- `fighter/BomberPayloadDetonation` reads the type when the payload detonates. Confirm that this happens outside any bracket.

`HitMix` reads the attacker's conversions lazily and caches the result, so all of our readers agree, in whatever order they run.

- `fighter/BomberPayloadDetonation` reads the type in `advance`, outside any hit, so it needs no change.

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

### Mods that change a hit's amounts (steps 11 and 12)

All enabled mods in the C: install and every mod in `E:Devother mods` were scanned again for listeners that read or set base damage, EMP, the modifier or the type during a hit, for beam type readers, for arc handling and for damage dealt from inside a hit. Where a mod is in both places, the newer copy was scanned. No mod checks for EMP arcs, and no mod reuses a hit's damage object for another hit.

| Mod | Class | What happens |
| --- | --- | --- |
| Second-in-Command, Random Assortment of Things, Ship Mastery System, Arthr's Ships | `FocusedLensesListener`, `MoonlightLanceEffect`, `SeabreamEffect`, `BeamPartialHardFlux`, `ass_BlyezeeysDamageDealtMod` | They deal damage to the same target from inside the attacker's dealt stage. Step 12 keeps that hit from undoing the outer hit's conversion. `BeamPartialHardFlux` reads the outer hit's already converted EMP, so its side hit is a little larger with Ion Spill. |
| Unthemed Weapons Collection | `EnergyBallLauncherEffect$DamageListener` | On charged-ball hits it sets the base damage to its own charge, which drops our type scale for that hit. |
| Nightcross Armory | `NA_Hardlight$NA_AtomicDriverDmgBoost` | It sets armour hits to a flat 50, which drops our conversion and scale for that hit. |
| Ship Mastery System | `HitAngleDR$HitAngleDRScript` | Its percent goes on the modifier after our pool is read, so our increases multiply with it instead of adding. |
| Emergent Threats (Vice and IX Revival) | `FermionDecouplerEffect`, `LaceratorOnArmorHitEffect` | Their beam kill shots use the beam's type, which can be the retyped type for a frame after the beam hits a missile. |

Mods that set damage, EMP or type during a hit, such as VoK's `DisgustingResilience` ricochet, Arthr's `ass_DodgeStats` and the LOST_SECTOR beam reductions, keep their change after our restore, because the restorer keeps outside changes as a ratio.

### Installed but not enabled

- ExoticaTechnologies: `DaemonCore`'s dealt and taken listeners and `EqualizerCore` check the type, and `NanotechArmor` and `PenanceEngine` use OTHER. If it is enabled later, its taken listeners would join the affected table.
- UAF: `uaf_reflectiveArmor` (both versions) and `uaf_shieldedTorpedo` use OTHER. Check them again if it is enabled.
- Leftover folders without a `mod_info.json` are never loaded and were skipped: an older Detailed Combat Results, Console Commands, Second-in-Command 2.1.0 and a second Vaults of Knowledge.

## As built

- `HitMix` doesn't keep a per-`DamageAPI` map cleared with the restorer. Each hit is a `HitMix.Hit` recorded in the blender per frame, keyed by damage and target. Listeners that change the mix implement `HitMix.Resolver`; the first reader of a hit runs every resolver on the attacker once, then applies every conversion at once (step 11), and every later reader gets the cached hit.
- The blender closes a hit from `HitDamageRestorer.reportDamageApplied`, which runs before the restorer's own entries, and only when the report is for the top hit's target with an OTHER or empty result type. The restorer leaves entries of still-open hits alone.
- The frame reset is a combat plugin (`MixedHitBlender.FrameReset`). There is no separate combat-end hook: every combat start and game load puts the defaults back, and the frame reset covers everything in between.

- Every hit that reaches the blender's taken stage on a ship while a mixed hit is open is pushed, mixed or not, so each report closes its own entry. A report closes the top entry when the target matches and the result type is empty or the damage's current type, so a hit another mod retypes during the bracket still closes.
- A nested OTHER hit takes the open mix both from the blender and from `HitMix.resolve`, so a redirect whose source is the attacker (Arthr's Close Support Wings) is seeded with the mix before the attacker's resolvers run.
- A beam's damage object persists and a report on a target without ship stats isn't guaranteed, so the blender never brackets a beam there. Instead `HitMix` retypes the beam's damage to its largest share directly and remembers its own type, which it puts back before the beam's next ship hit and treats as the original type of every later hit.
- `ChartEnemyDamageListener` is bound to its ship, so arc hits (which name no source) still resolve through that ship's resolvers.
- Ion Cascade converts its share of the EMP that reaches the nearest disabled part, as a fraction of the hit's whole EMP. So the converted share still scales with the target's EMP and part damage-taken, as it did when it was a separate hit.
- Checked: the engine-wide taken modifiers are read in their own call after the dealt stage, so a blender registered during the first hit's dealt stage still sees that hit.

## Decisions

- A conversion moves damage. The converted share leaves its source (for Ion Spill and Ion Cascade, the hit's EMP) and becomes the new type in the same hit. Adding a share on top without removing it is "gained as extra", not a conversion.
- Conversion follows Grim Dawn, not Path of Exile: every conversion is applied once, all at the same time, from the hit's original split. Converted damage is never converted again and doesn't keep its old type's bonuses. Increases and more multipliers apply to the post-conversion amount of each type. Conversions that take more than 100% of one source are scaled down in proportion. There is no separate tier for skill and equipment conversions.
- Wording: "N% of X damage ... is converted to Y damage". The "also deals a separate hit of ..." strings from the arc work are replaced. EMP-only hits still get the deferred hit in code, and the tooltip doesn't mention it.
- The arc work in this checkout (`ArcWeapons` attribution) is folded into this plan once its current session is done: arcs that carry damage go through `HitMix` like any other hit, and nothing deals a converted share as its own arc or hit.

## Steps

1. Done. `HitMix` and the default snapshot, with the JUnit extension that restores the multipliers. `skilleffect/HitMix`, `TypeShares`, `DamageTypeMults`; test extension `DamageTypeMultsExtension`; defaults restored on game load in `ExiledSectorModPlugin`.
2. Done. The blend formula as a pure function, with its tests. `skilleffect/HitBlend`, `HitBlendTest`.
3. Done. `MixedHitBlender`: the engine-wide taken modifier and the report restore, the stack, the non-ship fallback and the safety nets, plus the `whileDealingSideDamage` change. `skilleffect/MixedHitBlender`, `HitDamageRestorer`; `MixedHitBlenderTest`, `HitMixTest`.
4. Done. A share on `DamageConversion` and partial conversions through `HitMix`. `onhit/DamageConversion`, `DamageConversionListener`.
5. Done. Ion Spill and Ion Cascade move EMP into the same hit on damage-carrying hits, and keep the deferred hit for EMP-only hits. The moved EMP is removed from the hit's EMP and added to its base damage in the MOVE_EMP stage, so the hit's damage bonuses scale it. `onhit/EmpConversionListener` (a `HitMix.Resolver`), `HitDamageRestorer.moveEmpToDamageForThisHit`; `IonSpillListenerTest`, `IonCascadeListenerTest`, `HitDamageRestorerTest`.
6. Done. The Exiled Sector readers switch to `HitMix`. `weapon/DamageTypeListener`, `scar/HighExplosiveConversionListener`, `charts/effect/ChartEnemyDamageListener`, `charts/officer/scar/ScarDamageTally`, with share-weighting tests in `DamageTypeListenerTest`, `ChartEnemyShipEffectsTest` and `ScarDamageTallyTest`.
7. Done. The wording: "N% of X damage ... is converted to Y damage" for Ion Spill and Ion Cascade, in en and zh_CN. `effect.SHIELD_EMP_TO_ENERGY_PERCENT` and `effect.DISABLED_PART_EMP_TO_HIGH_EXPLOSIVE_PERCENT` in `data/strings/exiledSector/core/`.
8. Done. The arc work is folded in. `DamageConversionListener` and `HighExplosiveConversionListener` find an arc's weapon through `ArcWeapons.weaponBehind` in their stages, and the only conversion still dealt separately is the EMP-only deferred hit, which has no damage to carry it. `skilleffect/ArcWeapons`, `ArcWeaponsTest`.
9. Done. The EMP damage bonus is its own stage, SCALE_EMP, between CONVERT and MOVE_EMP. Ion Spill and Ion Cascade therefore always move their share of the EMP after the bonus, whatever order the listeners were added in. `weapon/EmpDamageListener` (a `HitMix.Resolver`); stage order in `HitMixTest`.
10. Done. EMP-only arcs land in the frame they would have landed in anyway.
    - `EmpArcLandings` is an engine entity, added when the `EmpOnlyHits` router is created, so it advances before every arc added after it.
    - A dealer with EMP effects hits a ship with a projectile that has an on-hit effect. On the next frame, `EmpArcLandings` searches around that ship for engine arcs from that dealer that carry no damage and haven't landed.
    - For each one, it scales the arc's EMP by the bonus and moves the conversions' shares. It applies the result with the arc as the damage parameter, through `HitMix.applyResolvedUpToRetype`, so only the RETYPE stage runs on it.
    - A shield arc, or a hull arc whose EMP only grows, still lands itself with its impact sound, and only the difference is applied.
    - A hull arc whose EMP ends below its own is stopped (`didDamage` set) and replaced by the one combined hit, so it loses its impact sound.
    - The router ignores every hit from an arc this has evaluated, including arcs it left unchanged, so nothing is converted again after the arc's own EMP has disabled a part.
    - The applied hits carry the dealer's stats the way the arc does. A hit with damage gets them from the router's dealt listener (`DamageAPI.setStats`), so its damage-to-shields, hull, weapons and engines multipliers apply. A hit with only EMP runs no listeners, so `DealerComponentMults` sets the dealer's weapon and engine multipliers on the target's matching damage-taken stats for the length of the call; the engine multiplies both at the same point, so the result is the same. The next-frame extra EMP hit in `EmpDamageListener` does the same.
    - A shield arc's moved damage is skipped if the shield was lowered or turned away before the arc landed, as step 5's next-frame hits do.
    - The router watches a ship only for dealers whose EMP handlers this can account for.
    - A damage-taken listener on the target now sees an EMP-only arc whenever part of its EMP was moved into damage, because that hit carries damage. Vanilla never shows such a listener an EMP-only arc. Exiled Sector's own Arc Strike speed burst therefore also triggers on those hits.
    - These arcs fall back to step 5's next-frame hit: arcs it can't read (renamed fields), arcs near no watched ship, arcs from a dealer with EMP handlers it can't account for, and arcs from other sources.
    - `skilleffect/EmpArcLandings`, `DealerComponentMults`, `PrivateField.namedOfType`; `EmpArcLandingsTest`, `PrivateFieldTest`.
11. Done. Single-stage conversion and per-type scaling, replacing the stages of steps 5, 9 and 10.
    - Resolvers only file requests on the hit: `convert`, `convertToEmp`, `convertEmp` and `gainEmp` for conversions, `increase`, `more`, `increaseEmp` and `moreEmp` for scaling. After every resolver has run, `HitConversions` works out all conversions from the original amounts (damage types plus EMP) at once and scales a source's conversions down when they add up past 100%. Gains are added on top and don't count toward the cap. Ship targets get the new base damage and EMP through `HitDamageRestorer.setForThisHit`.
    - Adaptive and the scar high explosive conversion are ordinary 100% conversions, so they share the cap with a partial mount conversion instead of replacing it.
    - Scaling is applied at the end of the hit in `MixedHitBlender`. A type's factor is (G + its increases) / G times its more multipliers, where G is the hit's generic additive pool, so type increases pool with the hit's generic increases. A single-type ship hit scales its base damage, a mixed ship hit folds the factors into the carrier blend, and a non-ship target gets a modifier multiplier the engine removes after the hit. EMP is its own portion with its own increases and more multipliers, and type bonuses don't scale it.
    - `HitDamageRestorer` undoes only its own edits to base damage and EMP. It remembers what it last wrote, and a change another mod made after that is kept on restore: as a ratio, or added on when the value it wrote was 0. The type is still put back exactly.
    - EMP-only hits run no listeners, so G is 1 for them. `HitMix.resolveEmpOnly` runs the dealer's resolvers on an EMP-only arc; `EmpArcLandings` lands the leftover EMP and each moved damage type as their own hits, the moved ones already converted. Arcs it can't land keep their EMP unconverted and get only the EMP bonus, as a next-frame hit from `EmpOnlyHits`. `DeferredHits` and the next-frame conversions are gone.
    - `skilleffect/HitConversions`, `HitScaling`, `HitMix`, `MixedHitBlender`, `HitBlend`, `TypeShares.ofAmounts`, `HitDamageRestorer`, `EmpOnlyHits`, `EmpArcLandings`, and the listeners `weapon/DamageTypeListener`, `weapon/EmpDamageListener`, `onhit/DamageConversionListener`, `onhit/EmpConversionListener`, `onhit/BeamEmpListener`, `fighter/StrikecraftEmpListener`, `scar/HighExplosiveConversionListener`, `charts/effect/ChartEnemyDamageListener`; `HitConversionsTest`, `HitScalingTest` and the listener tests.
12. Done. Hits are resolved and scaled before other mods read them, and a hit dealt from inside another hit leaves the outer one alone.
    - `MixedHitBlender` is also an engine-wide dealt modifier, which the engine runs before the attacker ship's own. When the hit's parameter names a ship, it resolves the hit there and scales a single-type hit and its EMP, so the attacker's dealt listeners (lifesteal, charge counters) read the converted and scaled damage. Hits whose parameter names no ship still resolve in the attacker's own dealt stage, as before.
    - The hit remembers the scale it applied. At the taken stage the blender works the scale out again from the full generic pool and applies only the difference, so increases added in the attacker's dealt stage still pool with ours. Mixed ship hits are still blended at the taken stage only.
    - `HitDamageRestorer` counts the hits in progress: the blender's dealt stage starts one, and a report for the top hit's target and type ends it. A change is undone only by a report or a new hit at its own depth or deeper, so a hit another mod deals to the same target from inside a hit no longer undoes the outer hit's changes.
    - A hit with no damage runs no hooks, so it never starts. Its report is taken as the top hit's only if the top hit would deal no damage either or the report shows damage dealt; otherwise it counts as one level deeper.
    - `MixedHitBlender.FrameReset` undoes every change still pending and clears the count each frame, since no hit is in progress between frames.
    - The applied scale is forgotten when the hit lands and when a later resolver writes the converted amounts again, so the same damage object hitting the same target twice in a frame is scaled both times.
    - `skilleffect/MixedHitBlender`, `HitMix`, `HitDamageRestorer`; `MixedHitBlenderTest`, `HitDamageRestorerTest`.
