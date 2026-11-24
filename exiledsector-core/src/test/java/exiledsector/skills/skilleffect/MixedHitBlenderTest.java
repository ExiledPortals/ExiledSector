package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.DamageTypeMults.Mults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static exiledsector.skills.skilleffect.MixedHitCombat.damage;
import static exiledsector.skills.skilleffect.MixedHitCombat.mix;
import static exiledsector.skills.skilleffect.MixedHitCombat.ship;
import static exiledsector.skills.skilleffect.MixedHitCombat.trackedDamage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MixedHitBlenderTest {

    private static final float TOLERANCE = 1e-4f;
    private static final Mults SIXTY_FORTY_KINETIC_HIGH_EXPLOSIVE = new Mults(1.4f, 1.1f, 1f);

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();

    @RegisterExtension
    final DamageTypeMultsExtension typeMults = new DamageTypeMultsExtension();

    private MixedHitCombat mixedHits;
    private MixedHitBlender blender;

    @BeforeEach
    void setUp() {
        mixedHits = new MixedHitCombat(combat.engine());
        blender = mixedHits.blender();
    }

    private HitMix.Hit recordMix(DamageAPI damage, CombatEntityAPI target, TypeShares shares) {
        HitMix.Hit hit = HitMix.Hit.inheriting(null, target, damage, null, false, shares);
        blender.record(hit);
        return hit;
    }

    private static DamagingProjectileAPI shotFrom(HitMix.Resolver resolver) {
        ShipAPI attacker = mock(ShipAPI.class);
        when(attacker.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(attacker.getListeners(HitMix.Resolver.class)).thenReturn(List.of(resolver));
        DamagingProjectileAPI shot = mock(DamagingProjectileAPI.class);
        when(shot.getSource()).thenReturn(attacker);
        return shot;
    }

    private static void assertCarrier(Mults expected) {
        Mults carrier = DamageTypeMults.current(MixedHitBlender.CARRIER);
        assertEquals(expected.shield(), carrier.shield(), TOLERANCE);
        assertEquals(expected.armor(), carrier.armor(), TOLERANCE);
        assertEquals(expected.hull(), carrier.hull(), TOLERANCE);
    }

    private static void assertCarrierAtDefaults() {
        assertTrue(DamageTypeMults.atDefaults(MixedHitBlender.CARRIER), DamageTypeMults.current(MixedHitBlender.CARRIER).toString());
    }

    @Test
    void aMixedHitLandsAsTheCarrierWithBlendedMultipliersUntilItsReport() {
        ShipAPI target = ship();
        DamageAPI damage = damage(DamageType.KINETIC);
        recordMix(damage, target, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));

        blender.modifyDamageTaken(null, target, damage, null, false);

        assertEquals(MixedHitBlender.CARRIER, damage.getType());
        assertCarrier(SIXTY_FORTY_KINETIC_HIGH_EXPLOSIVE);

        mixedHits.report(target, MixedHitBlender.CARRIER);

        assertEquals(DamageType.KINETIC, damage.getType());
        assertCarrierAtDefaults();
    }

    @Test
    void aHitIsFoundOnlyForItsOwnDamageAndTarget() {
        ShipAPI target = ship();
        ShipAPI otherTarget = ship();
        DamageAPI damage = damage(DamageType.KINETIC);
        assertNull(blender.recordedHit(damage, target));

        HitMix.Hit hit = recordMix(damage, target, TypeShares.single(DamageType.KINETIC));

        assertSame(hit, blender.recordedHit(damage, target));
        assertNull(blender.recordedHit(damage, otherTarget));
        assertNull(blender.recordedHit(damage(DamageType.KINETIC), target));
    }

    @Test
    void aHitOfOneTypeIsLeftAlone() {
        ShipAPI target = ship();
        DamageAPI damage = damage(DamageType.KINETIC);
        recordMix(damage, target, TypeShares.single(DamageType.KINETIC));

        blender.modifyDamageTaken(null, target, damage, null, false);

        assertEquals(DamageType.KINETIC, damage.getType());
        assertCarrierAtDefaults();
    }

    @Test
    void aTargetWithoutShipStatsTakesTheLargestShareForTheHit() {
        CombatEntityAPI asteroid = mock(CombatEntityAPI.class);
        DamageAPI damage = damage(DamageType.KINETIC);
        recordMix(damage, asteroid, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.7f));

        blender.modifyDamageTaken(null, asteroid, damage, null, false);

        assertEquals(DamageType.HIGH_EXPLOSIVE, damage.getType());
        assertCarrierAtDefaults();

        mixedHits.restorer().reportDamageApplied(null, asteroid, null);

        assertEquals(DamageType.KINETIC, damage.getType());
    }

    @Test
    void aCarrierHitLandingInsideAMixedHitTakesTheSameMix() {
        ShipAPI target = ship();
        DamageAPI outer = damage(DamageType.KINETIC);
        DamageAPI inner = damage(MixedHitBlender.CARRIER);
        recordMix(outer, target, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        blender.modifyDamageTaken(null, target, outer, null, false);

        blender.modifyDamageTaken(null, target, inner, null, false);

        assertEquals(mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f), HitMix.recorded(inner, target));
        assertCarrier(SIXTY_FORTY_KINETIC_HIGH_EXPLOSIVE);

        mixedHits.report(target, MixedHitBlender.CARRIER);

        assertEquals(MixedHitBlender.CARRIER, outer.getType());
        assertCarrier(SIXTY_FORTY_KINETIC_HIGH_EXPLOSIVE);

        mixedHits.report(target, MixedHitBlender.CARRIER);

        assertEquals(DamageType.KINETIC, outer.getType());
        assertCarrierAtDefaults();
    }

    @Test
    void aCarrierHitTheAttackerResolvesInsideAMixedHitStillTakesTheMix() {
        ShipAPI target = ship();
        ShipAPI fighter = ship();
        DamageAPI outer = damage(DamageType.KINETIC);
        DamageAPI redirected = damage(MixedHitBlender.CARRIER);
        ShipAPI attacker = mock(ShipAPI.class);
        HitMix.Resolver untouched = hit -> {
        };
        when(attacker.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(attacker.getListeners(HitMix.Resolver.class)).thenReturn(List.of(untouched));
        recordMix(outer, target, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        blender.modifyDamageTaken(null, target, outer, null, false);

        HitMix.resolve(attacker, attacker, fighter, redirected, null, false);
        blender.modifyDamageTaken(attacker, fighter, redirected, null, false);

        assertEquals(mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f), HitMix.recorded(redirected, fighter));
        mixedHits.report(fighter, MixedHitBlender.CARRIER);
        assertEquals(MixedHitBlender.CARRIER, outer.getType());
        mixedHits.report(target, MixedHitBlender.CARRIER);
        assertEquals(DamageType.KINETIC, outer.getType());
        assertCarrierAtDefaults();
    }

    @Test
    void anUnmixedHitLandingInsideAMixedHitClosesOnlyItself() {
        ShipAPI target = ship();
        DamageAPI outer = damage(DamageType.KINETIC);
        recordMix(outer, target, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        blender.modifyDamageTaken(null, target, outer, null, false);

        blender.modifyDamageTaken(null, target, damage(DamageType.ENERGY), null, false);
        mixedHits.report(target, null);

        assertEquals(MixedHitBlender.CARRIER, outer.getType());
        assertCarrier(SIXTY_FORTY_KINETIC_HIGH_EXPLOSIVE);

        mixedHits.report(target, MixedHitBlender.CARRIER);

        assertEquals(DamageType.KINETIC, outer.getType());
        assertCarrierAtDefaults();
    }

    @Test
    void aMixedHitRetypedByAnotherModStillClosesOnItsReport() {
        ShipAPI target = ship();
        DamageAPI damage = damage(DamageType.KINETIC);
        recordMix(damage, target, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        blender.modifyDamageTaken(null, target, damage, null, false);
        damage.setType(DamageType.ENERGY);

        mixedHits.report(target, DamageType.ENERGY);

        assertEquals(DamageType.KINETIC, damage.getType());
        assertCarrierAtDefaults();
    }

    @Test
    void aBeamOnATargetWithoutShipStatsKeepsItsType() {
        CombatEntityAPI missile = mock(CombatEntityAPI.class);
        DamageAPI beamDamage = damage(DamageType.ENERGY);
        recordMix(beamDamage, missile, mix(DamageType.ENERGY, DamageType.HIGH_EXPLOSIVE, 0.6f));

        blender.modifyDamageTaken(mock(BeamAPI.class), missile, beamDamage, null, false);

        assertEquals(DamageType.ENERGY, beamDamage.getType());
    }

    @Test
    void aReportOfAnotherTypeLeavesTheMixedHitOpen() {
        ShipAPI target = ship();
        DamageAPI damage = damage(DamageType.KINETIC);
        recordMix(damage, target, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        blender.modifyDamageTaken(null, target, damage, null, false);

        mixedHits.report(target, DamageType.ENERGY);
        mixedHits.report(ship(), MixedHitBlender.CARRIER);

        assertEquals(MixedHitBlender.CARRIER, damage.getType());
        assertCarrier(SIXTY_FORTY_KINETIC_HIGH_EXPLOSIVE);
    }

    @Test
    void sideDamageDealtDuringAMixedHitUsesTheCarriersOwnMultipliers() {
        ShipAPI target = ship();
        DamageAPI damage = damage(DamageType.KINETIC);
        DamageAPI sideDamage = damage(MixedHitBlender.CARRIER);
        recordMix(damage, target, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        blender.modifyDamageTaken(null, target, damage, null, false);

        List<Boolean> atDefaultsDuringSideDamage = HitDamageRestorer.whileDealingSideDamage(() -> {
            blender.modifyDamageTaken(null, target, sideDamage, null, false);
            return List.of(DamageTypeMults.atDefaults(MixedHitBlender.CARRIER));
        });

        assertEquals(List.of(true), atDefaultsDuringSideDamage);
        assertEquals(TypeShares.single(MixedHitBlender.CARRIER), HitMix.recorded(sideDamage, target));
        assertCarrier(SIXTY_FORTY_KINETIC_HIGH_EXPLOSIVE);
    }

    @Test
    void aMixedHitWhoseReportNeverCameIsUndoneOnTheNextFrame() {
        ShipAPI target = ship();
        DamageAPI damage = damage(DamageType.KINETIC);
        recordMix(damage, target, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        blender.modifyDamageTaken(null, target, damage, null, false);

        mixedHits.nextFrame();
        blender.modifyDamageTaken(null, target, damage(DamageType.ENERGY), null, false);

        assertEquals(DamageType.KINETIC, damage.getType());
        assertCarrierAtDefaults();
    }

    @Test
    void theFrameResetUndoesOpenHitsAndStrayMultipliers() {
        ShipAPI target = ship();
        DamageAPI damage = damage(DamageType.KINETIC);
        recordMix(damage, target, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        blender.modifyDamageTaken(null, target, damage, null, false);
        DamageType.ENERGY.setHullMult(3f);

        new MixedHitBlender.FrameReset().advance(0.016f, List.of());

        assertEquals(DamageType.KINETIC, damage.getType());
        assertCarrierAtDefaults();
        assertTrue(DamageTypeMults.atDefaults(DamageType.ENERGY));
    }

    @Test
    void aTypeIncreaseOnASingleTypeShipHitScalesItsBaseDamageUntilItsReport() {
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);
        recordMix(damage, target, TypeShares.single(DamageType.KINETIC)).increase(DamageType.KINETIC, 50f);

        String removedAfterHit = blender.modifyDamageTaken(null, target, damage, null, false);

        assertNull(removedAfterHit);
        assertEquals(150f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(DamageType.KINETIC, damage.getType());

        mixedHits.report(target, DamageType.KINETIC);

        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);
    }

    @Test
    void aTypeIncreasePoolsWithTheGenericIncreasesAlreadyOnTheHit() {
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.ENERGY, 100f, 0f);
        damage.getModifier().modifyPercent("generic", 100f);
        recordMix(damage, target, TypeShares.single(DamageType.ENERGY)).increase(DamageType.ENERGY, 50f);

        blender.modifyDamageTaken(null, target, damage, null, false);

        assertEquals(125f, damage.getBaseDamage(), TOLERANCE);
    }

    @Test
    void aShotFromAShipWithHitEffectsIsScaledBeforeTheShipsOwnHooksRun() {
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 40f);
        DamagingProjectileAPI shot = shotFrom(hit -> {
            hit.increase(DamageType.KINETIC, 50f);
            hit.increaseEmp(100f);
        });

        String removedAfterHit = blender.modifyDamageDealt(shot, target, damage, null, false);

        assertNull(removedAfterHit);
        assertEquals(150f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(80f, damage.getFluxComponent(), TOLERANCE);

        blender.modifyDamageTaken(shot, target, damage, null, false);

        assertEquals(150f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(80f, damage.getFluxComponent(), TOLERANCE);

        mixedHits.report(target, DamageType.KINETIC);

        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(40f, damage.getFluxComponent(), TOLERANCE);
    }

    @Test
    void theSameShotHittingTheSameTargetAgainInOneFrameIsScaledAgain() {
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);
        DamagingProjectileAPI shot = shotFrom(hit -> hit.increase(DamageType.KINETIC, 50f));
        blender.modifyDamageDealt(shot, target, damage, null, false);
        blender.modifyDamageTaken(shot, target, damage, null, false);
        mixedHits.report(target, DamageType.KINETIC);

        blender.modifyDamageDealt(shot, target, damage, null, false);

        assertEquals(150f, damage.getBaseDamage(), TOLERANCE);
    }

    @Test
    void aSecondAttackerResolvingTheHitAfterTheEarlyScaleStillGetsItScaledWhenTaken() {
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);
        DamagingProjectileAPI shot = shotFrom(hit -> {
            hit.convert(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 1f);
            hit.increase(DamageType.HIGH_EXPLOSIVE, 50f);
        });
        ShipAPI otherSource = mock(ShipAPI.class);
        HitMix.Resolver otherSourcesResolver = hit -> { };
        when(otherSource.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(otherSource.getListeners(HitMix.Resolver.class)).thenReturn(List.of(otherSourcesResolver));

        blender.modifyDamageDealt(shot, target, damage, null, false);
        HitMix.resolve(otherSource, shot, target, damage, null, false);
        blender.modifyDamageTaken(shot, target, damage, null, false);

        assertEquals(DamageType.HIGH_EXPLOSIVE, damage.getType());
        assertEquals(150f, damage.getBaseDamage(), TOLERANCE);
    }

    @Test
    void anIncreaseAddedAfterTheEarlyScaleStillPoolsWithItWhenTheHitIsTaken() {
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.ENERGY, 100f, 0f);
        DamagingProjectileAPI shot = shotFrom(hit -> hit.increase(DamageType.ENERGY, 50f));

        blender.modifyDamageDealt(shot, target, damage, null, false);
        damage.getModifier().modifyPercent("addedByTheShipsOwnHook", 100f);
        blender.modifyDamageTaken(shot, target, damage, null, false);

        assertEquals(125f, damage.getBaseDamage(), TOLERANCE);

        mixedHits.report(target, DamageType.ENERGY);

        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);
    }

    @Test
    void anEarlyScaleOnATargetWithoutShipStatsIsUpdatedWhenTheHitIsTaken() {
        CombatEntityAPI asteroid = mock(CombatEntityAPI.class);
        DamageAPI damage = trackedDamage(DamageType.ENERGY, 100f, 0f);
        DamagingProjectileAPI shot = shotFrom(hit -> hit.increase(DamageType.ENERGY, 50f));

        assertEquals(MixedHitBlender.TYPE_SCALE_MOD_ID, blender.modifyDamageDealt(shot, asteroid, damage, null, false));
        assertEquals(1.5f, damage.getModifier().getModifiedValue(), TOLERANCE);

        damage.getModifier().modifyPercent("addedByTheShipsOwnHook", 100f);

        assertEquals(MixedHitBlender.TYPE_SCALE_MOD_ID, blender.modifyDamageTaken(shot, asteroid, damage, null, false));
        assertEquals(2.5f, damage.getModifier().getModifiedValue(), TOLERANCE);
    }

    @Test
    void aShotFromAShipWithoutHitEffectsIsLeftAlone() {
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 40f);
        DamagingProjectileAPI shot = mock(DamagingProjectileAPI.class);
        when(shot.getSource()).thenReturn(mock(ShipAPI.class));

        assertNull(blender.modifyDamageDealt(shot, target, damage, null, false));
        assertNull(blender.modifyDamageTaken(shot, target, damage, null, false));

        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(40f, damage.getFluxComponent(), TOLERANCE);
    }

    @Test
    void anIncreaseToAnotherTypeLeavesASingleTypeHitAlone() {
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);
        recordMix(damage, target, TypeShares.single(DamageType.KINETIC)).increase(DamageType.ENERGY, 50f);

        blender.modifyDamageTaken(null, target, damage, null, false);

        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);
    }

    @Test
    void aMixedShipHitFoldsEachTypesScaleIntoTheCarrierBlend() {
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);
        recordMix(damage, target, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f)).more(DamageType.KINETIC, 2f);

        blender.modifyDamageTaken(null, target, damage, null, false);

        assertEquals(MixedHitBlender.CARRIER, damage.getType());
        assertCarrier(new Mults(0.6f * 2f * 2f + 0.4f * 0.5f, 0.6f * 2f * 0.5f + 0.4f * 2f, 0.6f * 2f + 0.4f));
        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);

        mixedHits.report(target, MixedHitBlender.CARRIER);

        assertCarrierAtDefaults();
    }

    @Test
    void anEmpIncreaseScalesTheShipHitsEmpUntilItsReport() {
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.ENERGY, 100f, 40f);
        HitMix.Hit hit = recordMix(damage, target, TypeShares.single(DamageType.ENERGY));
        hit.increaseEmp(50f);
        hit.moreEmp(2f);

        blender.modifyDamageTaken(null, target, damage, null, false);

        assertEquals(120f, damage.getFluxComponent(), TOLERANCE);
        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);

        mixedHits.report(target, DamageType.ENERGY);

        assertEquals(40f, damage.getFluxComponent(), TOLERANCE);
    }

    @Test
    void aScaledHitOnATargetWithoutShipStatsScalesItsModifierForTheEngineToRemove() {
        CombatEntityAPI asteroid = mock(CombatEntityAPI.class);
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 40f);
        HitMix.Hit hit = recordMix(damage, asteroid, TypeShares.single(DamageType.KINETIC));
        hit.increase(DamageType.KINETIC, 50f);
        hit.increaseEmp(100f);

        String removedAfterHit = blender.modifyDamageTaken(null, asteroid, damage, null, false);

        assertEquals(MixedHitBlender.TYPE_SCALE_MOD_ID, removedAfterHit);
        assertEquals(1.5f, multOf(damage.getModifier(), removedAfterHit), TOLERANCE);
        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(40f, damage.getFluxComponent(), TOLERANCE);
    }

    @Test
    void aMixedHitOnATargetWithoutShipStatsScalesItsModifierByTheWeightedTypeScales() {
        CombatEntityAPI asteroid = mock(CombatEntityAPI.class);
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);
        recordMix(damage, asteroid, mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.7f)).increase(DamageType.HIGH_EXPLOSIVE, 100f);

        String removedAfterHit = blender.modifyDamageTaken(null, asteroid, damage, null, false);

        assertEquals(MixedHitBlender.TYPE_SCALE_MOD_ID, removedAfterHit);
        assertEquals(0.3f + 0.7f * 2f, multOf(damage.getModifier(), removedAfterHit), TOLERANCE);
        assertEquals(DamageType.HIGH_EXPLOSIVE, damage.getType());
    }

    @Test
    void anUnscaledHitOnATargetWithoutShipStatsLeavesItsModifierAlone() {
        CombatEntityAPI asteroid = mock(CombatEntityAPI.class);
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);
        recordMix(damage, asteroid, TypeShares.single(DamageType.KINETIC));

        assertNull(blender.modifyDamageTaken(null, asteroid, damage, null, false));
        assertTrue(damage.getModifier().getMultMods().isEmpty());
    }

    private static float multOf(MutableStat modifier, String id) {
        Map<String, MutableStat.StatMod> multMods = modifier.getMultMods();
        return multMods.get(id).getValue();
    }
}
