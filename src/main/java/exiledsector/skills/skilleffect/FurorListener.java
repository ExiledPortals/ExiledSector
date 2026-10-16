package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

final class FurorListener extends ShipCombatListener implements DamageDealtModifier, AdvanceableListener {

    static final String RATE_OF_FIRE_PERCENT_KEY = "exiledSector_furorRateOfFirePercent";
    static final Map<HullSize, Float> DAMAGE_FOR_FULL_FUROR = Map.of(HullSize.FIGHTER, 2000f, HullSize.FRIGATE, 2000f,
            HullSize.DESTROYER, 4000f, HullSize.CRUISER, 8000f, HullSize.CAPITAL_SHIP, 20000f);
    static final float DECAY_PERCENT_PER_SECOND = 1f;
    static final float SHIELD_HIT_WEIGHT = 0.5f;
    static final float EMP_WEIGHT = 0.25f;
    private static final String MOD_ID_PREFIX = "exiledSector_furor_";

    private float furorPercent;
    private int appliedPercent;

    FurorListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static float damageForFullFuror(HullSize hullSize) {
        return DAMAGE_FOR_FULL_FUROR.getOrDefault(hullSize, 20000f);
    }

    float furorPercent() {
        return furorPercent;
    }

    static float countedDamage(DamageAPI damage, boolean shieldHit) {
        float seconds = (damage.isDps() ? damage.getDpsDuration() : 1f) * damage.getMultiplier();
        if (shieldHit) {
            return damage.getDamage() * seconds * SHIELD_HIT_WEIGHT;
        }
        return (damage.getDamage() + damage.getFluxComponent() * EMP_WEIGHT) * seconds;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (target instanceof ShipAPI targetShip && !targetShip.isHulk() && !targetShip.isFighter() && CombatQueries.isHostile(ownerShip, targetShip)
                && ownerIsAliveNotHulk()) {
            float maxPercent = magnitude(RATE_OF_FIRE_PERCENT_KEY);
            float gained = maxPercent * countedDamage(damage, shieldHit) / damageForFullFuror(ownerShip.getHullSize());
            furorPercent = Math.min(maxPercent, furorPercent + gained);
            applyIfChanged();
        }
        return null;
    }

    @Override
    public void advance(float amount) {
        if (furorPercent > 0f) {
            furorPercent = ownerIsAliveNotHulk() ? Math.max(0f, furorPercent - DECAY_PERCENT_PER_SECOND * amount) : 0f;
            applyIfChanged();
        }
    }

    private void applyIfChanged() {
        int percent = Math.round(furorPercent);
        if (percent == appliedPercent) {
            return;
        }
        appliedPercent = percent;
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        if (percent <= 0) {
            stats.getBallisticRoFMult().unmodify(modId);
            stats.getEnergyRoFMult().unmodify(modId);
            stats.getMissileRoFMult().unmodify(modId);
            return;
        }
        stats.getBallisticRoFMult().modifyPercent(modId, percent);
        stats.getEnergyRoFMult().modifyPercent(modId, percent);
        stats.getMissileRoFMult().modifyPercent(modId, percent);
    }
}
