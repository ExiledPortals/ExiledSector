package exiledlib.skills.skilleffect.special;

import exiledlib.i18n.StyledText;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.SkillEffect;

public record AffinityTally() implements EffectBacking {

    @Override
    public boolean supportsTemporaryGating() {
        return false;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return EffectText.signed(effect, magnitude).styled();
    }
}
