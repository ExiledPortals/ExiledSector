package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

final class SurveyPlanetsStep implements HarnessStep {

    @Override
    public String name() {
        return "surveyPlanets";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
        if (notInCampaign != null) {
            return notInCampaign;
        }
        List<String> surveyed = new ArrayList<>();
        for (PlanetAPI planet : Global.getSector().getPlayerFleet().getContainingLocation().getPlanets()) {
            if (planet.getMarket() != null && !planet.isStar()) {
                Misc.setFullySurveyed(planet.getMarket(), null, false);
                surveyed.add(planet.getName());
            }
        }
        return StepOutcome.passed("fully surveyed " + surveyed.size() + " planets: " + String.join(", ", surveyed));
    }
}
