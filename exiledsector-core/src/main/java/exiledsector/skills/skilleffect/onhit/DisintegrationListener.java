package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.BallisticAsEnergy;
import org.lwjgl.util.vector.Vector2f;

final class DisintegrationListener extends ShipCombatListener implements DamageDealtModifier {

    static final String ARMOR_DAMAGE_PERCENT_KEY = "exiledLib_disintegrationArmorDamagePercent";
    static final String BALLISTIC_ARMOR_DAMAGE_PERCENT_KEY = "exiledSector_ballisticDisintegrationArmorDamagePercent";
    static final float STRIP_SECONDS = 5f;
    private static final int KERNEL_RADIUS = 2;
    private static final float INNER_CELL_SHARE = 1f / 15f;
    private static final float OUTER_CELL_SHARE = 1f / 30f;

    DisintegrationListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; armour is stripped as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || !(target instanceof ShipAPI targetShip)) {
            return null;
        }
        float percent = armorDamagePercent(CombatQueries.weaponBehind(param));
        float armorDamage = CombatQueries.hitDamage(damage) * percent / 100f
                * targetShip.getMutableStats().getArmorDamageTakenMult().getModifiedValue();
        if (armorDamage > 0f) {
            queueStrip(targetShip, point, armorDamage);
        }
        return null;
    }

    private float armorDamagePercent(WeaponAPI weapon) {
        if (BallisticAsEnergy.countsAsEnergy(weapon)) {
            return magnitude(ARMOR_DAMAGE_PERCENT_KEY);
        }
        return weapon != null && weapon.getType() == WeaponAPI.WeaponType.BALLISTIC ? magnitude(BALLISTIC_ARMOR_DAMAGE_PERCENT_KEY) : 0f;
    }

    private static void queueStrip(ShipAPI target, Vector2f point, float armorDamage) {
        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid == null ? null : grid.getCellAtLocation(point);
        if (cell == null) {
            return;
        }
        ArmorDecay decay = ArmorDecay.on(target);
        for (int dx = -KERNEL_RADIUS; dx <= KERNEL_RADIUS; dx++) {
            for (int dy = -KERNEL_RADIUS; dy <= KERNEL_RADIUS; dy++) {
                decay.queue(grid, cell[0] + dx, cell[1] + dy, armorDamage * kernelShare(dx, dy), STRIP_SECONDS);
            }
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
}
