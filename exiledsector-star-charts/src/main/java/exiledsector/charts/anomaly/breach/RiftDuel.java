package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.items.ItemDrops;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record RiftDuel(int duplicatedRewards, int extraOffers) {

    public static final RiftDuel NONE = new RiftDuel(0, 0);
    static final String SEED_PREFIX = "riftDuel|";

    public static RiftDuel at(SectorAPI sector, ChartRun run) {
        CampaignFleetAPI player = sector == null ? null : sector.getPlayerFleet();
        if (run == null || player == null) {
            return NONE;
        }
        BreachTerrainPlugin breach = SpatialBreach.breachIn(player.getContainingLocation());
        return breach != null && breach.holds(player) ? of(ChartRunEffects.contextOf(run)) : NONE;
    }

    static RiftDuel of(ChartGenContext context) {
        return new RiftDuel(BreachLines.count(context, BreachLines.OFFICER_DUPLICATES_KEY), BreachLines.count(context, BreachLines.OFFICER_OFFERS_KEY));
    }

    public ItemDrops.SiteLoot duplicatedLoot(AtlasOfficer officer, ChartRun run) {
        if (duplicatedRewards <= 0 || officer == null || run == null) {
            return ItemDrops.SiteLoot.NONE;
        }
        Random random = Seeds.random(SEED_PREFIX + run.runId() + "|" + officer.recordId());
        List<CarriedItem> duplicated = duplicated(officer.carried(), duplicatedRewards, random);
        return duplicated.isEmpty() ? ItemDrops.SiteLoot.NONE : OfficerRewardDrop.roll(duplicated, run, random);
    }

    static List<CarriedItem> duplicated(List<CarriedItem> carried, int count, Random random) {
        List<CarriedItem> remaining = new ArrayList<>(carried == null ? List.of() : carried);
        List<CarriedItem> picked = new ArrayList<>();
        while (picked.size() < count && !remaining.isEmpty()) {
            picked.add(remaining.remove(random.nextInt(remaining.size())));
        }
        return picked;
    }
}
