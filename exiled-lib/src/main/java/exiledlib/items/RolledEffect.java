package exiledlib.items;

import exiledlib.modifiers.ModifierEffect;
import exiledlib.modifiers.ModifierEffects;
import exiledlib.modifiers.ModifierLine;
import exiledlib.modifiers.ValueScaling;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.DescriptionLine;

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
        String currentName = ItemEffects.anyCurrentName(effectName);
        return currentName == null ? effectName : currentName;
    }

    private Object readResolve() {
        String current = currentName(effectName);
        return Objects.equals(current, effectName) ? this : new RolledEffect(current, magnitude);
    }

    public String effectName() {
        return effectName;
    }

    public float magnitude() {
        return magnitude;
    }

    public ModifierEffect effect() {
        return effect(ItemEffects.SKILL_EFFECTS);
    }

    public ModifierEffect effect(ModifierEffects effects) {
        return effects.byName(effectName);
    }

    public StyledText description() {
        return description(null);
    }

    public StyledText description(ModifierLine rollRange) {
        return description(ItemEffects.SKILL_EFFECTS, rollRange);
    }

    public StyledText description(ModifierEffects effects, ModifierLine rollRange) {
        ModifierEffect effect = effect(effects);
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
        return scaledValuesDescription(ItemEffects.SKILL_EFFECTS, hullValues);
    }

    public StyledText scaledValuesDescription(ModifierEffects effects, List<Float> scaledValues) {
        ModifierEffect effect = effect(effects);
        if (effect == null || scaledValues.isEmpty()) {
            return effect == null ? null : display(effect, magnitude);
        }
        StyledText firstValueText = display(effect, scaledValues.get(0));
        List<String> valueTexts = new ArrayList<>(scaledValues.size());
        for (float scaledValue : scaledValues) {
            String valueText = valueText(effect, scaledValue);
            if (valueText == null || firstValueText.spans().isEmpty()) {
                return firstValueText;
            }
            valueTexts.add(valueText);
        }
        return replaceValue(firstValueText, joinSharingAffixes(valueTexts));
    }

    public StyledText scaledValuesDescription(ModifierEffects effects, ModifierLine scaledLine) {
        if (scaledLine.scaling() instanceof ValueScaling.PerTierBand tierBands && tierBands.variesWithinAPoint()) {
            return tierBandRangesDescription(effects, tierBands);
        }
        return scaledValuesDescription(effects, scaledLine.scaledValues());
    }

    private StyledText tierBandRangesDescription(ModifierEffects effects, ValueScaling.PerTierBand tierBands) {
        ModifierEffect effect = effect(effects);
        if (effect == null) {
            return null;
        }
        StyledText firstBandText = display(effect, tierBands.lowAt(0));
        List<String> bandTexts = new ArrayList<>(tierBands.values().size());
        for (int band = 0; band < tierBands.values().size(); band++) {
            String bandText = bandRangeText(effect, tierBands.lowAt(band), tierBands.highAt(band));
            if (bandText == null || firstBandText.spans().isEmpty()) {
                return firstBandText;
            }
            bandTexts.add(bandText);
        }
        return replaceValue(firstBandText, joinSharingAffixes(bandTexts));
    }

    private static String bandRangeText(ModifierEffect effect, float bandLow, float bandHigh) {
        boolean lowNearestZero = Math.abs(bandLow) <= Math.abs(bandHigh);
        String weakestText = valueText(effect, lowNearestZero ? bandLow : bandHigh);
        String strongestText = valueText(effect, lowNearestZero ? bandHigh : bandLow);
        if (weakestText == null || strongestText == null || weakestText.equals(strongestText)) {
            return weakestText;
        }
        List<String> bothTexts = List.of(weakestText, strongestText);
        String sharedPrefix = sharedNonNumericEdge(bothTexts, true);
        String sharedSuffix = sharedNonNumericEdge(bothTexts, false);
        if (sharedPrefix.length() + sharedSuffix.length() > Math.min(weakestText.length(), strongestText.length())) {
            sharedPrefix = "";
            sharedSuffix = "";
        }
        return Translation.msg("socketable.tierBandRange").arg("low", weakestText.substring(0, weakestText.length() - sharedSuffix.length()))
                .arg("high", strongestText.substring(sharedPrefix.length())).text();
    }

    public StyledText rangeDescription(ModifierLine rollRange) {
        return rangeDescription(ItemEffects.SKILL_EFFECTS, rollRange);
    }

    public StyledText rangeDescription(ModifierEffects effects, ModifierLine rollRange) {
        ModifierEffect effect = effect(effects);
        if (effect == null) {
            return null;
        }
        if (rollRange.isScaled()) {
            return scaledValuesDescription(effects, rollRange);
        }
        StyledText strongestText = display(effect, rollRange.boundFarthestFromZero());
        if (!rollRange.canVary() || strongestText.spans().isEmpty()) {
            return strongestText;
        }
        List<String> rangeValues = rangeValues(display(effect, rollRange.boundNearestZero()), strongestText);
        return rangeValues == null ? strongestText : replaceSpans(strongestText, rangeValues);
    }

    private static List<String> rangeValues(StyledText weakestText, StyledText strongestText) {
        List<StyledText.Span> weakestSpans = weakestText.spans();
        List<StyledText.Span> strongestSpans = strongestText.spans();
        if (weakestSpans.size() != strongestSpans.size()) {
            return null;
        }
        List<String> weakestValues = new ArrayList<>(weakestSpans.size());
        List<String> strongestValues = new ArrayList<>(strongestSpans.size());
        int varyingCount = 0;
        for (int index = 0; index < strongestSpans.size(); index++) {
            String weakestValue = spanText(weakestText, weakestSpans.get(index));
            String strongestValue = spanText(strongestText, strongestSpans.get(index));
            weakestValues.add(weakestValue);
            strongestValues.add(strongestValue);
            varyingCount += weakestValue.equals(strongestValue) ? 0 : 1;
        }
        if (varyingCount == 0) {
            return null;
        }
        String rangeKey = varyingCount > 1 ? "codex.socketables.rangeGrouped" : "codex.socketables.range";
        List<String> rangeValues = new ArrayList<>(strongestValues.size());
        for (int index = 0; index < strongestValues.size(); index++) {
            String weakestValue = weakestValues.get(index);
            String strongestValue = strongestValues.get(index);
            if (weakestValue.equals(strongestValue)) {
                rangeValues.add(strongestValue);
                continue;
            }
            String sharedSuffix = sharedNonNumericEdge(List.of(weakestValue, strongestValue), false);
            String weakestWithoutSuffix = weakestValue.substring(0, weakestValue.length() - sharedSuffix.length());
            rangeValues.add(Translation.msg(rangeKey).arg("low", weakestWithoutSuffix).arg("high", strongestValue).text());
        }
        return rangeValues;
    }

    private static String spanText(StyledText text, StyledText.Span span) {
        return text.plain().substring(span.start(), span.end());
    }

    private static StyledText replaceValue(StyledText effectText, String replacementValue) {
        List<String> values = new ArrayList<>(effectText.spans().size());
        values.add(replacementValue);
        for (StyledText.Span laterSpan : effectText.spans().subList(1, effectText.spans().size())) {
            values.add(spanText(effectText, laterSpan));
        }
        return replaceSpans(effectText, values);
    }

    private static StyledText replaceSpans(StyledText effectText, List<String> spanValues) {
        String plain = effectText.plain();
        StringBuilder rebuilt = new StringBuilder(plain.length() + 32);
        List<StyledText.Span> spans = new ArrayList<>(effectText.spans().size());
        int cursor = 0;
        for (int index = 0; index < effectText.spans().size(); index++) {
            StyledText.Span span = effectText.spans().get(index);
            rebuilt.append(plain, cursor, span.start());
            int rebuiltStart = rebuilt.length();
            rebuilt.append(spanValues.get(index));
            spans.add(new StyledText.Span(rebuiltStart, rebuilt.length(), span.style()));
            cursor = span.end();
        }
        rebuilt.append(plain.substring(cursor));
        return new StyledText(rebuilt.toString(), spans);
    }

    public static String joinSharingAffixes(List<String> valueTexts) {
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

    private static StyledText display(ModifierEffect effect, float magnitude) {
        return new DescriptionLine(effect.description(magnitude), effect.invertsDescriptionColours()).display();
    }

    private static String valueText(ModifierEffect effect, float magnitude) {
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
