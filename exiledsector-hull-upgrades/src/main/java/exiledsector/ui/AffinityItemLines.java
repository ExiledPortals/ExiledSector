package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.AffinityScaling;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.items.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

final class AffinityItemLines {

    private AffinityItemLines() {
    }

    static List<StyledText> current(Item item, HullSize hullSize, Supplier<Map<SkillEffect, Float>> affinityTotals) {
        if (item == null) {
            return List.of();
        }
        List<StyledText> lines = new ArrayList<>();
        Map<SkillEffect, Float> totals = null;
        for (SkillTypeEffect effect : item.skillEffects(hullSize)) {
            AffinityScaling scaling = effect.effect().affinityScaling();
            if (scaling == null) {
                continue;
            }
            if (totals == null) {
                totals = affinityTotals.get();
            }
            float bonus = scaling.stepsReached(totals) * effect.magnitude();
            lines.add(Translation.msg("hullItem.affinity.current").arg("line", scaling.target().description(bonus)).styled());
        }
        return lines;
    }
}
