package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

final class SeraphsGraceListener extends ShipCombatListener implements DamageListener, AdvanceableListener {

    static final String MAX_STACKS_KEY = "exiledSector_seraphsGraceMaxStacks";
    static final String PERCENT_PER_STACK_KEY = "exiledSector_seraphsGracePercentPerStack";
    static final float DAMAGE_PER_STACK = 100f;
    static final float STACK_DURATION_SECONDS = 15f;
    private static final String MOD_ID_PREFIX = "exiledSector_seraphsGrace_";

    private final Deque<Float> stackExpiryTimes = new ArrayDeque<>();
    private float graceClockSeconds;
    private float damageTowardNextStack;
    private int appliedStackCount;

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
        int stackCount = stackExpiryTimes.size();
        if (stackCount == appliedStackCount) {
            return;
        }
        appliedStackCount = stackCount;
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        List<MutableStat> raisedStats = List.of(stats.getMaxSpeed(), stats.getFluxDissipation(), stats.getBallisticRoFMult(),
                stats.getEnergyRoFMult(), stats.getMissileRoFMult());
        List<StatBonus> loweredFluxCosts = List.of(stats.getBallisticWeaponFluxCostMod(), stats.getEnergyWeaponFluxCostMod(),
                stats.getMissileWeaponFluxCostMod());
        List<MutableStat> loweredDamageTaken = List.of(stats.getArmorDamageTakenMult(), stats.getHullDamageTakenMult(),
                stats.getShieldDamageTakenMult(), stats.getEmpDamageTakenMult());
        if (stackCount == 0) {
            raisedStats.forEach(stat -> stat.unmodify(modId));
            loweredFluxCosts.forEach(bonus -> bonus.unmodify(modId));
            loweredDamageTaken.forEach(stat -> stat.unmodify(modId));
            return;
        }
        float percent = stackCount * magnitude(PERCENT_PER_STACK_KEY);
        raisedStats.forEach(stat -> stat.modifyPercent(modId, percent));
        loweredFluxCosts.forEach(bonus -> bonus.modifyPercent(modId, -percent));
        loweredDamageTaken.forEach(stat -> stat.modifyPercent(modId, -percent));
    }
}
