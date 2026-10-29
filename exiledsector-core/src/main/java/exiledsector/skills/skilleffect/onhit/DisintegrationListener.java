package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;
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
        float percent = armorDamagePercent(hitWeapon(param));
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

    private static WeaponAPI hitWeapon(Object param) {
        if (param instanceof DamagingProjectileAPI proj) {
            return proj.getWeapon();
        }
        return param instanceof BeamAPI beam ? beam.getWeapon() : null;
    }

    private static void queueStrip(ShipAPI target, Vector2f point, float armorDamage) {
        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid == null ? null : grid.getCellAtLocation(point);
        if (cell != null) {
            SkillEffectSupport.ensureListener(target, ArmorDecay.class, ArmorDecay::new).queue(grid, cell[0], cell[1], armorDamage);
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
        float armor = grid.getArmorValue(x, y);
        if (amount <= 0f || armor <= 0f) {
            return false;
        }
        grid.setArmorValue(x, y, Math.max(0f, armor - amount));
        return true;
    }

    static final class ArmorDecay implements AdvanceableListener {

        private final ShipAPI decayingShip;
        private float[][] pendingArmor;
        private float[][] stripRate;
        private int minX;
        private int maxX = -1;
        private int minY;
        private int maxY = -1;

        ArmorDecay(ShipAPI decayingShip) {
            this.decayingShip = decayingShip;
        }

        void queue(ArmorGridAPI grid, int cellX, int cellY, float armorDamage) {
            float[][] cells = grid.getGrid();
            if (pendingArmor == null || pendingArmor.length != cells.length) {
                pendingArmor = new float[cells.length][cells.length == 0 ? 0 : cells[0].length];
                stripRate = new float[pendingArmor.length][pendingArmor.length == 0 ? 0 : pendingArmor[0].length];
                maxX = -1;
            }
            for (int dx = -KERNEL_RADIUS; dx <= KERNEL_RADIUS; dx++) {
                for (int dy = -KERNEL_RADIUS; dy <= KERNEL_RADIUS; dy++) {
                    int x = cellX + dx;
                    int y = cellY + dy;
                    float amount = armorDamage * kernelShare(dx, dy);
                    if (amount > 0f && x >= 0 && y >= 0 && x < pendingArmor.length && y < pendingArmor[x].length) {
                        pendingArmor[x][y] += amount;
                        stripRate[x][y] += amount / STRIP_SECONDS;
                        widenBounds(x, y);
                    }
                }
            }
        }

        private void widenBounds(int x, int y) {
            if (maxX < minX) {
                minX = x;
                maxX = x;
                minY = y;
                maxY = y;
                return;
            }
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        @Override
        public void advance(float amount) {
            if (maxX < minX) {
                return;
            }
            ArmorGridAPI grid = decayingShip.getArmorGrid();
            boolean pending = false;
            boolean stripped = false;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    float cellPending = pendingArmor[x][y];
                    if (cellPending <= 0f) {
                        continue;
                    }
                    float strip = Math.min(cellPending, stripRate[x][y] * amount);
                    pendingArmor[x][y] = cellPending - strip;
                    if (pendingArmor[x][y] <= 0f) {
                        pendingArmor[x][y] = 0f;
                        stripRate[x][y] = 0f;
                    } else {
                        pending = true;
                    }
                    stripped |= grid != null && stripCell(grid, x, y, strip);
                }
            }
            if (stripped) {
                decayingShip.syncWithArmorGridState();
            }
            if (!pending) {
                maxX = -1;
            }
        }
    }
}
