package exiledsector.items;

import java.util.List;
import java.util.Random;

public final class StarChartCorruption {

    public static final int EXPANDED_MODIFIERS = 8;
    public static final int EXPANDED_PER_SIDE = 4;

    private StarChartCorruption() {
    }

    public static Item corrupt(CorruptionOutcome outcome, Item item, Random random) {
        if (!(item instanceof StarChart chart)) {
            return null;
        }
        return switch (outcome) {
            case TIER_RAISED -> raiseTier(chart);
            case MODIFIERS_EXPANDED -> expandModifiers(chart, random);
            default -> null;
        };
    }

    public static ItemData withExpandedModifiers(ItemData chartData, Random random) {
        if (!(chartData.preview() instanceof StarChart chart)) {
            return chartData;
        }
        StarChart expanded = expandModifiers(chart, random);
        chart.corrupt(expanded == null ? CorruptionOutcome.NOTHING : CorruptionOutcome.MODIFIERS_EXPANDED);
        return ItemData.of(chart);
    }

    static StarChart raiseTier(StarChart chart) {
        return chart.tier() < StarChart.HIGHEST_TIER ? StarChartCipher.raiseTier(chart) : null;
    }

    static StarChart expandModifiers(StarChart chart, Random random) {
        ItemDefinition definition = chart.definition();
        if (definition == null) {
            return null;
        }
        List<RolledModifier> rerolled = ItemRoller.rollPastLimits(definition, random.nextLong(), chart.tier(), EXPANDED_MODIFIERS,
                EXPANDED_PER_SIDE);
        if (rerolled.isEmpty()) {
            return null;
        }
        chart.replaceExplicits(rerolled);
        chart.refreezeName();
        return chart;
    }
}
