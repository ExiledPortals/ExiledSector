package exiledsector.modifiers;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipHullSizes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record ModifierLine(String effectName, float min, float max, List<Float> hullValues) {

    public static final int MAX_DECIMAL_PLACES = 3;
    private static final float STEP_TOLERANCE = 0.001f;
    private static final String FIELD_SEPARATOR = ":";
    private static final String HULL_VALUE_SEPARATOR = "/";

    public ModifierLine {
        hullValues = hullValues == null ? List.of() : List.copyOf(hullValues);
    }

    public static ModifierLine range(String effectName, float firstBound, float secondBound) {
        return new ModifierLine(effectName, Math.min(firstBound, secondBound), Math.max(firstBound, secondBound), List.of());
    }

    public static ModifierLine perHullSize(String effectName, List<Float> hullValues) {
        return new ModifierLine(effectName, hullValues.get(0), hullValues.get(0), hullValues);
    }

    public record Parsed(ModifierLine line, String trailingField) {
    }

    public static Parsed parse(String entryText) {
        String[] entryFields = entryText.split(FIELD_SEPARATOR);
        boolean perHullSize = entryFields.length >= 2 && entryFields[1].contains(HULL_VALUE_SEPARATOR);
        if (perHullSize ? entryFields.length > 3 : entryFields.length < 3 || entryFields.length > 4) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" is not EFFECT:min:max, EFFECT:min:max:weight, "
                    + "EFFECT:frigate/destroyer/cruiser/capital or EFFECT:frigate/destroyer/cruiser/capital:weight");
        }
        String effectName = entryFields[0].trim();
        int trailingIndex = perHullSize ? 2 : 3;
        String trailingField = entryFields.length > trailingIndex ? entryFields[trailingIndex] : null;
        if (perHullSize) {
            return new Parsed(perHullSize(effectName, parseHullValues(entryFields[1], entryText)), trailingField);
        }
        return new Parsed(range(effectName, parseNumber(entryFields[1], entryText), parseNumber(entryFields[2], entryText)), trailingField);
    }

    public ModifierLine named(String currentEffectName) {
        return new ModifierLine(currentEffectName, min, max, hullValues);
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

    public int stepCount() {
        RollSteps steps = steps();
        return Math.max(1, steps.highest() - steps.lowest() + 1);
    }

    public boolean canVary() {
        return stepCount() > 1;
    }

    public boolean scalesWithHullSize() {
        return !hullValues.isEmpty();
    }

    public boolean hasHullValueFor(HullSize hullSize) {
        return scalesWithHullSize() && ShipHullSizes.isShipSize(hullSize);
    }

    public boolean listsEveryHullValueFor(HullSize hullSize) {
        return scalesWithHullSize() && !hasHullValueFor(hullSize);
    }

    public float valueFor(float rolledMagnitude, HullSize hullSize) {
        return hasHullValueFor(hullSize) ? hullValues.get(ShipHullSizes.ORDER.indexOf(hullSize)) : rolledMagnitude;
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
