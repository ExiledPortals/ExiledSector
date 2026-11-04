package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.DeploymentRepairTally;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class UnhitArmorRegenListener extends ShipCombatListener implements AdvanceableListener {

    static final String REGEN_PERCENT_KEY = "exiledLib_unhitArmorRegenPercent";
    static final float UNHIT_SECONDS = 5f;
    static final float ENEMY_SHIP_RANGE = 1500f;
    static final float HARD_FLUX_PER_ARMOR = 1f;
    static final float DEPLOYMENT_CAP_PERCENT = 50f;
    static final float TICK_SECONDS = 0.5f;

    private final DeploymentRepairTally repairTally = new DeploymentRepairTally();
    private float[][] lastCellArmor;
    private float[][] cellSecondsUnhit;
    private float secondsSinceTick;

    UnhitArmorRegenListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        secondsSinceTick += amount;
        if (secondsSinceTick < TICK_SECONDS) {
            return;
        }
        float elapsed = secondsSinceTick;
        secondsSinceTick = 0f;
        ArmorGridAPI grid = ownerShip.getArmorGrid();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (grid == null || engine == null || !ownerIsAliveNotHulk()) {
            return;
        }
        trackHits(grid, elapsed);
        float percent = magnitude(REGEN_PERCENT_KEY);
        if (percent <= 0f || enemyShipNear(engine)) {
            return;
        }
        float budget = Math.min(repairTally.remainingBelow(ArmorCells.total(grid) * DEPLOYMENT_CAP_PERCENT / 100f), fluxRoom() / HARD_FLUX_PER_ARMOR);
        float restored = mend(grid, cellSecondsUnhit, grid.getMaxArmorInCell() * percent / 100f * elapsed, budget);
        if (restored > 0f) {
            repairTally.record(restored);
            ownerShip.getFluxTracker().increaseFlux(restored * HARD_FLUX_PER_ARMOR, true);
            ownerShip.syncWithArmorGridState();
            snapshot(grid);
        }
    }

    void trackHits(ArmorGridAPI grid, float elapsed) {
        float[][] cells = grid.getGrid();
        if (lastCellArmor == null) {
            lastCellArmor = new float[cells.length][];
            cellSecondsUnhit = new float[cells.length][];
            for (int x = 0; x < cells.length; x++) {
                lastCellArmor[x] = new float[cells[x].length];
                cellSecondsUnhit[x] = new float[cells[x].length];
            }
            snapshot(grid);
            return;
        }
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                float armor = grid.getArmorValue(x, y);
                cellSecondsUnhit[x][y] = armor < lastCellArmor[x][y] ? 0f : cellSecondsUnhit[x][y] + elapsed;
                lastCellArmor[x][y] = armor;
            }
        }
    }

    float[][] cellSecondsUnhit() {
        return cellSecondsUnhit;
    }

    private void snapshot(ArmorGridAPI grid) {
        for (int x = 0; x < lastCellArmor.length; x++) {
            for (int y = 0; y < lastCellArmor[x].length; y++) {
                lastCellArmor[x][y] = grid.getArmorValue(x, y);
            }
        }
    }

    static float mend(ArmorGridAPI grid, float[][] cellSecondsUnhit, float perCell, float budget) {
        if (!(perCell > 0f) || !(budget > 0f)) {
            return 0f;
        }
        float maxArmorInCell = grid.getMaxArmorInCell();
        float wanted = 0f;
        for (int x = 0; x < cellSecondsUnhit.length; x++) {
            for (int y = 0; y < cellSecondsUnhit[x].length; y++) {
                wanted += cellRepair(grid, cellSecondsUnhit, x, y, maxArmorInCell, perCell);
            }
        }
        if (!(wanted > 0f)) {
            return 0f;
        }
        float share = Math.min(1f, budget / wanted);
        for (int x = 0; x < cellSecondsUnhit.length; x++) {
            for (int y = 0; y < cellSecondsUnhit[x].length; y++) {
                float repair = cellRepair(grid, cellSecondsUnhit, x, y, maxArmorInCell, perCell);
                if (repair > 0f) {
                    grid.setArmorValue(x, y, grid.getArmorValue(x, y) + repair * share);
                }
            }
        }
        return wanted * share;
    }

    private static float cellRepair(ArmorGridAPI grid, float[][] cellSecondsUnhit, int x, int y, float maxArmorInCell, float perCell) {
        if (cellSecondsUnhit[x][y] < UNHIT_SECONDS) {
            return 0f;
        }
        return Math.max(0f, Math.min(perCell, maxArmorInCell - grid.getArmorValue(x, y)));
    }

    private float fluxRoom() {
        FluxTrackerAPI fluxTracker = ownerShip.getFluxTracker();
        return fluxTracker == null ? 0f : Math.max(0f, fluxTracker.getMaxFlux() - fluxTracker.getCurrFlux());
    }

    private boolean enemyShipNear(CombatEngineAPI engine) {
        return CombatQueries.anyNear(engine.getShipGrid(), ownerShip.getLocation(), ENEMY_SHIP_RANGE, candidate -> candidate instanceof ShipAPI ship
                && !ship.isFighter() && CombatQueries.isHostile(ownerShip, ship) && CombatQueries.isAliveNotHulk(ship)
                && CombatQueries.withinRadius(ship.getLocation(), ownerShip.getLocation(), ENEMY_SHIP_RANGE));
    }
}
