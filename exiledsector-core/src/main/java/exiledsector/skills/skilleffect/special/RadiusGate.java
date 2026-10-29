package exiledsector.skills.skilleffect.special;

import exiledsector.i18n.StyledText;
import exiledsector.skills.CombatGate;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SkillEffect;

public record RadiusGate(CombatGate gate) implements EffectBacking {

    @Override
    public boolean supportsTemporaryGating() {
        return false;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return EffectText.msg(effect).arg("value", magnitude).styled();
    }
}
