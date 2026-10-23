package exiledlib.skills.skilleffect;

import exiledlib.i18n.Message;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;

public final class EffectText {

    private EffectText() {
    }

    static String key(SkillEffect effect) {
        return "effect." + effect.name();
    }

    public static StyledText templated(SkillEffect effect, float magnitude) {
        return msg(effect).arg("value", magnitude).styled();
    }

    public static Message msg(SkillEffect effect) {
        return Translation.msg(key(effect));
    }

    public static Message msg(SkillEffect effect, String variant) {
        return Translation.msg(key(effect) + "." + variant);
    }

    public static Message signed(SkillEffect effect, float magnitude) {
        return msg(effect, magnitude >= 0 ? "more" : "less").arg("value", Math.abs(magnitude));
    }
}
