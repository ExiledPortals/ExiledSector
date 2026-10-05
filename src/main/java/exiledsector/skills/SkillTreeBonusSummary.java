package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.WeaponEffectTooltipAggregator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkillTreeBonusSummary {

    public record Summary(SkillType root, int level, int nodeCount, List<SkillType> notables, List<DescriptionLine> bonuses) {
    }

    private record Group(Float temporarySeconds) {
    }

    private SkillTreeBonusSummary() {
    }

    public static Summary of(ShipSkillData data, HullSize hullSize) {
        SkillType root = null;
        int nodeCount = 0;
        List<SkillType> notables = new ArrayList<>();
        Map<Group, Map<SkillEffect, Float>> totals = new LinkedHashMap<>();
        for (AllocatedNode allocated : AllocatedNode.of(data)) {
            SkillType type = allocated.effectiveType();
            SkillTier tier = allocated.node().getType().getTier();
            if (tier == SkillTier.ROOT && root == null) {
                root = type;
            } else {
                nodeCount++;
            }
            if (tier == SkillTier.NOTABLE || tier == SkillTier.KEYSTONE) {
                notables.add(type);
            }
            Map<SkillEffect, Float> group = totals.computeIfAbsent(new Group(type.getTemporaryAfterDeploymentSeconds()),
                    key -> new LinkedHashMap<>());
            for (SkillTypeEffect typeEffect : AllocatedSkillEffects.appliedEffects(data, allocated, hullSize)) {
                SkillEffect effect = typeEffect.effect();
                group.merge(effect, typeEffect.magnitude(), Float::sum);
            }
        }
        return new Summary(root, data.getLevel(), nodeCount, notables, describe(totals));
    }

    private static List<DescriptionLine> describe(Map<Group, Map<SkillEffect, Float>> totals) {
        List<DescriptionLine> lines = new ArrayList<>();
        for (Map.Entry<Group, Map<SkillEffect, Float>> group : totals.entrySet()) {
            List<SkillTypeEffect> effects = new ArrayList<>();
            group.getValue().forEach((effect, total) -> {
                if (!effect.isMultiplicative()) {
                    effects.add(new SkillTypeEffect(effect, rounded(total)));
                } else if (rounded(total) != 0f) {
                    effects.add(new SkillTypeEffect(effect, rounded(SkillEffect.addedMultiplier(total))));
                }
            });
            for (SkillTypeEffect effect : WeaponEffectTooltipAggregator.collapse(effects)) {
                StyledText text = effect.effect().description(effect.magnitude());
                if (text != null) {
                    lines.add(new DescriptionLine(withDuration(text, group.getKey().temporarySeconds()),
                            effect.effect().lowerIsBetter()));
                }
            }
        }
        return lines;
    }

    static float rounded(float total) {
        return Math.round(total * 100f) / 100f;
    }

    private static StyledText withDuration(StyledText text, Float seconds) {
        if (seconds == null) {
            return text;
        }
        return Translation.msg("summary.forFirst").arg("seconds", seconds).arg("text", text).styled();
    }
}
