package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.compat.LostSectorCompat;

final class InertialSuperchargerListener implements AdvanceableListener {

    static final String DAMAGE_PERCENT_PER_SPEED_KEY = "exiledSector_inertialDamagePercentPerSpeed";
    static final String AUGMENTED_PROJECTILE_SPEED_KEY = "exiledSector_inertialAugmentedProjectileSpeed";
    private static final String MOD_ID_PREFIX = "exiledSector_inertialSupercharger_";

    private final ShipAPI ownerShip;
    private final String modId;
    private int appliedPercent;
    private Float projectileSpeedShare;

    InertialSuperchargerListener(ShipAPI ownerShip) {
        this.ownerShip = ownerShip;
        this.modId = MOD_ID_PREFIX + ownerShip.getId();
    }

    @Override
    public void advance(float amount) {
        int percent = 0;
        if (ownerShip.isAlive() && !ownerShip.isHulk()) {
            float perSpeed = ownerShip.getMutableStats().getDynamic().getValue(DAMAGE_PERCENT_PER_SPEED_KEY, 0f);
            percent = (int) (ownerShip.getVelocity().length() * perSpeed);
        }
        if (percent != appliedPercent) {
            apply(percent);
        }
    }

    private void apply(int percent) {
        appliedPercent = percent;
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        if (percent <= 0) {
            stats.getBallisticWeaponDamageMult().unmodify(modId);
            stats.getEnergyWeaponDamageMult().unmodify(modId);
            stats.getBeamWeaponDamageMult().unmodify(modId);
            if (projectileSpeedShare() > 0f) {
                stats.getProjectileSpeedMult().unmodify(modId);
            }
            return;
        }
        stats.getBallisticWeaponDamageMult().modifyPercent(modId, percent);
        stats.getEnergyWeaponDamageMult().modifyPercent(modId, percent);
        stats.getBeamWeaponDamageMult().modifyPercent(modId, -percent);
        float share = projectileSpeedShare();
        if (share > 0f) {
            stats.getProjectileSpeedMult().modifyPercent(modId, percent * share / 100f);
        }
    }

    private float projectileSpeedShare() {
        if (projectileSpeedShare == null) {
            projectileSpeedShare = LostSectorCompat.hasAugmentedSystems(ownerShip.getVariant())
                    ? ownerShip.getMutableStats().getDynamic().getValue(AUGMENTED_PROJECTILE_SPEED_KEY, 0f) : 0f;
        }
        return projectileSpeedShare;
    }
}
