package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerrifyingPresenceFieldTest {

    private static TerrifyingPresenceListener source(float penaltyPercent) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(TerrifyingPresenceListener.ACCURACY_PENALTY_PERCENT_KEY, 0f)).thenReturn(penaltyPercent);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return new TerrifyingPresenceListener(ship);
    }

    private static ShipAPI shipAt(Vector2f location, int owner, MutableStat accuracy) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getAutofireAimAccuracy()).thenReturn(accuracy);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getLocation()).thenReturn(location);
        return ship;
    }

    private static AuraField.ShipsNear among(List<ShipAPI> ships) {
        return (sourceShip, radius, filter) -> ships.stream().filter(filter).toList();
    }

    @Test
    void penaltiesFromSeveralSourcesStackOnEnemiesInRangeOnly() {
        ShipAPI enemy = shipAt(new Vector2f(500f, 0f), 1, mock(MutableStat.class));
        ShipAPI farEnemy = shipAt(new Vector2f(TerrifyingPresenceField.RANGE + 100f, 0f), 1, mock(MutableStat.class));
        ShipAPI ally = shipAt(new Vector2f(100f, 0f), 0, mock(MutableStat.class));

        Map<ShipAPI, Float> penalties = TerrifyingPresenceField.penaltyPerTarget(List.of(source(25f), source(10f)),
                among(List.of(enemy, farEnemy, ally)));

        assertEquals(0.35f, penalties.get(enemy), 0.0001f);
        assertFalse(penalties.containsKey(farEnemy));
        assertFalse(penalties.containsKey(ally));
    }

    @Test
    void aRetreatingSourceTerrifiesNoOne() {
        TerrifyingPresenceListener retreating = source(25f);
        when(retreating.source().isRetreating()).thenReturn(true);
        ShipAPI enemy = shipAt(new Vector2f(500f, 0f), 1, mock(MutableStat.class));

        assertTrue(TerrifyingPresenceField.penaltyPerTarget(List.of(retreating), among(List.of(enemy))).isEmpty());
    }

    @Test
    void theFieldLowersEnemyAccuracyAndRestoresItWhenTheyLeave() {
        Vector2f enemyLocation = new Vector2f(500f, 0f);
        MutableStat accuracy = mock(MutableStat.class);
        ShipAPI enemy = shipAt(enemyLocation, 1, accuracy);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<Object>(List.of(enemy)).iterator());

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            TerrifyingPresenceField field = new TerrifyingPresenceField();
            field.sources.add(source(25f));

            field.update(0.25f);
            verify(accuracy).modifyFlat(TerrifyingPresenceField.MOD_ID, -0.25f);

            enemyLocation.set(TerrifyingPresenceField.RANGE + 100f, 0f);
            field.update(0.25f);
            verify(accuracy).unmodify(TerrifyingPresenceField.MOD_ID);
        }
    }
}
