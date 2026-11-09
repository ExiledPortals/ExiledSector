package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.DescribeContext;
import exiledsector.skills.skilleffect.weapon.WeaponEffectTooltipAggregator;

import java.util.ArrayList;
import java.util.List;

public final class DescribedEffects {

    public record Line(SkillTypeEffect effect, StyledText text) {
    }

    private DescribedEffects() {
    }

    public static List<Line> of(List<SkillTypeEffect> effects, HullSize hullSize) {
        List<SkillTypeEffect> collapsed = WeaponEffectTooltipAggregator.collapse(effects);
        DescribeContext context = DescribeContext.of(collapsed, hullSize);
        List<Line> lines = new ArrayList<>();
        for (SkillTypeEffect effect : collapsed) {
            if (context.showsInParentLine(effect.effect())) {
                continue;
            }
            StyledText text = effect.effect().description(effect.magnitude(), context);
            if (text != null) {
                lines.add(new Line(effect, text));
            }
        }
        return lines;
    }
}
