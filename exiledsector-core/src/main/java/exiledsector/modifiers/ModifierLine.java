package exiledsector.modifiers;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipHullSizes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ModifierLine(String effectName, float min, float max, ValueScaling scaling) {

    public static final int MAX_DECIMAL_PLACES = 3;
    private static final float STEP_TOLERANCE = 0.001f;
    private static final String FIELD_SEPARATOR = ":";
    private static final String HULL_VALUE_SEPARATOR = "/";
    private static final String TIER_GROUP_VALUE_SEPARATOR = "=";
    private static final String NUMBER = "-?\\d+(?:\\.\\d+)?";
    private static final Pattern TIER_GROUP = Pattern.compile("([a-z]+)=(" + NUMBER + ")(?:-(" + NUMBER + "))?");
    public static final float TIER_ROLL_POSITIONS = 100f;

    public ModifierLine {
        scaling = scaling == null || !scaling.scales() ? ValueScaling.NONE : scaling;
    }

    public static ModifierLine range(String effectName, float firstBound, float secondBound) {
        return new ModifierLine(effectName, Math.min(firstBound, secondBound), Math.max(firstBound, secondBound), ValueScaling.NONE);
    }

    public static ModifierLine perHullSize(String effectName, List<Float> hullValues) {
        return new ModifierLine(effectName, hullValues.get(0), hullValues.get(0), ValueScaling.perHullSize(hullValues));
    }

    public static ModifierLine perTierGroup(String effectName, List<TierGroup> groups, List<Float> groupValues) {
        ValueScaling tierGroups = ValueScaling.perTierGroup(groups, groupValues);
        return new ModifierLine(effectName, groupValues.get(0), groupValues.get(0), tierGroups);
    }

    public static ModifierLine perTierGroupRanges(String effectName, List<TierGroup> groups, List<Float> groupLowValues,
                                                  List<Float> groupHighValues) {
        ValueScaling tierGroups = ValueScaling.perTierGroup(groups, groupLowValues, groupHighValues);
        if (!tierGroups.variesWithinAPoint()) {
            return new ModifierLine(effectName, groupLowValues.get(0), groupLowValues.get(0), tierGroups);
        }
        return new ModifierLine(effectName, 0f, TIER_ROLL_POSITIONS, tierGroups);
    }

    public record Parsed(ModifierLine line, String trailingField) {
    }

    public static boolean isTierGroupText(String valuesText) {
        return valuesText != null && valuesText.contains(TIER_GROUP_VALUE_SEPARATOR);
    }

    public static Parsed parse(String entryText) {
        String[] entryFields = entryText.split(FIELD_SEPARATOR);
        boolean perTier = entryFields.length >= 2 && isTierGroupText(entryFields[1]);
        boolean perHullSize = !perTier && entryFields.length >= 2 && entryFields[1].contains(HULL_VALUE_SEPARATOR);
        boolean scaled = perTier || perHullSize;
        if (scaled ? entryFields.length > 3 : entryFields.length < 3 || entryFields.length > 4) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" is not EFFECT:min:max, EFFECT:min:max:weight, "
                    + "EFFECT:frigate/destroyer/cruiser/capital, EFFECT:frigate/destroyer/cruiser/capital:weight "
                    + "or EFFECT:low=min-max/mid=min-max/high=min-max with an optional :weight");
        }
        String effectName = entryFields[0].trim();
        int trailingIndex = scaled ? 2 : 3;
        String trailingField = entryFields.length > trailingIndex ? entryFields[trailingIndex] : null;
        if (perTier) {
            return new Parsed(parseTierGroups(effectName, entryFields[1], entryText), trailingField);
        }
        if (perHullSize) {
            return new Parsed(perHullSize(effectName, parseHullValues(entryFields[1], entryText)), trailingField);
        }
        return new Parsed(range(effectName, parseNumber(entryFields[1], entryText), parseNumber(entryFields[2], entryText)), trailingField);
    }

    private static ModifierLine parseTierGroups(String effectName, String groupsText, String entryText) {
        List<TierGroup> groups = new ArrayList<>();
        List<Float> lowValues = new ArrayList<>();
        List<Float> highValues = new ArrayList<>();
        for (String groupText : groupsText.trim().split(HULL_VALUE_SEPARATOR, -1)) {
            Matcher groupMatch = TIER_GROUP.matcher(groupText.trim());
            TierGroup group = groupMatch.matches() ? TierGroup.byIdOrNull(groupMatch.group(1)) : null;
            if (group == null) {
                throw new IllegalArgumentException("entry \"" + entryText + "\" has the tier group \"" + groupText.trim()
                        + "\"; write each group as " + String.join(", ", TierGroup.ids()) + " followed by =value or =min-max");
            }
            float firstValue = parseNumber(groupMatch.group(2), entryText);
            float secondValue = groupMatch.group(3) == null ? firstValue : parseNumber(groupMatch.group(3), entryText);
            groups.add(group);
            lowValues.add(Math.min(firstValue, secondValue));
            highValues.add(Math.max(firstValue, secondValue));
        }
        try {
            return perTierGroupRanges(effectName, groups, lowValues, highValues);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("entry \"" + entryText + "\": " + e.getMessage());
        }
    }

    public ModifierLine rangeFor(ScaleContext context) {
        if (!(scaling instanceof ValueScaling.PerTierGroup tierGroups)) {
            return this;
        }
        int point = tierGroups.pointFor(context);
        return point == ValueScaling.NO_POINT ? this : range(effectName, tierGroups.lowAt(point), tierGroups.highAt(point));
    }

    public ModifierLine named(String currentEffectName) {
        return new ModifierLine(currentEffectName, min, max, scaling);
    }

    public ModifierLine widened(float fraction) {
        float stepsPerUnit = steps().perUnit();
        float oneStep = 1f / stepsPerUnit;
        float widenedMin = Math.round((min - Math.abs(min) * fraction) * stepsPerUnit) / stepsPerUnit;
        float widenedMax = Math.round((max + Math.abs(max) * fraction) * stepsPerUnit) / stepsPerUnit;
        if (min > 0f && widenedMin <= 0f) {
            widenedMin = oneStep;
        }
        if (max < 0f && widenedMax >= 0f) {
            widenedMax = -oneStep;
        }
        return new ModifierLine(effectName, widenedMin + 0f, widenedMax + 0f, scaling);
    }

    public ModifierLine stretchedRollPositions(float fraction) {
        float stretch = (max - min) * Math.max(0f, fraction);
        return new ModifierLine(effectName, Math.round(min - stretch) + 0f, Math.round(max + stretch) + 0f, scaling);
    }

    public float roll(Random random) {
        RollSteps steps = steps();
        if (steps.highest() < steps.lowest()) {
            return Math.round(min * steps.perUnit()) / steps.perUnit();
        }
        return (steps.lowest() + random.nextInt(steps.highest() - steps.lowest() + 1)) / steps.perUnit();
    }

    public float average() {
        RollSteps steps = steps();
        if (steps.highest() < steps.lowest()) {
            return Math.round(min * steps.perUnit()) / steps.perUnit();
        }
        double middleStep = (steps.lowest() + (double) steps.highest()) / 2.0;
        return (float) (Math.signum(middleStep) * Math.round(Math.abs(middleStep)) / steps.perUnit());
    }

    public float bestRoll(ScaleContext context, boolean lowerIsBetter) {
        RollSteps steps = steps();
        if (steps.highest() < steps.lowest()) {
            return Math.round(min * steps.perUnit()) / steps.perUnit();
        }
        float lowestRoll = steps.lowest() / steps.perUnit();
        float highestRoll = steps.highest() / steps.perUnit();
        return isBetter(lowestRoll, highestRoll, context, lowerIsBetter) ? lowestRoll : highestRoll;
    }

    public boolean isBetter(float candidateRoll, float currentRoll, ScaleContext context, boolean lowerIsBetter) {
        float candidateValue = valueFor(candidateRoll, context);
        float currentValue = valueFor(currentRoll, context);
        return lowerIsBetter ? candidateValue < currentValue : candidateValue > currentValue;
    }

    public int stepCount() {
        RollSteps steps = steps();
        return Math.max(1, steps.highest() - steps.lowest() + 1);
    }

    public boolean canVary() {
        return stepCount() > 1;
    }

    public boolean isScaled() {
        return scaling.scales();
    }

    public boolean scalesWithHullSize() {
        return scaling instanceof ValueScaling.PerHullSize;
    }

    public boolean scalesWithTier() {
        return scaling instanceof ValueScaling.PerTierGroup;
    }

    public boolean rollsAtTier(int tier) {
        return !scalesWithTier() || tier == TierRange.NO_TIER || hasValueFor(ScaleContext.ofTier(tier));
    }

    public List<Float> hullValues() {
        return scalesWithHullSize() ? scaling.values() : List.of();
    }

    public List<Float> scaledValues() {
        return scaling.values();
    }

    public boolean hasHullValueFor(HullSize hullSize) {
        return hasValueFor(ScaleContext.ofHullSize(hullSize));
    }

    public boolean listsEveryHullValueFor(HullSize hullSize) {
        return listsEveryValueFor(ScaleContext.ofHullSize(hullSize));
    }

    public float valueFor(float rolledMagnitude, HullSize hullSize) {
        return valueFor(rolledMagnitude, ScaleContext.ofHullSize(hullSize));
    }

    public boolean hasValueFor(ScaleContext context) {
        return scaling.pointFor(context) != ValueScaling.NO_POINT;
    }

    public boolean listsEveryValueFor(ScaleContext context) {
        return isScaled() && !hasValueFor(context);
    }

    public float valueFor(float rolledMagnitude, ScaleContext context) {
        int point = scaling.pointFor(context);
        return point == ValueScaling.NO_POINT ? rolledMagnitude : scaling.valueAt(point, rollFraction(rolledMagnitude));
    }

    private float rollFraction(float rolledMagnitude) {
        return max > min ? (rolledMagnitude - min) / (max - min) : 0f;
    }

    public float boundNearestZero() {
        return Math.abs(min) <= Math.abs(max) ? min : max;
    }

    public float boundFarthestFromZero() {
        return Math.abs(min) <= Math.abs(max) ? max : min;
    }

    public static int decimalPlaces(float bound) {
        for (int places = 0; places < MAX_DECIMAL_PLACES; places++) {
            float scaled = bound * (float) Math.pow(10, places);
            if (Math.abs(scaled - Math.round(scaled)) < STEP_TOLERANCE) {
                return places;
            }
        }
        return MAX_DECIMAL_PLACES;
    }

    private RollSteps steps() {
        float stepsPerUnit = (float) Math.pow(10, Math.max(decimalPlaces(min), decimalPlaces(max)));
        int lowestStep = (int) Math.ceil(min * stepsPerUnit - STEP_TOLERANCE);
        int highestStep = (int) Math.floor(max * stepsPerUnit + STEP_TOLERANCE);
        return new RollSteps(stepsPerUnit, lowestStep, highestStep);
    }

    private record RollSteps(float perUnit, int lowest, int highest) {
    }

    private static List<Float> parseHullValues(String hullValuesText, String entryText) {
        String[] valueTexts = hullValuesText.split(HULL_VALUE_SEPARATOR, -1);
        if (valueTexts.length != ShipHullSizes.ORDER.size()) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" needs exactly four hull size values, frigate/destroyer/cruiser/capital");
        }
        List<Float> hullValues = new ArrayList<>(valueTexts.length);
        for (String valueText : valueTexts) {
            hullValues.add(parseNumber(valueText, entryText));
        }
        return hullValues;
    }

    public static float parseNumber(String numberText, String entryText) {
        try {
            return Float.parseFloat(numberText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" has a value that is not a number: " + numberText.trim());
        }
    }
}
