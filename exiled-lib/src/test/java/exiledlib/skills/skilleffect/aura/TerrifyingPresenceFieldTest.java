package exiledlib.skills.skilleffect.aura;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerrifyingPresenceListenerTest {

    private static ShipAPI shipWithDynamicValue(String key, float value) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(key, 0f)).thenReturn(value);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static void shipGridHolds(CombatEngineAPI engine, List<ShipAPI> ships) {
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<Object>(ships).iterator());
    }

    private static ShipAPI enemyAt(Vector2f location, MutableStat accuracy) {
        ShipAPI enemy = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(enemy.getMutableStats()).thenReturn(stats);
        when(stats.getAutofireAimAccuracy()).thenReturn(accuracy);
        when(enemy.isAlive()).thenReturn(true);
        when(enemy.getOwner()).thenReturn(1);
        when(enemy.getLocation()).thenReturn(location);
        return enemy;
    }

    @Test
    void terrifyingPresenceLowersNearbyEnemyAccuracyAndRestoresItWhenTheyLeave() {
        ShipAPI ship = shipWithDynamicValue(TerrifyingPresenceListener.ACCURACY_PENALTY_PERCENT_KEY, 25f);
        when(ship.getId()).thenReturn("source");
        when(ship.getLocation()).thenReturn(new Vector2f());
        Vector2f enemyLocation = new Vector2f(500f, 0f);
        MutableStat accuracy = mock(MutableStat.class);
        ShipAPI enemy = enemyAt(enemyLocation, accuracy);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        shipGridHolds(engine, List.of(ship, enemy));

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            TerrifyingPresenceListener listener = new TerrifyingPresenceListener(ship);

            listener.advance(0.3f);
            verify(accuracy).modifyFlat("exiledSector_terrifyingPresence_source", -0.25f);

            enemyLocation.set(TerrifyingPresenceListener.RANGE + 100f, 0f);
            listener.advance(0.3f);
            verify(accuracy).unmodify("exiledSector_terrifyingPresence_source");
        }
    }

    @Test
    void terrifyingPresenceLiftsThePenaltyWhenTheSourceDies() {
        ShipAPI ship = shipWithDynamicValue(TerrifyingPresenceListener.ACCURACY_PENALTY_PERCENT_KEY, 25f);
        when(ship.getId()).thenReturn("source");
        when(ship.getLocation()).thenReturn(new Vector2f());
        MutableStat accuracy = mock(MutableStat.class);
        ShipAPI enemy = enemyAt(new Vector2f(100f, 0f), accuracy);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        shipGridHolds(engine, List.of(ship, enemy));

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            TerrifyingPresenceListener listener = new TerrifyingPresenceListener(ship);
            listener.advance(0.3f);

            when(ship.isAlive()).thenReturn(false);
            listener.advance(0.3f);

            verify(accuracy).unmodify("exiledSector_terrifyingPresence_source");
        }
    }
}
