package exiledsector.skills;

import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DamageTakenCaps {

    public static final float MAX_REDUCTION_PERCENT = 80f;

    public record Cap(SkillEffect cappedEffect, Set<SkillEffect> contributingEffects) {
    }

    public static final List<Cap> CAPS = List.of(
            new Cap(DefenseSkillEffect.EMP_DAMAGE_TAKEN_MULT,
                    Set.of(DefenseSkillEffect.EMP_DAMAGE_TAKEN_PERCENT, DefenseSkillEffect.EMP_DAMAGE_TAKEN_MULT)),
            new Cap(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, Set.of(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT)),
            new Cap(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT, Set.of(DefenseSkillEffect.KINETIC_DAMAGE_TAKEN_PERCENT)),
            new Cap(DefenseSkillEffect.HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT, Set.of(DefenseSkillEffect.HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT)),
            new Cap(DefenseSkillEffect.FRAGMENTATION_DAMAGE_TAKEN_PERCENT, Set.of(DefenseSkillEffect.FRAGMENTATION_DAMAGE_TAKEN_PERCENT)));

    private DamageTakenCaps() {
    }

    public static boolean exceedsCap(float percentTotal, float multiplierProduct) {
        float damageTakenFactor = Math.max(0f, 1f + percentTotal / 100f) * multiplierProduct;
        return damageTakenFactor < 1f - MAX_REDUCTION_PERCENT / 100f;
    }

    public static void capTotals(Map<SkillEffect, Float> totals) {
        for (Cap cap : CAPS) {
            float percentTotal = 0f;
            float multiplierProduct = 1f;
            boolean contributes = false;
            for (SkillEffect effect : cap.contributingEffects()) {
                Float total = totals.get(effect);
                if (total == null) {
                    continue;
                }
                contributes = true;
                if (effect.isMultiplicative()) {
                    multiplierProduct *= 1f + SkillEffect.addedMultiplier(total) / 100f;
                } else {
                    percentTotal += total;
                }
            }
            if (contributes && exceedsCap(percentTotal, multiplierProduct)) {
                cap.contributingEffects().forEach(totals::remove);
                totals.put(cap.cappedEffect(), -MAX_REDUCTION_PERCENT);
            }
        }
    }
}
