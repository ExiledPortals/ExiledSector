package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.charts.StarCharts;
import exiledlib.i18n.StyledText;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChart;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Locale;

public class ChartListCommand implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        int absorbed = StarCharts.absorbCharts(Global.getSector());
        ItemStore store = ItemStore.starChartsIfCreated();
        List<Item> charts = store == null ? List.of() : store.owned();
        StringBuilder listing = new StringBuilder();
        if (absorbed > 0) {
            listing.append("Moved ").append(absorbed).append(" Star Chart(s) from your cargo into Star Chart storage.\n");
        }
        if (charts.isEmpty()) {
            listing.append("Star Chart storage is empty. ExiledSectorGrantStarCharts adds some to your cargo.");
        } else {
            listing.append(charts.size()).append(" stored Star Chart(s); open one with ExiledSectorChartOpenStored <index>:");
            for (int index = 0; index < charts.size(); index++) {
                listing.append("\n").append(index + 1).append(". ").append(describe(charts.get(index)));
            }
        }
        Console.showMessage(listing.toString());
        return CommandResult.SUCCESS;
    }

    static String describe(Item chart) {
        if (chart == null) {
            return "an unreadable chart";
        }
        StringBuilder text = new StringBuilder(chart.name()).append(" [").append(chart.rarity().name().toLowerCase(Locale.ROOT));
        if (chart instanceof StarChart starChart) {
            text.append(", tier ").append(starChart.tier()).append(", ").append(starChart.systemType());
            if (starChart.quality() > 0) {
                text.append(", quality ").append(starChart.quality());
            }
        }
        if (chart.corrupted()) {
            text.append(", corrupted");
        }
        text.append("]");
        appendLines(text, "implicit", chart.implicitLines(false));
        appendLines(text, "modifiers", chart.effectLines(false));
        return text.toString();
    }

    private static void appendLines(StringBuilder text, String label, List<StyledText> lines) {
        if (lines.isEmpty()) {
            return;
        }
        text.append("\n   ").append(label).append(": ");
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                text.append("; ");
            }
            text.append(lines.get(index).plain());
        }
    }
}
