package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GravityDragFieldTest {

    private static ShipAPI ship(String id, int owner, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    @Test
    void gravityDragMeasuresRangeToTheTargetsEdge() {
        ShipAPI bigEnemy = enemyAt(GravityDragField.RANGE);
        when(bigEnemy.getCollisionRadius()).thenReturn(300f);
        GravityDragField.ShipsNear everyone = (source, radius, filter) -> List.of(bigEnemy).stream().filter(filter).toList();

        float reduction = GravityDragField.reductionsFrom(List.of(dragSource("source", 5f, false)), everyone).get(bigEnemy);

        assertEquals(5f * 300f / GravityDragField.RANGE, reduction, 0.001f);
    }

    private static GravityDragListener dragSource(String id, float percent, boolean phased) {
        ShipAPI source = ship(id, 0, Map.of(GravityDragListener.TIMEFLOW_PERCENT_KEY, percent));
        when(source.isPhased()).thenReturn(phased);
        return new GravityDragListener(source);
    }

    private static ShipAPI enemyAt(float x) {
        ShipAPI enemy = ship("enemy", 1, Map.of());
        when(enemy.getLocation()).thenReturn(new Vector2f(x, 0f));
        return enemy;
    }

    @Test
    void gravityDragFallsWithDistanceAndDoublesWhilePhased() {
        ShipAPI near = enemyAt(0f);
        ShipAPI halfway = enemyAt(GravityDragField.RANGE / 2f);
        GravityDragField.ShipsNear everyone = (source, radius, filter) -> List.of(near, halfway).stream().filter(filter).toList();

        Map<ShipAPI, Float> calm = GravityDragField.reductionsFrom(List.of(dragSource("calm", 5f, false)), everyone);
        Map<ShipAPI, Float> phased = GravityDragField.reductionsFrom(List.of(dragSource("phased", 5f, true)), everyone);

        assertEquals(5f, calm.get(near), 0.001f);
        assertEquals(2.5f, calm.get(halfway), 0.001f);
        assertEquals(10f, phased.get(near), 0.001f);
    }

    @Test
    void gravityDragFromManySourcesIsCappedPerShip() {
        ShipAPI near = enemyAt(0f);
        GravityDragField.ShipsNear everyone = (source, radius, filter) -> List.of(near).stream().filter(filter).toList();
        List<GravityDragListener> sources = List.of(dragSource("a", 5f, true), dragSource("b", 5f, true), dragSource("c", 5f, true));

        assertEquals(GravityDragField.MAX_TOTAL_REDUCTION_PERCENT, GravityDragField.reductionsFrom(sources, everyone).get(near), 0.001f);
    }

    @Test
    void gravityDragSharesItsCapWithNightcrossOwnHullMod() {
        assertEquals(20f, GravityDragField.combinedPercent(5f, 15f), 0.001f);
        assertEquals(GravityDragField.MAX_TOTAL_REDUCTION_PERCENT, GravityDragField.combinedPercent(20f, 20f), 0.001f);

        float ownMult = GravityDragField.ownTimeMult(20f, 20f);
        assertEquals(0.75f, ownMult * 0.8f, 0.0001f);
        assertEquals(0.95f, GravityDragField.ownTimeMult(5f, 0f), 0.0001f);
    }

    @Test
    void gravityDragLeavesAlliesFightersAndHulksAlone() {
        ShipAPI ally = ship("ally", 0, Map.of());
        ShipAPI fighter = enemyAt(0f);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI hulk = enemyAt(0f);
        when(hulk.isHulk()).thenReturn(true);
        GravityDragField.ShipsNear everyone = (source, radius, filter) -> List.of(ally, fighter, hulk).stream().filter(filter).toList();

        assertTrue(GravityDragField.reductionsFrom(List.of(dragSource("source", 5f, false)), everyone).isEmpty());
    }

    @Test
    void aRetreatingCandlesGraceShipStopsDraggingLikeTheOtherAuras() {
        GravityDragListener source = dragSource("source", 5f, false);
        when(source.source().isRetreating()).thenReturn(true);
        GravityDragField.ShipsNear everyone = (ship, radius, filter) -> List.of(enemyAt(0f)).stream().filter(filter).toList();

        assertEquals(0f, source.reductionPercentAtPointBlank());
        assertTrue(GravityDragField.reductionsFrom(List.of(source), everyone).isEmpty());
    }

    @Test
    void thePlayerStatusShowsOnlyOurShareSoItDoesNotRepeatNightcrossOwnDragIcon() {
        assertEquals(5f, GravityDragField.shownPercent(5f, 0f), 0.001f);
        assertEquals(5f, GravityDragField.shownPercent(5f, 15f), 0.001f);
        assertEquals(5f, GravityDragField.shownPercent(20f, 20f), 0.001f);
        assertEquals(0f, GravityDragField.shownPercent(5f, 30f), 0.001f);
    }
}
