package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FieldRepairNanitesListenerTest {

    private static ShipAPI ship(float maxHitpoints, float hullPerCrBonus) {
        ShipAPI ship = CombatTestShips.ship(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS)).id("nanites")
                .dynamic(Map.of(FieldRepairNanitesListener.REGEN_PERCENT_KEY, 1f,
                        FieldRepairNanitesListener.HULL_PER_CR_BONUS_KEY, hullPerCrBonus))
                .alive().at(new Vector2f()).build();
        when(ship.getMaxHitpoints()).thenReturn(maxHitpoints);
        when(ship.getCurrentCR()).thenReturn(0.7f);
        return ship;
    }

    @Test
    void theStepGrowsByTheBonus() {
        assertEquals(3f, FieldRepairNanitesListener.hullPercentPerCrLost(0f), 1e-5f);
        assertEquals(5f, FieldRepairNanitesListener.hullPercentPerCrLost(2f), 1e-5f);
    }

    @Test
    void repairsAPercentOfMaximumHullEachSecond() {
        ShipAPI ship = ship(1000f, 0f);
        when(ship.getHitpoints()).thenReturn(500f);

        new FieldRepairNanitesListener(ship).advance(1f);

        verify(ship).setHitpoints(510f);
    }

    @Test
    void repairsAtMostOneHundredHullPointsEachSecond() {
        ShipAPI ship = ship(50000f, 0f);
        when(ship.getHitpoints()).thenReturn(1000f);

        new FieldRepairNanitesListener(ship).advance(1f);

        verify(ship).setHitpoints(1100f);
    }

    @Test
    void losesOnePercentCombatReadinessForEveryStepOfHullRepaired() {
        ShipAPI ship = ship(1000f, 0f);
        when(ship.getHitpoints()).thenReturn(500f);
        FieldRepairNanitesListener listener = new FieldRepairNanitesListener(ship);

        listener.advance(2f);
        verify(ship, never()).setCurrentCR(anyFloat());

        listener.advance(1f);
        verify(ship).setCurrentCR(eq(0.69f, 1e-5f));
    }

    @Test
    void theBonusStretchesTheStep() {
        ShipAPI ship = ship(1000f, 2f);
        when(ship.getHitpoints()).thenReturn(500f);
        FieldRepairNanitesListener listener = new FieldRepairNanitesListener(ship);

        listener.advance(4f);
        verify(ship, never()).setCurrentCR(anyFloat());

        listener.advance(1f);
        verify(ship).setCurrentCR(eq(0.69f, 1e-5f));
    }

    @Test
    void stopsAtOrBelowTheCombatReadinessFloor() {
        ShipAPI ship = ship(1000f, 0f);
        when(ship.getHitpoints()).thenReturn(500f);
        when(ship.getCurrentCR()).thenReturn(0.15f);

        new FieldRepairNanitesListener(ship).advance(1f);

        verify(ship, never()).setHitpoints(anyFloat());
    }

    @Test
    void anUndamagedHullCostsNothing() {
        ShipAPI ship = ship(1000f, 0f);
        when(ship.getHitpoints()).thenReturn(1000f);

        new FieldRepairNanitesListener(ship).advance(1f);

        verify(ship, never()).setCurrentCR(anyFloat());
    }
}
