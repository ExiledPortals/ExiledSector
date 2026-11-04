package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhaseEntryRepairListenerTest {

    private final float[][] cells = {{0f, 0f}};

    private ShipAPI deployment() {
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        when(grid.getMaxArmorInCell()).thenReturn(100f);
        when(grid.getGrid()).thenReturn(cells);
        when(grid.getArmorValue(anyInt(), anyInt())).thenAnswer(call -> cells[(int) call.getArgument(0)][(int) call.getArgument(1)]);
        doAnswer(call -> {
            cells[(int) call.getArgument(0)][(int) call.getArgument(1)] = call.getArgument(2);
            return null;
        }).when(grid).setArmorValue(anyInt(), anyInt(), anyFloat());
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(PhaseEntryRepairListener.REPAIR_PERCENT_KEY, 0f)).thenReturn(30f);
        when(ship.getArmorGrid()).thenReturn(grid);
        return ship;
    }

    private float totalArmor() {
        return cells[0][0] + cells[0][1];
    }

    private static void enterPhaseRepeatedly(ShipAPI ship, PhaseEntryRepairListener listener, int entries) {
        for (int entry = 0; entry < entries; entry++) {
            when(ship.isPhased()).thenReturn(true);
            listener.advance(0.1f);
            when(ship.isPhased()).thenReturn(false);
            listener.advance(0.1f);
        }
    }

    @Test
    void repairsStopAtTheDeploymentCapAndANewDeploymentStartsAFreshCap() {
        float deploymentCap = 200f * PhaseEntryRepairListener.DEPLOYMENT_CAP_PERCENT / 100f;
        ShipAPI firstDeployment = deployment();
        enterPhaseRepeatedly(firstDeployment, new PhaseEntryRepairListener(firstDeployment), 5);
        assertEquals(deploymentCap, totalArmor(), 0.001f);

        ShipAPI redeployed = deployment();
        enterPhaseRepeatedly(redeployed, new PhaseEntryRepairListener(redeployed), 5);
        assertEquals(2f * deploymentCap, totalArmor(), 0.001f);
    }
}
