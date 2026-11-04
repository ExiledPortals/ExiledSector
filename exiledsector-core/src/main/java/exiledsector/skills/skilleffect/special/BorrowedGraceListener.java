package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

import java.util.ArrayDeque;
import java.util.Deque;

import static exiledsector.skills.skilleffect.ScaledBonus.fixed;
import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.scaled;
import static exiledsector.skills.skilleffect.StatTarget.stat;

final class BorrowedGraceListener extends ShipCombatListener implements DamageListener, AdvanceableListener {

    static final String MAX_STACKS_KEY = "exiledSector_seraphsGraceMaxStacks";
    static final String SLOW_MAX_STACKS_KEY = "exiledSector_seraphsGraceSlowMaxStacks";
    static final float PERCENT_PER_STACK = 1f;
    static final float DAMAGE_PER_STACK = 100f;
    static final float SLOW_DAMAGE_PER_STACK = 150f;
    static final float STACK_DURATION_SECONDS = 15f;
    private static final String MOD_ID_PREFIX = "exiledSector_seraphsGrace_";
    private static final ScaledBonus GRACE_BONUS = new ScaledBonus(
            fixed(all(stat(MutableShipStatsAPI::getMaxSpeed), stat(MutableShipStatsAPI::getFluxDissipation),
                    WeaponStatFamily.RATE_OF_FIRE.target(WeaponScope.ALL)), StatMode.PERCENT, PERCENT_PER_STACK),
            fixed(scaled(all(WeaponStatFamily.FLUX_COST.target(WeaponScope.ALL), stat(MutableShipStatsAPI::getArmorDamageTakenMult),
                    stat(MutableShipStatsAPI::getHullDamageTakenMult), stat(MutableShipStatsAPI::getShieldDamageTakenMult),
                    stat(MutableShipStatsAPI::getEmpDamageTakenMult)), -1f), StatMode.PERCENT, PERCENT_PER_STACK));

    private final Deque<Float> stackExpiryTimes = new ArrayDeque<>();
    private final ScaledBonus.Tracker graceBonus = GRACE_BONUS.tracker();
    private float graceClockSeconds;
    private float damageTowardNextStack;

    BorrowedGraceListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    static StyledText describe(SkillEffect effect, float maxStacks, float damagePerStack) {
        return EffectText.msg(effect).arg("value", maxStacks).arg("damage", damagePerStack)
                .arg("duration", STACK_DURATION_SECONDS).arg("percent", PERCENT_PER_STACK).styled();
    }

    int stacks() {
        return stackExpiryTimes.size();
    }

    int maxStacks() {
        return Math.max(0, Math.round(magnitude(MAX_STACKS_KEY) + magnitude(SLOW_MAX_STACKS_KEY)));
    }

    float damagePerStack() {
        return magnitude(MAX_STACKS_KEY) > 0f ? DAMAGE_PER_STACK : SLOW_DAMAGE_PER_STACK;
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (target != ownerShip || !ownerIsAliveNotHulk()) {
            return;
        }
        damageTowardNextStack += result.getDamageToHull() + result.getTotalDamageToArmor();
        int maxStacks = maxStacks();
        float damagePerStack = damagePerStack();
        while (damageTowardNextStack >= damagePerStack && maxStacks > 0) {
            damageTowardNextStack -= damagePerStack;
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
