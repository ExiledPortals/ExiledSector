package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbsorbReserveListenerTest {

    private final List<Object> gridShips = new ArrayList<>();

    private MockedStatic<Global> globalMock;

    private CombatEngineAPI combatEngine;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        combatEngine = engine;
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridShips).iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(String id, float x, Map<String, Float> dynamicValues) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getId()).thenReturn(id);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        dynamicValues.forEach((key, value) -> when(dynamic.getValue(key, 0f)).thenReturn(value));
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getVelocity()).thenReturn(new Vector2f());
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static ApplyDamageResultAPI shieldHit(float flux) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getDamageToShields()).thenReturn(flux);
        return result;
    }

    @Test
    void shieldFluxChargesTheReserveThatDrivesRateOfFireAndFluxCost() {
        ShipAPI ship = ship("a", 0f, Map.of(AbsorbReserveListener.RATE_OF_FIRE_PERCENT_KEY, 50f));
        MutableShipStatsAPI stats = ship.getMutableStats();
        MutableStat ballisticRof = mock(MutableStat.class);
        MutableStat energyRof = mock(MutableStat.class);
        StatBonus ballisticFlux = mock(StatBonus.class);
        StatBonus energyFlux = mock(StatBonus.class);
        MutableStat beamFlux = mock(MutableStat.class);
        when(stats.getBallisticRoFMult()).thenReturn(ballisticRof);
        when(stats.getEnergyRoFMult()).thenReturn(energyRof);
        when(stats.getBallisticWeaponFluxCostMod()).thenReturn(ballisticFlux);
        when(stats.getEnergyWeaponFluxCostMod()).thenReturn(energyFlux);
        when(stats.getBeamWeaponFluxCostMult()).thenReturn(beamFlux);
        AbsorbReserveListener listener = new AbsorbReserveListener(ship);

        listener.reportDamageApplied(null, mock(ShipAPI.class), shieldHit(400f));
        listener.reportDamageApplied(null, ship, shieldHit(250f));
        listener.advance(0.5f);

        verify(energyRof).modifyPercent("exiledLib_absorbReserve_a", 20f);
        verify(energyFlux).modifyMult("exiledLib_absorbReserve_a", 100f / 120f);
        verify(beamFlux).modifyMult("exiledLib_absorbReserve_a", 1f / (100f / 120f));

        listener.reportDamageApplied(null, ship, shieldHit(5000f));
        listener.advance(1f);

        verify(ballisticRof).modifyPercent("exiledLib_absorbReserve_a", 50f);

        listener.advance(10f);

        verify(ballisticRof).unmodify("exiledLib_absorbReserve_a");
        verify(energyFlux).unmodify("exiledLib_absorbReserve_a");
        verify(beamFlux).unmodify("exiledLib_absorbReserve_a");
    }
}
