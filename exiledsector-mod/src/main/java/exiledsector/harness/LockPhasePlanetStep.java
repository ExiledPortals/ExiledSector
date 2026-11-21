package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.phasing.PhaseLock;
import exiledsector.charts.phasing.PhasedPlanetRecord;
import exiledsector.charts.run.ChartRun;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class LockPhasePlanetStep implements HarnessStep {

    static final String LOCK_DATA = "phaseLock";

    @Override
    public String name() {
        return "lockPhasePlanet";
    }

    @Override
    public StepOutcome advance(StepContext context) throws JSONException {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
        if (notInCampaign != null) {
            return notInCampaign;
        }
        SectorAPI sector = Global.getSector();
        ChartRun run = HarnessCampaign.runAroundPlayer(sector);
        if (run == null) {
            return StepOutcome.failed("the player fleet isn't in a live Star Chart run");
        }
        if (PhaseLock.idleGateMarket(sector) == null) {
            return StepOutcome.failed("the player has no idle Phase Displacement Gate");
        }
        StarSystemAPI system = sector.getStarSystem(run.systemId());
        List<String> skipped = new ArrayList<>();
        for (PlanetAPI planet : system.getPlanets()) {
            if (!PhaseLock.isEligible(planet)) {
                skipped.add(planet.getName());
                continue;
            }
            Misc.setFullySurveyed(planet.getMarket(), null, false);
            PhasedPlanetRecord lock = PhaseLock.lock(sector, planet);
            if (lock != null) {
                context.results().addData(LOCK_DATA, lockData(lock));
                return StepOutcome.passed("locked " + lock.name() + " (" + lock.typeId() + ", radius " + Math.round(lock.radius()) + ") with "
                        + lock.conditionIds().size() + " kept conditions and traits " + lock.traitConditionIds());
            }
            skipped.add(planet.getName() + " (offer " + PhaseLock.offerFor(sector, planet) + ")");
        }
        return StepOutcome.failed("no planet in " + system.getName() + " could be locked; passed over " + skipped);
    }

    static JSONObject lockData(PhasedPlanetRecord lock) throws JSONException {
        JSONObject data = new JSONObject();
        data.put("runId", lock.runId());
        data.put("planetId", lock.planetId());
        data.put("name", lock.name());
        data.put("type", lock.typeId());
        data.put("radius", lock.radius());
        data.put("conditions", new JSONArray(lock.conditionIds()));
        data.put("traits", new JSONArray(lock.traitConditionIds()));
        data.put("origin", lock.originName());
        data.put("region", lock.regionId());
        data.put("tier", lock.tier());
        return data;
    }
}
