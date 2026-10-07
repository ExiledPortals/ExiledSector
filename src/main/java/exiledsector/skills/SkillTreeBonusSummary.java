package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.WeaponEffectTooltipAggregator;

import java.util.ArrayList;
import java.util.List;

public final class SkillTreeBonusSummary {

    public record Summary(SkillType root, int level, int nodeCount, List<SkillType> notables, List<DescriptionLine> bonuses) {
    }

    private SkillTreeBonusSummary() {
    }

    public static Summary of(ShipSkillData data, HullSize hullSize) {
        return of(data, hullSize, 1f);
    }

    public static Summary of(ShipSkillData data, HullSize hullSize, float bonusScale) {
        SkillType root = null;
        int nodeCount = 0;
        List<SkillType> notables = new ArrayList<>();
        List<AllocatedNode> allocatedNodes = AllocatedNode.of(data);
        for (AllocatedNode allocated : allocatedNodes) {
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
        }
        EffectTotals effectTotals = EffectTotals.of(data, allocatedNodes, hullSize, bonusScale);
        return new Summary(root, data.getLevel(), nodeCount, notables, describe(effectTotals.groups()));
    }

    private static List<DescriptionLine> describe(List<EffectTotals.Group> groups) {
        List<DescriptionLine> lines = new ArrayList<>();
        for (EffectTotals.Group group : groups) {
            List<SkillTypeEffect> effects = new ArrayList<>();
            group.totals().forEach((effect, total) -> {
                float shownTotal = rounded(total);
                if (!effect.isMultiplicative() || shownTotal != 0f) {
                    effects.add(new SkillTypeEffect(effect, shownTotal));
                }
            });
            for (SkillTypeEffect effect : WeaponEffectTooltipAggregator.collapse(effects)) {
                StyledText text = effect.effect().description(effect.magnitude());
                if (text != null) {
                    lines.add(new DescriptionLine(withDuration(text, group.temporarySeconds()), effect.effect().lowerIsBetter()));
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
