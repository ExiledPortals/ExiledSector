package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NpcBonusScaling {

    static final Set<String> UNSCALED_NODE_TAGS = Set.of("logistics", "fleet_support", "campaign_only");
    static final Set<String> UNSCALED_EFFECTS = Set.of("BURN_LEVEL_FLAT", "SYSTEM_CHARGES_FLAT");
    static final NpcBonusScaling NONE = new NpcBonusScaling(1f, Map.of());

    private final float bonusScale;
    private final Map<SkillEffect, Float> factors;

    private NpcBonusScaling(float bonusScale, Map<SkillEffect, Float> factors) {
        this.bonusScale = bonusScale;
        this.factors = factors;
    }

    static NpcBonusScaling of(float bonusScale, ShipSkillData shipData, List<AllocatedNode> allocatedNodes, HullSize hullSize) {
        if (Float.compare(bonusScale, 1f) == 0) {
            return NONE;
        }
        Map<SkillEffect, Float> bonusTotals = new HashMap<>();
        for (AllocatedNode node : allocatedNodes) {
            if (node.effectiveType().getVanillaHullModId() != null || !scalesNode(node)) {
                continue;
            }
            for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(shipData, node, hullSize)) {
                if (isScalableBonus(effect.effect(), effect.magnitude())) {
                    bonusTotals.merge(effect.effect(), effect.magnitude(), Float::sum);
                }
            }
        }
        Map<SkillEffect, Float> effectFactors = new HashMap<>();
        bonusTotals.forEach((effect, bonusTotal) -> effectFactors.put(effect, factor(effect.statMode(), bonusTotal, bonusScale)));
        return new NpcBonusScaling(bonusScale, Collections.unmodifiableMap(effectFactors));
    }

    float scaled(AllocatedNode node, SkillEffect effect, float magnitude) {
        if (Float.compare(bonusScale, 1f) == 0 || !isScalableBonus(effect, magnitude) || !scalesNode(node)) {
            return magnitude;
        }
        return magnitude * factors.getOrDefault(effect, 1f);
    }

    static boolean isScalableBonus(SkillEffect effect, float magnitude) {
        if (effect.statMode() == null || UNSCALED_EFFECTS.contains(effect.name()) || Float.compare(magnitude, 0f) == 0) {
            return false;
        }
        return (magnitude > 0f) != effect.lowerIsBetter();
    }

    static boolean scalesNode(AllocatedNode node) {
        return Collections.disjoint(node.node().effectiveTags(node.effectiveType()), UNSCALED_NODE_TAGS);
    }

    static float factor(StatMode mode, float bonusTotal, float bonusScale) {
        if (bonusTotal >= 0f || mode == StatMode.FLAT) {
            return bonusScale;
        }
        float reduction = Math.min(1f, -bonusTotal / 100f);
        float scaledReduction = 1f - (float) Math.pow(1f - reduction, bonusScale);
        return scaledReduction / reduction;
    }
}
