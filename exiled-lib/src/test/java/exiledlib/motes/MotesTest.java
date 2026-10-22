package exiledlib.motes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MotesTest {

    private MockedStatic<Global> global;
    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        when(engine.isMissileAlive(any())).thenReturn(true);
        when(engine.isEntityInPlay(any())).thenReturn(true);
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        global.close();
    }

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        when(ship.getVelocity()).thenReturn(new Vector2f());
        return ship;
    }

    private MissileAPI spawnedMote() {
        MissileAPI mote = mock(MissileAPI.class, RETURNS_DEEP_STUBS);
        when(mote.getActiveLayers()).thenReturn(EnumSet.of(CombatEngineLayers.FF_INDICATORS_LAYER));
        when(engine.spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any())).thenReturn(mote);
        return mote;
    }

    private static Motes.Launch launchFrom(ShipAPI source, ShipAPI target) {
        return new Motes.Launch(MoteKind.STARDUST, source, new Vector2f(), 0f, target, 200f, 150f, 8f);
    }

    @Test
    void launchSetsUpTheMoteTheWayVanillaMotesAreSetUp() {
        ShipAPI source = ship(0);
        ShipAPI target = ship(1);
        MissileAPI mote = spawnedMote();

        assertSame(mote, Motes.launch(launchFrom(source, target)));

        verify(engine).spawnProjectile(eq(source), eq(null), eq("exiledsector_stardust_mote_launcher"), any(), eq(0f), any());
        verify(mote).setWeaponSpec("exiledsector_stardust_mote_launcher");
        verify(mote).setMissileAI(any(MoteHomingAI.class));
        verify(mote).setEmpResistance(10000);
        verify(mote).setEccmChanceOverride(1f);
        verify(mote).setMaxFlightTime(8f);
        verify(mote.getDamage()).setDamage(200f);
        verify(mote.getDamage()).setFluxComponent(150f);
        verify(engine).applyDamageModifiersToSpawnedProjectileWithNullWeapon(source, WeaponAPI.WeaponType.ENERGY, false, mote.getDamage());
        assertFalse(mote.getActiveLayers().contains(CombatEngineLayers.FF_INDICATORS_LAYER));
    }

    @Test
    void eachSourceIsCappedButDeadMotesFreeTheirSlot() {
        ShipAPI source = ship(0);
        spawnedMote();
        for (int launched = 0; launched < Motes.MAX_LIVE_MOTES_PER_SOURCE; launched++) {
            Motes.launch(launchFrom(source, ship(1)));
        }

        assertNull(Motes.launch(launchFrom(source, ship(1))));
        assertTrue(Motes.launch(launchFrom(ship(0), ship(1))) != null);

        when(engine.isMissileAlive(any())).thenReturn(false);
        assertTrue(Motes.launch(launchFrom(source, ship(1))) != null);
    }

    @Test
    void deadSourcesLaunchNothing() {
        ShipAPI source = ship(0);
        when(source.isAlive()).thenReturn(false);

        assertNull(Motes.launch(launchFrom(source, ship(1))));
        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
    }

    private static MissileAPI flyingMote(ShipAPI source) {
        MissileAPI mote = mock(MissileAPI.class, RETURNS_DEEP_STUBS);
        when(mote.getSource()).thenReturn(source);
        when(mote.getLocation()).thenReturn(new Vector2f());
        return mote;
    }

    @Test
    void theHomingAiDriftsFirstThenHeadsForItsTargetWhileSpinning() {
        ShipAPI source = ship(0);
        ShipAPI target = ship(1);
        when(target.getLocation()).thenReturn(new Vector2f(100f, 0f));
        when(engine.getAimPointWithLeadForAutofire(any(), anyFloat(), eq(target), anyFloat())).thenReturn(new Vector2f(100f, 0f));
        MissileAPI mote = flyingMote(source);
        MoteHomingAI ai = new MoteHomingAI(mote, target, true);

        ai.advance(MoteHomingAI.DRIFT_SECONDS / 2f);
        verify(engine, never()).headInDirectionWithoutTurning(any(MissileAPI.class), anyFloat(), anyFloat());

        ai.advance(MoteHomingAI.DRIFT_SECONDS);
        verify(engine).headInDirectionWithoutTurning(eq(mote), eq(0f), anyFloat());
        verify(mote).giveCommand(ShipCommand.TURN_LEFT);
        verify(mote.getEngineController(), times(2)).forceShowAccelerating();
    }

    @Test
    void theHomingAiRetargetsTheNearestEnemyShipWhenItsTargetDies() {
        ShipAPI source = ship(0);
        ShipAPI deadTarget = ship(1);
        when(deadTarget.isAlive()).thenReturn(false);
        ShipAPI far = ship(1);
        when(far.getLocation()).thenReturn(new Vector2f(900f, 0f));
        ShipAPI near = ship(1);
        when(near.getLocation()).thenReturn(new Vector2f(300f, 0f));
        ShipAPI ally = ship(0);
        CollisionGridAPI grid = mock(CollisionGridAPI.class);
        when(grid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(call -> List.<Object>of(far, ally, near).iterator());
        when(engine.getShipGrid()).thenReturn(grid);
        when(engine.getAimPointWithLeadForAutofire(any(), anyFloat(), any(), anyFloat())).thenReturn(new Vector2f(300f, 0f));
        MoteHomingAI ai = new MoteHomingAI(flyingMote(source), deadTarget, false);

        ai.advance(1f);

        assertSame(near, ai.getTarget());
    }

    @Test
    void flaresAndFriendlyShipsCannotBeForcedOntoAMote() {
        ShipAPI source = ship(0);
        ShipAPI enemy = ship(1);
        MoteHomingAI ai = new MoteHomingAI(flyingMote(source), enemy, true);

        ai.setTarget(mock(MissileAPI.class));
        ai.setTarget(ship(0));
        assertSame(enemy, ai.getTarget());

        ShipAPI otherEnemy = ship(1);
        ai.setTarget(otherEnemy);
        assertSame(otherEnemy, ai.getTarget());
    }

    @Test
    void retargetScansAreThrottledWhileNothingIsInRange() {
        ShipAPI source = ship(0);
        ShipAPI deadTarget = ship(1);
        when(deadTarget.isAlive()).thenReturn(false);
        CollisionGridAPI grid = mock(CollisionGridAPI.class);
        when(grid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(call -> List.of().iterator());
        when(engine.getShipGrid()).thenReturn(grid);
        MoteHomingAI ai = new MoteHomingAI(flyingMote(source), deadTarget, true);
        ai.advance(MoteHomingAI.DRIFT_SECONDS);

        for (int frame = 0; frame < 10; frame++) {
            ai.advance(MoteHomingAI.RETARGET_SCAN_SECONDS / 10f);
        }

        verify(grid, times(2)).getCheckIterator(any(), anyFloat(), anyFloat());
    }

    @Test
    void motesFizzleWhenTheirSourceDies() {
        ShipAPI source = ship(0);
        when(source.isAlive()).thenReturn(false);
        MissileAPI mote = flyingMote(source);

        new MoteHomingAI(mote, ship(1), true).advance(1f);

        verify(mote).flameOut();
    }

    @Test
    void phasedOrHulkedTargetsAreNotValid() {
        ShipAPI phased = ship(1);
        when(phased.isPhased()).thenReturn(true);
        ShipAPI hulk = ship(1);
        when(hulk.isHulk()).thenReturn(true);

        assertFalse(MoteHomingAI.isValidTarget(engine, phased));
        assertFalse(MoteHomingAI.isValidTarget(engine, hulk));
        assertTrue(MoteHomingAI.isValidTarget(engine, ship(1)));
        assertEquals(false, MoteHomingAI.isValidTarget(engine, null));
    }
}
