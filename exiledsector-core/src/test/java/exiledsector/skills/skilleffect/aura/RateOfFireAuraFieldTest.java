package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateOfFireAuraFieldTest {

    private static final float EPSILON = 1e-4f;

    private static ShipAPI ship(int owner, Vector2f location, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(location);
        return ship;
    }

    private static RateOfFireAuraListener auraSource(float percent) {
        return new RateOfFireAuraListener(ship(0, new Vector2f(), Map.of(RateOfFireAuraListener.RATE_OF_FIRE_PERCENT_KEY, percent)));
    }

    @Test
    void onlyTheStrongestSourceAppliesToEachEnemyInRange() {
        ShipAPI enemy = ship(1, new Vector2f(RateOfFireAuraField.RANGE, 0f), Map.of());
        ShipAPI distantEnemy = ship(1, new Vector2f(RateOfFireAuraField.RANGE + 1f, 0f), Map.of());
        ShipAPI ally = ship(0, new Vector2f(), Map.of());
        AuraField.ShipsNear everyone = (source, radius, filter) -> List.of(enemy, distantEnemy, ally).stream().filter(filter).toList();

        Map<ShipAPI, Float> penalties = RateOfFireAuraField.strongestPerTarget(List.of(auraSource(10f), auraSource(6f), auraSource(10f)), everyone);

        assertEquals(10f, penalties.get(enemy), EPSILON);
        assertFalse(penalties.containsKey(distantEnemy));
        assertFalse(penalties.containsKey(ally));
    }

    @Test
    void thePenaltyMultipliesEveryWeaponRateOfFireAndClearsAgain() {
        ShipAPI enemy = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat ballistic = new MutableStat(1f);
        MutableStat energy = new MutableStat(1f);
        MutableStat missile = new MutableStat(1f);
        when(enemy.getMutableStats()).thenReturn(stats);
        when(stats.getBallisticRoFMult()).thenReturn(ballistic);
        when(stats.getEnergyRoFMult()).thenReturn(energy);
        when(stats.getMissileRoFMult()).thenReturn(missile);

        RateOfFireAuraField.apply(enemy, 10f);

        assertEquals(0.9f, ballistic.getModifiedValue(), EPSILON);
        assertEquals(0.9f, energy.getModifiedValue(), EPSILON);
        assertEquals(0.9f, missile.getModifiedValue(), EPSILON);

        RateOfFireAuraField.remove(enemy);

        assertEquals(1f, ballistic.getModifiedValue(), EPSILON);
        assertEquals(1f, energy.getModifiedValue(), EPSILON);
        assertEquals(1f, missile.getModifiedValue(), EPSILON);
    }
}
