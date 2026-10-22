package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
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

    public record NodeEffects(AllocatedNode node, List<SkillTypeEffect> effects) {
    }

    public record SectionEffects(ShipSections.UnlockedSection unlockedSection, List<SkillTypeEffect> effects) {
    }

    public record Group(Float temporarySeconds, Map<SkillEffect, Float> totals) {
    }

    private record Duration(Float temporarySeconds) {
    }

    private static final Duration PERMANENT = new Duration(null);

    private final List<NodeEffects> nodeEffects;
    private final List<SectionEffects> sectionEffects;
    private final Map<SkillEffect, Float> pooledMultipliers;
    private final List<DamageTakenCaps.Cap> reachedCaps;
    private final Set<DiminishedStat> diminishedStats;
    private final List<Group> groups;

    private EffectTotals(List<NodeEffects> nodeEffects, List<SectionEffects> sectionEffects, Map<SkillEffect, Float> pooledMultipliers,
                         List<DamageTakenCaps.Cap> reachedCaps, Set<DiminishedStat> diminishedStats, List<Group> groups) {
        this.nodeEffects = nodeEffects;
        this.sectionEffects = sectionEffects;
        this.pooledMultipliers = pooledMultipliers;
        this.reachedCaps = reachedCaps;
        this.diminishedStats = diminishedStats;
        this.groups = groups;
    }

    public static EffectTotals of(ShipSkillData shipData, List<AllocatedNode> allocatedNodes, HullSize hullSize, float bonusScale) {
        return of(shipData, allocatedNodes, hullSize, bonusScale, List.of());
    }

    public static EffectTotals of(ShipSkillData shipData, List<AllocatedNode> allocatedNodes, HullSize hullSize, float bonusScale,
                                  List<ShipSections.UnlockedSection> sections) {
        return of(shipData, allocatedNodes, hullSize, bonusScale, sections, StatBaseline.NONE, DiminishingReturns.OFF);
    }

    public static EffectTotals of(ShipSkillData shipData, List<AllocatedNode> allocatedNodes, HullSize hullSize, float bonusScale,
                                  List<ShipSections.UnlockedSection> sections, StatBaseline baseline, DiminishingReturns.Config diminishing) {
        NpcBonusScaling scaling = NpcBonusScaling.of(bonusScale, shipData, allocatedNodes, hullSize, sections);
        List<NodeEffects> scaledNodeEffects = new ArrayList<>();
        for (AllocatedNode node : allocatedNodes) {
            scaledNodeEffects.add(new NodeEffects(node, scaledEffects(shipData, node, hullSize, scaling)));
        }
        List<SectionEffects> scaledSectionEffects = new ArrayList<>();
        for (ShipSections.UnlockedSection unlocked : sections) {
            List<SkillTypeEffect> scaledEffects = new ArrayList<>();
            for (SkillTypeEffect effect : ShipSections.appliedEffects(shipData, unlocked, hullSize)) {
                scaledEffects.add(new SkillTypeEffect(effect.effect(), scaling.scaledSectionItem(effect.effect(), effect.magnitude())));
            }
            if (!scaledEffects.isEmpty()) {
                scaledSectionEffects.add(new SectionEffects(unlocked, Collections.unmodifiableList(scaledEffects)));
            }
        }
        Map<SkillEffect, Float> diminishingRatios = DiminishingReturns.ratios(diminishing, baseline,
                permanentSums(scaledNodeEffects, scaledSectionEffects));
        List<NodeEffects> resolvedNodeEffects = new ArrayList<>();
        Map<Duration, Map<SkillEffect, Float>> totalsByDuration = new LinkedHashMap<>();
        for (NodeEffects scaledNode : scaledNodeEffects) {
            AllocatedNode node = scaledNode.node();
            Duration duration = new Duration(node.effectiveType().getTemporaryAfterDeploymentSeconds());
            Map<SkillEffect, Float> durationTotals = totalsByDuration.computeIfAbsent(duration, key -> new LinkedHashMap<>());
            List<SkillTypeEffect> nodeEffects = PERMANENT.equals(duration) ? diminished(scaledNode.effects(), diminishingRatios) : scaledNode.effects();
            resolvedNodeEffects.add(new NodeEffects(node, nodeEffects));
            for (SkillTypeEffect effect : nodeEffects) {
                boolean compounds = !PERMANENT.equals(duration) && effect.effect().isMultiplicative();
                durationTotals.merge(effect.effect(), effect.magnitude(), compounds ? EffectTotals::compounded : Float::sum);
            }
        }
        List<SectionEffects> resolvedSectionEffects = new ArrayList<>();
        for (SectionEffects scaledSection : scaledSectionEffects) {
            List<SkillTypeEffect> sectionEffects = diminished(scaledSection.effects(), diminishingRatios);
            resolvedSectionEffects.add(new SectionEffects(scaledSection.unlockedSection(), sectionEffects));
            Map<SkillEffect, Float> permanentSums = totalsByDuration.computeIfAbsent(PERMANENT, key -> new LinkedHashMap<>());
            sectionEffects.forEach(effect -> permanentSums.merge(effect.effect(), effect.magnitude(), Float::sum));
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
                ? new Group(null, Collections.unmodifiableMap(DamageTakenCaps.capped(permanentTotals, reachedCaps)))
                : new Group(duration.temporarySeconds(), Collections.unmodifiableMap(durationTotals))));
        return new EffectTotals(List.copyOf(resolvedNodeEffects), List.copyOf(resolvedSectionEffects), Collections.unmodifiableMap(pooledMultipliers),
                List.copyOf(reachedCaps), diminishedStatsOf(diminishingRatios), List.copyOf(groups));
    }

    private static DiminishingReturns.Sums permanentSums(List<NodeEffects> nodeEffects, List<SectionEffects> sectionEffects) {
        DiminishingReturns.Sums sums = new DiminishingReturns.Sums(new HashMap<>(), new HashMap<>());
        for (NodeEffects node : nodeEffects) {
            if (node.node().effectiveType().getTemporaryAfterDeploymentSeconds() == null) {
                addSigned(sums, node.effects());
            }
        }
        sectionEffects.forEach(section -> addSigned(sums, section.effects()));
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

    private static List<SkillTypeEffect> scaledEffects(ShipSkillData shipData, AllocatedNode node, HullSize hullSize,
                                                       NpcBonusScaling scaling) {
        List<SkillTypeEffect> scaledEffects = new ArrayList<>();
        for (SkillTypeEffect effect : AllocatedSkillEffects.treeEffects(shipData, node, hullSize)) {
            scaledEffects.add(new SkillTypeEffect(effect.effect(), scaling.scaled(node, effect.effect(), effect.magnitude())));
        }
        return Collections.unmodifiableList(scaledEffects);
    }

    public static Map<SkillEffect, Float> permanentTotals(Map<SkillEffect, Float> sums) {
        Map<SkillEffect, Float> totals = new LinkedHashMap<>();
        sums.forEach((effect, sum) -> totals.put(effect, effect.isMultiplicative() ? SkillEffect.addedMultiplier(sum) : sum));
        return totals;
    }

    static float compounded(float firstMultiplier, float secondMultiplier) {
        return ((1f + firstMultiplier / 100f) * (1f + secondMultiplier / 100f) - 1f) * 100f;
    }

    public List<NodeEffects> nodeEffects() {
        return nodeEffects;
    }

    public List<SectionEffects> sectionEffects() {
        return sectionEffects;
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
