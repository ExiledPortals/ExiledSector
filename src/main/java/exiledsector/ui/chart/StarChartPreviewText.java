package exiledsector.ui.chart;

import exiledsector.charts.ChartFleetSpread;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartPreview;
import exiledsector.charts.ChartTier;
import exiledsector.charts.ChartTiers;
import exiledsector.charts.StarChartSettings;
import exiledsector.i18n.NumberText;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.skills.npc.NpcTreeSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StarChartPreviewText {

    private StarChartPreviewText() {
    }

    public static List<StyledText> modifierLines(Item chart) {
        return chart == null ? List.of() : modifierLines(ChartGenContext.from(chart), ChartTiers.forTier(chart.tier()), StarChartSettings.fleetLimit());
    }

    public static List<StyledText> modifierLines(ChartGenContext context, ChartTier base) {
        return modifierLines(context, base, StarChartSettings.DEFAULT_FLEET_LIMIT);
    }

    public static List<StyledText> modifierLines(ChartGenContext context, ChartTier base, int fleetLimit) {
        List<StyledText> lines = new ArrayList<>();
        for (ChartPreview.Line line : ChartPreview.of(context, base, fleetLimit)) {
            lines.add(describe(line));
        }
        return lines;
    }

    public static StyledText describe(ChartPreview.Line line) {
        StyledText value = StyledText.styled(valueText(line), line.favourable() ? Style.GOOD : Style.BAD);
        return Translation.msg(statKey(line.stat())).arg("value", value).styled();
    }

    public static String statKey(ChartPreview.Stat stat) {
        return "chart.ui.preview.stat." + stat.name().toLowerCase(Locale.ROOT);
    }

    static String valueText(ChartPreview.Line line) {
        if (line.stat().kind() == ChartPreview.Kind.MULTIPLIER) {
            return Translation.msg("chart.ui.preview.multiplier").arg("value", ChartPreview.rounded(line.value())).text();
        }
        String signed = line.isRange()
                ? Translation.msg("chart.ui.preview.range").arg("low", signed(line.value())).arg("high", signed(line.upperValue())).text()
                : signed(line.value());
        if (line.stat().kind() == ChartPreview.Kind.PERCENT) {
            return Translation.msg("chart.ui.preview.percent").arg("value", signed).text();
        }
        return signed;
    }

    private static String signed(float value) {
        float rounded = ChartPreview.rounded(value);
        return (rounded > 0f ? "+" : "") + NumberText.format(rounded);
    }

    public static StyledText tierLine(int tier) {
        return tierLine(tier, StarChartSettings.fleetLimit());
    }

    public static StyledText tierLine(int tier, int fleetLimit) {
        ChartTier base = ChartTiers.forTier(tier);
        ChartFleetSpread fewest = ChartFleetSpread.of(base.fleetsMin(), base.fleetSize(), 0, fleetLimit);
        ChartFleetSpread most = ChartFleetSpread.of(base.fleetsMax(), base.fleetSize(), 0, fleetLimit);
        int strengthMin = Math.min(NpcTreeSize.HIGHEST_PERCENT, base.fleetStrengthMin() + fewest.totalStrengthBonus());
        int strengthMax = Math.min(NpcTreeSize.HIGHEST_PERCENT, base.fleetStrengthMax() + most.totalStrengthBonus());
        String key = base.boss() ? "chart.ui.preview.tierBaseWarden" : "chart.ui.preview.tierBase";
        return Translation.msg(key).arg("tier", base.tier()).arg("fleetsMin", fewest.fleets()).arg("fleetsMax", most.fleets())
                .arg("size", percentRange(Math.round(fewest.cappedFleetSize()), Math.round(most.cappedFleetSize())))
                .arg("quality", Math.round(base.fleetQuality())).arg("strength", percentRange(strengthMin, strengthMax))
                .arg("warden", Math.round(base.wardenSize())).arg("loot", Math.round(base.lootBudget())).styled();
    }

    private static String percentRange(int low, int high) {
        String lowText = Translation.msg("chart.ui.preview.percent").arg("value", low).text();
        return low == high ? lowText
                : Translation.msg("chart.ui.preview.range").arg("low", lowText).arg("high", Translation.msg("chart.ui.preview.percent").arg("value", high).text()).text();
    }
}
