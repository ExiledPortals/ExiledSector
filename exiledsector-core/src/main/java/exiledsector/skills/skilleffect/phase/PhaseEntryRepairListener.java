package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.defense.ArmorCells;

import java.util.Map;

final class PhaseEntryRepairListener extends ShipCombatListener implements AdvanceableListener {

    static final String REPAIR_PERCENT_KEY = "exiledLib_phaseEntryArmorRepairPercent";
    static final float BATTLE_CAP_PERCENT = 50f;
    static final String REPAIRED_KEY_PREFIX = "exiledLib_phaseEntryArmorRepaired_";

    private boolean wasPhased;

    PhaseEntryRepairListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        boolean phased = ownerShip.isPhased();
        if (phased && !wasPhased) {
            repair();
        }
        wasPhased = phased;
    }

    private void repair() {
        ArmorGridAPI grid = ownerShip.getArmorGrid();
        CombatEngineAPI engine = Global.getCombatEngine();
        float percent = magnitude(REPAIR_PERCENT_KEY);
        if (grid == null || engine == null || percent <= 0f) {
            return;
        }
        Map<String, Object> customData = engine.getCustomData();
        String repairedKey = repairedKey(ownerShip);
        float repaired = customData.get(repairedKey) instanceof Float earlier ? earlier : 0f;
        float budget = ArmorCells.total(grid) * BATTLE_CAP_PERCENT / 100f - repaired;
        float restored = restore(grid, grid.getMaxArmorInCell() * percent / 100f, budget);
        if (restored > 0f) {
            customData.put(repairedKey, repaired + restored);
            ownerShip.syncWithArmorGridState();
        }
    }

    static String repairedKey(ShipAPI ship) {
        String memberId = ship.getFleetMemberId();
        return REPAIRED_KEY_PREFIX + (memberId != null ? memberId : ship.getId());
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
        ArmorCells.update(grid, current -> current + cellRepair(current, maxArmorInCell, perCell) * share);
        return wanted * share;
    }

    private static float cellRepair(float current, float maxArmorInCell, float perCell) {
        return Math.max(0f, Math.min(perCell, maxArmorInCell - current));
    }
}
