package exiledsector.skills.skilleffect;

import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

final class EffectText {

    private EffectText() {
    }

    static String key(SkillEffect effect) {
        return "effect." + effect.name();
    }

    static StyledText templated(SkillEffect effect, float magnitude) {
        return msg(effect).arg("value", magnitude).styled();
    }

    static Message msg(SkillEffect effect) {
        return Translation.msg(key(effect));
    }

    static Message msg(SkillEffect effect, String variant) {
        return Translation.msg(key(effect) + "." + variant);
    }

    static Message signed(SkillEffect effect, float magnitude) {
        return msg(effect, magnitude >= 0 ? "more" : "less").arg("value", Math.abs(magnitude));
    }
}
