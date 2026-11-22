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
import java.util.Comparator;
import java.util.List;
import java.util.Random;

final class LockPhasePlanetStep implements HarnessStep {

    static final String LOCK_DATA = "phaseLock";
    static final long TYPE_CHANGE_SEED = 1L;

    private final String wantedType;

    LockPhasePlanetStep(String wantedType) {
        this.wantedType = wantedType == null || wantedType.isBlank() ? null : wantedType;
    }

    @Override
    public String name() {
        return wantedType == null ? "lockPhasePlanet" : "lockPhasePlanet " + wantedType;
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
        List<PlanetAPI> planets = new ArrayList<>(system.getPlanets());
        planets.sort(Comparator.comparing(planet -> !isWantedType(planet)));
        for (PlanetAPI planet : planets) {
            if (!PhaseLock.isEligible(planet)) {
                skipped.add(planet.getName());
                continue;
            }
            if (wantedType != null && !isWantedType(planet)) {
                planet.changeType(wantedType, new Random(TYPE_CHANGE_SEED));
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

    private boolean isWantedType(PlanetAPI planet) {
        return wantedType != null && wantedType.equals(planet.getTypeId());
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
