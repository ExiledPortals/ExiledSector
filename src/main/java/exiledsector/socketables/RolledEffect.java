package exiledsector.socketables;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RolledEffect {

    private static final String ENTRY_SEPARATOR = ";";
    private static final String VALUE_SEPARATOR = ":";

    private final String effectName;
    private final float magnitude;

    public RolledEffect(String effectName, float magnitude) {
        this.effectName = effectName;
        this.magnitude = magnitude;
    }

    static String encode(List<RolledEffect> effects) {
        StringBuilder encoded = new StringBuilder();
        for (RolledEffect effect : effects) {
            if (!encoded.isEmpty()) {
                encoded.append(ENTRY_SEPARATOR);
            }
            String magnitude = Float.toString(effect.magnitude);
            encoded.append(effect.effectName).append(VALUE_SEPARATOR)
                    .append(magnitude.endsWith(".0") ? magnitude.substring(0, magnitude.length() - 2) : magnitude);
        }
        return encoded.toString();
    }

    static List<RolledEffect> decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        List<RolledEffect> effects = new ArrayList<>();
        for (String entry : encoded.split(ENTRY_SEPARATOR)) {
            if (entry.isEmpty()) {
                continue;
            }
            int separator = entry.lastIndexOf(VALUE_SEPARATOR);
            if (separator <= 0) {
                return null;
            }
            try {
                effects.add(new RolledEffect(entry.substring(0, separator), Float.parseFloat(entry.substring(separator + 1))));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return effects;
    }

    public String effectName() {
        return effectName;
    }

    public float magnitude() {
        return magnitude;
    }

    public SkillEffect effect() {
        try {
            return SkillEffect.byName(effectName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public StyledText description() {
        return description(null);
    }

    public StyledText description(SocketableDefinition.PoolEntry range) {
        SkillEffect effect = effect();
        if (effect == null) {
            return null;
        }
        StyledText text = display(effect, magnitude);
        if (range == null || range.min() == range.max() || text.spans().isEmpty()) {
            return text;
        }
        float low = Math.abs(range.min()) <= Math.abs(range.max()) ? range.min() : range.max();
        float high = low == range.min() ? range.max() : range.min();
        String lowText = valueText(effect, low);
        String highText = valueText(effect, high);
        if (lowText == null || highText == null) {
            return text;
        }
        return text.insert(text.spans().get(0).end(), Translation.msg("socketable.rollRange").arg("low", lowText).arg("high", highText).text());
    }

    private static StyledText display(SkillEffect effect, float magnitude) {
        return new DescriptionLine(effect.description(magnitude), effect.lowerIsBetter()).display();
    }

    private static String valueText(SkillEffect effect, float magnitude) {
        StyledText text = effect.description(magnitude);
        if (text.spans().isEmpty()) {
            return null;
        }
        StyledText.Span value = text.spans().get(0);
        return text.plain().substring(value.start(), value.end());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RolledEffect rolled && rolled.magnitude == magnitude && rolled.effectName.equals(effectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effectName, magnitude);
    }

    @Override
    public String toString() {
        return effectName + "=" + magnitude;
    }
}
