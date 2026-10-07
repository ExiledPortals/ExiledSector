package exiledsector.socketables;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
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

    public StyledText description(SocketableDefinition.PoolEntry rollRange) {
        SkillEffect effect = effect();
        if (effect == null) {
            return null;
        }
        StyledText effectText = display(effect, magnitude);
        if (rollRange == null || rollRange.min() == rollRange.max() || effectText.spans().isEmpty()) {
            return effectText;
        }
        float lowBound = Math.abs(rollRange.min()) <= Math.abs(rollRange.max()) ? rollRange.min() : rollRange.max();
        float highBound = lowBound == rollRange.min() ? rollRange.max() : rollRange.min();
        String lowText = valueText(effect, lowBound);
        String highText = valueText(effect, highBound);
        if (lowText == null || highText == null) {
            return effectText;
        }
        return effectText.insert(effectText.spans().get(0).end(), Translation.msg("socketable.rollRange").arg("low", lowText).arg("high", highText).text());
    }

    private static StyledText display(SkillEffect effect, float magnitude) {
        return new DescriptionLine(effect.description(magnitude), effect.lowerIsBetter()).display();
    }

    private static String valueText(SkillEffect effect, float magnitude) {
        StyledText descriptionText = effect.description(magnitude);
        if (descriptionText.spans().isEmpty()) {
            return null;
        }
        StyledText.Span valueSpan = descriptionText.spans().get(0);
        return descriptionText.plain().substring(valueSpan.start(), valueSpan.end());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RolledEffect otherEffect && otherEffect.magnitude == magnitude && otherEffect.effectName.equals(effectName);
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
