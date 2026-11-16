package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.FleetMembers;
import exiledsector.Seeds;
import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.objective.ChartObjectives;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.officer.reward.RewardGroup;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.ScarMemory;
import exiledsector.charts.officer.scar.Scars;
import exiledsector.charts.run.ChartRun;

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

    record Offering(int choices, int extraScars) {

        static final Offering PLAIN = new Offering(OfficerRewardPool.OFFER_COUNT, 0);

        static Offering of(ChartGenContext context) {
            return new Offering(RelayTuning.choiceCount(context), RelayTuning.extraScars(context));
        }
    }

    private RelayChoices() {
    }

    static boolean open(ChartRun run, RelaySlot slot) {
        return run != null && run.hasBoss() && !run.isObjectiveComplete()
                && RelayStates.isWoken(run, slot) && picked(run, slot) == null;
    }

    static List<RelayChoice> offers(SectorAPI sector, ChartRun run, RelaySlot slot) {
        if (!open(run, slot)) {
            return List.of();
        }
        List<RelayChoice> stored = storedOffers(run, slot);
        if (!stored.isEmpty()) {
            return stored;
        }
        RewardContext context = RewardContext.forRun(sector, run, run.contentFaction());
        List<RelayChoice> rolled = roll(context, bossHull(sector, run), Offering.of(RelayTuning.of(run)), offerRandom(run, slot));
        storeOffers(run, slot, rolled);
        return rolled;
    }

    static Random offerRandom(ChartRun run, RelaySlot slot) {
        return Seeds.random(OFFER_SEED_PREFIX + run.runId() + "|" + slot.key());
    }

    static List<RelayChoice> roll(RewardContext context, HullFacts bossHull, Offering offering, Random random) {
        return paired(OfficerRewardPool.offeredGroups(context, offering.choices(), random), Scars.withEffectsOn(bossHull), offering.extraScars(), random);
    }

    static List<RelayChoice> paired(List<RewardGroup> groups, List<String> scarIds, int extraScars, Random random) {
        List<String> unpairedScars = new ArrayList<>(scarIds);
        List<RelayChoice> choices = new ArrayList<>();
        for (RewardGroup group : groups) {
            if (unpairedScars.isEmpty()) {
                break;
            }
            String scarId = unpairedScars.remove(random.nextInt(unpairedScars.size()));
            choices.add(new RelayChoice(group, scarId, extraScarsFor(scarIds, scarId, extraScars, random)));
        }
        return List.copyOf(choices);
    }

    static List<String> extraScarsFor(List<String> scarIds, String ownScarId, int extraScars, Random random) {
        List<String> candidates = new ArrayList<>(scarIds);
        candidates.remove(ownScarId);
        List<String> extras = new ArrayList<>();
        while (extras.size() < extraScars && !candidates.isEmpty()) {
            extras.add(candidates.remove(random.nextInt(candidates.size())));
        }
        return extras;
    }

    static List<RelayChoice> storedOffers(ChartRun run, RelaySlot slot) {
        String stored = state(run).text(OFFERS_FIELD_PREFIX + slot.key());
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

    static void storeOffers(ChartRun run, RelaySlot slot, List<RelayChoice> offers) {
        state(run).put(OFFERS_FIELD_PREFIX + slot.key(), String.join(OFFER_SEPARATOR, offers.stream().map(RelayChoice::encoded).toList()));
    }

    static boolean pick(SectorAPI sector, ChartRun run, RelaySlot slot, int index) {
        List<RelayChoice> offers = offers(sector, run, slot);
        if (index < 0 || index >= offers.size()) {
            return false;
        }
        RelayChoice choice = offers.get(index);
        state(run).put(PICK_FIELD_PREFIX + slot.key(), choice.encoded());
        scarBoss(sector, run);
        if (RelayTuning.sharedBroadcast(RelayTuning.of(run))) {
            RelayBroadcast.share(sector, run, slot, choice);
        }
        return true;
    }

    public static RelayChoice picked(ChartRun run, RelaySlot slot) {
        return RelayChoice.decode(state(run).text(PICK_FIELD_PREFIX + slot.key()));
    }

    public static List<RelayChoice> picks(ChartRun run) {
        List<RelayChoice> picks = new ArrayList<>();
        for (RelaySlot slot : RelayStates.slots(run)) {
            RelayChoice pick = picked(run, slot);
            if (pick != null) {
                picks.add(pick);
            }
        }
        return picks;
    }

    static Map<String, Integer> scarStacks(List<RelayChoice> picks) {
        Map<String, Integer> stacks = new LinkedHashMap<>();
        for (RelayChoice pick : picks) {
            for (String scarId : pick.scarIds()) {
                stacks.merge(scarId, 1, Integer::sum);
            }
        }
        return stacks;
    }

    static int scarCount(List<RelayChoice> picks) {
        return picks.stream().mapToInt(pick -> pick.scarIds().size()).sum();
    }

    static void scarBoss(SectorAPI sector, ChartRun run) {
        CampaignFleetAPI boss = ChartObjectives.bossFleet(sector, run);
        if (boss != null) {
            Scars.grantRunScars(boss, scarredFlagshipId(boss, run), scarStacks(picks(run)));
        }
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
