package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class HardlightFocusListener extends ShipCombatListener implements WeaponBaseRangeModifier, DamageDealtModifier {

    static final String DAMAGE_PERCENT_PER_RANGE_KEY = "exiledSector_hardlightFocusDamagePercentPerRange";
    static final float SMALL_RANGE_CAP = 500f;
    static final float MEDIUM_RANGE_CAP = 600f;
    static final float LARGE_RANGE_CAP = 700f;
    static final float RANGE_STEP = 100f;
    static final float MEDIUM_DAMAGE_SHARE = 2f / 3f;
    static final float LARGE_DAMAGE_SHARE = 0.5f;
    private static final String MOD_ID_PREFIX = "exiledSector_hardlightFocus_";

    HardlightFocusListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
        return 0f;
    }

    @Override
    public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
        return 1f;
    }

    @Override
    public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
        if (weapon.getSlot() == null || !weapon.isBeam() || magnitude(DAMAGE_PERCENT_PER_RANGE_KEY) <= 0f) {
            return 0f;
        }
        float excessRange = excessRange(weapon);
        return excessRange > 0f ? -excessRange : 0f;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(param instanceof BeamAPI beam) || beam.getSource() != ownerShip) {
            return null;
        }
        WeaponAPI weapon = beam.getWeapon();
        if (weapon == null || weapon.getSlot() == null) {
            return null;
        }
        float damagePercent = damagePercent(weapon, magnitude(DAMAGE_PERCENT_PER_RANGE_KEY));
        if (damagePercent <= 0f) {
            return null;
        }
        damage.getModifier().modifyMult(modId, 1f + damagePercent / 100f);
        return modId;
    }

    static float damagePercent(WeaponAPI weapon, float smallPercentPerStep) {
        return excessRange(weapon) / RANGE_STEP * smallPercentPerStep * damageShare(weapon.getSize());
    }

    static float excessRange(WeaponAPI weapon) {
        return Math.max(0f, weapon.getSpec().getMaxRange() - rangeCap(weapon.getSize()));
    }

    static float rangeCap(WeaponSize size) {
        if (size == WeaponSize.LARGE) {
            return LARGE_RANGE_CAP;
        }
        return size == WeaponSize.MEDIUM ? MEDIUM_RANGE_CAP : SMALL_RANGE_CAP;
    }

    private static float damageShare(WeaponSize size) {
        if (size == WeaponSize.LARGE) {
            return LARGE_DAMAGE_SHARE;
        }
        return size == WeaponSize.MEDIUM ? MEDIUM_DAMAGE_SHARE : 1f;
    }
}
