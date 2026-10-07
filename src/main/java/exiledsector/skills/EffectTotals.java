package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EffectTotals {

    public record NodeEffects(AllocatedNode node, List<SkillTypeEffect> effects) {
    }

    public record Group(Float temporarySeconds, Map<SkillEffect, Float> totals) {
    }

    private record Duration(Float temporarySeconds) {
    }

    private static final Duration PERMANENT = new Duration(null);

    private final List<NodeEffects> nodeEffects;
    private final Map<SkillEffect, Float> pooledMultipliers;
    private final List<DamageTakenCaps.Cap> reachedCaps;
    private final List<Group> groups;

    private EffectTotals(List<NodeEffects> nodeEffects, Map<SkillEffect, Float> pooledMultipliers,
                         List<DamageTakenCaps.Cap> reachedCaps, List<Group> groups) {
        this.nodeEffects = nodeEffects;
        this.pooledMultipliers = pooledMultipliers;
        this.reachedCaps = reachedCaps;
        this.groups = groups;
    }

    public static EffectTotals of(ShipSkillData shipData, List<AllocatedNode> allocatedNodes, HullSize hullSize, float bonusScale) {
        NpcBonusScaling scaling = NpcBonusScaling.of(bonusScale, shipData, allocatedNodes, hullSize);
        List<NodeEffects> resolvedNodeEffects = new ArrayList<>();
        Map<Duration, Map<SkillEffect, Float>> totalsByDuration = new LinkedHashMap<>();
        for (AllocatedNode node : allocatedNodes) {
            Duration duration = new Duration(node.effectiveType().getTemporaryAfterDeploymentSeconds());
            Map<SkillEffect, Float> durationTotals = totalsByDuration.computeIfAbsent(duration, key -> new LinkedHashMap<>());
            List<SkillTypeEffect> scaledEffects = scaledEffects(shipData, node, hullSize, scaling);
            resolvedNodeEffects.add(new NodeEffects(node, scaledEffects));
            for (SkillTypeEffect effect : scaledEffects) {
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
                ? new Group(null, Collections.unmodifiableMap(cappedTotals(permanentTotals, reachedCaps)))
                : new Group(duration.temporarySeconds(), Collections.unmodifiableMap(durationTotals))));
        return new EffectTotals(List.copyOf(resolvedNodeEffects), Collections.unmodifiableMap(pooledMultipliers),
                List.copyOf(reachedCaps), List.copyOf(groups));
    }

    private static List<SkillTypeEffect> scaledEffects(ShipSkillData shipData, AllocatedNode node, HullSize hullSize,
                                                       NpcBonusScaling scaling) {
        if (node.effectiveType().getVanillaHullModId() != null) {
            return List.of();
        }
        List<SkillTypeEffect> scaledEffects = new ArrayList<>();
        for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(shipData, node, hullSize)) {
            scaledEffects.add(new SkillTypeEffect(effect.effect(), scaling.scaled(node, effect.effect(), effect.magnitude())));
        }
        return Collections.unmodifiableList(scaledEffects);
    }

    private static Map<SkillEffect, Float> permanentTotals(Map<SkillEffect, Float> sums) {
        Map<SkillEffect, Float> totals = new LinkedHashMap<>();
        sums.forEach((effect, sum) -> totals.put(effect, effect.isMultiplicative() ? SkillEffect.addedMultiplier(sum) : sum));
        return totals;
    }

    private static Map<SkillEffect, Float> cappedTotals(Map<SkillEffect, Float> permanentTotals, List<DamageTakenCaps.Cap> reachedCaps) {
        Map<SkillEffect, Float> capped = new LinkedHashMap<>(permanentTotals);
        for (DamageTakenCaps.Cap cap : reachedCaps) {
            cap.contributingEffects().forEach(capped::remove);
            capped.put(cap.cappedEffect(), -DamageTakenCaps.MAX_REDUCTION_PERCENT);
        }
        return capped;
    }

    static float compounded(float firstMultiplier, float secondMultiplier) {
        return ((1f + firstMultiplier / 100f) * (1f + secondMultiplier / 100f) - 1f) * 100f;
    }

    public List<NodeEffects> nodeEffects() {
        return nodeEffects;
    }

    public Map<SkillEffect, Float> pooledMultipliers() {
        return pooledMultipliers;
    }

    public List<DamageTakenCaps.Cap> reachedCaps() {
        return reachedCaps;
    }

    public List<Group> groups() {
        return groups;
    }
}
