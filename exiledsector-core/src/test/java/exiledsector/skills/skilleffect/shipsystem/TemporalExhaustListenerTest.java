package exiledsector.skills.skilleffect.shipsystem;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

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

class TemporalExhaustListenerTest {

    private static final String MOD_ID = "exiledSector_temporalExhaust_tempest";

    private CombatEngineAPI engine;
    private MissileAPI rift;
    private MockedStatic<Global> globalMock;
    private ShipAPI ship;
    private ShipSystemAPI system;

    @BeforeEach
    void setUp() {
        rift = mock(MissileAPI.class, RETURNS_DEEP_STUBS);
        engine = mock(CombatEngineAPI.class);
        when(engine.spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any())).thenReturn(rift);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);

        ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("tempest");
        when(ship.isAlive()).thenReturn(true);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic().getValue(TemporalExhaustListener.TIMEFLOW_PERCENT_KEY, 0f)).thenReturn(20f);
        when(ship.getFacing()).thenReturn(90f);
        system = mock(ShipSystemAPI.class);
        when(ship.getSystem()).thenReturn(system);
        ShipEngineAPI shipEngine = mock(ShipEngineAPI.class);
        when(shipEngine.getLocation()).thenReturn(new Vector2f(0f, -100f));
        ShipEngineControllerAPI engineController = mock(ShipEngineControllerAPI.class);
        when(engineController.getShipEngines()).thenReturn(List.of(shipEngine));
        when(ship.getEngineController()).thenReturn(engineController);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private TemporalExhaustListener listener() {
        Random random = mock(Random.class);
        when(random.nextFloat()).thenReturn(0.5f);
        return new TemporalExhaustListener(ship, random);
    }

    private void activateSystem(TemporalExhaustListener listener) {
        when(system.isActive()).thenReturn(true);
        listener.advance(0f);
        when(system.isActive()).thenReturn(false);
    }

    @Test
    void activatingTheSystemGivesTaperingTimeFlowAndATrailOfRiftsBehindTheEngines() {
        TemporalExhaustListener listener = listener();
        listener.advance(0.1f);
        verify(ship.getMutableStats().getTimeMult(), never()).modifyPercent(anyString(), anyFloat());

        activateSystem(listener);
        verify(ship.getMutableStats().getTimeMult()).modifyPercent(MOD_ID, 20f);
        listener.advance(TemporalExhaustListener.TAPER_SECONDS / 2f);
        verify(ship.getMutableStats().getTimeMult()).modifyPercent(MOD_ID, 10f);
        listener.advance(TemporalExhaustListener.TAPER_SECONDS / 2f);
        verify(ship.getMutableStats().getTimeMult()).unmodify(MOD_ID);

        ArgumentCaptor<Vector2f> riftPoints = ArgumentCaptor.forClass(Vector2f.class);
        verify(engine, times(TemporalExhaustListener.RIFT_COUNT)).spawnProjectile(eq(ship), eq(null), eq(RiftExplosions.RIFT_MINE_WEAPON_ID),
                riftPoints.capture(), anyFloat(), eq(null));
        for (Vector2f riftPoint : riftPoints.getAllValues()) {
            assertTrue(riftPoint.y < -100f, "rift at " + riftPoint + " is not behind the engine");
        }
        verify(rift.getDamage(), times(TemporalExhaustListener.RIFT_COUNT)).setDamage(TemporalExhaustListener.RIFT_DAMAGE);
    }

    @Test
    void itTriggersAtMostOncePerCooldown() {
        TemporalExhaustListener listener = listener();
        activateSystem(listener);
        listener.advance(TemporalExhaustListener.TAPER_SECONDS);

        activateSystem(listener);
        listener.advance(TemporalExhaustListener.TAPER_SECONDS);
        verify(engine, times(TemporalExhaustListener.RIFT_COUNT)).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());

        listener.advance(TemporalExhaustListener.COOLDOWN_SECONDS);
        activateSystem(listener);
        verify(engine, times(TemporalExhaustListener.RIFT_COUNT + 1)).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
    }
}
