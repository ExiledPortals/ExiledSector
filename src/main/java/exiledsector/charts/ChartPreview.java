package exiledsector.charts;

import java.util.ArrayList;
import java.util.List;

public final class ChartPreview {

    public static final float DISPLAY_STEPS = 100f;

    public enum Kind {MULTIPLIER, COUNT, PERCENT}

    public enum Stat {
        FLEET_SIZE(Kind.PERCENT, false),
        EXTRA_FLEETS(Kind.COUNT, false),
        FLEET_STRENGTH(Kind.PERCENT, false),
        OFFICER_LEVELS(Kind.COUNT, false),
        EXTRA_OFFICERS(Kind.COUNT, false),
        WARDEN_SIZE(Kind.PERCENT, false),
        FLEET_QUALITY(Kind.PERCENT, false),
        SENSOR_RANGE(Kind.MULTIPLIER, true),
        CHART_QUALITY(Kind.PERCENT, true),
        LOOT(Kind.MULTIPLIER, true),
        EXTRA_POIS(Kind.COUNT, true),
        ITEM_CHANCE(Kind.MULTIPLIER, true),
        VAULT_RICHNESS(Kind.PERCENT, true),
        REWARD(Kind.MULTIPLIER, true);

        private final Kind kind;
        private final boolean higherIsBetter;

        Stat(Kind kind, boolean higherIsBetter) {
            this.kind = kind;
            this.higherIsBetter = higherIsBetter;
        }

        public Kind kind() {
            return kind;
        }

        public boolean higherIsBetter() {
            return higherIsBetter;
        }

        public float neutral() {
            return kind == Kind.MULTIPLIER ? 1f : 0f;
        }
    }

    public record Line(Stat stat, float value, float upperValue) {

        public boolean isRange() {
            return rounded(value) != rounded(upperValue);
        }

        public boolean favourable() {
            return stat.higherIsBetter() == ((value + upperValue) / 2f > stat.neutral());
        }
    }

    private ChartPreview() {
    }

    public static float rounded(float value) {
        return Math.round(value * DISPLAY_STEPS) / DISPLAY_STEPS;
    }

    public static List<Line> of(ChartGenContext context, ChartTier base) {
        return of(context, base, StarChartSettings.DEFAULT_FLEET_LIMIT);
    }

    public static List<Line> of(ChartGenContext context, ChartTier base, int fleetLimit) {
        List<Line> lines = new ArrayList<>();
        if (context == null || context.isEmpty() || base == null) {
            return lines;
        }
        ChartTier adjusted = context.adjust(base);
        int extraFleets = context.extraFleets();
        ChartFleetSpread baseLow = ChartFleetSpread.of(base.fleetsMin(), base.fleetSize(), 0, fleetLimit);
        ChartFleetSpread baseHigh = ChartFleetSpread.of(base.fleetsMax(), base.fleetSize(), 0, fleetLimit);
        ChartFleetSpread adjustedLow = ChartFleetSpread.of(adjusted.fleetsMin(), adjusted.fleetSize(), extraFleets, fleetLimit);
        ChartFleetSpread adjustedHigh = ChartFleetSpread.of(adjusted.fleetsMax(), adjusted.fleetSize(), extraFleets, fleetLimit);
        add(lines, Stat.FLEET_SIZE, adjustedLow.cappedFleetSize() - baseLow.cappedFleetSize(), adjustedHigh.cappedFleetSize() - baseHigh.cappedFleetSize());
        add(lines, Stat.EXTRA_FLEETS, adjustedLow.fleets() - baseLow.fleets(), adjustedHigh.fleets() - baseHigh.fleets());
        add(lines, Stat.FLEET_STRENGTH, adjustedLow.totalStrengthBonus() - baseLow.totalStrengthBonus(),
                adjustedHigh.totalStrengthBonus() - baseHigh.totalStrengthBonus());
        add(lines, Stat.OFFICER_LEVELS, adjusted.officerLevelBonus() - base.officerLevelBonus());
        add(lines, Stat.EXTRA_OFFICERS, adjusted.officerCountBonus() - base.officerCountBonus());
        add(lines, Stat.WARDEN_SIZE, adjusted.wardenSize() - base.wardenSize());
        add(lines, Stat.FLEET_QUALITY, adjusted.fleetQuality() - base.fleetQuality());
        add(lines, Stat.SENSOR_RANGE, context.sensorRangeMult());
        add(lines, Stat.CHART_QUALITY, context.quality());
        add(lines, Stat.LOOT, context.lootMult());
        add(lines, Stat.EXTRA_POIS, context.extraPois());
        add(lines, Stat.ITEM_CHANCE, context.itemChanceMult());
        add(lines, Stat.VAULT_RICHNESS, context.vaultRichnessPercent());
        add(lines, Stat.REWARD, context.rewardMult());
        return lines;
    }

    private static void add(List<Line> lines, Stat stat, float value) {
        add(lines, stat, value, value);
    }

    private static void add(List<Line> lines, Stat stat, float firstValue, float secondValue) {
        if (rounded(firstValue) != stat.neutral() || rounded(secondValue) != stat.neutral()) {
            lines.add(new Line(stat, Math.min(firstValue, secondValue), Math.max(firstValue, secondValue)));
        }
    }
}
