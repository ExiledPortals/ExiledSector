package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.FleetMembers;
import exiledsector.Seeds;
import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.objective.ChartObjectives;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.officer.reward.RewardGroup;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.ScarMemory;
import exiledsector.charts.officer.scar.Scars;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemDrops;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class RelayChoices {

    static final String OFFERS_FIELD_PREFIX = "offers.";
    static final String PICK_FIELD_PREFIX = "pick.";
    static final String OFFER_SEPARATOR = ",";
    static final String OFFER_SEED_PREFIX = "relayChoices|";
    static final String REWARD_SEED_PREFIX = "relayReward|";

    private RelayChoices() {
    }

    static boolean open(ChartRun run, RelayKind kind) {
        return run != null && run.hasBoss() && !run.isObjectiveComplete() && !run.isObjectiveLost()
                && RelayStates.isWoken(run, kind) && picked(run, kind) == null;
    }

    static List<RelayChoice> offers(SectorAPI sector, ChartRun run, RelayKind kind) {
        if (!open(run, kind)) {
            return List.of();
        }
        List<RelayChoice> stored = storedOffers(run, kind);
        if (!stored.isEmpty()) {
            return stored;
        }
        RewardContext context = RewardContext.forRun(sector, run, run.contentFaction());
        List<RelayChoice> rolled = roll(context, bossHull(sector, run), offerRandom(run, kind));
        storeOffers(run, kind, rolled);
        return rolled;
    }

    static Random offerRandom(ChartRun run, RelayKind kind) {
        return Seeds.random(OFFER_SEED_PREFIX + run.runId() + "|" + kind.key());
    }

    static List<RelayChoice> roll(RewardContext context, HullFacts bossHull, Random random) {
        return paired(OfficerRewardPool.offeredGroups(context, random), Scars.withEffectsOn(bossHull), random);
    }

    static List<RelayChoice> paired(List<RewardGroup> groups, List<String> scarIds, Random random) {
        List<String> unpairedScars = new ArrayList<>(scarIds);
        List<RelayChoice> choices = new ArrayList<>();
        for (RewardGroup group : groups) {
            if (unpairedScars.isEmpty()) {
                break;
            }
            choices.add(new RelayChoice(group, unpairedScars.remove(random.nextInt(unpairedScars.size()))));
        }
        return List.copyOf(choices);
    }

    static List<RelayChoice> storedOffers(ChartRun run, RelayKind kind) {
        String stored = state(run).text(OFFERS_FIELD_PREFIX + kind.key());
        if (stored == null) {
            return List.of();
        }
        List<RelayChoice> offers = new ArrayList<>();
        for (String encoded : stored.split(OFFER_SEPARATOR)) {
            RelayChoice offer = RelayChoice.decode(encoded);
            if (offer != null) {
                offers.add(offer);
            }
        }
        return List.copyOf(offers);
    }

    static void storeOffers(ChartRun run, RelayKind kind, List<RelayChoice> offers) {
        state(run).put(OFFERS_FIELD_PREFIX + kind.key(), String.join(OFFER_SEPARATOR, offers.stream().map(RelayChoice::encoded).toList()));
    }

    static boolean pick(SectorAPI sector, ChartRun run, RelayKind kind, int index) {
        List<RelayChoice> offers = offers(sector, run, kind);
        if (index < 0 || index >= offers.size()) {
            return false;
        }
        state(run).put(PICK_FIELD_PREFIX + kind.key(), offers.get(index).encoded());
        scarBoss(sector, run);
        return true;
    }

    public static RelayChoice picked(ChartRun run, RelayKind kind) {
        return RelayChoice.decode(state(run).text(PICK_FIELD_PREFIX + kind.key()));
    }

    public static List<RelayChoice> picks(ChartRun run) {
        List<RelayChoice> picks = new ArrayList<>();
        for (RelayKind kind : RelayKind.values()) {
            RelayChoice pick = picked(run, kind);
            if (pick != null) {
                picks.add(pick);
            }
        }
        return picks;
    }

    static Map<String, Integer> scarStacks(List<RelayChoice> picks) {
        Map<String, Integer> stacks = new LinkedHashMap<>();
        for (RelayChoice pick : picks) {
            stacks.merge(pick.scarId(), 1, Integer::sum);
        }
        return stacks;
    }

    static void scarBoss(SectorAPI sector, ChartRun run) {
        CampaignFleetAPI boss = ChartObjectives.bossFleet(sector, run);
        if (boss != null) {
            Scars.grantRunScars(boss, scarredFlagshipId(boss, run), scarStacks(picks(run)));
        }
    }

    static ItemDrops.SiteLoot bossReward(SectorAPI sector, ChartRun run) {
        List<RelayChoice> picks = picks(run);
        if (picks.isEmpty()) {
            return ItemDrops.SiteLoot.NONE;
        }
        return bossReward(picks, RewardContext.forRun(sector, run, run.contentFaction()), run, Seeds.random(REWARD_SEED_PREFIX + run.runId()));
    }

    static ItemDrops.SiteLoot bossReward(List<RelayChoice> picks, RewardContext context, ChartRun run, Random random) {
        List<RewardGroup> groups = picks.stream().map(RelayChoice::group).toList();
        return OfficerRewardDrop.roll(OfficerRewardPool.drawn(groups, context, random), run, random);
    }

    static HullFacts bossHull(SectorAPI sector, ChartRun run) {
        CampaignFleetAPI boss = ChartObjectives.bossFleet(sector, run);
        return boss == null ? null : HullFacts.of(FleetMembers.withId(boss, scarredFlagshipId(boss, run)));
    }

    static String scarredFlagshipId(CampaignFleetAPI boss, ChartRun run) {
        String scarred = Scars.flagshipIdOf(boss) != null ? Scars.flagshipIdOf(boss) : ScarMemory.runFlagshipId(boss.getMemoryWithoutUpdate());
        if (scarred != null) {
            return scarred;
        }
        return run.bossFlagshipId() != null ? run.bossFlagshipId() : ChartObjectives.flagshipIdOf(boss);
    }

    private static AnomalyState state(ChartRun run) {
        return RelayStates.of(run);
    }
}
