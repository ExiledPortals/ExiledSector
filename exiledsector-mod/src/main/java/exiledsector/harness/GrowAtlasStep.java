package exiledsector.harness;

import com.fs.starfarer.api.Global;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerSightings;

import java.util.Comparator;
import java.util.List;

final class GrowAtlasStep implements HarnessStep {

    static final int DEFAULT_TIER = 1;

    private final int completions;
    private final int tier;
    private final boolean identifyOfficers;

    GrowAtlasStep(int completions, int tier, boolean identifyOfficers) {
        if (completions < 0) {
            throw new IllegalArgumentException("growAtlas needs at least 0 completions, not " + completions);
        }
        if (tier < 1) {
            throw new IllegalArgumentException("growAtlas needs a tier of at least 1, not " + tier);
        }
        this.completions = completions;
        this.tier = tier;
        this.identifyOfficers = identifyOfficers;
    }

    @Override
    public String name() {
        return "growAtlas by " + completions + " systems at tier " + tier + (identifyOfficers ? ", identifying every officer" : "");
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("growAtlas");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        AtlasGenerator generator = AtlasRuntime.generator(Global.getSector());
        AtlasState state = AtlasState.get(Global.getSector());
        int completed = 0;
        while (completed < completions) {
            AtlasCell nearest = AtlasReach.reachable(generator, state).stream()
                    .min(Comparator.comparingDouble(AtlasCell::centreDistance).thenComparingInt(AtlasCell::gridX).thenComparingInt(AtlasCell::gridY))
                    .orElse(null);
            if (nearest == null) {
                break;
            }
            AtlasDevTools.complete(generator, state, nearest, tier);
            completed++;
        }
        int identified = 0;
        if (identifyOfficers) {
            List<Integer> hidden = state.officers().stream().filter(officer -> !officer.identified()).map(AtlasOfficer::recordId).toList();
            OfficerSightings.identify(state, hidden);
            identified = hidden.size();
        }
        return StepOutcome.passed("completed " + completed + " atlas systems (" + state.atlasPoints() + " atlas skill points)"
                + (identifyOfficers ? " and identified " + identified + " officers" : ""));
    }
}
