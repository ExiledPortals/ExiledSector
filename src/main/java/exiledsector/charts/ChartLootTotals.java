package exiledsector.charts;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.StarChart;

import java.util.ArrayList;
import java.util.List;

final class ChartLootTotals {

    static final String DROP_TYPE_KEY_PREFIX = "chart.dropType.";

    private ChartLootTotals() {
    }

    static List<StyledText> lines(StarChart chart) {
        return lines(ChartGenContext.from(chart));
    }

    static List<StyledText> lines(ChartGenContext context) {
        List<StyledText> lines = new ArrayList<>();
        addLine(lines, Translation.text("chart.totals.quantity"), context.quantityPercent());
        addLine(lines, Translation.text("chart.totals.items"), context.itemPercent());
        for (DropType type : DropType.values()) {
            addLine(lines, Translation.text(DROP_TYPE_KEY_PREFIX + type.name()), context.dropPercent(type));
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
