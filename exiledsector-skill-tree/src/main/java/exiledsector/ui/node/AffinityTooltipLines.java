package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.AffinityScaling;
import exiledsector.skills.AffinitySteps;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.TypeAffinity;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.SimpleStatEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.fighter.FighterSkillEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class AffinityTooltipLines {

    private static final Set<SkillEffect> OTHER_AFFINITY_EFFECTS = Set.of(SpecialSkillEffect.HIGHEST_AFFINITY_FLAT_PER_S_MOD,
            SpecialSkillEffect.LOWEST_AFFINITY_AS_HIGHEST, SpecialSkillEffect.AFFINITY_EXCHANGE_TO_LOW_TECH_PERCENT,
            SpecialSkillEffect.AFFINITY_EXCHANGE_TO_HIGH_TECH_PERCENT, SpecialSkillEffect.AFFINITY_EXCHANGE_TO_MIDLINE_PERCENT,
            FighterSkillEffect.STRIKECRAFT_GAIN_AFFINITY_BONUSES, SpecialSkillEffect.LOW_TECH_AFFINITY_PERCENT,
            SpecialSkillEffect.AFFINITY_EQUALITY_TOLERANCE_FLAT);
    private static final StyledText LINE_BREAK = StyledText.of("\n");

    private AffinityTooltipLines() {
    }

    static NodeDescription forType(SkillType type, List<EffectContribution> shipContributions, HullSize hullSize) {
        SkillTypeEffect typeAffinity = TypeAffinity.of(type);
        if (typeAffinity == null) {
            return new NodeDescription(List.of(), List.of());
        }
        SkillEffect affinity = typeAffinity.effect();
        return new NodeDescription(current(affinity, shipContributions, hullSize), grants(affinity, shipContributions, hullSize));
    }

    private static List<DescriptionLine> current(SkillEffect affinity, List<EffectContribution> shipContributions, HullSize hullSize) {
        String affinityName = affinityName(affinity);
        Map<SkillEffect, Float> totals = AffinitySteps.totals(shipContributions);
        float total = totals.getOrDefault(affinity, 0f);
        List<StyledText> ownBonuses = bonusLines(AffinitySteps.bonuses(Map.of(affinity, total)), hullSize);
        ownBonuses.addAll(bonusLines(scaledBonuses(shipContributions, totals,
                filter -> filter.readsOneAffinity() && filter.reads(affinity, totals)), hullSize));
        List<StyledText> combinedBonuses = bonusLines(scaledBonuses(shipContributions, totals,
                filter -> !filter.readsOneAffinity() && filter.reads(affinity, totals)), hullSize);

        List<StyledText> totalBlock = new ArrayList<>();
        totalBlock.add(Translation.msg("ui.node.affinity.total").arg("affinity", affinityName).arg("total", total).styled());
        if (!ownBonuses.isEmpty()) {
            totalBlock.add(bulletList(Translation.msg("ui.node.affinity.currentlyGrants").arg("affinity", affinityName).styled(), ownBonuses));
        }
        List<DescriptionLine> lines = new ArrayList<>();
        lines.add(new DescriptionLine(StyledText.join(LINE_BREAK, totalBlock), false));
        if (!combinedBonuses.isEmpty()) {
            lines.add(new DescriptionLine(bulletList(Translation.styled("ui.node.affinity.combinedGrants"), combinedBonuses), false));
        }
        return lines;
    }

    private static List<DescriptionLine> grants(SkillEffect affinity, List<EffectContribution> shipContributions, HullSize hullSize) {
        String affinityName = affinityName(affinity);
        Map<SkillEffect, Float> totals = AffinitySteps.totals(shipContributions);
        List<StyledText> perStep = new ArrayList<>();
        for (AffinitySteps.Step step : AffinitySteps.stepsFor(affinity)) {
            String statKey = statKeyOf(step.effect());
            if (statKey != null) {
                perStep.add(Translation.msg("ui.node.affinity.perStep").arg("value", step.bonusPerStep())
                        .arg("stat", Translation.text(statKey)).arg("step", step.size()).arg("affinity", affinityName).styled());
            }
        }
        perStep.addAll(bonusLines(permanentEffects(shipContributions,
                effect -> effect.affinityScaling() != null && effect.affinityScaling().filter().reads(affinity, totals)), hullSize));

        List<DescriptionLine> lines = new ArrayList<>();
        lines.add(new DescriptionLine(bulletList(Translation.msg("ui.node.affinity.grants").arg("affinity", affinityName).styled(), perStep), false));
        List<StyledText> otherEffects = otherEffectLines(permanentEffects(shipContributions, OTHER_AFFINITY_EFFECTS::contains), hullSize);
        if (!otherEffects.isEmpty()) {
            lines.add(new DescriptionLine(bulletList(Translation.styled("ui.node.affinity.otherEffects"), otherEffects), false));
        }
        return lines;
    }

    private static List<SkillTypeEffect> scaledBonuses(List<EffectContribution> shipContributions, Map<SkillEffect, Float> totals,
                                                       Predicate<AffinityScaling.Filter> reads) {
        Map<SkillEffect, Float> bonuses = new LinkedHashMap<>();
        float equalTolerance = AffinitySteps.equalTolerance(shipContributions);
        for (SkillTypeEffect effect : permanentEffects(shipContributions, effect -> effect.affinityScaling() != null
                && reads.test(effect.affinityScaling().filter()))) {
            AffinityScaling scaling = effect.effect().affinityScaling();
            int steps = scaling.stepsReached(totals, equalTolerance);
            if (steps > 0) {
                bonuses.merge(scaling.target(), steps * effect.magnitude(), Float::sum);
            }
        }
        return asEffects(bonuses);
    }

    private static List<SkillTypeEffect> permanentEffects(List<EffectContribution> shipContributions, Predicate<SkillEffect> wanted) {
        Map<SkillEffect, Float> magnitudes = new LinkedHashMap<>();
        for (EffectContribution contribution : shipContributions) {
            if (contribution.permanent()) {
                for (SkillTypeEffect effect : contribution.effects()) {
                    if (wanted.test(effect.effect())) {
                        magnitudes.merge(effect.effect(), effect.magnitude(), Float::sum);
                    }
                }
            }
        }
        return asEffects(magnitudes);
    }

    private static List<SkillTypeEffect> asEffects(Map<SkillEffect, Float> magnitudes) {
        List<SkillTypeEffect> effects = new ArrayList<>();
        magnitudes.forEach((effect, magnitude) -> effects.add(new SkillTypeEffect(effect, magnitude)));
        return effects;
    }

    private static List<StyledText> bonusLines(List<SkillTypeEffect> bonuses, HullSize hullSize) {
        List<StyledText> lines = new ArrayList<>();
        for (SkillTypeEffect bonus : bonuses) {
            StyledText line = bonus.effect().description(bonus.magnitude(), hullSize);
            if (line != null) {
                lines.add(new DescriptionLine(line, bonus.effect().invertsDescriptionColours()).display());
            }
        }
        return lines;
    }

    private static List<StyledText> otherEffectLines(List<SkillTypeEffect> effects, HullSize hullSize) {
        List<StyledText> lines = new ArrayList<>();
        for (SkillTypeEffect effect : effects) {
            if (effect.effect() != SpecialSkillEffect.HIGHEST_AFFINITY_FLAT_PER_S_MOD) {
                lines.addAll(bonusLines(List.of(effect), hullSize));
            } else if (effect.magnitude() != 0f) {
                lines.add(Translation.msg("ui.node.affinity.highestFromSMods").arg("value", effect.magnitude()).styled());
            }
        }
        return lines;
    }

    private static StyledText bulletList(StyledText header, List<StyledText> items) {
        List<StyledText> lines = new ArrayList<>();
        lines.add(header);
        items.forEach(item -> lines.add(Translation.msg("ui.node.affinity.item").arg("line", item).styled()));
        return StyledText.join(LINE_BREAK, lines);
    }

    static String affinityName(SkillEffect affinity) {
        return Translation.text("affinity." + affinity.name() + ".name");
    }

    private static String statKeyOf(SkillEffect effect) {
        return effect instanceof BackedSkillEffect backed && backed.backing() instanceof SimpleStatEffect stat ? stat.statKey() : null;
    }
}
