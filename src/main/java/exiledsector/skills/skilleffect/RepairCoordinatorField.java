package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class RepairCoordinatorField extends BaseEveryFrameCombatPlugin {

    static final float RANGE = 1000f;
    static final float MAX_REPAIR_PERCENT_PER_COORDINATOR = 10f;
    static final float MAX_REPAIR_PERCENT_OF_ARMOR = 30f;
    static final float UPDATE_INTERVAL_SECONDS = 0.25f;
    private static final String CUSTOM_DATA_KEY = "exiledSector_repairCoordinatorField";

    private final List<RepairCoordinatorListener> sources = new ArrayList<>();
    private final Map<ShipAPI, Float> repairedArmorByAlly = new IdentityHashMap<>();
    private float secondsSinceUpdate;

    static void register(RepairCoordinatorListener source) {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        RepairCoordinatorField field = (RepairCoordinatorField) engine.getCustomData().get(CUSTOM_DATA_KEY);
        if (field == null) {
            field = new RepairCoordinatorField();
            engine.getCustomData().put(CUSTOM_DATA_KEY, field);
            engine.addPlugin(field);
            engine.addLayeredRenderingPlugin(new RepairDroneRenderer(field));
        }
        field.sources.add(source);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        secondsSinceUpdate += amount;
        if (secondsSinceUpdate < UPDATE_INTERVAL_SECONDS) {
            return;
        }
        float elapsedSeconds = secondsSinceUpdate;
        secondsSinceUpdate = 0f;
        sources.removeIf(source -> !engine.isEntityInPlay(source.source()) || !CombatQueries.isAliveNotHulk(source.source()));
        repair(sources, (source, radius, filter) -> CombatQueries.shipsNear(source.getLocation(), radius, filter), repairedArmorByAlly, elapsedSeconds);
    }

    static void repair(List<RepairCoordinatorListener> sources, GravityDragField.ShipsNear shipsNear, Map<ShipAPI, Float> repairedArmorByAlly,
                       float elapsedSeconds) {
        for (RepairCoordinatorListener source : sources) {
            float percentPerSecond = source.armorRepairPercentPerSecond();
            ShipAPI ally = percentPerSecond <= 0f ? null : closestAllyNeedingArmor(source, shipsNear, repairedArmorByAlly);
            if (ally == null) {
                source.stopWork();
                continue;
            }
            ArmorGridAPI grid = ally.getArmorGrid();
            int[] worstCell = mostDamagedCell(grid);
            float restored = restoreArmor(grid, Math.min(totalArmor(grid) * percentPerSecond / 100f * elapsedSeconds,
                    remainingAllowance(source, ally, repairedArmorByAlly)));
            if (restored > 0f) {
                repairedArmorByAlly.merge(ally, restored, Float::sum);
                source.recordRepair(ally, restored);
                source.workOn(ally, worstCell[0], worstCell[1]);
                ally.syncWithArmorGridState();
            } else {
                source.stopWork();
            }
        }
    }

    static int[] mostDamagedCell(ArmorGridAPI grid) {
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

    List<RepairCoordinatorListener> sources() {
        return sources;
    }

    private static ShipAPI closestAllyNeedingArmor(RepairCoordinatorListener source, GravityDragField.ShipsNear shipsNear,
                                                   Map<ShipAPI, Float> repairedArmorByAlly) {
        ShipAPI sourceShip = source.source();
        List<ShipAPI> allies = new ArrayList<>(shipsNear.find(sourceShip, RANGE, other -> other != sourceShip && !other.isFighter()
                && other.getOwner() == sourceShip.getOwner() && CombatQueries.isAliveNotHulk(other) && !other.isPhased()
                && CombatQueries.withinRadius(other.getLocation(), sourceShip.getLocation(), RANGE)
                && other.getArmorGrid() != null && remainingAllowance(source, other, repairedArmorByAlly) > 0f
                && missingArmor(other.getArmorGrid()) > 0f));
        return allies.isEmpty() ? null : CombatQueries.nearestFirst(allies, sourceShip.getLocation(), 1).get(0);
    }

    static float remainingAllowance(RepairCoordinatorListener source, ShipAPI ally, Map<ShipAPI, Float> repairedArmorByAlly) {
        float totalArmor = totalArmor(ally.getArmorGrid());
        float coordinatorRemaining = totalArmor * MAX_REPAIR_PERCENT_PER_COORDINATOR / 100f - source.armorRepaired(ally);
        float allyRemaining = totalArmor * MAX_REPAIR_PERCENT_OF_ARMOR / 100f - repairedArmorByAlly.getOrDefault(ally, 0f);
        return Math.max(0f, Math.min(coordinatorRemaining, allyRemaining));
    }

    static float totalArmor(ArmorGridAPI grid) {
        float[][] cells = grid.getGrid();
        int cellCount = 0;
        for (float[] column : cells) {
            cellCount += column.length;
        }
        return grid.getMaxArmorInCell() * cellCount;
    }

    static float missingArmor(ArmorGridAPI grid) {
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

    static float restoreArmor(ArmorGridAPI grid, float armorPoints) {
        float missing = missingArmor(grid);
        if (armorPoints <= 0f || missing <= 0f) {
            return 0f;
        }
        float share = Math.min(1f, armorPoints / missing);
        float maxArmorInCell = grid.getMaxArmorInCell();
        ArmorCells.update(grid, current -> current < maxArmorInCell ? current + (maxArmorInCell - current) * share : current);
        return missing * share;
    }
}
