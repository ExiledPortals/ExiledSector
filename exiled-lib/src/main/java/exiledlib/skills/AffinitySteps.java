package exiledlib.skills;

import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.defense.DefenseSkillEffect;
import exiledlib.skills.skilleffect.flux.FluxSkillEffect;
import exiledlib.skills.skilleffect.logistics.LogisticsSkillEffect;
import exiledlib.skills.skilleffect.special.SpecialSkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AffinitySteps {

    public static final String MOD_ID_PREFIX = "exiledLib_affinity_";
    static final int SMALL_STEP = 5;
    static final int LARGE_STEP = 10;
    static final int LOW_TECH_ARMOR_STEP = 15;
    static final int MIDLINE_ARMOR_STEP = 20;

    public record Step(int size, SkillEffect effect, float bonusPerStep) {
    }

    public record Origin() {
    }

    public record ScaledOrigin() {
    }

    private static final Origin ORIGIN = new Origin();
    private static final ScaledOrigin SCALED_ORIGIN = new ScaledOrigin();
    private static final Map<SkillEffect, List<Step>> STEPS_BY_AFFINITY = stepsByAffinity();

    private AffinitySteps() {
    }

    private static Map<SkillEffect, List<Step>> stepsByAffinity() {
        Map<SkillEffect, List<Step>> steps = new LinkedHashMap<>();
        steps.put(SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT, List.of(
                new Step(SMALL_STEP, DefenseSkillEffect.HULL_MULT, 2f),
                new Step(LOW_TECH_ARMOR_STEP, DefenseSkillEffect.ARMOR_MULT, 1f),
                new Step(SMALL_STEP, LogisticsSkillEffect.FUEL_CAPACITY_MULT, 1f)));
        steps.put(SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT, List.of(
                new Step(SMALL_STEP, FluxSkillEffect.FLUX_CAPACITY_MULT, 2f),
                new Step(LARGE_STEP, FluxSkillEffect.FLUX_DISSIPATION_MULT, 1f),
                new Step(SMALL_STEP, LogisticsSkillEffect.CREW_CAPACITY_MULT, 1f)));
        steps.put(SpecialSkillEffect.MIDLINE_AFFINITY_FLAT, List.of(
                new Step(SMALL_STEP, FluxSkillEffect.FLUX_CAPACITY_MULT, 2f),
                new Step(SMALL_STEP, DefenseSkillEffect.HULL_MULT, 2f),
                new Step(MIDLINE_ARMOR_STEP, DefenseSkillEffect.ARMOR_MULT, 1f),
                new Step(LARGE_STEP, FluxSkillEffect.FLUX_DISSIPATION_MULT, 1f),
                new Step(SMALL_STEP, LogisticsSkillEffect.CARGO_CAPACITY_MULT, 1f)));
        return Collections.unmodifiableMap(steps);
    }

    public static boolean isAffinity(SkillEffect effect) {
        return STEPS_BY_AFFINITY.containsKey(effect);
    }

    public static List<SkillEffect> affinities() {
        return List.copyOf(STEPS_BY_AFFINITY.keySet());
    }

    public static List<Step> stepsFor(SkillEffect affinity) {
        return STEPS_BY_AFFINITY.getOrDefault(affinity, List.of());
    }

    public static int stepsReached(float affinity, int stepSize) {
        return Math.max(0, (int) Math.floor(affinity / stepSize));
    }

    public static Map<SkillEffect, Float> totals(List<EffectContribution> contributions) {
        Map<SkillEffect, Float> totals = new LinkedHashMap<>();
        float toHighest = 0f;
        boolean lowestAsHighest = false;
        Map<SkillEffect, Float> exchangePercents = new LinkedHashMap<>();
        for (EffectContribution contribution : contributions) {
            if (contribution.permanent()) {
                for (SkillTypeEffect effect : contribution.effects()) {
                    SkillEffect exchangeTarget = exchangeTarget(effect.effect());
                    if (isAffinity(effect.effect())) {
                        totals.merge(effect.effect(), effect.magnitude(), Float::sum);
                    } else if (effect.effect() == SpecialSkillEffect.HIGHEST_AFFINITY_FLAT_PER_S_MOD) {
                        toHighest += effect.magnitude();
                    } else if (effect.effect() == SpecialSkillEffect.LOWEST_AFFINITY_AS_HIGHEST) {
                        lowestAsHighest = true;
                    } else if (exchangeTarget != null) {
                        exchangePercents.merge(exchangeTarget, effect.magnitude(), Float::sum);
                    }
                }
            }
        }
        if (toHighest != 0f) {
            totals.merge(highest(totals), toHighest, Float::sum);
        }
        if (lowestAsHighest) {
            totals.put(lowest(totals), totals.getOrDefault(highest(totals), 0f));
        }
        exchangePercents.forEach((target, percent) -> exchange(totals, target, percent));
        return totals;
    }

    static SkillEffect exchangeTarget(SkillEffect effect) {
        if (effect == SpecialSkillEffect.AFFINITY_EXCHANGE_TO_LOW_TECH_PERCENT) {
            return SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT;
        }
        if (effect == SpecialSkillEffect.AFFINITY_EXCHANGE_TO_HIGH_TECH_PERCENT) {
            return SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT;
        }
        if (effect == SpecialSkillEffect.AFFINITY_EXCHANGE_TO_MIDLINE_PERCENT) {
            return SpecialSkillEffect.MIDLINE_AFFINITY_FLAT;
        }
        return null;
    }

    private static void exchange(Map<SkillEffect, Float> totals, SkillEffect target, float percent) {
        float removed = 0f;
        for (SkillEffect affinity : STEPS_BY_AFFINITY.keySet()) {
            if (affinity != target && totals.containsKey(affinity)) {
                removed += Math.max(0f, totals.get(affinity));
                totals.put(affinity, 0f);
            }
        }
        totals.merge(target, removed * percent / 100f, Float::sum);
    }

    private static SkillEffect highest(Map<SkillEffect, Float> totals) {
        SkillEffect highest = SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT;
        for (SkillEffect affinity : STEPS_BY_AFFINITY.keySet()) {
            if (totals.getOrDefault(affinity, 0f) > totals.getOrDefault(highest, 0f)) {
                highest = affinity;
            }
        }
        return highest;
    }

    private static SkillEffect lowest(Map<SkillEffect, Float> totals) {
        SkillEffect lowest = SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT;
        for (SkillEffect affinity : STEPS_BY_AFFINITY.keySet()) {
            if (totals.getOrDefault(affinity, 0f) < totals.getOrDefault(lowest, 0f)) {
                lowest = affinity;
            }
        }
        return lowest;
    }

    public static List<SkillTypeEffect> bonuses(Map<SkillEffect, Float> affinityTotals) {
        Map<SkillEffect, Float> bonusTotals = new LinkedHashMap<>();
        affinityTotals.forEach((affinity, total) -> {
            for (Step step : stepsFor(affinity)) {
                int steps = stepsReached(total, step.size());
                if (steps > 0) {
                    bonusTotals.merge(step.effect(), steps * step.bonusPerStep(), Float::sum);
                }
            }
        });
        List<SkillTypeEffect> bonuses = new ArrayList<>();
        bonusTotals.forEach((effect, bonus) -> bonuses.add(new SkillTypeEffect(effect, bonus)));
        return bonuses;
    }

    public static List<SkillTypeEffect> scaledBonuses(List<EffectContribution> contributions, Map<SkillEffect, Float> affinityTotals) {
        Map<SkillEffect, Float> bonusTotals = new LinkedHashMap<>();
        for (EffectContribution contribution : contributions) {
            if (!contribution.permanent()) {
                continue;
            }
            for (SkillTypeEffect effect : contribution.effects()) {
                AffinityScaling scaling = effect.effect().affinityScaling();
                int steps = scaling == null ? 0 : scaling.stepsReached(affinityTotals);
                if (steps > 0) {
                    bonusTotals.merge(scaling.target(), steps * effect.magnitude(), Float::sum);
                }
            }
        }
        List<SkillTypeEffect> bonuses = new ArrayList<>();
        bonusTotals.forEach((effect, bonus) -> bonuses.add(new SkillTypeEffect(effect, bonus)));
        return bonuses;
    }

    static List<EffectContribution> withSteppedBonuses(List<EffectContribution> contributions) {
        Map<SkillEffect, Float> affinityTotals = totals(contributions);
        List<SkillTypeEffect> bonuses = bonuses(affinityTotals);
        List<SkillTypeEffect> scaled = scaledBonuses(contributions, affinityTotals);
        if (bonuses.isEmpty() && scaled.isEmpty()) {
            return contributions;
        }
        List<EffectContribution> withBonuses = new ArrayList<>(contributions);
        if (!bonuses.isEmpty()) {
            withBonuses.add(EffectContribution.scaledPermanent(ORIGIN, MOD_ID_PREFIX, List.copyOf(bonuses)));
        }
        if (!scaled.isEmpty()) {
            withBonuses.add(EffectContribution.scaledPermanent(SCALED_ORIGIN, MOD_ID_PREFIX + "scaled_", List.copyOf(scaled)));
        }
        return withBonuses;
    }
}
