package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HighFluxMendingListenerTest {

    private static final float DEPLOYMENT_LIMIT = 1000f * HighFluxMendingListener.MAX_TOTAL_REGEN_PERCENT_OF_HULL / 100f;

    private final float[] hitpoints = {500f};
    private FluxTrackerAPI flux;

    @BeforeEach
    void setUp() {
        flux = mock(FluxTrackerAPI.class);
    }

    private ShipAPI deployment() {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(HighFluxMendingListener.REGEN_PERCENT_KEY, 0f)).thenReturn(1f);
        when(ship.getFluxTracker()).thenReturn(flux);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenAnswer(call -> hitpoints[0]);
        doAnswer(call -> {
            hitpoints[0] = call.getArgument(0);
            return null;
        }).when(ship).setHitpoints(anyFloat());
        return ship;
    }

    @Test
    void repairsOnlyAboveHalfFlux() {
        HighFluxMendingListener listener = new HighFluxMendingListener(deployment());

        when(flux.getFluxLevel()).thenReturn(0.5f);
        listener.advance(1f);
        assertEquals(500f, hitpoints[0], 0.001f);

        when(flux.getFluxLevel()).thenReturn(0.6f);
        listener.advance(1f);
        assertEquals(510f, hitpoints[0], 0.001f);
    }

    @Test
    void repairsStopOnceTheyTotalTheDeploymentLimit() {
        HighFluxMendingListener listener = new HighFluxMendingListener(deployment());
        when(flux.getFluxLevel()).thenReturn(0.9f);

        for (int second = 0; second < 30; second++) {
            listener.advance(1f);
        }

        assertEquals(500f + DEPLOYMENT_LIMIT, hitpoints[0], 0.001f);
    }

    @Test
    void aRedeployedShipGetsAFreshDeploymentLimit() {
        when(flux.getFluxLevel()).thenReturn(0.9f);
        HighFluxMendingListener firstDeployment = new HighFluxMendingListener(deployment());
        for (int second = 0; second < 30; second++) {
            firstDeployment.advance(1f);
        }

        HighFluxMendingListener redeployed = new HighFluxMendingListener(deployment());
        for (int second = 0; second < 30; second++) {
            redeployed.advance(1f);
        }

        assertEquals(500f + 2f * DEPLOYMENT_LIMIT, hitpoints[0], 0.001f);
    }
}
