package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlakBurstListenerTest {

    private static final float DAMAGE = 100f;

    private MockedStatic<Global> global;
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private FlakBurstListener listener;
    private final List<Object> missilesNearby = new ArrayList<>();
    private final List<Object> shipsNearby = new ArrayList<>();

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        CollisionGridAPI missileGrid = mock(CollisionGridAPI.class);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getMissileGrid()).thenReturn(missileGrid);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(missileGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(missilesNearby).iterator());
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(shipsNearby).iterator());
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getCombatEngine).thenReturn(engine);

        ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(FlakBurstListener.DAMAGE_KEY, 0f)).thenReturn(DAMAGE);
        listener = new FlakBurstListener(ship);
    }

    @AfterEach
    void tearDown() {
        global.close();
    }

    private static MissileAPI missileAt(Vector2f location, int owner, float hitpoints) {
        MissileAPI missile = mock(MissileAPI.class);
        when(missile.getLocation()).thenReturn(location);
        when(missile.getOwner()).thenReturn(owner);
        when(missile.getHitpoints()).thenReturn(hitpoints);
        return missile;
    }

    private static ShipAPI fighterAt(Vector2f location, int owner) {
        ShipAPI fighter = mock(ShipAPI.class);
        when(fighter.getLocation()).thenReturn(location);
        when(fighter.getOwner()).thenReturn(owner);
        when(fighter.isFighter()).thenReturn(true);
        when(fighter.isAlive()).thenReturn(true);
        when(fighter.getCollisionClass()).thenReturn(CollisionClass.FIGHTER);
        return fighter;
    }

    private void hit(MissileAPI missile) {
        listener.modifyDamageDealt(mock(DamagingProjectileAPI.class), missile, mock(DamageAPI.class), missile.getLocation(), false);
    }

    @Test
    void aShotDownMissileBurstsOnEnemyMissilesAndFightersNearby() {
        MissileAPI shotDown = missileAt(new Vector2f(), 1, 0f);
        MissileAPI enemyMissile = missileAt(new Vector2f(50f, 0f), 1, 200f);
        MissileAPI farMissile = missileAt(new Vector2f(FlakBurstListener.BURST_RADIUS + 50f, 0f), 1, 200f);
        MissileAPI ownMissile = missileAt(new Vector2f(20f, 0f), 0, 200f);
        ShipAPI enemyFighter = fighterAt(new Vector2f(60f, 0f), 1);
        ShipAPI alliedFighter = fighterAt(new Vector2f(60f, 0f), 0);
        missilesNearby.addAll(List.of(shotDown, enemyMissile, farMissile, ownMissile));
        shipsNearby.addAll(List.of(enemyFighter, alliedFighter));

        hit(shotDown);
        listener.advance(0.016f);

        verify(engine).applyDamage(eq(enemyMissile), any(), eq(DAMAGE), eq(DamageType.FRAGMENTATION), eq(0f), anyBoolean(), anyBoolean(), eq(ship));
        verify(engine).applyDamage(eq(enemyFighter), any(), eq(DAMAGE), eq(DamageType.FRAGMENTATION), eq(0f), anyBoolean(), anyBoolean(), eq(ship));
        verify(engine, never()).applyDamage(eq(farMissile), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
        verify(engine, never()).applyDamage(eq(ownMissile), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
        verify(engine, never()).applyDamage(eq(alliedFighter), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void aMissileThatSurvivesTheHitDoesNotBurst() {
        MissileAPI survivor = missileAt(new Vector2f(), 1, 50f);
        MissileAPI enemyMissile = missileAt(new Vector2f(50f, 0f), 1, 200f);
        missilesNearby.addAll(List.of(survivor, enemyMissile));

        hit(survivor);
        listener.advance(0.016f);

        verify(engine, never()).applyDamage(any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void missilesKilledByABurstDoNotBurstAgain() {
        MissileAPI shotDown = missileAt(new Vector2f(), 1, 0f);
        MissileAPI caught = missileAt(new Vector2f(50f, 0f), 1, 200f);
        MissileAPI beyondCaught = missileAt(new Vector2f(140f, 0f), 1, 200f);
        missilesNearby.addAll(List.of(shotDown, caught, beyondCaught));
        doAnswer(invocation -> {
            when(caught.getHitpoints()).thenReturn(0f);
            listener.modifyDamageDealt(mock(DamagingProjectileAPI.class), caught, mock(DamageAPI.class), caught.getLocation(), false);
            return null;
        }).when(engine).applyDamage(eq(caught), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());

        hit(shotDown);
        listener.advance(0.016f);
        listener.advance(0.016f);

        verify(engine, never()).applyDamage(eq(beyondCaught), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }
}
