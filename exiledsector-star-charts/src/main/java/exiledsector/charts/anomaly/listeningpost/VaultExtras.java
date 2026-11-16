package exiledsector.charts.anomaly.listeningpost;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemRoll;
import exiledsector.items.StarChartCorruption;
import exiledsector.loot.Loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class VaultExtras {

    private static final long STOLEN_CHARTS_SALT = 0x53544f4c454e43L;
    private static final long LEDGER_SALT = 0x4c454447455252L;

    private VaultExtras() {
    }

    static Loot roll(SectorAPI sector, ChartRun run, PostState state, DepotTuning tuning) {
        Loot extras = Loot.NONE;
        if (state.quiet()) {
            List<ItemData> charts = stolenCharts(run.tier(), tuning.maxStolenCharts(), Seeds.derived(state.seed(), STOLEN_CHARTS_SALT));
            if (!charts.isEmpty()) {
                extras = extras.plus(charts, List.of(), Map.of());
            }
        }
        List<CarriedItem> ledger = ledgerPicks(territoryRewards(sector, run), tuning.ledgerRewards(), Seeds.derived(state.seed(), LEDGER_SALT));
        if (!ledger.isEmpty()) {
            extras = extras.plus(OfficerRewardDrop.roll(ledger, run, Seeds.derived(state.seed(), LEDGER_SALT + 1)));
        }
        return extras;
    }

    static List<ItemData> stolenCharts(int runTier, int maxCharts, Random random) {
        int count = ListeningPostRules.stolenChartCount(random, maxCharts);
        List<ItemData> charts = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int tier = ListeningPostRules.stolenChartTier(random, runTier);
            ItemDefinition definition = ChartDrops.pickChartAtTier(random, tier);
            if (definition != null) {
                charts.add(StarChartCorruption.withExpandedModifiers(ItemData.rolled(definition, random.nextLong(), ItemRoll.atTier(tier)), random));
            }
        }
        return charts;
    }

    static List<CarriedItem> ledgerPicks(List<CarriedItem> rewards, int count, Random random) {
        List<CarriedItem> left = new ArrayList<>(rewards);
        List<CarriedItem> picked = new ArrayList<>();
        while (picked.size() < count && !left.isEmpty()) {
            picked.add(left.remove(random.nextInt(left.size())));
        }
        return picked;
    }

    static List<CarriedItem> territoryRewards(SectorAPI sector, ChartRun run) {
        AtlasRunNode node = run == null ? null : run.atlasNode();
        AtlasState state = node == null ? null : AtlasState.getIfCreated(sector);
        if (state == null) {
            return List.of();
        }
        AtlasCell centre = state.commandCentreOf(AtlasCell.unpack(node.cell()));
        AtlasOfficer officer = centre == null ? null : state.officerAt(centre);
        return officer == null || !officer.isActive() ? List.of() : officer.carried();
    }
}
