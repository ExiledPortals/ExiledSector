package exiledsector.charts.atlas;

import exiledsector.data.EnumKeys;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierEffect;

public enum AtlasModifier implements ModifierEffect {

    ATLAS_VISIBILITY_LANES(true);

    static final String KEY_PREFIX = "atlas.modifier.";

    private final boolean counted;

    AtlasModifier(boolean counted) {
        this.counted = counted;
    }

    public boolean isCounted() {
        return counted;
    }

    public String key() {
        return KEY_PREFIX + name();
    }

    @Override
    public StyledText description(float magnitude) {
        if (counted) {
            int count = Math.round(magnitude);
            return Translation.msg(key()).count(count).arg("value", count).styled();
        }
        return Translation.msg(key()).arg("value", magnitude).styled();
    }

    public static AtlasModifier byNameOrNull(String effectName) {
        return EnumKeys.parse(AtlasModifier.class, effectName);
    }
}
