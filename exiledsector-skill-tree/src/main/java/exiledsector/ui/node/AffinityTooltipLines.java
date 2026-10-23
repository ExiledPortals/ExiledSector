package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.AffinitySteps;
import exiledlib.skills.DescriptionLine;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.SimpleStatEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class AffinityTooltipLines {

    private AffinityTooltipLines() {
    }

    static NodeDescription forRoot(SkillType rootType, ShipSkillData shipData, HullSize hullSize) {
        SkillTypeEffect rootAffinity = RootAffinityNumber.rootAffinity(rootType);
        if (rootAffinity == null) {
            return new NodeDescription(List.of(), List.of());
        }
        SkillEffect affinity = rootAffinity.effect();
        String affinityName = affinityName(affinity);
        List<DescriptionLine> applied = new ArrayList<>();
        if (ShipTrees.resolveStartingRootId(shipData) != null) {
            float total = AffinitySteps.totals(SkillTreeEffectSource.contributions(shipData, hullSize)).getOrDefault(affinity, 0f);
            applied.add(new DescriptionLine(Translation.msg("ui.node.affinity.total").arg("affinity", affinityName).arg("total", total).styled(), false));
            for (SkillTypeEffect bonus : AffinitySteps.bonuses(Map.of(affinity, total))) {
                StyledText line = bonus.effect().description(bonus.magnitude(), hullSize);
                if (line != null) {
                    applied.add(new DescriptionLine(line, bonus.effect().invertsDescriptionColours()));
                }
            }
        }
        List<DescriptionLine> perStep = new ArrayList<>();
        for (AffinitySteps.Step step : AffinitySteps.stepsFor(affinity)) {
            String statKey = statKeyOf(step.effect());
            if (statKey != null) {
                perStep.add(new DescriptionLine(Translation.msg("ui.node.affinity.perStep").arg("value", step.bonusPerStep())
                        .arg("stat", Translation.text(statKey)).arg("step", step.size()).arg("affinity", affinityName).styled(), false));
            }
        }
        return new NodeDescription(applied, perStep);
    }

    static String affinityName(SkillEffect affinity) {
        return Translation.text("affinity." + affinity.name() + ".name");
    }

    private static String statKeyOf(SkillEffect effect) {
        return effect instanceof BackedSkillEffect backed && backed.backing() instanceof SimpleStatEffect stat ? stat.statKey() : null;
    }
}
