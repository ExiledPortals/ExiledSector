package exiledsector.socketables;

import exiledsector.i18n.StyledText;
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
        SkillEffect effect = effect();
        return effect == null ? null : effect.description(magnitude);
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
