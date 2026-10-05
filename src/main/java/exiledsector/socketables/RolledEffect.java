package exiledsector.socketables;

import exiledsector.i18n.StyledText;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.Objects;

public final class RolledEffect {

    private final String effectName;
    private final float magnitude;

    public RolledEffect(String effectName, float magnitude) {
        this.effectName = effectName;
        this.magnitude = magnitude;
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
        return text.insert(text.spans().get(0).end(), " (" + lowText + "-" + highText + ")");
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
