package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.compat.LostSectorCompat;

final class InertialSuperchargerListener implements AdvanceableListener {

    static final String DAMAGE_PERCENT_PER_SPEED_KEY = "exiledSector_inertialDamagePercentPerSpeed";
    static final String AUGMENTED_PROJECTILE_SPEED_KEY = "exiledSector_inertialAugmentedProjectileSpeed";
    private static final String MOD_ID_PREFIX = "exiledSector_inertialSupercharger_";

    private final ShipAPI ship;
    private final String modId;
    private int appliedPercent;
    private Float projectileSpeedShare;

    InertialSuperchargerListener(ShipAPI ship) {
        this.ship = ship;
        this.modId = MOD_ID_PREFIX + ship.getId();
    }

    @Override
    public void advance(float amount) {
        int percent = 0;
        if (ship.isAlive() && !ship.isHulk()) {
            float perSpeed = ship.getMutableStats().getDynamic().getValue(DAMAGE_PERCENT_PER_SPEED_KEY, 0f);
            percent = (int) (ship.getVelocity().length() * perSpeed);
        }
        if (percent != appliedPercent) {
            apply(percent);
        }
    }

    private void apply(int percent) {
        appliedPercent = percent;
        MutableShipStatsAPI stats = ship.getMutableStats();
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
            projectileSpeedShare = LostSectorCompat.hasAugmentedSystems(ship.getVariant())
                    ? ship.getMutableStats().getDynamic().getValue(AUGMENTED_PROJECTILE_SPEED_KEY, 0f) : 0f;
        }
        return projectileSpeedShare;
    }
}
