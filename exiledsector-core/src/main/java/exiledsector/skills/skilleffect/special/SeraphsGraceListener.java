package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

import java.util.ArrayDeque;
import java.util.Deque;

import static exiledsector.skills.skilleffect.ScaledBonus.percent;
import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.scaled;
import static exiledsector.skills.skilleffect.StatTarget.stat;

final class SeraphsGraceListener extends ShipCombatListener implements DamageListener, AdvanceableListener {

    static final String MAX_STACKS_KEY = "exiledLib_seraphsGraceMaxStacks";
    static final String PERCENT_PER_STACK_KEY = "exiledLib_seraphsGracePercentPerStack";
    static final float DAMAGE_PER_STACK = 100f;
    static final float STACK_DURATION_SECONDS = 15f;
    private static final String MOD_ID_PREFIX = "exiledLib_seraphsGrace_";
    private static final ScaledBonus GRACE_BONUS = new ScaledBonus(
            percent(all(stat(MutableShipStatsAPI::getMaxSpeed), stat(MutableShipStatsAPI::getFluxDissipation),
                    WeaponStatFamily.FIRE_RATE.target(WeaponScope.ALL)), PERCENT_PER_STACK_KEY),
            percent(scaled(all(WeaponStatFamily.FLUX_COST.target(WeaponScope.ALL), stat(MutableShipStatsAPI::getArmorDamageTakenMult),
                    stat(MutableShipStatsAPI::getHullDamageTakenMult), stat(MutableShipStatsAPI::getShieldDamageTakenMult),
                    stat(MutableShipStatsAPI::getEmpDamageTakenMult)), -1f), PERCENT_PER_STACK_KEY));

    private final Deque<Float> stackExpiryTimes = new ArrayDeque<>();
    private final ScaledBonus.Tracker graceBonus = GRACE_BONUS.tracker();
    private float graceClockSeconds;
    private float damageTowardNextStack;

    SeraphsGraceListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    int stacks() {
        return stackExpiryTimes.size();
    }

    int maxStacks() {
        return Math.max(0, Math.round(magnitude(MAX_STACKS_KEY)));
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (target != ownerShip || !ownerIsAliveNotHulk()) {
            return;
        }
        damageTowardNextStack += result.getDamageToHull() + result.getTotalDamageToArmor();
        int maxStacks = maxStacks();
        while (damageTowardNextStack >= DAMAGE_PER_STACK && maxStacks > 0) {
            damageTowardNextStack -= DAMAGE_PER_STACK;
            if (stackExpiryTimes.size() >= maxStacks) {
                stackExpiryTimes.pollFirst();
            }
            stackExpiryTimes.addLast(graceClockSeconds + STACK_DURATION_SECONDS);
        }
        applyIfChanged();
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            stackExpiryTimes.clear();
        } else if (!ownerShip.isPhased()) {
            graceClockSeconds += amount;
        }
        while (!stackExpiryTimes.isEmpty() && stackExpiryTimes.peekFirst() <= graceClockSeconds) {
            stackExpiryTimes.pollFirst();
        }
        applyIfChanged();
    }

    private void applyIfChanged() {
        graceBonus.apply(ownerShip.getMutableStats(), modId, stackExpiryTimes.size());
    }
}
