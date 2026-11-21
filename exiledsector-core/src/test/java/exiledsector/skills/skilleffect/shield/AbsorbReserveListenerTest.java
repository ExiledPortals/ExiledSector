package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbsorbReserveListenerTest {

    private final List<Object> gridShips = new ArrayList<>();

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();

    private CombatEngineAPI combatEngine;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = combat.engine();
        combatEngine = engine;
        combat.shipGridOver(gridShips);
    }

    private static ShipAPI ship(String id, float x, Map<String, Float> dynamicValues) {
        ShipAPI ship = CombatTestShips.ship().id(id).dynamic(dynamicValues).at(new Vector2f(x, 0f)).alive().build();
        when(ship.getVelocity()).thenReturn(new Vector2f());
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

        verify(energyRof).modifyPercent("exiledSector_absorbReserve_a", 20f);
        verify(energyFlux).modifyMult("exiledSector_absorbReserve_a", 100f / 120f);
        verify(beamFlux).modifyMult("exiledSector_absorbReserve_a", 1f / (100f / 120f));

        listener.reportDamageApplied(null, ship, shieldHit(5000f));
        listener.advance(1f);

        verify(ballisticRof).modifyPercent("exiledSector_absorbReserve_a", 50f);

        listener.advance(10f);

        verify(ballisticRof).unmodify("exiledSector_absorbReserve_a");
        verify(energyFlux).unmodify("exiledSector_absorbReserve_a");
        verify(beamFlux).unmodify("exiledSector_absorbReserve_a");
    }

    @Test
    void maxReservePercentRaisesHowMuchFluxTheReserveHolds() {
        ShipAPI ship = ship("a", 0f, Map.of(AbsorbReserveListener.RATE_OF_FIRE_PERCENT_KEY, 50f,
                AbsorbReserveListener.MAX_RESERVE_PERCENT_KEY, 50f));
        MutableShipStatsAPI stats = ship.getMutableStats();
        MutableStat ballisticRof = mock(MutableStat.class);
        when(stats.getBallisticRoFMult()).thenReturn(ballisticRof);
        when(stats.getEnergyRoFMult()).thenReturn(mock(MutableStat.class));
        when(stats.getBallisticWeaponFluxCostMod()).thenReturn(mock(StatBonus.class));
        when(stats.getEnergyWeaponFluxCostMod()).thenReturn(mock(StatBonus.class));
        when(stats.getBeamWeaponFluxCostMult()).thenReturn(mock(MutableStat.class));
        AbsorbReserveListener listener = new AbsorbReserveListener(ship);

        listener.reportDamageApplied(null, ship, shieldHit(5000f));
        listener.advance(12.5f);

        verify(ballisticRof).modifyPercent("exiledSector_absorbReserve_a", 25f);
    }
}
