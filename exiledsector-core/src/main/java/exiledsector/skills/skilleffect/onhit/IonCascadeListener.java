package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

final class IonCascadeListener extends EmpConversionListener {

    static final String HIGH_EXPLOSIVE_SHARE_PERCENT_KEY = "exiledSector_ionCascadeHighExplosiveSharePercent";
    static final float INNER_CELL_EMP_SHARE = 0.5f;
    static final float OUTER_CELL_EMP_SHARE = 0.25f;
    static final int REACH_CELLS = 2;

    IonCascadeListener(ShipAPI ownerShip) {
        super(ownerShip, HIGH_EXPLOSIVE_SHARE_PERCENT_KEY, false, DamageType.HIGH_EXPLOSIVE);
    }

    static float empShareAtCellOffset(int cellOffsetX, int cellOffsetY) {
        int reachX = Math.abs(cellOffsetX);
        int reachY = Math.abs(cellOffsetY);
        if (reachX > REACH_CELLS || reachY > REACH_CELLS || reachX == REACH_CELLS && reachY == REACH_CELLS) {
            return 0f;
        }
        return reachX == REACH_CELLS || reachY == REACH_CELLS ? OUTER_CELL_EMP_SHARE : INNER_CELL_EMP_SHARE;
    }

    @Override
    float convertibleEmp(ShipAPI targetShip, Vector2f point, float emp, DamageAPI damage) {
        return empOnClosestDisabledPart(targetShip, point, emp, damage.getStats());
    }

    static float empOnClosestDisabledPart(ShipAPI targetShip, Vector2f point, float emp, MutableShipStatsAPI attackerStats) {
        ArmorGridAPI armorGrid = targetShip.getArmorGrid();
        int[] hitCell = armorGrid == null ? null : armorGrid.getCellAtLocation(point);
        if (hitCell == null) {
            return 0f;
        }
        MutableShipStatsAPI targetStats = targetShip.getMutableStats();
        float empTaken = emp * targetStats.getEmpDamageTakenMult().getModifiedValue();
        float weaponMult = targetStats.getWeaponDamageTakenMult().getModifiedValue()
                * (attackerStats == null ? 1f : attackerStats.getDamageToTargetWeaponsMult().getModifiedValue());
        float engineMult = targetStats.getEngineDamageTakenMult().getModifiedValue()
                * (attackerStats == null ? 1f : attackerStats.getDamageToTargetEnginesMult().getModifiedValue());
        float closestPartEmp = 0f;
        for (WeaponAPI weapon : targetShip.getAllWeapons()) {
            if (weapon.isDisabled()) {
                closestPartEmp = Math.max(closestPartEmp, empTaken * weaponMult * shareAt(armorGrid, hitCell, weapon.getLocation()));
            }
        }
        ShipEngineControllerAPI engines = targetShip.getEngineController();
        if (engines != null) {
            for (ShipEngineControllerAPI.ShipEngineAPI shipEngine : engines.getShipEngines()) {
                if (shipEngine.isDisabled()) {
                    closestPartEmp = Math.max(closestPartEmp, empTaken * engineMult * shareAt(armorGrid, hitCell, shipEngine.getLocation()));
                }
            }
        }
        return closestPartEmp;
    }

    private static float shareAt(ArmorGridAPI armorGrid, int[] hitCell, Vector2f partLocation) {
        int[] partCell = partLocation == null ? null : armorGrid.getCellAtLocation(partLocation);
        return partCell == null ? 0f : empShareAtCellOffset(partCell[0] - hitCell[0], partCell[1] - hitCell[1]);
    }
}
