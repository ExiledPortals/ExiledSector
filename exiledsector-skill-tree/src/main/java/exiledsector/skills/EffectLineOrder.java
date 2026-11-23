package exiledsector.skills;

import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;

import java.util.ArrayList;
import java.util.List;

final class EffectLineOrder {

    private EffectLineOrder() {
    }

    static List<SkillTypeEffect> withHullSizedUpsidesFirst(List<SkillTypeEffect> baseEffects, List<SkillTypeEffect> hullSizedEffects) {
        List<SkillTypeEffect> ordered = new ArrayList<>(baseEffects);
        int upsideSlot = firstDownsideIndex(baseEffects);
        for (SkillTypeEffect effect : hullSizedEffects) {
            if (isDownside(effect)) {
                ordered.add(effect);
            } else {
                ordered.add(upsideSlot++, effect);
            }
        }
        return ordered;
    }

    private static int firstDownsideIndex(List<SkillTypeEffect> effects) {
        for (int index = 0; index < effects.size(); index++) {
            if (isDownside(effects.get(index))) {
                return index;
            }
        }
        return effects.size();
    }

    private static boolean isDownside(SkillTypeEffect effect) {
        StyledText text = effect.effect().description(effect.magnitude());
        if (text == null) {
            return false;
        }
        StyledText shown = effect.effect().invertsDescriptionColours() ? text.inverted() : text;
        return hasSpan(shown, Style.BAD) && !hasSpan(shown, Style.GOOD);
    }

    private static boolean hasSpan(StyledText text, Style style) {
        return text.spans().stream().anyMatch(span -> span.style() == style);
    }
}
