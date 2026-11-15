package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArmorArcListenerTest {

    private final List<Object> gridShips = new ArrayList<>();
    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = combat.engine();
        combat.shipGridOver(gridShips);
    }

    private static ShipAPI plated(float arcs) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(0);
        when(ship.getLocation()).thenReturn(new Vector2f());
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.getHullSize()).thenReturn(HullSize.CRUISER);
        when(ship.getMutableStats().getDynamic().getValue(ArmorArcListener.ARC_COUNT_KEY, 0f)).thenReturn(arcs);
        return ship;
    }

    private void addEnemy() {
        ShipAPI enemy = mock(ShipAPI.class);
        when(enemy.getOwner()).thenReturn(1);
        when(enemy.getLocation()).thenReturn(new Vector2f(300f, 0f));
        when(enemy.isAlive()).thenReturn(true);
        when(enemy.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        gridShips.add(enemy);
    }

    private static ApplyDamageResultAPI armorHit(float armorDamage) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getTotalDamageToArmor()).thenReturn(armorDamage);
        return result;
    }

    private void verifyArcCount(int arcs) {
        verify(engine, times(arcs)).spawnEmpArc(any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat(), any(), anyFloat(), any(), any());
    }

    @Test
    void everyFullShareOfAbsorbedDamageDischargesTheArcsAndKeepsTheRest() {
        ShipAPI ship = plated(2f);
        addEnemy();
        ArmorArcListener listener = new ArmorArcListener(ship);

        listener.reportDamageApplied(null, ship, armorHit(300f));
        verifyArcCount(0);

        listener.reportDamageApplied(null, ship, armorHit(1300f));
        verifyArcCount(6);
        assertEquals(100f, listener.armorDamageTowardDischarge(), 0.001f);
    }

    @Test
    void withNoEnemyInRangeOneDischargeIsHeldForLater() {
        ShipAPI ship = plated(1f);
        ArmorArcListener listener = new ArmorArcListener(ship);

        listener.reportDamageApplied(null, ship, armorHit(1700f));
        verifyArcCount(0);
        assertEquals(ArmorArcListener.ARMOR_DAMAGE_PER_DISCHARGE, listener.armorDamageTowardDischarge(), 0.001f);

        addEnemy();
        listener.reportDamageApplied(null, ship, armorHit(50f));
        verifyArcCount(1);
        assertEquals(50f, listener.armorDamageTowardDischarge(), 0.001f);
    }

    @Test
    void damageToOtherShipsBuildsNothing() {
        ShipAPI ship = plated(1f);
        ArmorArcListener listener = new ArmorArcListener(ship);

        listener.reportDamageApplied(null, mock(ShipAPI.class), armorHit(900f));

        assertEquals(0f, listener.armorDamageTowardDischarge());
    }
}
