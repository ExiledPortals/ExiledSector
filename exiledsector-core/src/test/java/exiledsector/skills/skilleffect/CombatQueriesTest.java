package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CombatQueriesTest {

    private static CollisionGridAPI gridOf(Object... objects) {
        CollisionGridAPI grid = mock(CollisionGridAPI.class);
        when(grid.getCheckIterator(Mockito.any(), Mockito.anyFloat(), Mockito.anyFloat()))
                .thenAnswer(invocation -> List.of(objects).iterator());
        return grid;
    }

    @Test
    void shipsNearQueriesTheShipGridAroundThePointAndDropsRepeatsAndNonShips() {
        ShipAPI near = mock(ShipAPI.class);
        ShipAPI filteredOut = mock(ShipAPI.class);
        CollisionGridAPI grid = gridOf(near, "not a ship", near, filteredOut);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getShipGrid()).thenReturn(grid);
        Vector2f point = new Vector2f(10f, 20f);

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            List<ShipAPI> found = CombatQueries.shipsNear(point, 150f, ship -> ship != filteredOut);

            assertEquals(List.of(near), found);
            verify(grid).getCheckIterator(point, 300f, 300f);
        }
    }

    @Test
    void anyNearStopsAtTheFirstMatch() {
        CollisionGridAPI grid = gridOf("a", "b", "c");

        assertTrue(CombatQueries.anyNear(grid, new Vector2f(), 50f, "b"::equals));
        assertFalse(CombatQueries.anyNear(grid, new Vector2f(), 50f, "z"::equals));
    }
}
