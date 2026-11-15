package exiledsector.charts.phasing;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.Seeds;
import exiledsector.charts.ChartIds;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class PhaseLock {

    static final String TRAIT_SEED_PREFIX = "phaseTraits|";

    public enum Offer {
        AVAILABLE, NOT_SURVEYED, LOCKED
    }

    private PhaseLock() {
    }

    public static MarketAPI idleGateMarket(SectorAPI sector) {
        MarketAPI gateMarket = PhaseDisplacementGate.playerGateMarket(sector);
        if (gateMarket == null) {
            return null;
        }
        Industry gate = gateMarket.getIndustry(PhaseDisplacementGate.INDUSTRY_ID);
        return gate == null || gate.isBuilding() || PhaseGateState.hasJob(gateMarket) ? null : gateMarket;
    }

    public static boolean isEligible(SectorEntityToken target) {
        if (!(target instanceof PlanetAPI planet) || planet.isStar() || planet.getMarket() == null) {
            return false;
        }
        if (planet.hasTag(ChartIds.CONTENT_TAG) || planet.hasTag(Tags.NON_CLICKABLE)) {
            return false;
        }
        return ChartRuns.liveIn(planet.getContainingLocation()) != null && ChartAnomalies.dialogFor(planet) == null;
    }

    public static Offer offerFor(SectorAPI sector, SectorEntityToken target) {
        if (!StarChartSettings.enabled() || !isEligible(target)) {
            return null;
        }
        MarketAPI gateMarket = idleGateMarket(sector);
        if (gateMarket == null) {
            return null;
        }
        PhasedPlanetRecord lock = currentLock(gateMarket);
        if (lock != null && target.getId().equals(lock.planetId())) {
            return Offer.LOCKED;
        }
        return target.getMarket().getSurveyLevel() == SurveyLevel.FULL ? Offer.AVAILABLE : Offer.NOT_SURVEYED;
    }

    public static PhasedPlanetRecord currentLock(MarketAPI gateMarket) {
        PhasedPlanetRecord lock = PhaseGateState.lock(gateMarket);
        if (lock == null) {
            return null;
        }
        if (ChartRuns.unclosed(lock.runId()) == null) {
            PhaseGateState.clearLock(gateMarket);
            return null;
        }
        return lock;
    }

    public static PhasedPlanetRecord lock(SectorAPI sector, PlanetAPI planet) {
        if (offerFor(sector, planet) != Offer.AVAILABLE) {
            return null;
        }
        ChartRun run = ChartRuns.liveIn(planet.getContainingLocation());
        MarketAPI gateMarket = idleGateMarket(sector);
        PhasedPlanetRecord lock = recordOf(planet, run, AtlasRuntime.regionOf(sector, run.atlasNode()));
        PhaseGateState.setLock(gateMarket, lock);
        return lock;
    }

    static PhasedPlanetRecord recordOf(PlanetAPI planet, ChartRun run, String regionId) {
        StarSystemAPI system = planet.getStarSystem();
        String texture = planet.getSpec() == null ? null : planet.getSpec().getTexture();
        Random random = Seeds.random(TRAIT_SEED_PREFIX + run.runId() + "|" + planet.getId());
        List<String> traits = PhasedPlanetTraits.roll(run.tier(), regionId, run.anomalyIds(), random);
        return new PhasedPlanetRecord(run.runId(), planet.getId(), planet.getName(), planet.getTypeId(), planet.getRadius(), texture,
                keptConditions(planet.getMarket()), system == null ? null : system.getBaseName(), regionId, run.anomalyIds(), run.tier(),
                traits);
    }

    static List<String> keptConditions(MarketAPI market) {
        List<String> kept = new ArrayList<>();
        for (MarketConditionAPI condition : market.getConditions()) {
            MarketConditionSpecAPI spec = condition.getSpec();
            if (spec != null && !spec.isDecivRemove() && !kept.contains(condition.getId())) {
                kept.add(condition.getId());
            }
        }
        return kept;
    }

    public static boolean runClosing(SectorAPI sector, ChartRun run) {
        MarketAPI gateMarket = PhaseDisplacementGate.playerGateMarket(sector);
        PhasedPlanetRecord lock = PhaseGateState.lock(gateMarket);
        if (lock == null || run == null || !run.runId().equals(lock.runId())) {
            return false;
        }
        PhaseGateState.clearLock(gateMarket);
        if (!run.isObjectiveComplete() || PhaseGateState.hasJob(gateMarket)) {
            return false;
        }
        PhaseGateState.startJob(gateMarket, lock);
        PhasePull.started(sector, gateMarket);
        return true;
    }
}
