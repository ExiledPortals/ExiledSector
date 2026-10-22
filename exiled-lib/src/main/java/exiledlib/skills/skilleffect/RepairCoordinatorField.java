package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class RepairCoordinatorField extends AuraField<RepairCoordinatorListener> {

    static final float RANGE = 1000f;
    static final float MAX_REPAIR_PERCENT_PER_COORDINATOR = 10f;
    static final float MAX_REPAIR_PERCENT_OF_ARMOR = 30f;
    private static final String CUSTOM_DATA_KEY = "exiledSector_repairCoordinatorField";

    private final Map<ShipAPI, Float> repairedArmorByAlly = new IdentityHashMap<>();

    RepairCoordinatorField() {
        super(false);
    }

    public static void register(RepairCoordinatorListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, RepairCoordinatorField.class, RepairCoordinatorField::new,
                (engine, field) -> engine.addLayeredRenderingPlugin(new RepairDroneRenderer(field)));
    }

    @Override
    void update(float elapsedSeconds) {
        repair(sources, ENGINE_SHIPS_NEAR, repairedArmorByAlly, elapsedSeconds);
    }

    static void repair(List<RepairCoordinatorListener> sources, ShipsNear shipsNear, Map<ShipAPI, Float> repairedArmorByAlly,
                       float elapsedSeconds) {
        for (RepairCoordinatorListener source : sources) {
            float percentPerSecond = source.armorRepairPercentPerSecond();
            ShipAPI ally = percentPerSecond <= 0f ? null : closestAllyNeedingArmor(source, shipsNear, repairedArmorByAlly);
            if (ally == null) {
                source.stopWork();
                continue;
            }
            ArmorGridAPI grid = ally.getArmorGrid();
            int[] worstCell = ArmorCells.mostDamaged(grid);
            float restored = restoreArmor(grid, Math.min(ArmorCells.total(grid) * percentPerSecond / 100f * elapsedSeconds,
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

    List<RepairCoordinatorListener> sources() {
        return sources;
    }

    private static ShipAPI closestAllyNeedingArmor(RepairCoordinatorListener source, ShipsNear shipsNear,
                                                   Map<ShipAPI, Float> repairedArmorByAlly) {
        ShipAPI sourceShip = source.source();
        List<ShipAPI> allies = new ArrayList<>(shipsNear.find(sourceShip, RANGE, other -> other != sourceShip && !other.isFighter()
                && other.getOwner() == sourceShip.getOwner() && CombatQueries.isAliveNotHulk(other) && !other.isPhased()
                && CombatQueries.withinRadius(other.getLocation(), sourceShip.getLocation(), RANGE)
                && other.getArmorGrid() != null && remainingAllowance(source, other, repairedArmorByAlly) > 0f
                && ArmorCells.missing(other.getArmorGrid()) > 0f));
        return allies.isEmpty() ? null : CombatQueries.nearestFirst(allies, sourceShip.getLocation(), 1).get(0);
    }

    static float remainingAllowance(RepairCoordinatorListener source, ShipAPI ally, Map<ShipAPI, Float> repairedArmorByAlly) {
        float totalArmor = ArmorCells.total(ally.getArmorGrid());
        float coordinatorRemaining = totalArmor * MAX_REPAIR_PERCENT_PER_COORDINATOR / 100f - source.armorRepaired(ally);
        float allyRemaining = totalArmor * MAX_REPAIR_PERCENT_OF_ARMOR / 100f - repairedArmorByAlly.getOrDefault(ally, 0f);
        return Math.max(0f, Math.min(coordinatorRemaining, allyRemaining));
    }

    static float restoreArmor(ArmorGridAPI grid, float armorPoints) {
        if (grid == null || armorPoints <= 0f) {
            return 0f;
        }
        float missing = ArmorCells.missing(grid);
        if (missing <= 0f) {
            return 0f;
        }
        float share = Math.min(1f, armorPoints / missing);
        float maxArmorInCell = grid.getMaxArmorInCell();
        ArmorCells.update(grid, current -> current < maxArmorInCell ? current + (maxArmorInCell - current) * share : current);
        return missing * share;
    }
}
