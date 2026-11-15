package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class MalfunctionAuraFieldTest {

    private static ShipAPI ship(String id, int owner, Map<String, Float> magnitudes) {
        return CombatTestShips.ship(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS)).id(id).owner(owner).dynamic(magnitudes).alive().at(new Vector2f()).build();
    }

    private static MalfunctionAuraListener auraSource(String id, float percent) {
        return new MalfunctionAuraListener(ship(id, 0, Map.of(MalfunctionAuraListener.MALFUNCTION_PERCENT_KEY, percent)));
    }

    @Test
    void malfunctionAuraTakesTheStrongestSourceRatherThanStacking() {
        ShipAPI enemy = ship("enemy", 1, Map.of());
        ShipAPI ally = ship("ally", 0, Map.of());
        GravityDragField.ShipsNear everyone = (source, radius, filter) -> List.of(enemy, ally).stream().filter(filter).toList();

        Map<ShipAPI, Float> malfunctions = MalfunctionAuraField.strongestPerTarget(List.of(auraSource("a", 5f), auraSource("b", 3f), auraSource("c", 5f)),
                everyone);

        assertEquals(5f, malfunctions.get(enemy), 0.001f);
        assertFalse(malfunctions.containsKey(ally));
    }
}
