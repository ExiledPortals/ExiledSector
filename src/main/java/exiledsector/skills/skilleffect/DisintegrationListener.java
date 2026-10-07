package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

final class DisintegrationListener implements DamageDealtModifier {

    static final String ARMOR_DAMAGE_PERCENT_KEY = "exiledSector_disintegrationArmorDamagePercent";
    private static final int KERNEL_RADIUS = 2;
    private static final float INNER_CELL_SHARE = 1f / 15f;
    private static final float OUTER_CELL_SHARE = 1f / 30f;

    private final ShipAPI ownerShip;

    DisintegrationListener(ShipAPI ownerShip) {
        this.ownerShip = ownerShip;
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; armour is stripped as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || !(target instanceof ShipAPI targetShip) || !isEnergyWeaponHit(param)) {
            return null;
        }
        float percent = ownerShip.getMutableStats().getDynamic().getValue(ARMOR_DAMAGE_PERCENT_KEY, 0f);
        float hitDamage = damage.isDps() ? damage.getDamage() * damage.getDpsDuration() : damage.getDamage();
        float armorDamage = hitDamage * percent / 100f
                * targetShip.getMutableStats().getArmorDamageTakenMult().getModifiedValue();
        if (armorDamage > 0f) {
            stripArmor(targetShip, point, armorDamage);
        }
        return null;
    }

    private static boolean isEnergyWeaponHit(Object param) {
        WeaponAPI weapon = null;
        if (param instanceof DamagingProjectileAPI proj) {
            weapon = proj.getWeapon();
        } else if (param instanceof BeamAPI beam) {
            weapon = beam.getWeapon();
        }
        return weapon != null && weapon.getType() == WeaponAPI.WeaponType.ENERGY;
    }

    private static void stripArmor(ShipAPI target, Vector2f point, float armorDamage) {
        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid == null ? null : grid.getCellAtLocation(point);
        if (cell == null) {
            return;
        }
        boolean stripped = false;
        for (int dx = -KERNEL_RADIUS; dx <= KERNEL_RADIUS; dx++) {
            for (int dy = -KERNEL_RADIUS; dy <= KERNEL_RADIUS; dy++) {
                stripped |= stripCell(grid, cell[0] + dx, cell[1] + dy, armorDamage * kernelShare(dx, dy));
            }
        }
        if (stripped) {
            target.syncWithArmorGridState();
        }
    }

    private static float kernelShare(int dx, int dy) {
        int absX = Math.abs(dx);
        int absY = Math.abs(dy);
        if (absX <= 1 && absY <= 1) {
            return INNER_CELL_SHARE;
        }
        return absX == KERNEL_RADIUS && absY == KERNEL_RADIUS ? 0f : OUTER_CELL_SHARE;
    }

    private static boolean stripCell(ArmorGridAPI grid, int x, int y, float amount) {
        float[][] cells = grid.getGrid();
        if (amount <= 0f || x < 0 || y < 0 || x >= cells.length || y >= cells[x].length) {
            return false;
        }
        float armor = grid.getArmorValue(x, y);
        if (armor <= 0f) {
            return false;
        }
        grid.setArmorValue(x, y, Math.max(0f, armor - amount));
        return true;
    }
}
