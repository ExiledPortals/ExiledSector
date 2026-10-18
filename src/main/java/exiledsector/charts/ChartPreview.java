package exiledsector.charts;

import java.util.ArrayList;
import java.util.List;

public final class ChartPreview {

    static final float NEUTRAL_TOLERANCE = 0.0005f;

    public enum Kind {MULTIPLIER, COUNT, PERCENT}

    public enum Stat {
        FLEET_POWER(Kind.MULTIPLIER, false),
        EXTRA_FLEETS(Kind.COUNT, false),
        OFFICER_LEVELS(Kind.COUNT, false),
        EXTRA_OFFICERS(Kind.COUNT, false),
        AVERAGE_SMODS(Kind.COUNT, false),
        BOSS_POWER(Kind.MULTIPLIER, false),
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

    public record Line(Stat stat, float value) {

        public boolean favourable() {
            return stat.higherIsBetter() == (value > stat.neutral());
        }
    }

    private ChartPreview() {
    }

    public static List<Line> of(ChartGenContext context) {
        List<Line> lines = new ArrayList<>();
        if (context == null || context.isEmpty()) {
            return lines;
        }
        add(lines, Stat.FLEET_POWER, context.fleetPowerMult());
        add(lines, Stat.EXTRA_FLEETS, context.extraFleets());
        add(lines, Stat.OFFICER_LEVELS, context.officerLevelBonus());
        add(lines, Stat.EXTRA_OFFICERS, context.extraOfficers());
        add(lines, Stat.AVERAGE_SMODS, context.averageSModsBonus());
        add(lines, Stat.BOSS_POWER, context.bossPowerMult());
        add(lines, Stat.FLEET_QUALITY, context.fleetQualityPercent());
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
        if (Math.abs(value - stat.neutral()) > NEUTRAL_TOLERANCE) {
            lines.add(new Line(stat, value));
        }
    }
}
