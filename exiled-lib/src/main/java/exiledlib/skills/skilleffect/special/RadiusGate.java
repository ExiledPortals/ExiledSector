package exiledlib.skills.skilleffect.special;

import exiledlib.i18n.StyledText;
import exiledlib.skills.CombatGate;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.SkillEffect;

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
