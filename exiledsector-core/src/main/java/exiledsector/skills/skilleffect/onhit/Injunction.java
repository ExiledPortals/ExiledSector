package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ExpiringStacks;
import exiledsector.skills.skilleffect.SkillEffectSupport;

final class Injunction implements AdvanceableListener {

    static final int MAX_STACKS = 10;
    static final float STACK_SECONDS = 10f;
    static final float SECONDS_BETWEEN_STACKS = 1f;
    static final String MOD_ID = "exiledSector_injunction";

    record Slowdown(float rechargePercent, float ventPercent) {

        boolean slowsAnything() {
            return rechargePercent > 0f || ventPercent > 0f;
        }
    }

    private final ShipAPI debuffedShip;
    private final ExpiringStacks<Slowdown> slowdownStacks = new ExpiringStacks<>(MAX_STACKS, STACK_SECONDS);
    private float clockSeconds;
    private float nextStackAtSeconds;
    private float appliedRechargeSlowdownPercent;
    private float appliedVentSlowdownPercent;

    Injunction(ShipAPI debuffedShip) {
        this.debuffedShip = debuffedShip;
    }

    static void inflict(ShipAPI target, Slowdown slowdown) {
        SkillEffectSupport.ensureListener(target, Injunction.class, Injunction::new).gain(slowdown);
    }

    static float slowdownTimeMult(float slowdownPercent) {
        return 1f + slowdownPercent / 100f;
    }

    int stacks() {
        return slowdownStacks.size();
    }

    float rechargeSlowdownPercent() {
        return slowdownStacks.sum(Slowdown::rechargePercent);
    }

    float ventSlowdownPercent() {
        return slowdownStacks.sum(Slowdown::ventPercent);
    }

    void gain(Slowdown slowdown) {
        if (!slowdown.slowsAnything() || clockSeconds < nextStackAtSeconds) {
            return;
        }
        nextStackAtSeconds = clockSeconds + SECONDS_BETWEEN_STACKS;
        slowdownStacks.add(clockSeconds, slowdown);
        applyIfChanged();
    }

    @Override
    public void advance(float amount) {
        clockSeconds += amount;
        slowdownStacks.dropExpired(clockSeconds);
        applyIfChanged();
        if (slowdownStacks.isEmpty()) {
            debuffedShip.removeListener(this);
        }
    }

    private void applyIfChanged() {
        MutableShipStatsAPI stats = debuffedShip.getMutableStats();
        float rechargeSlowdownPercent = rechargeSlowdownPercent();
        if (rechargeSlowdownPercent != appliedRechargeSlowdownPercent) {
            appliedRechargeSlowdownPercent = rechargeSlowdownPercent;
            applyRechargeSlowdown(stats, rechargeSlowdownPercent);
        }
        float ventSlowdownPercent = ventSlowdownPercent();
        if (ventSlowdownPercent != appliedVentSlowdownPercent) {
            appliedVentSlowdownPercent = ventSlowdownPercent;
            applyVentSlowdown(stats, ventSlowdownPercent);
        }
    }

    private static void applyRechargeSlowdown(MutableShipStatsAPI stats, float slowdownPercent) {
        if (slowdownPercent <= 0f) {
            stats.getSystemCooldownBonus().unmodify(MOD_ID);
            stats.getSystemRegenBonus().unmodify(MOD_ID);
            return;
        }
        float timeMult = slowdownTimeMult(slowdownPercent);
        stats.getSystemCooldownBonus().modifyMult(MOD_ID, timeMult);
        stats.getSystemRegenBonus().modifyMult(MOD_ID, 1f / timeMult);
    }

    private static void applyVentSlowdown(MutableShipStatsAPI stats, float slowdownPercent) {
        if (slowdownPercent <= 0f) {
            stats.getVentRateMult().unmodify(MOD_ID);
            return;
        }
        stats.getVentRateMult().modifyMult(MOD_ID, 1f / slowdownTimeMult(slowdownPercent));
    }
}
