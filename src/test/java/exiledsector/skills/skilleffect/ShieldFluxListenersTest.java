package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShieldFluxListenersTest {

    private static final String HARD_FLUX_PERCENT_KEY = "exiledSector_beamDamageHardFluxPercent";
    private static final String SHARED_PERCENT_KEY = "exiledSector_shieldDamageSharedPercent";

    private final List<ShipAPI> ships = new ArrayList<>();
    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;
    private CollisionGridAPI shipGrid;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getShips()).thenReturn(ships);
        shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<Object>(ships).iterator());
        when(engine.getTotalElapsedTime(false)).thenReturn(5f);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(String statKey, float statValue) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(statKey, 0f)).thenReturn(statValue);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f(0f, 0f));
        when(ship.getFluxTracker()).thenReturn(mock(FluxTrackerAPI.class));
        return ship;
    }

    private static ShipAPI shipAt(float x, int owner) {
        ShipAPI ship = ship(SHARED_PERCENT_KEY, 0f);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getOwner()).thenReturn(owner);
        return ship;
    }

    private static <T> T attachedListener(ShieldSkillEffect effect, ShipAPI ship, Class<T> type) {
        effect.applyAfterShipCreation(ship, "mod_id", 0f);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ship).addListener(captor.capture());
        return type.cast(captor.getValue());
    }

    private static ApplyDamageResultAPI shieldFlux(float flux) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getDamageToShields()).thenReturn(flux);
        return result;
    }

    private static DamageAPI damageWithModifier(MutableStat modifier) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(modifier);
        return damage;
    }

    @Test
    void highScatterAmpNeverTouchesTheBeamsDamageAndInstallsAConverterOnTheShipItHits() {
        DamageDealtModifier listener = attachedListener(ShieldSkillEffect.BEAM_WEAPON_HARD_FLUX_PERCENT,
                ship(HARD_FLUX_PERCENT_KEY, 50f), DamageDealtModifier.class);
        ShipAPI target = mock(ShipAPI.class);
        DamageAPI damage = mock(DamageAPI.class);

        assertNull(listener.modifyDamageDealt(mock(BeamAPI.class), target, damage, new Vector2f(), true));

        verifyNoInteractions(damage);
        verify(target).addListener(any(DamageListener.class));
    }

    @Test
    void highScatterAmpIgnoresHullHitsNonBeamsAndNonShipTargets() {
        DamageDealtModifier listener = attachedListener(ShieldSkillEffect.BEAM_WEAPON_HARD_FLUX_PERCENT,
                ship(HARD_FLUX_PERCENT_KEY, 50f), DamageDealtModifier.class);
        ShipAPI target = mock(ShipAPI.class);

        listener.modifyDamageDealt(mock(BeamAPI.class), target, mock(DamageAPI.class), new Vector2f(), false);
        listener.modifyDamageDealt(new Object(), target, mock(DamageAPI.class), new Vector2f(), true);
        listener.modifyDamageDealt(mock(BeamAPI.class), mock(CombatEntityAPI.class), mock(DamageAPI.class), new Vector2f(), true);

        verify(target, never()).addListener(any());
    }

    private DamageListener converterOn(ShipAPI target) {
        DamageDealtModifier listener = attachedListener(ShieldSkillEffect.BEAM_WEAPON_HARD_FLUX_PERCENT,
                ship(HARD_FLUX_PERCENT_KEY, 50f), DamageDealtModifier.class);
        listener.modifyDamageDealt(mock(BeamAPI.class), target, mock(DamageAPI.class), new Vector2f(), true);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(target).addListener(captor.capture());
        return (DamageListener) captor.getValue();
    }

    private static BeamAPI beamFrom(ShipAPI source, boolean forceHardFlux) {
        BeamAPI beam = mock(BeamAPI.class);
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.isForceHardFlux()).thenReturn(forceHardFlux);
        when(beam.getDamage()).thenReturn(damage);
        when(beam.getSource()).thenReturn(source);
        return beam;
    }

    @Test
    void theConverterTurnsTheStatedShareOfTheShieldFluxTheBeamDealtIntoHardFlux() {
        ShipAPI target = mock(ShipAPI.class);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(target.getFluxTracker()).thenReturn(flux);
        when(flux.getCurrFlux()).thenReturn(300f);
        when(flux.getHardFlux()).thenReturn(100f);

        DamageListener converter = converterOn(target);
        ShipAPI attacker = ship(HARD_FLUX_PERCENT_KEY, 50f);
        BeamAPI beam = beamFrom(attacker, false);
        when(target.getParamAboutToApplyDamage()).thenReturn(beam);

        converter.reportDamageApplied(attacker, target, shieldFlux(40f));

        verify(flux).setHardFlux(120f);
        verify(flux, never()).increaseFlux(anyFloat(), anyBoolean());
    }

    @Test
    void theConverterSkipsTheZeroDamageFramesBetweenBeamTicksWithoutLookingAtTheBeam() {
        ShipAPI target = mock(ShipAPI.class);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(target.getFluxTracker()).thenReturn(flux);
        DamageListener converter = converterOn(target);

        converter.reportDamageApplied(new Object(), target, shieldFlux(0f));

        verify(target, never()).getParamAboutToApplyDamage();
        verifyNoInteractions(flux);
    }

    @Test
    void theConverterStandsDownWhileASimulatedSplitHitIsBeingApplied() {
        ShipAPI target = mock(ShipAPI.class);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(target.getFluxTracker()).thenReturn(flux);
        DamageListener converter = converterOn(target);
        ShipAPI attacker = ship(HARD_FLUX_PERCENT_KEY, 50f);
        BeamAPI beam = beamFrom(attacker, false);
        when(target.getParamAboutToApplyDamage()).thenReturn(beam);
        BeamSplitListener.beginSimulatedHit();
        try {
            converter.reportDamageApplied(attacker, target, shieldFlux(40f));
        } finally {
            BeamSplitListener.endSimulatedHit();
        }

        verify(flux, never()).setHardFlux(anyFloat());
    }

    @Test
    void theConverterNeverMakesHardFluxExceedTheTotalFlux() {
        ShipAPI target = mock(ShipAPI.class);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(target.getFluxTracker()).thenReturn(flux);
        when(flux.getCurrFlux()).thenReturn(110f);
        when(flux.getHardFlux()).thenReturn(100f);

        DamageListener converter = converterOn(target);
        ShipAPI attacker = ship(HARD_FLUX_PERCENT_KEY, 50f);
        BeamAPI beam = beamFrom(attacker, false);
        when(target.getParamAboutToApplyDamage()).thenReturn(beam);

        converter.reportDamageApplied(attacker, target, shieldFlux(40f));

        verify(flux).setHardFlux(110f);
    }

    @Test
    void theConverterLeavesOtherBeamsAndHitsAlone() {
        ShipAPI target = mock(ShipAPI.class);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(target.getFluxTracker()).thenReturn(flux);
        DamageListener converter = converterOn(target);

        ShipAPI withoutSkill = ship(HARD_FLUX_PERCENT_KEY, 0f);
        ShipAPI withSkill = ship(HARD_FLUX_PERCENT_KEY, 50f);

        BeamAPI unskilledBeam = beamFrom(withoutSkill, false);
        when(target.getParamAboutToApplyDamage()).thenReturn(unskilledBeam);
        converter.reportDamageApplied(withoutSkill, target, shieldFlux(40f));
        BeamAPI hardBeam = beamFrom(withSkill, true);
        when(target.getParamAboutToApplyDamage()).thenReturn(hardBeam);
        converter.reportDamageApplied(withSkill, target, shieldFlux(40f));
        when(target.getParamAboutToApplyDamage()).thenReturn(new Object());
        converter.reportDamageApplied(withSkill, target, shieldFlux(40f));

        verify(flux, never()).setHardFlux(anyFloat());
    }

    @Test
    void sharedFateMovesTheStatedShareSplitEvenlyAcrossNearbyAlliedShipsOnly() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ShipAPI allyA = shipAt(300f, 0);
        ShipAPI allyB = shipAt(-500f, 0);
        ShipAPI fighter = shipAt(100f, 0);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI farAlly = shipAt(1500f, 0);
        ShipAPI enemy = shipAt(200f, 1);
        ships.addAll(List.of(defender, allyA, allyB, fighter, farAlly, enemy));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);
        MutableStat modifier = new MutableStat(1f);
        Object projectile = new Object();

        when(defender.getParamAboutToApplyDamage()).thenReturn(projectile);

        String modifierId = listener.modifyDamageTaken(projectile, defender, damageWithModifier(modifier), new Vector2f(), true);
        ((DamageListener) listener).reportDamageApplied(enemy, defender, shieldFlux(80f));

        assertEquals(0.8f, modifier.getModifiedValue(), 1e-6f);
        modifier.unmodify(modifierId);
        assertEquals(1f, modifier.getModifiedValue(), 1e-6f);
        verify(allyA.getFluxTracker()).increaseFlux(10f, true);
        verify(allyB.getFluxTracker()).increaseFlux(10f, true);
        verifyNoInteractions(fighter.getFluxTracker(), farAlly.getFluxTracker(), enemy.getFluxTracker());
    }

    @Test
    void sharedFateSkipsAlliesThatAreOverloadedOrVenting() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ShipAPI ready = shipAt(300f, 0);
        ShipAPI busy = shipAt(-300f, 0);
        when(busy.getFluxTracker().isOverloadedOrVenting()).thenReturn(true);
        ships.addAll(List.of(defender, ready, busy));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);
        Object projectile = new Object();
        when(defender.getParamAboutToApplyDamage()).thenReturn(projectile);

        listener.modifyDamageTaken(projectile, defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        ((DamageListener) listener).reportDamageApplied(projectile, defender, shieldFlux(80f));

        verify(ready.getFluxTracker()).increaseFlux(20f, true);
        verify(busy.getFluxTracker(), never()).increaseFlux(anyFloat(), anyBoolean());
    }

    @Test
    void sharedFateLeavesTheHitAloneWithNoAlliesNearbyOrOnHullHits() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ships.add(defender);
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);
        DamageAPI damage = mock(DamageAPI.class);

        assertNull(listener.modifyDamageTaken(new Object(), defender, damage, new Vector2f(), true));
        ships.add(shipAt(300f, 0));
        assertNull(listener.modifyDamageTaken(new Object(), defender, damage, new Vector2f(), false));

        verifyNoInteractions(damage);
    }

    @Test
    void sharedFateOnlySharesTheHitItReduced() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ShipAPI ally = shipAt(300f, 0);
        ships.addAll(List.of(defender, ally));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);

        Object reducedHit = new Object();
        when(defender.getParamAboutToApplyDamage()).thenReturn(reducedHit);
        listener.modifyDamageTaken(reducedHit, defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        when(defender.getParamAboutToApplyDamage()).thenReturn(new Object());
        ((DamageListener) listener).reportDamageApplied(new Object(), defender, shieldFlux(80f));

        verify(ally.getFluxTracker(), never()).increaseFlux(anyFloat(), anyBoolean());
    }

    @Test
    void sharedFateLooksForAlliesOncePerFrame() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ships.addAll(List.of(defender, shipAt(300f, 0)));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);

        listener.modifyDamageTaken(new Object(), defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        listener.modifyDamageTaken(new Object(), defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        when(engine.getTotalElapsedTime(false)).thenReturn(5.1f);
        listener.modifyDamageTaken(new Object(), defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);

        verify(shipGrid, times(2)).getCheckIterator(any(), anyFloat(), anyFloat());
    }
}
