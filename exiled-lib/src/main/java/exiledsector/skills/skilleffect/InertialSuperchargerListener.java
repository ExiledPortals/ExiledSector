package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.compat.LostSectorCompat;

import static exiledsector.skills.skilleffect.ScaledBonus.percent;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;

final class InertialSuperchargerListener extends ShipCombatListener implements AdvanceableListener {

    static final String DAMAGE_PERCENT_PER_SPEED_KEY = "exiledSector_inertialDamagePercentPerSpeed";
    static final String AUGMENTED_PROJECTILE_SPEED_KEY = "exiledSector_inertialAugmentedProjectileSpeed";
    private static final String MOD_ID_PREFIX = "exiledSector_inertialSupercharger_";
    private static final ScaledBonus NON_BEAM_DAMAGE = new ScaledBonus(
            percent(liveStat(MutableShipStatsAPI::getBallisticWeaponDamageMult)),
            percent(liveStat(MutableShipStatsAPI::getEnergyWeaponDamageMult)),
            percent(StatTarget.scaled(liveStat(MutableShipStatsAPI::getBeamWeaponDamageMult), -1f)));

    private int appliedPercent;
    private Float projectileSpeedShare;

    InertialSuperchargerListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        int percent = 0;
        if (ownerIsAliveNotHulk()) {
            percent = (int) (ownerShip.getVelocity().length() * magnitude(DAMAGE_PERCENT_PER_SPEED_KEY));
        }
        if (percent != appliedPercent) {
            apply(percent);
        }
    }

    private void apply(int percent) {
        appliedPercent = percent;
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        NON_BEAM_DAMAGE.apply(stats, modId, percent);
        float share = projectileSpeedShare();
        if (share <= 0f) {
            return;
        }
        if (percent <= 0) {
            stats.getProjectileSpeedMult().unmodify(modId);
        } else {
            stats.getProjectileSpeedMult().modifyPercent(modId, percent * share / 100f);
        }
    }

    private float projectileSpeedShare() {
        if (projectileSpeedShare == null) {
            projectileSpeedShare = LostSectorCompat.hasAugmentedSystems(ownerShip.getVariant())
                    ? magnitude(AUGMENTED_PROJECTILE_SPEED_KEY) : 0f;
        }
        return projectileSpeedShare;
    }
}
