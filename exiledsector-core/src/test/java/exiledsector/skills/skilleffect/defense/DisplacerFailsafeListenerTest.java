package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisplacerFailsafeListenerTest {

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private MockedStatic<CombatQueries> queries;
    private ShipAPI ship;
    private ShipAPI enemy;
    private FluxTrackerAPI flux;
    private DisplacerFailsafeListener listener;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = combat.engine();
        when(engine.getMapWidth()).thenReturn(20000f);
        when(engine.getMapHeight()).thenReturn(20000f);
        combat.global().when(Global::getSoundPlayer).thenReturn(mock(SoundPlayerAPI.class));
        ship = CombatTestShips.ship().at(new Vector2f(0f, 0f)).dynamic(DisplacerFailsafeListener.HULL_PERCENT_KEY, 30f).build();
        enemy = mock(ShipAPI.class);
        when(ship.getVelocity()).thenReturn(new Vector2f(50f, 0f));
        when(enemy.getLocation()).thenReturn(new Vector2f(500f, 0f));
        flux = mock(FluxTrackerAPI.class);
        when(ship.getFluxTracker()).thenReturn(flux);
        when(flux.getCurrFlux()).thenReturn(500f);
        queries = Mockito.mockStatic(CombatQueries.class);
        queries.when(() -> CombatQueries.isAliveNotHulk(ship)).thenReturn(true);
        queries.when(() -> CombatQueries.nearestShip(any(), anyFloat(), any())).thenReturn(enemy);
        listener = new DisplacerFailsafeListener(ship);
    }

    @AfterEach
    void tearDown() {
        queries.close();
    }

    @Test
    void theFirstHitThatDropsHullBelowTheThresholdThrowsTheShipAwayFromTheNearestEnemyAndVentsOnce() {
        when(ship.getHullLevel()).thenReturn(0.5f);
        listener.reportDamageApplied(null, ship, null);
        assertFalse(listener.used());

        when(ship.getHullLevel()).thenReturn(0.2f);
        listener.reportDamageApplied(null, ship, null);
        listener.reportDamageApplied(null, ship, null);

        assertTrue(listener.used());
        assertEquals(-DisplacerFailsafeListener.DISPLACE_DISTANCE, ship.getLocation().x, 0.01f);
        assertEquals(0f, ship.getVelocity().x, 1e-4f);
        verify(flux, times(1)).ventFlux();
    }

    @Test
    void aSpotTakenByAnotherShipIsSkippedForTheNextAngle() {
        ShipAPI blocker = mock(ShipAPI.class);
        queries.when(() -> CombatQueries.nearestShip(argThat(point -> point != null && point.x < -1100f && Math.abs(point.y) < 1f), anyFloat(), any()))
                .thenReturn(blocker);
        queries.when(() -> CombatQueries.nearestShip(argThat(point -> point != null && point.x > -1100f && Math.abs(point.y) > 1f), anyFloat(), any()))
                .thenReturn(null);

        Vector2f to = listener.clearDestination(new Vector2f(0f, 0f), new Vector2f(500f, 0f), 20000f, 20000f);

        assertEquals(DisplacerFailsafeListener.DISPLACE_DISTANCE, Vector2f.sub(to, new Vector2f(), null).length(), 0.1f);
        assertTrue(Math.abs(to.y) > 1f);
    }

    @Test
    void damageToAnotherShipIsIgnored() {
        when(ship.getHullLevel()).thenReturn(0.1f);

        listener.reportDamageApplied(null, enemy, null);

        assertFalse(listener.used());
        verify(flux, never()).ventFlux();
    }

    @Test
    void theDestinationStaysInsideTheMap() {
        Vector2f to = DisplacerFailsafeListener.destination(new Vector2f(-4000f, 0f), new Vector2f(-3500f, 0f), 9000f, 9000f);

        assertEquals(-4200f, to.x, 0.01f);
        assertEquals(0f, to.y, 0.01f);
    }
}
