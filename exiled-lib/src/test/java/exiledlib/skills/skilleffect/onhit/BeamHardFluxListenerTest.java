package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledlib.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BeamHardFluxListenerTest {

    private static final String HARD_FLUX_PERCENT_KEY = "exiledLib_beamDamageHardFluxPercent";

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

    private static <T> T attachedListener(SkillEffect effect, ShipAPI ship, Class<T> type) {
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

    @Test
    void highScatterAmpNeverTouchesTheBeamsDamageAndInstallsAConverterOnTheShipItHits() {
        DamageDealtModifier listener = attachedListener(OnHitSkillEffect.BEAM_WEAPON_HARD_FLUX_PERCENT,
                ship(HARD_FLUX_PERCENT_KEY, 50f), DamageDealtModifier.class);
        ShipAPI target = mock(ShipAPI.class);
        DamageAPI damage = mock(DamageAPI.class);

        assertNull(listener.modifyDamageDealt(mock(BeamAPI.class), target, damage, new Vector2f(), true));

        verifyNoInteractions(damage);
        verify(target).addListener(any(DamageListener.class));
    }

    @Test
    void highScatterAmpIgnoresHullHitsNonBeamsAndNonShipTargets() {
        DamageDealtModifier listener = attachedListener(OnHitSkillEffect.BEAM_WEAPON_HARD_FLUX_PERCENT,
                ship(HARD_FLUX_PERCENT_KEY, 50f), DamageDealtModifier.class);
        ShipAPI target = mock(ShipAPI.class);

        listener.modifyDamageDealt(mock(BeamAPI.class), target, mock(DamageAPI.class), new Vector2f(), false);
        listener.modifyDamageDealt(new Object(), target, mock(DamageAPI.class), new Vector2f(), true);
        listener.modifyDamageDealt(mock(BeamAPI.class), mock(CombatEntityAPI.class), mock(DamageAPI.class), new Vector2f(), true);

        verify(target, never()).addListener(any());
    }

    private DamageListener converterOn(ShipAPI target) {
        DamageDealtModifier listener = attachedListener(OnHitSkillEffect.BEAM_WEAPON_HARD_FLUX_PERCENT,
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
}
