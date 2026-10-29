package exiledsector.skills;

import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EffectTotals {

    public record Group(Float temporarySeconds, CombatGate gate, Map<SkillEffect, Float> totals) {
    }

    private record Duration(Float temporarySeconds, CombatGate gate) {
    }

    private static final Duration PERMANENT = new Duration(null, null);

    private final List<EffectContribution> contributions;
    private final Map<SkillEffect, Float> pooledMultipliers;
    private final List<DamageTakenCaps.Cap> reachedCaps;
    private final Set<DiminishedStat> diminishedStats;
    private final List<Group> groups;

    private EffectTotals(List<EffectContribution> contributions, Map<SkillEffect, Float> pooledMultipliers, List<DamageTakenCaps.Cap> reachedCaps,
                         Set<DiminishedStat> diminishedStats, List<Group> groups) {
        this.contributions = contributions;
        this.pooledMultipliers = pooledMultipliers;
        this.reachedCaps = reachedCaps;
        this.diminishedStats = diminishedStats;
        this.groups = groups;
    }

    public static EffectTotals of(List<EffectContribution> contributions, float bonusScale) {
        return of(contributions, bonusScale, StatBaseline.NONE, DiminishingReturns.OFF);
    }

    public static EffectTotals of(List<EffectContribution> treeContributions, float bonusScale, StatBaseline baseline,
                                  DiminishingReturns.Config diminishing) {
        List<EffectContribution> contributions = BallisticEnergyMirror.withMirroredBonuses(
                AffinitySteps.withSteppedBonuses(SocketedItemScaling.withScaledItems(treeContributions)));
        NpcBonusScaling scaling = NpcBonusScaling.of(bonusScale, contributions);
        List<EffectContribution> scaledContributions = new ArrayList<>();
        for (EffectContribution contribution : contributions) {
            List<SkillTypeEffect> scaledEffects = new ArrayList<>();
            for (SkillTypeEffect effect : contribution.effects()) {
                scaledEffects.add(new SkillTypeEffect(effect.effect(), scaling.scaled(contribution, effect.effect(), effect.magnitude())));
            }
            scaledContributions.add(contribution.withEffects(Collections.unmodifiableList(scaledEffects)));
        }
        Map<SkillEffect, Float> diminishingRatios = DiminishingReturns.ratios(diminishing, baseline, permanentSums(scaledContributions));
        List<EffectContribution> resolvedContributions = new ArrayList<>();
        Map<Duration, Map<SkillEffect, Float>> totalsByDuration = new LinkedHashMap<>();
        for (EffectContribution scaled : scaledContributions) {
            Duration duration = new Duration(scaled.temporarySeconds(), scaled.gate());
            Map<SkillEffect, Float> durationTotals = totalsByDuration.computeIfAbsent(duration, key -> new LinkedHashMap<>());
            List<SkillTypeEffect> effects = PERMANENT.equals(duration) ? diminished(scaled.effects(), diminishingRatios) : scaled.effects();
            resolvedContributions.add(scaled.withEffects(effects));
            for (SkillTypeEffect effect : effects) {
                boolean compounds = !PERMANENT.equals(duration) && effect.effect().isMultiplicative();
                durationTotals.merge(effect.effect(), effect.magnitude(), compounds ? EffectTotals::compounded : Float::sum);
            }
        }
        Map<SkillEffect, Float> permanentTotals = permanentTotals(totalsByDuration.getOrDefault(PERMANENT, Map.of()));
        Map<SkillEffect, Float> pooledMultipliers = new LinkedHashMap<>();
        permanentTotals.forEach((effect, total) -> {
            if (effect.isMultiplicative()) {
                pooledMultipliers.put(effect, total);
            }
        });
        List<DamageTakenCaps.Cap> reachedCaps = DamageTakenCaps.reachedBy(permanentTotals);
        List<Group> groups = new ArrayList<>();
        totalsByDuration.forEach((duration, durationTotals) -> groups.add(PERMANENT.equals(duration)
                ? new Group(null, null, Collections.unmodifiableMap(DamageTakenCaps.capped(permanentTotals, reachedCaps)))
                : new Group(duration.temporarySeconds(), duration.gate(), Collections.unmodifiableMap(durationTotals))));
        return new EffectTotals(List.copyOf(resolvedContributions), Collections.unmodifiableMap(pooledMultipliers), List.copyOf(reachedCaps),
                diminishedStatsOf(diminishingRatios), List.copyOf(groups));
    }

    private static DiminishingReturns.Sums permanentSums(List<EffectContribution> contributions) {
        DiminishingReturns.Sums sums = new DiminishingReturns.Sums(new HashMap<>(), new HashMap<>());
        for (EffectContribution contribution : contributions) {
            if (contribution.permanent()) {
                addSigned(sums, contribution.effects());
            }
        }
        return sums;
    }

    private static void addSigned(DiminishingReturns.Sums sums, List<SkillTypeEffect> effects) {
        for (SkillTypeEffect effect : effects) {
            if (DiminishedStat.of(effect.effect()) != null) {
                (effect.magnitude() > 0f ? sums.bonuses() : sums.drawbacks()).merge(effect.effect(), effect.magnitude(), Float::sum);
            }
        }
    }

    private static Set<DiminishedStat> diminishedStatsOf(Map<SkillEffect, Float> diminishingRatios) {
        Set<DiminishedStat> stats = EnumSet.noneOf(DiminishedStat.class);
        diminishingRatios.keySet().forEach(effect -> stats.add(DiminishedStat.of(effect)));
        return Collections.unmodifiableSet(stats);
    }

    private static List<SkillTypeEffect> diminished(List<SkillTypeEffect> effects, Map<SkillEffect, Float> diminishingRatios) {
        if (diminishingRatios.isEmpty()) {
            return effects;
        }
        List<SkillTypeEffect> diminishedEffects = new ArrayList<>(effects.size());
        for (SkillTypeEffect effect : effects) {
            Float ratio = effect.magnitude() > 0f ? diminishingRatios.get(effect.effect()) : null;
            diminishedEffects.add(ratio == null ? effect : new SkillTypeEffect(effect.effect(), effect.magnitude() * ratio));
        }
        return Collections.unmodifiableList(diminishedEffects);
    }

    public static Map<SkillEffect, Float> permanentTotals(Map<SkillEffect, Float> sums) {
        Map<SkillEffect, Float> totals = new LinkedHashMap<>();
        sums.forEach((effect, sum) -> totals.put(effect, effect.isMultiplicative() ? SkillEffect.addedMultiplier(sum) : sum));
        return totals;
    }

    static float compounded(float firstMultiplier, float secondMultiplier) {
        return ((1f + firstMultiplier / 100f) * (1f + secondMultiplier / 100f) - 1f) * 100f;
    }

    public List<EffectContribution> contributions() {
        return contributions;
    }

    public Map<SkillEffect, Float> pooledMultipliers() {
        return pooledMultipliers;
    }

    public List<DamageTakenCaps.Cap> reachedCaps() {
        return reachedCaps;
    }

    public Set<DiminishedStat> diminishedStats() {
        return diminishedStats;
    }

    public List<Group> groups() {
        return groups;
    }
}
