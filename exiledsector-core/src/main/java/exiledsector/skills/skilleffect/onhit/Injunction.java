package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;

import java.util.ArrayDeque;
import java.util.Deque;

final class Injunction implements AdvanceableListener {

    static final int MAX_STACKS = 10;
    static final float STACK_SECONDS = 10f;
    static final float SECONDS_BETWEEN_STACKS = 1f;
    static final String MOD_ID = "exiledSector_injunction";

    private record Stack(float expiresAtSeconds, float slowdownPercent) {
    }

    private final ShipAPI debuffedShip;
    private final Deque<Stack> stacks = new ArrayDeque<>();
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
        return stacks.size();
    }

    float slowdownPercent() {
        float total = 0f;
        for (Stack stack : stacks) {
            total += stack.slowdownPercent();
        }
        return total;
    }

    void gain(float slowdownPercent) {
        if (slowdownPercent <= 0f || clockSeconds < nextStackAtSeconds) {
            return;
        }
        nextStackAtSeconds = clockSeconds + SECONDS_BETWEEN_STACKS;
        if (stacks.size() >= MAX_STACKS) {
            stacks.pollFirst();
        }
        stacks.addLast(new Stack(clockSeconds + STACK_SECONDS, slowdownPercent));
        applyIfChanged();
    }

    @Override
    public void advance(float amount) {
        clockSeconds += amount;
        while (!stacks.isEmpty() && stacks.peekFirst().expiresAtSeconds() <= clockSeconds) {
            stacks.pollFirst();
        }
        applyIfChanged();
        if (stacks.isEmpty()) {
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
