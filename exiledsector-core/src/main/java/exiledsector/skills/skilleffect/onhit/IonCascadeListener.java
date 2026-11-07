package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class IonCascadeListener extends ShipCombatListener implements DamageDealtModifier, AdvanceableListener {

    static final String HIGH_EXPLOSIVE_SHARE_PERCENT_KEY = "exiledSector_ionCascadeHighExplosiveSharePercent";
    static final float INNER_CELL_EMP_SHARE = 0.5f;
    static final float OUTER_CELL_EMP_SHARE = 0.25f;
    static final int REACH_CELLS = 2;

    record CascadeHit(ShipAPI target, Vector2f point, float damage) {
    }

    private final List<CascadeHit> pendingHits = new ArrayList<>();

    IonCascadeListener(ShipAPI ownerShip) {
        super(ownerShip);
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
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || point == null || !(target instanceof ShipAPI targetShip) || !CombatQueries.isAliveNotHulk(targetShip)
                || !CombatQueries.isHostile(ownerShip, targetShip)) {
            return null;
        }
        float sharePercent = magnitude(HIGH_EXPLOSIVE_SHARE_PERCENT_KEY);
        float emp = damage.computeFluxDealt(damage.getDpsDuration());
        if (sharePercent <= 0f || emp <= 0f) {
            return null;
        }
        float wastedEmp = empOnDisabledParts(targetShip, point, emp, damage.getStats());
        if (wastedEmp > 0f) {
            pendingHits.add(new CascadeHit(targetShip, new Vector2f(point), wastedEmp * sharePercent / 100f));
        }
        return null;
    }

    static float empOnDisabledParts(ShipAPI targetShip, Vector2f point, float emp, MutableShipStatsAPI attackerStats) {
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
        float wasted = 0f;
        for (WeaponAPI weapon : targetShip.getAllWeapons()) {
            if (weapon.isDisabled()) {
                wasted += empTaken * weaponMult * shareAt(armorGrid, hitCell, weapon.getLocation());
            }
        }
        ShipEngineControllerAPI engines = targetShip.getEngineController();
        if (engines != null) {
            for (ShipEngineControllerAPI.ShipEngineAPI shipEngine : engines.getShipEngines()) {
                if (shipEngine.isDisabled()) {
                    wasted += empTaken * engineMult * shareAt(armorGrid, hitCell, shipEngine.getLocation());
                }
            }
        }
        return wasted;
    }

    private static float shareAt(ArmorGridAPI armorGrid, int[] hitCell, Vector2f partLocation) {
        int[] partCell = partLocation == null ? null : armorGrid.getCellAtLocation(partLocation);
        return partCell == null ? 0f : empShareAtCellOffset(partCell[0] - hitCell[0], partCell[1] - hitCell[1]);
    }

    @Override
    public void advance(float amount) {
        if (pendingHits.isEmpty()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        List<CascadeHit> hits = new ArrayList<>(pendingHits);
        pendingHits.clear();
        if (engine == null) {
            return;
        }
        for (CascadeHit hit : hits) {
            if (CombatQueries.isAliveNotHulk(hit.target())) {
                engine.applyDamage(hit.target(), hit.point(), hit.damage(), DamageType.HIGH_EXPLOSIVE, 0f, true, false, ownerShip);
            }
        }
    }
}
