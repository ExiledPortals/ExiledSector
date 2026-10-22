package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.ChartRunActions;
import exiledsector.charts.ChartRunActions.OpenResult;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;

import java.util.Locale;
import java.util.Random;

public final class StarChartOpening {

    public interface Opener {

        OpenResult open(SectorAPI sector, int entriesAllowed, Random random, ItemStore store, Item chart);
    }

    public static final Opener FROM_STORE = ChartRunActions::openFromStore;

    public record Outcome(OpenResult result, WormholeOpenCheck.Blocker blocker) {

        public boolean opened() {
            return result == OpenResult.OPENED;
        }

        public String messageKey() {
            if (blocker != null && !blocker.allowsOpening()) {
                return blocker.reasonKey();
            }
            return "chart.ui.open.result." + result.name().toLowerCase(Locale.ROOT);
        }
    }

    private StarChartOpening() {
    }

    public static Outcome open(Opener opener, SectorAPI sector, ItemStore store, Item chart, int entriesAllowed, Random random) {
        WormholeOpenCheck.Blocker blocker = WormholeOpenCheck.current(sector, store, chart);
        return open(opener, blocker, sector, store, chart, entriesAllowed, random);
    }

    static Outcome open(Opener opener, WormholeOpenCheck.Blocker blocker, SectorAPI sector, ItemStore store, Item chart, int entriesAllowed,
                        Random random) {
        if (!blocker.allowsOpening()) {
            return new Outcome(null, blocker);
        }
        OpenResult result = opener.open(sector, entriesAllowed, random, store, chart);
        return new Outcome(result == null ? OpenResult.FAILED : result, null);
    }
}
