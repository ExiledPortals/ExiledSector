package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    private static ShipAPI afterimage() {
        ShipAPI afterimage = mock(ShipAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(afterimage.getVariant()).thenReturn(variant);
        when(variant.hasTag(CombatQueries.AFTERIMAGE_TAG)).thenReturn(true);
        when(afterimage.isAlive()).thenReturn(true);
        return afterimage;
    }

    private static ShipAPI liveShipOnSide(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getOwner()).thenReturn(owner);
        return ship;
    }

    @Test
    void gridQueriesNeverSeeAPhaseAfterimage() {
        ShipAPI ship = liveShipOnSide(0);
        ShipAPI afterimage = afterimage();
        CollisionGridAPI grid = gridOf(afterimage, ship);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getShipGrid()).thenReturn(grid);
        when(ship.getLocation()).thenReturn(new Vector2f(10f, 0f));
        when(afterimage.getLocation()).thenReturn(new Vector2f());

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            assertEquals(List.of(ship), CombatQueries.shipsNear(new Vector2f(), 100f, other -> true));
            assertSame(ship, CombatQueries.nearestShip(new Vector2f(), 100f, other -> true));
            assertFalse(CombatQueries.anyNear(grid, new Vector2f(), 100f, other -> other == afterimage));
        }
    }

    @Test
    void anAlliedShipIsAnotherLiveWarshipOfTheSameSideAndNeverAnAfterimage() {
        ShipAPI source = liveShipOnSide(0);
        ShipAPI ally = liveShipOnSide(0);
        ShipAPI enemy = liveShipOnSide(1);
        ShipAPI fighter = liveShipOnSide(0);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI wreck = liveShipOnSide(0);
        when(wreck.isHulk()).thenReturn(true);
        ShipAPI afterimage = afterimage();

        assertTrue(CombatQueries.isAlliedShip(source, ally));
        assertFalse(CombatQueries.isAlliedShip(source, source));
        assertFalse(CombatQueries.isAlliedShip(source, enemy));
        assertFalse(CombatQueries.isAlliedShip(source, fighter));
        assertFalse(CombatQueries.isAlliedShip(source, wreck));
        assertFalse(CombatQueries.isAlliedShip(source, afterimage));
        assertTrue(CombatQueries.isAfterimage(afterimage));
        assertFalse(CombatQueries.isAfterimage(ally));
    }

    @Test
    void strikeCraftAreOwnedByTheirCarrier() {
        ShipAPI carrier = mock(ShipAPI.class);
        ShipAPI fighter = mock(ShipAPI.class);
        FighterWingAPI wing = mock(FighterWingAPI.class);
        when(fighter.isFighter()).thenReturn(true);
        when(fighter.getWing()).thenReturn(wing);
        when(wing.getSourceShip()).thenReturn(carrier);
        ShipAPI orphan = mock(ShipAPI.class);
        when(orphan.isFighter()).thenReturn(true);

        assertSame(carrier, CombatQueries.owningShip(fighter));
        assertSame(orphan, CombatQueries.owningShip(orphan));
        assertSame(carrier, CombatQueries.owningShip(carrier));
        assertNull(CombatQueries.owningShip(null));
    }

    @Test
    void anyNearStopsAtTheFirstMatch() {
        CollisionGridAPI grid = gridOf("a", "b", "c");

        assertTrue(CombatQueries.anyNear(grid, new Vector2f(), 50f, "b"::equals));
        assertFalse(CombatQueries.anyNear(grid, new Vector2f(), 50f, "z"::equals));
    }
}
