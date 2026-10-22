package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ArmorGridAPI;

final class ArmorCells {

    private ArmorCells() {
    }

    @FunctionalInterface
    interface CellUpdate {
        float next(float currentArmor);
    }

    static void update(ArmorGridAPI grid, CellUpdate cellUpdate) {
        float[][] cells = grid.getGrid();
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                grid.setArmorValue(x, y, cellUpdate.next(grid.getArmorValue(x, y)));
            }
        }
    }

    static int[] mostDamaged(ArmorGridAPI grid) {
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

    static float total(ArmorGridAPI grid) {
        float[][] cells = grid.getGrid();
        int cellCount = 0;
        for (float[] column : cells) {
            cellCount += column.length;
        }
        return grid.getMaxArmorInCell() * cellCount;
    }

    static float missing(ArmorGridAPI grid) {
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
