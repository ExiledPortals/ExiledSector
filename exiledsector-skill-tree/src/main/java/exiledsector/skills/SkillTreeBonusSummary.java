package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.CombatGate;
import exiledlib.skills.DescriptionLine;
import exiledlib.skills.DiminishedStat;
import exiledlib.skills.DiminishingReturns;
import exiledlib.skills.EffectContribution;
import exiledlib.skills.EffectTotals;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.StatBaseline;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.weapon.WeaponEffectTooltipAggregator;
import exiledsector.effects.SkillTreeEffectSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class SkillTreeBonusSummary {

    public record Summary(SkillType root, int level, int nodeCount, List<SkillType> notables, List<DescriptionLine> bonuses,
                          Set<DiminishedStat> diminishedStats) {
    }

    public record EffectLine(SkillEffect effect, float magnitude, Float temporarySeconds, CombatGate gate, DescriptionLine line) {
    }

    private SkillTreeBonusSummary() {
    }

    public static Summary of(ShipSkillData data, HullSize hullSize) {
        return of(data, hullSize, 1f);
    }

    public static Summary of(ShipSkillData data, HullSize hullSize, float bonusScale) {
        return of(data, hullSize, bonusScale, List.of());
    }

    public static Summary of(ShipSkillData data, HullSize hullSize, float bonusScale, List<EffectContribution> otherContributions) {
        return of(data, hullSize, bonusScale, otherContributions, StatBaseline.NONE, DiminishingReturns.OFF);
    }

    public static Summary of(ShipSkillData data, HullSize hullSize, float bonusScale, List<EffectContribution> otherContributions,
                             StatBaseline statBaseline, DiminishingReturns.Config diminishing) {
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
        List<EffectContribution> contributions = new ArrayList<>(SkillTreeEffectSource.contributions(data, hullSize));
        contributions.addAll(otherContributions);
        EffectTotals effectTotals = EffectTotals.of(contributions, bonusScale, statBaseline, diminishing);
        return new Summary(root, data.getLevel(), nodeCount, notables, describe(effectTotals.groups()), effectTotals.diminishedStats());
    }

    private static List<DescriptionLine> describe(List<EffectTotals.Group> groups) {
        return lines(groups, effect -> true).stream().map(EffectLine::line).toList();
    }

    public static List<EffectLine> lines(List<EffectTotals.Group> groups, Predicate<SkillEffect> permanentShown) {
        List<EffectLine> lines = new ArrayList<>();
        for (EffectTotals.Group group : groups) {
            boolean permanent = group.temporarySeconds() == null && group.gate() == null;
            List<SkillTypeEffect> effects = new ArrayList<>();
            group.totals().forEach((effect, total) -> {
                float shownTotal = rounded(total);
                if ((!effect.isMultiplicative() || shownTotal != 0f) && (!permanent || permanentShown.test(effect))) {
                    effects.add(new SkillTypeEffect(effect, shownTotal));
                }
            });
            for (SkillTypeEffect effect : WeaponEffectTooltipAggregator.collapse(effects)) {
                StyledText text = effect.effect().description(effect.magnitude());
                if (text != null) {
                    lines.add(new EffectLine(effect.effect(), effect.magnitude(), group.temporarySeconds(), group.gate(),
                            new DescriptionLine(withGate(withDuration(text, group.temporarySeconds()), group.gate()),
                                    effect.effect().invertsDescriptionColours())));
                }
            }
        }
        return lines;
    }

    static float rounded(float total) {
        return Math.round(total * 100f) / 100f;
    }

    private static StyledText withGate(StyledText text, CombatGate gate) {
        return gate == null ? text : Translation.msg("summary.gated." + gate.name()).arg("text", text).styled();
    }

    private static StyledText withDuration(StyledText text, Float seconds) {
        if (seconds == null) {
            return text;
        }
        return Translation.msg("summary.forFirst").arg("seconds", seconds).arg("text", text).styled();
    }
}
