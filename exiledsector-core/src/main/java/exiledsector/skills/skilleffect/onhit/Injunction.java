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

    private final ShipAPI debuffedShip;
    private final ExpiringStacks<Float> slowdownStacks = new ExpiringStacks<>(MAX_STACKS, STACK_SECONDS);
    private float clockSeconds;
    private float nextStackAtSeconds;
    private float appliedSlowdownPercent;

    Injunction(ShipAPI debuffedShip) {
        this.debuffedShip = debuffedShip;
    }

    static void inflict(ShipAPI target, float slowdownPercent) {
        SkillEffectSupport.ensureListener(target, Injunction.class, Injunction::new).gain(slowdownPercent);
    }

    static float rechargeTimeMult(float slowdownPercent) {
        return 1f + slowdownPercent / 100f;
    }

    int stacks() {
        return slowdownStacks.size();
    }

    float slowdownPercent() {
        return slowdownStacks.sum(Float::doubleValue);
    }

    void gain(float slowdownPercent) {
        if (slowdownPercent <= 0f || clockSeconds < nextStackAtSeconds) {
            return;
        }
        nextStackAtSeconds = clockSeconds + SECONDS_BETWEEN_STACKS;
        slowdownStacks.add(clockSeconds, slowdownPercent);
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
        float slowdownPercent = slowdownPercent();
        if (slowdownPercent == appliedSlowdownPercent) {
            return;
        }
        appliedSlowdownPercent = slowdownPercent;
        MutableShipStatsAPI stats = debuffedShip.getMutableStats();
        if (slowdownPercent <= 0f) {
            stats.getSystemCooldownBonus().unmodify(MOD_ID);
            stats.getSystemRegenBonus().unmodify(MOD_ID);
            return;
        }
        float timeMult = rechargeTimeMult(slowdownPercent);
        stats.getSystemCooldownBonus().modifyMult(MOD_ID, timeMult);
        stats.getSystemRegenBonus().modifyMult(MOD_ID, 1f / timeMult);
    }
}
