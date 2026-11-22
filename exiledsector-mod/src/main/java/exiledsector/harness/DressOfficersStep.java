package exiledsector.harness;

import com.fs.starfarer.api.Global;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerStartingItems;
import exiledsector.charts.officer.StartingItemRoll;
import exiledsector.charts.officer.reward.RewardGroup;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.Scars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class DressOfficersStep implements HarnessStep {

    static final long DRESS_SEED = 7L;
    static final int ROLLS_PER_REWARD = 8;

    private final Range scars;
    private final Range rewards;
    private final long seed;

    DressOfficersStep(Range scars, Range rewards, long seed) {
        this.scars = scars;
        this.rewards = rewards;
        this.seed = seed;
    }

    @Override
    public String name() {
        return "dressOfficers with " + scars + " scars and " + rewards + " rewards";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("dressOfficers");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        AtlasState state = AtlasState.get(Global.getSector());
        StartingItemRoll roll = OfficerStartingItems.forSector(Global.getSector());
        Random random = new Random(seed);
        int dressed = 0;
        for (AtlasOfficer officer : state.officers()) {
            if (!officer.identified()) {
                continue;
            }
            state.putOfficer(withRewards(withScars(officer, random), roll, random));
            dressed++;
        }
        return StepOutcome.passed("gave " + dressed + " identified officers " + scars + " scars and " + rewards + " rewards");
    }

    private AtlasOfficer withScars(AtlasOfficer officer, Random random) {
        List<String> scarIds = new ArrayList<>(Scars.withEffectsOn(HullFacts.of(OfficerFleets.flagshipVariant(officer))));
        Collections.shuffle(scarIds, random);
        Map<String, Integer> chosen = new LinkedHashMap<>();
        for (String scarId : scarIds.subList(0, Math.min(scarIds.size(), scars.pick(random)))) {
            chosen.put(scarId, 1);
        }
        return officer.withScars(chosen);
    }

    private AtlasOfficer withRewards(AtlasOfficer officer, StartingItemRoll roll, Random random) {
        int wantedRows = rewards.pick(random);
        AtlasOfficer dressed = officer.withCarried(List.of());
        for (int attempt = 0; attempt < wantedRows * ROLLS_PER_REWARD && rewardRows(dressed) < wantedRows; attempt++) {
            AtlasOfficer rolled = OfficerStartingItems.dealt(dressed, roll, random);
            if (rewardRows(rolled) > rewardRows(dressed)) {
                dressed = rolled;
            }
        }
        return dressed;
    }

    private static int rewardRows(AtlasOfficer officer) {
        return RewardGroup.entryCounts(officer.carried()).size();
    }

    record Range(int least, int most) {

        Range {
            if (least < 0 || most < least) {
                throw new IllegalArgumentException("dressOfficers needs a range like [3, 5], not [" + least + ", " + most + "]");
            }
        }

        int pick(Random random) {
            return least + random.nextInt(most - least + 1);
        }

        @Override
        public String toString() {
            return least == most ? Integer.toString(least) : least + "-" + most;
        }
    }
}
