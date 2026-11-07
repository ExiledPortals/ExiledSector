package exiledsector.skills;

import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;

import java.util.List;
import java.util.Map;

public record AffinityScaling(Filter filter, int stepSize, SkillEffect target) implements EffectBacking {

    public static final int SMALL_STEP = 5;
    public static final int LARGE_STEP = 10;
    public static final int LARGEST_STEP = 15;
    public static final int NAV_RATING_STEP = 50;
    private static final int NO_STEP = 0;

    public enum Filter {
        LOW_TECH, HIGH_TECH, MIDLINE, ALL, ALL_EQUAL, HIGHEST;

        public List<SkillEffect> affinities() {
            return switch (this) {
                case LOW_TECH -> List.of(SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT);
                case HIGH_TECH -> List.of(SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT);
                case MIDLINE -> List.of(SpecialSkillEffect.MIDLINE_AFFINITY_FLAT);
                case ALL, ALL_EQUAL, HIGHEST -> List.of(SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT, SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT,
                        SpecialSkillEffect.MIDLINE_AFFINITY_FLAT);
            };
        }

        public boolean reads(SkillEffect affinity, Map<SkillEffect, Float> affinityTotals) {
            return this == HIGHEST ? affinity == AffinitySteps.highest(affinityTotals) : affinities().contains(affinity);
        }

        public boolean readsOneAffinity() {
            return this != ALL && this != ALL_EQUAL;
        }
    }

    public static AffinityScaling whileAllEqual(SkillEffect target) {
        return new AffinityScaling(Filter.ALL_EQUAL, NO_STEP, target);
    }

    public int stepsReached(Map<SkillEffect, Float> affinityTotals) {
        if (filter == Filter.ALL_EQUAL) {
            return AffinitySteps.allEqual(affinityTotals) ? 1 : 0;
        }
        if (filter == Filter.HIGHEST) {
            return AffinitySteps.stepsReached(affinityTotals.getOrDefault(AffinitySteps.highest(affinityTotals), 0f), stepSize);
        }
        float affinity = 0f;
        for (SkillEffect counted : filter.affinities()) {
            affinity += affinityTotals.getOrDefault(counted, 0f);
        }
        return AffinitySteps.stepsReached(affinity, stepSize);
    }

    @Override
    public boolean supportsTemporaryGating() {
        return false;
    }

    @Override
    public boolean lowerIsBetter() {
        return target.lowerIsBetter();
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        if (filter == Filter.ALL_EQUAL) {
            return EffectText.signed(effect, magnitude).styled();
        }
        return EffectText.signed(effect, magnitude).arg("step", stepSize).styled();
    }
}
