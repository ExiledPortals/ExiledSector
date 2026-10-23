package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.i18n.Translation;
import exiledlib.skills.AffinitySteps;
import exiledlib.skills.DescriptionLine;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.SimpleStatEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class AffinityTooltipLines {

    private AffinityTooltipLines() {
    }

    static List<DescriptionLine> forShip(ShipSkillData shipData, HullSize hullSize) {
        Map<SkillEffect, Float> totals = AffinitySteps.totals(SkillTreeEffectSource.contributions(shipData, hullSize));
        List<DescriptionLine> lines = new ArrayList<>();
        for (SkillEffect affinity : AffinitySteps.affinities()) {
            float total = totals.getOrDefault(affinity, 0f);
            String affinityName = affinityName(affinity);
            lines.add(new DescriptionLine(Translation.msg("ui.node.affinity.total").arg("affinity", affinityName).arg("total", total).styled(), false));
            if (total > 0f) {
                for (AffinitySteps.Step step : AffinitySteps.stepsFor(affinity)) {
                    String statKey = statKeyOf(step.effect());
                    if (statKey != null) {
                        float current = AffinitySteps.stepsReached(total, step.size()) * step.bonusPerStep();
                        lines.add(new DescriptionLine(Translation.msg("ui.node.affinity.bonus").arg("value", step.bonusPerStep())
                                .arg("stat", Translation.text(statKey)).arg("step", step.size()).arg("affinity", affinityName)
                                .arg("current", current).styled(), false));
                    }
                }
            }
        }
        return lines;
    }

    static String affinityName(SkillEffect affinity) {
        return Translation.text("affinity." + affinity.name() + ".name");
    }

    private static String statKeyOf(SkillEffect effect) {
        return effect instanceof BackedSkillEffect backed && backed.backing() instanceof SimpleStatEffect stat ? stat.statKey() : null;
    }
}
