package exiledsector.modifiers;

import exiledsector.skills.ShipHullSizes;

import java.util.List;

public sealed interface ValueScaling permits ValueScaling.Unscaled, ValueScaling.PerHullSize, ValueScaling.PerTierGroup {

    ValueScaling NONE = new Unscaled();
    int NO_POINT = -1;
    float MAX_STRETCH_FRACTION = 1f;

    List<Float> values();

    int pointFor(ScaleContext context);

    default boolean scales() {
        return !values().isEmpty();
    }

    static ValueScaling perHullSize(List<Float> hullValues) {
        return new PerHullSize(hullValues);
    }

    static ValueScaling perTierGroup(List<TierGroup> groups, List<Float> groupValues) {
        return new PerTierGroup(groups, groupValues);
    }

    static ValueScaling perTierGroup(List<TierGroup> groups, List<Float> groupLowValues, List<Float> groupHighValues) {
        return new PerTierGroup(groups, groupLowValues, groupHighValues);
    }

    default float valueAt(int point, float rollFraction) {
        return values().get(point);
    }

    default boolean variesWithinAPoint() {
        return false;
    }

    record Unscaled() implements ValueScaling {

        @Override
        public List<Float> values() {
            return List.of();
        }

        @Override
        public int pointFor(ScaleContext context) {
            return NO_POINT;
        }
    }

    record PerHullSize(List<Float> values) implements ValueScaling {

        public PerHullSize {
            values = List.copyOf(values);
        }

        @Override
        public int pointFor(ScaleContext context) {
            return context != null && ShipHullSizes.isShipSize(context.hullSize()) ? ShipHullSizes.ORDER.indexOf(context.hullSize()) : NO_POINT;
        }
    }

    record PerTierGroup(List<TierGroup> groups, List<Float> values, List<Float> highValues) implements ValueScaling {

        public PerTierGroup {
            groups = List.copyOf(groups);
            values = List.copyOf(values);
            highValues = highValues == null ? values : List.copyOf(highValues);
            if (groups.isEmpty() || groups.size() != values.size()) {
                throw new IllegalArgumentException("tier groups need one value per group, but there are " + groups.size()
                        + " groups and " + values.size() + " values");
            }
            if (highValues.size() != values.size()) {
                throw new IllegalArgumentException("tier groups need a high value for each of their " + values.size() + " low values");
            }
            if (!groups.equals(TierGroup.fromUpwards(groups.get(0)))) {
                throw new IllegalArgumentException("tier groups must run in order from their lowest group up to " + TierGroup.HIGH.id()
                        + " without gaps, but " + groups.stream().map(TierGroup::id).toList() + " does not");
            }
        }

        public PerTierGroup(List<TierGroup> groups, List<Float> values) {
            this(groups, values, values);
        }

        @Override
        public float valueAt(int point, float rollFraction) {
            float low = lowAt(point);
            float high = highAt(point);
            if (low == high) {
                return low;
            }
            float stepsPerUnit = (float) Math.pow(10, Math.max(ModifierLine.decimalPlaces(low), ModifierLine.decimalPlaces(high)));
            if (rollFraction < 0f) {
                return stretchedPast(low, -1f, -rollFraction, stepsPerUnit);
            }
            if (rollFraction > 1f) {
                return stretchedPast(high, 1f, rollFraction - 1f, stepsPerUnit);
            }
            int lowStep = Math.round(low * stepsPerUnit);
            int groupSteps = Math.round(high * stepsPerUnit) - lowStep + 1;
            int rollPositions = Math.round(ModifierLine.TIER_ROLL_POSITIONS) + 1;
            int position = Math.round(Math.max(0f, Math.min(1f, rollFraction)) * ModifierLine.TIER_ROLL_POSITIONS);
            int step = Math.min(groupSteps - 1, position * groupSteps / rollPositions);
            return (lowStep + step) / stepsPerUnit + 0f;
        }

        private static float stretchedPast(float bound, float outward, float stretchFraction, float stepsPerUnit) {
            float stretch = Math.min(MAX_STRETCH_FRACTION, stretchFraction);
            float stretched = Math.round((bound + outward * Math.abs(bound) * stretch) * stepsPerUnit) / stepsPerUnit;
            float oneStep = 1f / stepsPerUnit;
            if (bound > 0f && stretched <= 0f) {
                stretched = oneStep;
            } else if (bound < 0f && stretched >= 0f) {
                stretched = -oneStep;
            }
            return stretched + 0f;
        }

        @Override
        public boolean variesWithinAPoint() {
            return !values.equals(highValues);
        }

        public float lowAt(int point) {
            return Math.min(values.get(point), highValues.get(point));
        }

        public float highAt(int point) {
            return Math.max(values.get(point), highValues.get(point));
        }

        @Override
        public int pointFor(ScaleContext context) {
            if (context == null || context.tier() == TierRange.NO_TIER) {
                return NO_POINT;
            }
            for (int index = 0; index < groups.size(); index++) {
                if (groups.get(index).contains(context.tier())) {
                    return index;
                }
            }
            return NO_POINT;
        }
    }
}
