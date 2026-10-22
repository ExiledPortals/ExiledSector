package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WingSharedFateListenerTest {

    private static final float FLUX_CAPACITY = 200f;

    @SuppressWarnings("java:S101") // must end with Arthr's Ships' listener class name, which the share check matches by suffix
    private static final class fighterDamageNullifyer implements DamageTakenModifier {
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            return null;
        }
    }

    private static final class DamperFieldFightersScript implements DamageTakenModifier {
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            return null;
        }
    }

    private static MutableStat stat(float value) {
        MutableStat stat = mock(MutableStat.class);
        when(stat.getModifiedValue()).thenReturn(value);
        return stat;
    }

    private static ShipAPI fighter(float hitpoints) {
        return damagedFighter(hitpoints, hitpoints);
    }

    private static ShipAPI damagedFighter(float hitpoints, float maxHitpoints) {
        ShipAPI fighter = mock(ShipAPI.class);
        when(fighter.isAlive()).thenReturn(true);
        float[] hull = {hitpoints};
        when(fighter.getHitpoints()).thenAnswer(invocation -> hull[0]);
        doAnswer(invocation -> {
            hull[0] = invocation.getArgument(0);
            return null;
        }).when(fighter).setHitpoints(anyFloat());
        when(fighter.getMaxHitpoints()).thenReturn(maxHitpoints);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(flux.getMaxFlux()).thenReturn(FLUX_CAPACITY);
        when(fighter.getFluxTracker()).thenReturn(flux);
        when(fighter.getLocation()).thenReturn(new Vector2f());
        ShieldAPI shield = mock(ShieldAPI.class);
        when(shield.isOn()).thenReturn(true);
        when(shield.getFluxPerPointOfDamage()).thenReturn(1f);
        when(fighter.getShield()).thenReturn(shield);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat unchanged = stat(1f);
        when(stats.getHullDamageTakenMult()).thenReturn(unchanged);
        when(stats.getShieldDamageTakenMult()).thenReturn(unchanged);
        when(stats.getEnergyDamageTakenMult()).thenReturn(unchanged);
        when(stats.getProjectileDamageTakenMult()).thenReturn(unchanged);
        when(stats.getArmorDamageTakenMult()).thenReturn(unchanged);
        when(fighter.getMutableStats()).thenReturn(stats);
        return fighter;
    }

    private record Hit(WingSharedFateListener listener, ShipAPI hitFighter, Object projectile, DamageAPI damage) {
        Hit(WingSharedFateListener listener, ShipAPI hitFighter, Object projectile) {
            this(listener, hitFighter, projectile, freshDamage());
        }

        private static DamageAPI freshDamage() {
            DamageAPI damage = mock(DamageAPI.class);
            when(damage.getModifier()).thenReturn(mock(MutableStat.class));
            return damage;
        }

        String strikes(boolean shieldHit) {
            when(hitFighter.getParamAboutToApplyDamage()).thenReturn(projectile);
            return listener.modifyDamageTaken(projectile, hitFighter, damage, new Vector2f(), shieldHit);
        }

        void applies(float shieldFlux, float hullDamage) {
            ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
            when(result.getDamageToShields()).thenReturn(shieldFlux);
            when(result.getDamageToHull()).thenReturn(hullDamage);
            listener.reportDamageApplied(projectile, hitFighter, result);
        }

        void lands(float shieldFlux, float hullDamage) {
            strikes(shieldFlux > 0f);
            applies(shieldFlux, hullDamage);
        }

        void keeps(float keptFraction) {
            verify(damage.getModifier()).modifyMult(eq(WingSharedFateListener.SHARE_MOD_ID), about(keptFraction, 0.0001f));
        }
    }

    @Test
    void aHullHitAtLeastTheStruckFightersMaxHullIsNotShared() {
        ShipAPI hit = fighter(100f);
        ShipAPI wingmate = fighter(100f);
        ShipAPI otherWingmate = fighter(100f);
        wingOf(hit, wingmate, otherWingmate);

        new Hit(attachedTo(hit, 100f), hit, new Object()).lands(0f, 40f);

        verify(wingmate, never()).setHitpoints(anyFloat());
        verify(otherWingmate, never()).setHitpoints(anyFloat());
        verify(hit).setHitpoints(about(20f, 0.001f));
    }

    @Test
    void aShieldHitAtLeastTheStruckFightersFluxCapacityIsNotShared() {
        ShipAPI hit = fighter(100f);
        ShipAPI wingmate = fighter(100f);
        wingOf(hit, wingmate);

        new Hit(attachedTo(hit, 100f), hit, new Object()).lands(100f, 0f);

        verify(wingmate.getFluxTracker(), never()).increaseFlux(anyFloat(), eq(true));
        verify(hit.getFluxTracker()).increaseFlux(about(100f, 0.001f), eq(true));
    }

    @Test
    void aHitSmallerThanTheStruckFightersMaxHullIsShared() {
        ShipAPI hit = fighter(100f);
        ShipAPI wingmate = fighter(100f);
        wingOf(hit, wingmate);

        new Hit(attachedTo(hit, 100f), hit, new Object()).lands(0f, 45f);

        verify(wingmate).setHitpoints(about(55f, 0.001f));
    }

    private static List<ShipAPI> wingOf(ShipAPI... members) {
        List<ShipAPI> wingMembers = new ArrayList<>(List.of(members));
        FighterWingAPI wing = mock(FighterWingAPI.class);
        when(wing.getWingMembers()).thenReturn(wingMembers);
        for (ShipAPI member : members) {
            when(member.getWing()).thenReturn(wing);
        }
        return wingMembers;
    }

    private static WingSharedFateListener attachedTo(ShipAPI fighter, float sharedPercent) {
        ArgumentCaptor<Object> listener = ArgumentCaptor.forClass(Object.class);
        WingSharedFateListener.attach(fighter, sharedPercent);
        verify(fighter).addListener(listener.capture());
        return (WingSharedFateListener) listener.getValue();
    }

    private static CombatEngineAPI engineWithEverythingInPlay(MockedStatic<Global> global) {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.isEntityInPlay(any())).thenReturn(true);
        global.when(Global::getCombatEngine).thenReturn(engine);
        return engine;
    }

    @Test
    void sharingEverythingSplitsEachHitEvenlyAcrossTheWing() {
        WingSharedFateListener.Split split = WingSharedFateListener.split(100f, 2);

        assertEquals(1f / 3f, split.keptFraction(), 0.0001f);
        assertEquals(1f, split.perWingmateRatio(), 0.0001f, "each wingmate takes exactly what the hit fighter takes");
    }

    @Test
    void sharingHalfLeavesTheHitFighterWithMostOfTheDamage() {
        WingSharedFateListener.Split split = WingSharedFateListener.split(50f, 1);

        assertEquals(0.75f, split.keptFraction(), 0.0001f);
        assertEquals(1f / 3f, split.perWingmateRatio(), 0.0001f);
    }

    @Test
    void aLoneFighterKeepsTheWholeHit() {
        assertEquals(new WingSharedFateListener.Split(1f, 0f), WingSharedFateListener.split(100f, 0));
    }

    @Test
    void aHitIsCutDownAndItsShieldAndHullDamagePassedToEveryWingmate() {
        ShipAPI hit = fighter(300f);
        ShipAPI near = fighter(300f);
        ShipAPI far = fighter(300f);
        wingOf(hit, near, far);
        Hit shieldHit = new Hit(attachedTo(hit, 100f), hit, new Object());

        assertEquals(WingSharedFateListener.SHARE_MOD_ID, shieldHit.strikes(true));
        shieldHit.applies(40f, 0f);
        new Hit(shieldHit.listener(), hit, new Object()).lands(0f, 30f);

        shieldHit.keeps(1f / 3f);
        for (ShipAPI wingmate : List.of(near, far)) {
            verify(wingmate.getFluxTracker()).increaseFlux(about(40f, 0.001f), eq(true));
            verify(wingmate).setHitpoints(about(270f, 0.001f));
        }
    }

    @Test
    void aFighterWhoseWingmatesAreGoneTakesTheHitAlone() {
        ShipAPI hit = fighter(300f);
        ShipAPI destroyed = fighter(0f);
        when(destroyed.isAlive()).thenReturn(false);
        wingOf(hit, destroyed);
        Hit lone = new Hit(attachedTo(hit, 100f), hit, new Object());

        assertNull(lone.strikes(false));
        verify(lone.damage().getModifier(), never()).modifyMult(any(), anyFloat());
    }

    @Test
    void aLethalShareKillsTheWingmateWithABlowSizedToItAndCreditedToTheOriginalAttacker() {
        ShipAPI hit = fighter(300f);
        ShipAPI wingmate = damagedFighter(20f, 100f);
        ArmorGridAPI armor = mock(ArmorGridAPI.class);
        when(armor.getArmorRating()).thenReturn(50f);
        when(wingmate.getArmorGrid()).thenReturn(armor);
        wingOf(hit, wingmate);
        WingNumbersDamageListener.attach(wingmate, 10f);
        ArgumentCaptor<Object> wingNumbers = ArgumentCaptor.forClass(Object.class);
        verify(wingmate).addListener(wingNumbers.capture());
        ShipAPI attacker = mock(ShipAPI.class);
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        when(projectile.getSource()).thenReturn(attacker);
        AtomicBoolean wingNumbersStoodAside = new AtomicBoolean();

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            CombatEngineAPI engine = engineWithEverythingInPlay(global);
            doAnswer(invocation -> {
                DamageAPI blow = mock(DamageAPI.class);
                wingNumbersStoodAside.set(WingSharedFateListener.isApplyingSharedDamage()
                        && ((WingNumbersDamageListener) wingNumbers.getValue()).modifyDamageTaken(null, wingmate, blow, new Vector2f(), false) == null);
                return null;
            }).when(engine).applyDamage(any(CombatEntityAPI.class), any(Vector2f.class), anyFloat(), any(DamageType.class), anyFloat(), anyBoolean(), anyBoolean(), any());

            new Hit(attachedTo(hit, 100f), hit, projectile).lands(0f, 30f);

            verify(wingmate).setHitpoints(WingSharedFateListener.KILLING_BLOW_HULL_LEFT);
            verify(engine).applyDamage(eq(wingmate), any(Vector2f.class), about(3f * (WingSharedFateListener.KILLING_BLOW_HULL_LEFT + 50f), 0.01f),
                    eq(DamageType.ENERGY), eq(0f), eq(true), eq(false), eq(attacker));
        }
        assertTrue(wingNumbersStoodAside.get(), "the wing's damage reduction was already applied to the shared hit");
        assertFalse(WingSharedFateListener.isApplyingSharedDamage());
    }

    @Test
    void theKillingBlowGrowsToBeatTheWingmatesDamageResistance() {
        ShipAPI wingmate = damagedFighter(40f, 100f);
        MutableStat halved = stat(0.5f);
        when(wingmate.getMutableStats().getHullDamageTakenMult()).thenReturn(halved);
        when(wingmate.getMutableStats().getEnergyDamageTakenMult()).thenReturn(halved);

        assertEquals(3f * 40f / 0.25f, WingSharedFateListener.killingBlowDamage(wingmate), 0.01f);
    }

    @Test
    void theKillingBlowGrowsToCutThroughArmourThatTakesLessDamage() {
        ShipAPI wingmate = damagedFighter(10f, 100f);
        ArmorGridAPI armor = mock(ArmorGridAPI.class);
        when(armor.getArmorRating()).thenReturn(100f);
        when(wingmate.getArmorGrid()).thenReturn(armor);
        MutableStat halved = stat(0.5f);
        when(wingmate.getMutableStats().getArmorDamageTakenMult()).thenReturn(halved);

        assertEquals(3f * (10f + 100f / 0.5f), WingSharedFateListener.killingBlowDamage(wingmate), 0.01f);
    }

    @Test
    void aStatThatMakesTheWingmateImmuneDoesNotTurnTheKillingBlowIntoNothing() {
        ShipAPI wingmate = damagedFighter(10f, 100f);
        MutableStat immune = stat(0f);
        when(wingmate.getMutableStats().getEnergyDamageTakenMult()).thenReturn(immune);

        assertEquals(30f, WingSharedFateListener.killingBlowDamage(wingmate), 0.01f);
    }

    @Test
    void aFighterUnderShieldsistersProjectedShieldingLetsHerTakeTheWholeHit() {
        ShipAPI hit = fighter(300f);
        ShipAPI wingmate = fighter(300f);
        wingOf(hit, wingmate);
        when(hit.getListeners(DamageTakenModifier.class)).thenReturn(List.of(new fighterDamageNullifyer()));
        Hit shielded = new Hit(attachedTo(hit, 100f), hit, new Object());

        assertNull(shielded.strikes(false));
        shielded.applies(0f, 30f);

        verify(shielded.damage().getModifier(), never()).modifyMult(any(), anyFloat());
        verify(wingmate, never()).setHitpoints(anyFloat());
    }

    @Test
    void wingmatesTheOtherModsProtectAreLeftOutOfTheSplit() {
        ShipAPI hit = fighter(300f);
        ShipAPI underShieldsister = fighter(300f);
        ShipAPI underDamperFloor = fighter(300f);
        ShipAPI exposed = fighter(300f);
        wingOf(hit, underShieldsister, underDamperFloor, exposed);
        when(underShieldsister.getListeners(DamageTakenModifier.class)).thenReturn(List.of(new fighterDamageNullifyer()));
        when(underDamperFloor.getListeners(DamageTakenModifier.class)).thenReturn(List.of(new DamperFieldFightersScript()));
        ShipAPI carrier = mock(ShipAPI.class);
        ShipSystemAPI damper = mock(ShipSystemAPI.class);
        when(damper.isActive()).thenReturn(true);
        when(carrier.getSystem()).thenReturn(damper);
        when(underDamperFloor.getWing().getSourceShip()).thenReturn(carrier);
        Hit hitOnExposedWing = new Hit(attachedTo(hit, 100f), hit, new Object());

        hitOnExposedWing.lands(0f, 30f);

        hitOnExposedWing.keeps(0.5f);
        verify(exposed).setHitpoints(about(270f, 0.001f));
        verify(underShieldsister, never()).setHitpoints(anyFloat());
        verify(underDamperFloor, never()).setHitpoints(anyFloat());
    }

    @Test
    void phasedDockingLaunchingAndOutOfPlayWingmatesAreLeftOutOfTheSplit() {
        ShipAPI hit = fighter(300f);
        ShipAPI phased = fighter(300f);
        when(phased.isPhased()).thenReturn(true);
        ShipAPI landing = fighter(300f);
        when(landing.isLanding()).thenReturn(true);
        ShipAPI liftingOff = fighter(300f);
        when(liftingOff.isLiftingOff()).thenReturn(true);
        ShipAPI docked = fighter(300f);
        when(docked.isFinishedLanding()).thenReturn(true);
        ShipAPI removed = fighter(300f);
        ShipAPI flying = fighter(300f);
        wingOf(hit, phased, landing, liftingOff, docked, removed, flying);

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            CombatEngineAPI engine = engineWithEverythingInPlay(global);
            when(engine.isEntityInPlay(removed)).thenReturn(false);
            Hit hitOnPhaseWing = new Hit(attachedTo(hit, 100f), hit, new Object());

            hitOnPhaseWing.lands(0f, 30f);

            hitOnPhaseWing.keeps(0.5f);
        }
        verify(flying).setHitpoints(about(270f, 0.001f));
        for (ShipAPI absent : List.of(phased, landing, liftingOff, docked, removed)) {
            verify(absent, never()).setHitpoints(anyFloat());
        }
    }

    @Test
    void aShieldHitIsSharedOnlyWithWingmatesWhoseShieldsAreUp() {
        ShipAPI hit = fighter(300f);
        ShipAPI shieldsDown = fighter(300f);
        when(shieldsDown.getShield().isOn()).thenReturn(false);
        ShipAPI overloaded = fighter(300f);
        when(overloaded.getFluxTracker().isOverloadedOrVenting()).thenReturn(true);
        ShipAPI unshielded = fighter(300f);
        when(unshielded.getShield()).thenReturn(null);
        ShipAPI raised = fighter(300f);
        wingOf(hit, shieldsDown, overloaded, unshielded, raised);
        Hit shieldHit = new Hit(attachedTo(hit, 100f), hit, new Object());

        shieldHit.lands(40f, 0f);

        shieldHit.keeps(0.5f);
        verify(raised.getFluxTracker()).increaseFlux(about(40f, 0.001f), eq(true));
        for (ShipAPI left : List.of(shieldsDown, overloaded, unshielded)) {
            verify(left.getFluxTracker(), never()).increaseFlux(anyFloat(), anyBoolean());
        }
    }

    @Test
    void aWingmateTakesOnlyTheFluxItCanHoldAndTheRestStaysWithTheStruckFighter() {
        ShipAPI hit = fighter(300f);
        ShipAPI nearlyFull = fighter(300f);
        when(nearlyFull.getFluxTracker().getCurrFlux()).thenReturn(190f);
        wingOf(hit, nearlyFull);

        new Hit(attachedTo(hit, 100f), hit, new Object()).lands(40f, 0f);

        float spare = FLUX_CAPACITY - 190f - WingSharedFateListener.OVERLOAD_MARGIN;
        verify(nearlyFull.getFluxTracker()).increaseFlux(about(spare, 0.001f), eq(true));
        verify(hit.getFluxTracker()).increaseFlux(about(40f - spare, 0.001f), eq(true));
    }

    @Test
    void aWingmateWithWeakerShieldsTakesMoreFluxForTheSameShare() {
        ShipAPI hit = fighter(300f);
        ShipAPI weakShields = fighter(300f);
        when(weakShields.getShield().getFluxPerPointOfDamage()).thenReturn(2f);
        wingOf(hit, weakShields);

        new Hit(attachedTo(hit, 100f), hit, new Object()).lands(40f, 0f);

        verify(weakShields.getFluxTracker()).increaseFlux(about(80f, 0.001f), eq(true));
    }

    @Test
    void sharedHullIsScaledByEachWingmatesHullDamageTaken() {
        ShipAPI hit = fighter(300f);
        ShipAPI armoured = fighter(300f);
        MutableStat halved = stat(0.5f);
        when(armoured.getMutableStats().getHullDamageTakenMult()).thenReturn(halved);
        ShipAPI invulnerable = fighter(300f);
        MutableStat none = stat(0f);
        when(invulnerable.getMutableStats().getHullDamageTakenMult()).thenReturn(none);
        wingOf(hit, armoured, invulnerable);
        Hit hitOnMixedWing = new Hit(attachedTo(hit, 100f), hit, new Object());

        hitOnMixedWing.lands(0f, 30f);

        hitOnMixedWing.keeps(0.5f);
        verify(armoured).setHitpoints(about(285f, 0.001f));
        verify(invulnerable, never()).setHitpoints(anyFloat());
    }

    @Test
    void aHitThatWouldDestroyASmallerWingmateOutrightIsNotSharedWithIt() {
        ShipAPI hit = fighter(300f);
        ShipAPI small = fighter(50f);
        ShipAPI big = fighter(300f);
        wingOf(hit, small, big);

        new Hit(attachedTo(hit, 100f), hit, new Object()).lands(0f, 30f);

        verify(small, never()).setHitpoints(anyFloat());
        verify(big).setHitpoints(about(270f, 0.001f));
        verify(hit).setHitpoints(about(270f, 0.001f));
    }

    @Test
    void aLowCapacityWingmateIsNotHandedAShieldHitBiggerThanItsFluxPool() {
        ShipAPI hit = fighter(300f);
        ShipAPI smallPool = fighter(300f);
        when(smallPool.getFluxTracker().getMaxFlux()).thenReturn(60f);
        wingOf(hit, smallPool);

        new Hit(attachedTo(hit, 100f), hit, new Object()).lands(40f, 0f);

        verify(smallPool.getFluxTracker(), never()).increaseFlux(anyFloat(), anyBoolean());
        verify(hit.getFluxTracker()).increaseFlux(about(40f, 0.001f), eq(true));
    }

    @Test
    void empIsLeftWholeOnTheStruckFighterAndRestoredAfterTheHit() {
        ShipAPI hit = fighter(300f);
        wingOf(hit, fighter(300f));
        Hit empHit = new Hit(attachedTo(hit, 100f), hit, new Object());
        when(empHit.damage().getFluxComponent()).thenReturn(60f);

        empHit.lands(0f, 30f);

        InOrder order = inOrder(empHit.damage());
        order.verify(empHit.damage()).setFluxComponent(about(120f, 0.001f));
        order.verify(empHit.damage()).setFluxComponent(about(60f, 0.001f));
    }

    @Test
    void theTooBigRefundIsNeverDealtToAFighterTheHitAlreadyDestroyed() {
        ShipAPI hit = fighter(100f);
        ShipAPI wingmate = fighter(100f);
        wingOf(hit, wingmate);
        Hit lethal = new Hit(attachedTo(hit, 100f), hit, new Object());

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            CombatEngineAPI engine = engineWithEverythingInPlay(global);
            lethal.strikes(false);
            when(hit.getHitpoints()).thenReturn(0f);
            lethal.applies(0f, 100f);

            verify(engine, never()).applyDamage(any(CombatEntityAPI.class), any(Vector2f.class), anyFloat(), any(DamageType.class), anyFloat(), anyBoolean(), anyBoolean(), any());
        }
        verify(hit, never()).setHitpoints(anyFloat());
        verify(wingmate, never()).setHitpoints(anyFloat());
    }

    private static float about(float value, float tolerance) {
        return org.mockito.AdditionalMatchers.eq(value, tolerance);
    }
}
