package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArmorCellsTest {

    private static ArmorGridAPI grid(float maxArmorInCell, float[][] cells) {
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        when(grid.getMaxArmorInCell()).thenReturn(maxArmorInCell);
        when(grid.getGrid()).thenReturn(cells);
        when(grid.getArmorValue(anyInt(), anyInt())).thenAnswer(call -> cells[(int) call.getArgument(0)][(int) call.getArgument(1)]);
        doAnswer(call -> {
            cells[(int) call.getArgument(0)][(int) call.getArgument(1)] = call.getArgument(2);
            return null;
        }).when(grid).setArmorValue(anyInt(), anyInt(), anyFloat());
        return grid;
    }

    @Test
    void everyCellRepairsItsShareUpToFullArmor() {
        float[][] cells = {{50f, 95f}, {100f, 0f}};

        float restored = ArmorCells.restore(grid(100f, cells), 10f, 1000f);

        assertEquals(10f + 5f + 0f + 10f, restored, 0.001f);
        assertArrayEquals(new float[]{60f, 100f}, cells[0], 0.001f);
        assertArrayEquals(new float[]{100f, 10f}, cells[1], 0.001f);
    }

    @Test
    void undamagedArmorRepairsNothingSoNothingCountsTowardTheCap() {
        float[][] cells = {{100f, 100f}};

        assertEquals(0f, ArmorCells.restore(grid(100f, cells), 10f, 1000f));
    }

    @Test
    void theLastRepairIsCutShortToLandExactlyOnTheCap() {
        float[][] cells = {{0f, 0f}};

        float restored = ArmorCells.restore(grid(100f, cells), 10f, 5f);

        assertEquals(5f, restored, 0.001f);
        assertArrayEquals(new float[]{2.5f, 2.5f}, cells[0], 0.001f);
        assertEquals(0f, ArmorCells.restore(grid(100f, cells), 10f, 0f));
    }

    @Test
    void repairsStopForTheBattleOnceTheyTotalTheCapEvenForARedeployedShip() {
        float[][] cells = {{0f, 0f}};
        ArmorGridAPI grid = grid(100f, cells);
        try (MockSector mockSector = new MockSector()) {
            CombatEngineAPI engine = mock(CombatEngineAPI.class);
            when(engine.getCustomData()).thenReturn(new HashMap<>());
            mockSector.global.when(Global::getCombatEngine).thenReturn(engine);
            ShipAPI firstDeployment = mock(ShipAPI.class);
            ShipAPI redeployed = mock(ShipAPI.class);
            for (ShipAPI ship : new ShipAPI[]{firstDeployment, redeployed}) {
                when(ship.getArmorGrid()).thenReturn(grid);
                when(ship.getFleetMemberId()).thenReturn("member");
            }

            assertEquals(20f, ArmorCells.repairWithinBattleCap(firstDeployment, "prefix_", 10f, 15f), 0.001f);
            assertEquals(10f, ArmorCells.repairWithinBattleCap(redeployed, "prefix_", 10f, 15f), 0.001f);
            assertEquals(0f, ArmorCells.repairWithinBattleCap(redeployed, "prefix_", 10f, 15f));
            assertArrayEquals(new float[]{15f, 15f}, cells[0], 0.001f);
        }
    }
}
