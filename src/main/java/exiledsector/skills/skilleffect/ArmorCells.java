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
}
