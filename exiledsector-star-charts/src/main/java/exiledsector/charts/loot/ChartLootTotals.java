package exiledsector.charts.loot;

import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.content.ChartPoi;
import exiledsector.items.StarChart;

import java.util.ArrayList;
import java.util.List;

public final class ChartLootTotals {

    static final String DROP_TYPE_KEY_PREFIX = "chart.dropType.";
    static final String SITE_CATEGORY_KEY_PREFIX = "chart.siteCategory.";

    private ChartLootTotals() {
    }

    public static List<StyledText> lines(StarChart chart) {
        return lines(ChartGenContext.from(chart));
    }

    public static List<StyledText> lines(ChartGenContext context) {
        List<StyledText> lines = new ArrayList<>();
        addLine(lines, Translation.text("chart.totals.quantity"), context.quantityPercent());
        addLine(lines, Translation.text("chart.totals.items"), context.itemPercent());
        addLine(lines, Translation.text("chart.totals.itemRarity"), context.itemRarityPercent());
        for (DropType type : DropType.values()) {
            addLine(lines, Translation.text(DROP_TYPE_KEY_PREFIX + type.name()), context.dropPercent(type));
        }
        addLine(lines, Translation.text("chart.totals.bossSpoils"), context.bossSpoilsPercent());
        for (ChartPoi.Kind category : ChartPoi.Kind.values()) {
            addLine(lines, Translation.text(SITE_CATEGORY_KEY_PREFIX + category.name()), context.siteBudgetPercent(category));
        }
        return lines;
    }

    private static void addLine(List<StyledText> lines, String name, float percent) {
        if (Math.round(percent) == 0) {
            return;
        }
        String key = percent > 0f ? "chart.totals.bonus" : "chart.totals.penalty";
        lines.add(Translation.msg(key).arg("name", name).arg("value", Math.abs(percent)).styled());
    }
}
