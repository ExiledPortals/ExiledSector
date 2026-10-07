package exiledsector.socketables;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RolledEffect {

    private static final String HULL_VALUE_SEPARATOR = "/";

    private final String effectName;
    private final float magnitude;

    public RolledEffect(String effectName, float magnitude) {
        this.effectName = currentName(effectName);
        this.magnitude = magnitude;
    }

    static String currentName(String effectName) {
        if (effectName == null) {
            return null;
        }
        try {
            return SkillEffect.byName(effectName).name();
        } catch (IllegalArgumentException e) {
            return effectName;
        }
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

    public StyledText description(PoolEntry rollRange) {
        SkillEffect effect = effect();
        if (effect == null) {
            return null;
        }
        StyledText effectText = display(effect, magnitude);
        if (rollRange == null || !rollRange.canVary() || effectText.spans().isEmpty()) {
            return effectText;
        }
        String lowText = valueText(effect, rollRange.boundNearestZero());
        String highText = valueText(effect, rollRange.boundFarthestFromZero());
        if (lowText == null || highText == null) {
            return effectText;
        }
        return effectText.insert(effectText.spans().get(0).end(), Translation.msg("socketable.rollRange").arg("low", lowText).arg("high", highText).text());
    }

    public StyledText hullValuesDescription(List<Float> hullValues) {
        SkillEffect effect = effect();
        if (effect == null || hullValues.isEmpty()) {
            return effect == null ? null : display(effect, magnitude);
        }
        StyledText firstValueText = display(effect, hullValues.get(0));
        List<String> valueTexts = new ArrayList<>(hullValues.size());
        for (float hullValue : hullValues) {
            String valueText = valueText(effect, hullValue);
            if (valueText == null || firstValueText.spans().isEmpty()) {
                return firstValueText;
            }
            valueTexts.add(valueText);
        }
        StyledText.Span valueSpan = firstValueText.spans().get(0);
        String joinedValues = joinSharingAffixes(valueTexts);
        int lengthChange = joinedValues.length() - (valueSpan.end() - valueSpan.start());
        List<StyledText.Span> spans = new ArrayList<>(firstValueText.spans().size());
        spans.add(new StyledText.Span(valueSpan.start(), valueSpan.end() + lengthChange, valueSpan.style()));
        for (StyledText.Span laterSpan : firstValueText.spans().subList(1, firstValueText.spans().size())) {
            spans.add(new StyledText.Span(laterSpan.start() + lengthChange, laterSpan.end() + lengthChange, laterSpan.style()));
        }
        String plain = firstValueText.plain();
        return new StyledText(plain.substring(0, valueSpan.start()) + joinedValues + plain.substring(valueSpan.end()), spans);
    }

    static String joinSharingAffixes(List<String> valueTexts) {
        String sharedPrefix = sharedNonNumericEdge(valueTexts, true);
        String sharedSuffix = sharedNonNumericEdge(valueTexts, false);
        if (sharedPrefix.length() + sharedSuffix.length() > valueTexts.stream().mapToInt(String::length).min().orElse(0)) {
            sharedPrefix = "";
            sharedSuffix = "";
        }
        StringBuilder joined = new StringBuilder(sharedPrefix);
        for (int i = 0; i < valueTexts.size(); i++) {
            String valueText = valueTexts.get(i);
            if (i > 0) {
                joined.append(HULL_VALUE_SEPARATOR);
            }
            joined.append(valueText, sharedPrefix.length(), valueText.length() - sharedSuffix.length());
        }
        return joined.append(sharedSuffix).toString();
    }

    private static String sharedNonNumericEdge(List<String> valueTexts, boolean leading) {
        String first = valueTexts.get(0);
        int edgeLength = 0;
        while (edgeLength < first.length()) {
            char candidate = leading ? first.charAt(edgeLength) : first.charAt(first.length() - 1 - edgeLength);
            if (Character.isDigit(candidate) || candidate == '.') {
                break;
            }
            int position = edgeLength;
            boolean shared = valueTexts.stream().allMatch(valueText -> valueText.length() > position
                    && (leading ? valueText.charAt(position) : valueText.charAt(valueText.length() - 1 - position)) == candidate);
            if (!shared) {
                break;
            }
            edgeLength++;
        }
        return leading ? first.substring(0, edgeLength) : first.substring(first.length() - edgeLength);
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
