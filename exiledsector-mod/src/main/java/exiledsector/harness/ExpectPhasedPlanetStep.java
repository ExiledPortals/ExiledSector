package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.phasing.PhaseDestination;
import exiledsector.charts.phasing.PhaseGateState;
import exiledsector.charts.phasing.PhasePull;
import exiledsector.charts.phasing.PhasedPlanetPlacement;
import exiledsector.charts.phasing.PhasedPlanetTraits;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class ExpectPhasedPlanetStep implements HarnessStep {

    static final String PLANET_DATA = "phasedPlanet";

    @Override
    public String name() {
        return "expectPhasedPlanet";
    }

    @Override
    public StepOutcome advance(StepContext context) throws JSONException {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
        if (notInCampaign != null) {
            return notInCampaign;
        }
        SectorAPI sector = Global.getSector();
        MarketAPI market = HarnessCampaign.gateMarket(sector);
        if (market == null || market.getStarSystem() == null) {
            return StepOutcome.failed("the player has no Phase Displacement Gate in a star system; " + HarnessCampaign.colonyStatus(sector, context.results()));
        }
        StarSystemAPI system = market.getStarSystem();
        if (PhaseGateState.hasJob(market)) {
            return StepOutcome.failed("the pull is still running, " + Math.round(PhaseGateState.jobDays(market)) + " days in");
        }
        if (sector.getEntityById(PhasePull.wormholeId(market)) != null) {
            return StepOutcome.failed("the pull finished but its wormhole is still in " + system.getName());
        }
        JSONObject lock = context.results().data(LockPhasePlanetStep.LOCK_DATA);
        String expectedName = lock == null ? null : lock.getString("name");
        PlanetAPI arrived = null;
        List<String> phasedNames = new ArrayList<>();
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet.hasTag(PhasedPlanetPlacement.PHASED_TAG)) {
                phasedNames.add(planet.getName());
                if (expectedName == null || expectedName.equals(planet.getName())) {
                    arrived = planet;
                }
            }
        }
        if (arrived == null) {
            return StepOutcome.failed("no phased planet" + (expectedName == null ? "" : " named " + expectedName) + " in " + system.getName()
                    + "; phased planets there: " + phasedNames);
        }
        List<String> missing = lock == null ? List.of() : missingConditions(lock, arrived.getMarket());
        float gateOrbitRadius = PhaseDestination.distanceFromFocus(arrived.getOrbitFocus(), market.getPrimaryEntity());
        JSONObject planetData = planetData(arrived);
        planetData.put("gateOrbitRadius", gateOrbitRadius);
        context.results().addData(PLANET_DATA, planetData);
        String arrival = arrived.getName() + " (" + arrived.getTypeId() + ") orbits " + arrived.getOrbitFocus().getName() + " at "
                + Math.round(arrived.getCircularOrbitRadius()) + " in " + system.getName() + ", beside " + market.getName() + " at "
                + Math.round(gateOrbitRadius);
        if (lock != null && !lock.getString("type").equals(arrived.getTypeId())) {
            return StepOutcome.failed(arrival + ", but was locked as " + lock.getString("type"));
        }
        return missing.isEmpty() ? StepOutcome.passed(arrival + " with every kept condition and trait")
                : StepOutcome.failed(arrival + " without the conditions " + missing);
    }

    private static List<String> missingConditions(JSONObject lock, MarketAPI market) throws JSONException {
        List<String> missing = new ArrayList<>();
        if (market == null || !market.hasCondition(PhasedPlanetTraits.PHASE_DISPLACED_CONDITION_ID)) {
            missing.add(PhasedPlanetTraits.PHASE_DISPLACED_CONDITION_ID);
        }
        for (String key : List.of("conditions", "traits")) {
            JSONArray expected = lock.getJSONArray(key);
            for (int index = 0; index < expected.length(); index++) {
                String conditionId = expected.getString(index);
                if (market == null || !market.hasCondition(conditionId)) {
                    missing.add(conditionId);
                }
            }
        }
        return missing;
    }

    private static JSONObject planetData(PlanetAPI planet) throws JSONException {
        JSONObject data = new JSONObject();
        data.put("id", planet.getId());
        data.put("name", planet.getName());
        data.put("type", planet.getTypeId());
        data.put("radius", planet.getRadius());
        data.put("focus", planet.getOrbitFocus() == null ? JSONObject.NULL : planet.getOrbitFocus().getName());
        data.put("orbitRadius", planet.getCircularOrbitRadius());
        JSONArray conditions = new JSONArray();
        if (planet.getMarket() != null) {
            for (MarketConditionAPI condition : planet.getMarket().getConditions()) {
                conditions.put(condition.getId());
            }
        }
        data.put("conditions", conditions);
        MemoryAPI memory = planet.getMemoryWithoutUpdate();
        data.put("origin", memory.getString(PhasedPlanetPlacement.ORIGIN_MEMORY_KEY));
        data.put("region", memory.getString(PhasedPlanetPlacement.REGION_MEMORY_KEY));
        data.put("tier", memory.get(PhasedPlanetPlacement.TIER_MEMORY_KEY));
        return data;
    }
}
