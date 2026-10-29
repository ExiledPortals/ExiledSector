package exiledsector.ui.node;

import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;

import java.awt.Color;

final class AffinityColours {

    static final Color LOW_TECH = new Color(226, 154, 72);
    static final Color HIGH_TECH = new Color(140, 228, 255);
    static final Color MIDLINE = new Color(156, 207, 150);

    private AffinityColours() {
    }

    static Color of(SkillEffect affinity) {
        if (affinity == SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT) {
            return LOW_TECH;
        }
        return affinity == SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT ? HIGH_TECH : MIDLINE;
    }
}
