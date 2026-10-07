package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record PoolEntry(String effectName, float min, float max, float weight, List<Float> hullValues) {

    static final int MAX_DECIMAL_PLACES = 3;
    static final List<HullSize> HULL_VALUE_ORDER = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);
    private static final float STEP_TOLERANCE = 0.001f;
    private static final String FIELD_SEPARATOR = ":";
    private static final String HULL_VALUE_SEPARATOR = "/";

    public PoolEntry {
        hullValues = hullValues == null ? List.of() : List.copyOf(hullValues);
    }

    public PoolEntry(String effectName, float min, float max, float weight) {
        this(effectName, min, max, weight, List.of());
    }

    public static PoolEntry perHullSize(String effectName, float weight, List<Float> hullValues) {
        return new PoolEntry(effectName, hullValues.get(0), hullValues.get(0), weight, hullValues);
    }

    static PoolEntry parse(String entryText) {
        String[] entryFields = entryText.split(FIELD_SEPARATOR);
        boolean perHullSize = entryFields.length >= 2 && entryFields[1].contains(HULL_VALUE_SEPARATOR);
        if (perHullSize ? entryFields.length > 3 : entryFields.length < 3 || entryFields.length > 4) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" is not EFFECT:min:max, EFFECT:min:max:weight, "
                    + "EFFECT:frigate/destroyer/cruiser/capital or EFFECT:frigate/destroyer/cruiser/capital:weight");
        }
        String effectName = entryFields[0].trim();
        int weightField = perHullSize ? 2 : 3;
        float weight = entryFields.length > weightField ? parseNumber(entryFields[weightField], entryText) : 1f;
        if (!(weight > 0f)) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" needs a weight above zero");
        }
        if (perHullSize) {
            return perHullSize(effectName, weight, parseHullValues(entryFields[1], entryText));
        }
        float firstBound = parseNumber(entryFields[1], entryText);
        float secondBound = parseNumber(entryFields[2], entryText);
        return new PoolEntry(effectName, Math.min(firstBound, secondBound), Math.max(firstBound, secondBound), weight);
    }

    PoolEntry named(String currentEffectName) {
        return new PoolEntry(currentEffectName, min, max, weight, hullValues);
    }

    public float roll(Random random) {
        RollSteps steps = steps();
        if (steps.highest() < steps.lowest()) {
            return Math.round(min * steps.perUnit()) / steps.perUnit();
        }
        return (steps.lowest() + random.nextInt(steps.highest() - steps.lowest() + 1)) / steps.perUnit();
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
        return scalesWithHullSize() && hullSize != null && HULL_VALUE_ORDER.contains(hullSize);
    }

    public boolean listsEveryHullValueFor(HullSize hullSize) {
        return scalesWithHullSize() && !hasHullValueFor(hullSize);
    }

    public float valueFor(float rolledMagnitude, HullSize hullSize) {
        return hasHullValueFor(hullSize) ? hullValues.get(HULL_VALUE_ORDER.indexOf(hullSize)) : rolledMagnitude;
    }

    public float boundNearestZero() {
        return Math.abs(min) <= Math.abs(max) ? min : max;
    }

    public float boundFarthestFromZero() {
        return Math.abs(min) <= Math.abs(max) ? max : min;
    }

    static int decimalPlaces(float bound) {
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
        if (valueTexts.length != HULL_VALUE_ORDER.size()) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" needs exactly four hull size values, frigate/destroyer/cruiser/capital");
        }
        List<Float> hullValues = new ArrayList<>(valueTexts.length);
        for (String valueText : valueTexts) {
            hullValues.add(parseNumber(valueText, entryText));
        }
        return hullValues;
    }

    private static float parseNumber(String numberText, String entryText) {
        try {
            return Float.parseFloat(numberText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("entry \"" + entryText + "\" has a value that is not a number: " + numberText.trim());
        }
    }
}
