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

    record PerTierBand(List<Integer> bandStartTiers, List<Float> values) implements ValueScaling {

        public PerTierBand {
            bandStartTiers = List.copyOf(bandStartTiers);
            values = List.copyOf(values);
            if (bandStartTiers.isEmpty() || bandStartTiers.size() != values.size()) {
                throw new IllegalArgumentException("tier bands need one value per band, but there are " + bandStartTiers.size()
                        + " bands and " + values.size() + " values");
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
