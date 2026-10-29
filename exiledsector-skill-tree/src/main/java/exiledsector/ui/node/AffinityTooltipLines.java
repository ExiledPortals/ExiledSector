package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.AffinitySteps;
import exiledsector.skills.SocketedItemScaling;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.SimpleStatEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class AffinityTooltipLines {

    private AffinityTooltipLines() {
    }

    static NodeDescription forRoot(SkillType rootType, ShipSkillData shipData, HullSize hullSize) {
        return forAffinity(rootType, shipData, hullSize, ShipTrees.resolveStartingRootId(shipData) != null);
    }

    static NodeDescription forNode(SkillNode node, SkillType effectiveType, ShipSkillData shipData, HullSize hullSize) {
        return forAffinity(effectiveType, shipData, hullSize, shipData.isAllocated(node.getId()));
    }

    private static NodeDescription forAffinity(SkillType type, ShipSkillData shipData, HullSize hullSize, boolean showTotal) {
        SkillTypeEffect typeAffinity = RootAffinityNumber.affinityOf(type);
        if (typeAffinity == null) {
            return new NodeDescription(List.of(), List.of());
        }
        SkillEffect affinity = typeAffinity.effect();
        String affinityName = affinityName(affinity);
        List<DescriptionLine> applied = new ArrayList<>();
        if (showTotal) {
            float total = AffinitySteps.totals(SocketedItemScaling.withScaledItems(SkillTreeEffectSource.contributions(shipData, hullSize))).getOrDefault(affinity, 0f);
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
