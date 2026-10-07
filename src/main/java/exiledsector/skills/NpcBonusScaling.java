package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class NpcBonusScaling {

    static final Set<String> UNSCALED_NODE_TAGS = Set.of("logistics", "fleet_support", "campaign_only");
    static final Set<String> CAMPAIGN_EFFECTS = Set.of("BURN_LEVEL_FLAT", "FUEL_CAPACITY_PERCENT", "FUEL_CAPACITY_FLAT",
            "CARGO_CAPACITY_PERCENT", "CARGO_CAPACITY_FLAT", "CREW_CAPACITY_PERCENT", "CREW_CAPACITY_FLAT", "SENSOR_PROFILE_PERCENT",
            "SENSOR_PROFILE_MULT", "SENSOR_STRENGTH_PERCENT", "SENSOR_STRENGTH_FLAT", "CR_RECOVERY_RATE_PERCENT", "REPAIR_RATE_PER_DAY_PERCENT",
            "MIN_CREW_MULT", "MIN_CREW_PERCENT", "MIN_CREW_FLAT", "SUPPLIES_PER_MONTH_MULT", "FUEL_USE_MULT", "CREW_LOSS_PERCENT",
            "CREW_LOSS_MULT", "GROUND_SUPPORT_FLAT", "CORONA_RESISTANCE_MULT");
    static final Set<String> COUNT_EFFECTS = Set.of("SYSTEM_CHARGES_FLAT");
    static final Set<String> DRAWBACK_PARAMETER_EFFECTS = Set.of("WEAPON_RANGE_THRESHOLD_FLAT", "CONVERTED_HANGAR_REFIT_TIME_MULT",
            "CONVERTED_HANGAR_REPLACEMENT_RATE_MULT", "CONVERTED_HANGAR_RELAUNCH_TIME_FLAT", "CONVERTED_HANGAR_MIN_CREW_FLAT");
    public static final Set<String> UNSCALED_EFFECTS = Stream.of(CAMPAIGN_EFFECTS, COUNT_EFFECTS, DRAWBACK_PARAMETER_EFFECTS)
            .flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());
    public static final NpcBonusScaling NONE = new NpcBonusScaling(1f, Map.of());

    private final float bonusScale;
    private final Map<SkillEffect, Float> factors;

    private NpcBonusScaling(float bonusScale, Map<SkillEffect, Float> factors) {
        this.bonusScale = bonusScale;
        this.factors = factors;
    }

    public static NpcBonusScaling of(float bonusScale, ShipSkillData shipData, List<AllocatedNode> allocatedNodes, HullSize hullSize) {
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

    public float scaled(AllocatedNode node, SkillEffect effect, float magnitude) {
        if (Float.compare(bonusScale, 1f) == 0 || node.effectiveType().getVanillaHullModId() != null
                || !isScalableBonus(effect, magnitude) || !scalesNode(node)) {
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

    public static float factor(StatMode mode, float bonusTotal, float bonusScale) {
        if (bonusTotal >= 0f || mode == StatMode.FLAT) {
            return bonusScale;
        }
        float reduction = Math.min(1f, -bonusTotal / 100f);
        float scaledReduction = 1f - (float) Math.pow(1f - reduction, bonusScale);
        return scaledReduction / reduction;
    }
}
