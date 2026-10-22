package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;

final class AbsorbReserveListener extends ShipCombatListener implements DamageListener, AdvanceableListener {

    static final String RATE_OF_FIRE_PERCENT_KEY = "exiledSector_absorbRateOfFirePercent";
    static final float FULL_RESERVE = 500f;
    static final float MAX_RESERVE = 1000f;
    static final float DRAIN_PER_SECOND = 100f;
    private static final String MOD_ID_PREFIX = "exiledSector_absorbReserve_";

    private float reserve;
    private int appliedPercent;

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
        if (percent != appliedPercent) {
            apply(percent);
        }
    }

    private void apply(int percent) {
        appliedPercent = percent;
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        if (percent <= 0) {
            stats.getBallisticRoFMult().unmodify(modId);
            stats.getEnergyRoFMult().unmodify(modId);
            stats.getBallisticWeaponFluxCostMod().unmodify(modId);
            stats.getEnergyWeaponFluxCostMod().unmodify(modId);
            stats.getBeamWeaponFluxCostMult().unmodify(modId);
            return;
        }
        float fluxMult = 100f / (100f + percent);
        stats.getBallisticRoFMult().modifyPercent(modId, percent);
        stats.getEnergyRoFMult().modifyPercent(modId, percent);
        stats.getBallisticWeaponFluxCostMod().modifyMult(modId, fluxMult);
        stats.getEnergyWeaponFluxCostMod().modifyMult(modId, fluxMult);
        stats.getBeamWeaponFluxCostMult().modifyMult(modId, 1f / fluxMult);
    }
}
