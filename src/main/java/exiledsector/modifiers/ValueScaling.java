package exiledsector.modifiers;

import exiledsector.skills.ShipHullSizes;

import java.util.List;

public sealed interface ValueScaling permits ValueScaling.Unscaled, ValueScaling.PerHullSize, ValueScaling.PerTierBand {

    ValueScaling NONE = new Unscaled();
    int NO_POINT = -1;

    List<Float> values();

    int pointFor(ScaleContext context);

    default boolean scales() {
        return !values().isEmpty();
    }

    static ValueScaling perHullSize(List<Float> hullValues) {
        return new PerHullSize(hullValues);
    }

    static ValueScaling perTierBand(List<Integer> bandStartTiers, List<Float> bandValues) {
        return new PerTierBand(bandStartTiers, bandValues);
    }

    static ValueScaling perTierBand(List<Integer> bandStartTiers, List<Float> bandLowValues, List<Float> bandHighValues) {
        return new PerTierBand(bandStartTiers, bandLowValues, bandHighValues);
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

    record PerTierBand(List<Integer> bandStartTiers, List<Float> values, List<Float> highValues) implements ValueScaling {

        public PerTierBand {
            bandStartTiers = List.copyOf(bandStartTiers);
            values = List.copyOf(values);
            highValues = highValues == null ? values : List.copyOf(highValues);
            if (bandStartTiers.isEmpty() || bandStartTiers.size() != values.size()) {
                throw new IllegalArgumentException("tier bands need one value per band, but there are " + bandStartTiers.size()
                        + " bands and " + values.size() + " values");
            }
            if (highValues.size() != values.size()) {
                throw new IllegalArgumentException("tier bands need a high value for each of their " + values.size() + " low values");
            }
            int previousStart = TierRange.NO_TIER;
            for (int bandStart : bandStartTiers) {
                if (bandStart <= previousStart) {
                    throw new IllegalArgumentException("tier bands must start at tier " + TierRange.LOWEST_TIER
                            + " or above and rise, but " + bandStartTiers + " does not");
                }
                previousStart = bandStart;
            }
        }

        public PerTierBand(List<Integer> bandStartTiers, List<Float> values) {
            this(bandStartTiers, values, values);
        }

        @Override
        public float valueAt(int point, float rollFraction) {
            float low = values.get(point);
            float high = highValues.get(point);
            if (low == high) {
                return low;
            }
            float stepsPerUnit = (float) Math.pow(10, Math.max(ModifierLine.decimalPlaces(low), ModifierLine.decimalPlaces(high)));
            float fraction = Math.max(0f, Math.min(1f, rollFraction));
            return Math.round((low + (high - low) * fraction) * stepsPerUnit) / stepsPerUnit + 0f;
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
            int band = NO_POINT;
            for (int index = 0; index < bandStartTiers.size() && bandStartTiers.get(index) <= context.tier(); index++) {
                band = index;
            }
            return band;
        }
    }
}
