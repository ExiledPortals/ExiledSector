package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatMode;

import static exiledsector.skills.skilleffect.ScaledBonus.fixed;
import static exiledsector.skills.skilleffect.ScaledBonus.percent;
import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.stat;

final class AbsorbReserveListener extends ShipCombatListener implements DamageListener, AdvanceableListener {

    static final String RATE_OF_FIRE_PERCENT_KEY = "exiledLib_absorbRateOfFirePercent";
    static final float FULL_RESERVE = 500f;
    static final float MAX_RESERVE = 1000f;
    static final float DRAIN_PER_SECOND = 100f;
    private static final String MOD_ID_PREFIX = "exiledLib_absorbReserve_";
    private static final ScaledBonus RATE_OF_FIRE = new ScaledBonus(
            percent(all(stat(MutableShipStatsAPI::getBallisticRoFMult), stat(MutableShipStatsAPI::getEnergyRoFMult))),
            fixed(stat(MutableShipStatsAPI::getBeamWeaponFluxCostMult), StatMode.MULT, 1f));

    private final ScaledBonus.Tracker rateOfFire = RATE_OF_FIRE.tracker();
    private float reserve;

    AbsorbReserveListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (target == ownerShip && result.getDamageToShields() > 0f) {
            reserve = Math.min(MAX_RESERVE, reserve + result.getDamageToShields());
        }
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            reserve = 0f;
        } else if (reserve > 0f) {
            reserve = Math.max(0f, reserve - DRAIN_PER_SECOND * amount);
        }
        float maxPercent = magnitude(RATE_OF_FIRE_PERCENT_KEY);
        int percent = Math.round(maxPercent * Math.min(reserve, FULL_RESERVE) / FULL_RESERVE);
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        if (rateOfFire.apply(stats, modId, percent)) {
            applyShotFluxCost(stats, percent);
        }
    }

    private void applyShotFluxCost(MutableShipStatsAPI stats, int percent) {
        if (percent <= 0) {
            stats.getBallisticWeaponFluxCostMod().unmodify(modId);
            stats.getEnergyWeaponFluxCostMod().unmodify(modId);
            return;
        }
        float fluxMult = 100f / (100f + percent);
        stats.getBallisticWeaponFluxCostMod().modifyMult(modId, fluxMult);
        stats.getEnergyWeaponFluxCostMod().modifyMult(modId, fluxMult);
    }
}
