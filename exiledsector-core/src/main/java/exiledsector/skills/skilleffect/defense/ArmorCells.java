package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.Map;

public final class ArmorCells {

    private ArmorCells() {
    }

    @FunctionalInterface
    public interface CellUpdate {
        float next(float currentArmor);
    }

    public static void update(ArmorGridAPI grid, CellUpdate cellUpdate) {
        float[][] cells = grid.getGrid();
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                grid.setArmorValue(x, y, cellUpdate.next(grid.getArmorValue(x, y)));
            }
        }
    }

    public static int[] mostDamaged(ArmorGridAPI grid) {
        float[][] cells = grid.getGrid();
        int[] worst = {0, 0};
        float lowest = Float.MAX_VALUE;
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                float armor = grid.getArmorValue(x, y);
                if (armor < lowest) {
                    lowest = armor;
                    worst[0] = x;
                    worst[1] = y;
                }
            }
        }
        return worst;
    }

    public static float total(ArmorGridAPI grid) {
        float[][] cells = grid.getGrid();
        int cellCount = 0;
        for (float[] column : cells) {
            cellCount += column.length;
        }
        return grid.getMaxArmorInCell() * cellCount;
    }

    public static float repairWithinBattleCap(ShipAPI ship, String repairedKeyPrefix, float perCellPercent, float battleCapPercent) {
        ArmorGridAPI grid = ship.getArmorGrid();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (grid == null || engine == null || perCellPercent <= 0f) {
            return 0f;
        }
        Map<String, Object> customData = engine.getCustomData();
        String repairedKey = repairedKey(ship, repairedKeyPrefix);
        float repaired = customData.get(repairedKey) instanceof Float earlier ? earlier : 0f;
        float budget = total(grid) * battleCapPercent / 100f - repaired;
        float restored = restore(grid, grid.getMaxArmorInCell() * perCellPercent / 100f, budget);
        if (restored > 0f) {
            customData.put(repairedKey, repaired + restored);
            ship.syncWithArmorGridState();
        }
        return restored;
    }

    private static String repairedKey(ShipAPI ship, String repairedKeyPrefix) {
        String memberId = ship.getFleetMemberId();
        return repairedKeyPrefix + (memberId != null ? memberId : ship.getId());
    }

    static float restore(ArmorGridAPI grid, float perCell, float budget) {
        if (!(perCell > 0f) || !(budget > 0f)) {
            return 0f;
        }
        float maxArmorInCell = grid.getMaxArmorInCell();
        float wanted = 0f;
        float[][] cells = grid.getGrid();
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                wanted += cellRepair(grid.getArmorValue(x, y), maxArmorInCell, perCell);
            }
        }
        if (!(wanted > 0f)) {
            return 0f;
        }
        float share = Math.min(1f, budget / wanted);
        update(grid, current -> current + cellRepair(current, maxArmorInCell, perCell) * share);
        return wanted * share;
    }

    private static float cellRepair(float current, float maxArmorInCell, float perCell) {
        return Math.max(0f, Math.min(perCell, maxArmorInCell - current));
    }

    public static float missing(ArmorGridAPI grid) {
        if (grid == null) {
            return 0f;
        }
        float maxArmorInCell = grid.getMaxArmorInCell();
        float missing = 0f;
        float[][] cells = grid.getGrid();
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                missing += Math.max(0f, maxArmorInCell - grid.getArmorValue(x, y));
            }
        }
        return missing;
    }
}
