package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledlib.skills.HullSizeTable;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.ScaledBonus;
import exiledlib.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import static exiledlib.skills.skilleffect.ScaledBonus.percent;

final class FurorListener extends ShipCombatListener implements DamageDealtModifier, AdvanceableListener {

    static final String RATE_OF_FIRE_PERCENT_KEY = "exiledSector_furorRateOfFirePercent";
    static final HullSizeTable DAMAGE_FOR_FULL_FUROR = new HullSizeTable(2000f, 2000f, 4000f, 8000f, 20000f, 20000f);
    static final float DECAY_PERCENT_PER_SECOND = 1f;
    static final float SHIELD_HIT_WEIGHT = 0.5f;
    static final float EMP_WEIGHT = 0.25f;
    private static final String MOD_ID_PREFIX = "exiledSector_furor_";
    private static final ScaledBonus RATE_OF_FIRE = new ScaledBonus(percent(WeaponStatFamily.FIRE_RATE.target(WeaponScope.ALL)));

    private final ScaledBonus.Tracker rateOfFire = RATE_OF_FIRE.tracker();
    private float furorPercent;

    FurorListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    float furorPercent() {
        return furorPercent;
    }

    static float countedDamage(DamageAPI damage, boolean shieldHit) {
        float hitDamage = CombatQueries.hitDamage(damage);
        if (shieldHit) {
            return hitDamage * SHIELD_HIT_WEIGHT;
        }
        return hitDamage + damage.computeFluxDealt(0f) * EMP_WEIGHT;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (target instanceof ShipAPI targetShip && !targetShip.isHulk() && !targetShip.isFighter() && CombatQueries.isHostile(ownerShip, targetShip)
                && ownerIsAliveNotHulk()) {
            float maxPercent = magnitude(RATE_OF_FIRE_PERCENT_KEY);
            float gained = maxPercent * countedDamage(damage, shieldHit) / DAMAGE_FOR_FULL_FUROR.of(ownerShip.getHullSize());
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
        rateOfFire.apply(ownerShip.getMutableStats(), modId, Math.round(furorPercent));
    }
}
