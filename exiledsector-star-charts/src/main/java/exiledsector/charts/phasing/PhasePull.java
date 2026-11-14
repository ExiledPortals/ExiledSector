package exiledsector.charts.phasing;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Pair;
import exiledsector.Seeds;
import exiledsector.charts.ChartIds;
import exiledsector.charts.StarChartSettings;
import exiledsector.i18n.Translation;
import org.lwjgl.util.vector.Vector2f;

public final class PhasePull {

    public static final float PULL_DAYS = 120f;
    public static final int TRANSPLUTONICS_DEMAND = 10;
    public static final int VOLATILES_DEMAND = 8;
    public static final String WORMHOLE_TYPE = "exiledSector_phase_wormhole";
    public static final String WORMHOLE_ID_PREFIX = "exiledSector_phase_wormhole_";
    static final float CHECK_DAYS = 1f;
    static final float WORMHOLE_START_RADIUS = 30f;
    static final float WORMHOLE_PLANET_SHARE = 1.25f;
    static final String ANGLE_SEED_PREFIX = "phaseAngle|";

    public enum Pause {
        DOMAIN_OFF, NOT_FUNCTIONAL, NO_GATEWAY, NO_HYPERSHUNT, SHORTAGE
    }

    private PhasePull() {
    }

    public static Pause pauseFor(PhaseDisplacementGate gate) {
        MarketAPI market = gate.getMarket();
        if (!StarChartSettings.enabled()) {
            return Pause.DOMAIN_OFF;
        }
        if (!gate.isFunctional()) {
            return Pause.NOT_FUNCTIONAL;
        }
        if (!PhaseGateRules.holdsRequiredGateway(market)) {
            return Pause.NO_GATEWAY;
        }
        if (!HypershuntPower.reaches(market)) {
            return Pause.NO_HYPERSHUNT;
        }
        Pair<String, Integer> deficit = gate.getMaxDeficit(Commodities.RARE_METALS, Commodities.VOLATILES);
        return deficit != null && deficit.two != null && deficit.two > 0 ? Pause.SHORTAGE : null;
    }

    public static String pauseKey(Pause pause) {
        return switch (pause) {
            case DOMAIN_OFF -> "chart.phasing.pull.paused.domainOff";
            case NOT_FUNCTIONAL -> "chart.phasing.pull.paused.notFunctional";
            case NO_GATEWAY -> "chart.phasing.pull.paused.gateway";
            case NO_HYPERSHUNT -> "chart.phasing.pull.paused.hypershunt";
            case SHORTAGE -> "chart.phasing.pull.paused.shortage";
        };
    }

    public static PlanetAPI advance(SectorAPI sector, PhaseDisplacementGate gate, float days) {
        MarketAPI market = gate.getMarket();
        PhasedPlanetRecord job = PhaseGateState.job(market);
        StarSystemAPI system = market == null ? null : market.getStarSystem();
        if (job == null || system == null) {
            return null;
        }
        SectorEntityToken focus = PhaseDestination.focusOf(system);
        if (focus == null) {
            return null;
        }
        float orbitRadius = orbitFor(market, system, focus, job);
        CustomCampaignEntityAPI wormhole = ensureWormhole(sector, market, system, focus, job, orbitRadius);
        if (pauseFor(gate) != null) {
            return null;
        }
        float progress = PhaseGateState.jobDays(market) + days;
        if (progress < PULL_DAYS) {
            PhaseGateState.setJobDays(market, progress);
            wormhole.setRadius(wormholeRadius(job, progress));
            return null;
        }
        return complete(sector, market, system, focus, job, orbitRadius, wormhole);
    }

    static float orbitFor(MarketAPI market, StarSystemAPI system, SectorEntityToken focus, PhasedPlanetRecord job) {
        float orbitRadius = PhaseGateState.jobOrbit(market);
        if (orbitRadius <= 0f) {
            orbitRadius = PhaseDestination.orbitRadius(system, focus, job.radius());
            PhaseGateState.setJobOrbit(market, orbitRadius);
        }
        return orbitRadius;
    }

    public static String wormholeId(MarketAPI market) {
        return WORMHOLE_ID_PREFIX + market.getId();
    }

    static CustomCampaignEntityAPI ensureWormhole(SectorAPI sector, MarketAPI market, StarSystemAPI system, SectorEntityToken focus,
                                                  PhasedPlanetRecord job, float orbitRadius) {
        String wormholeId = wormholeId(market);
        if (sector.getEntityById(wormholeId) instanceof CustomCampaignEntityAPI existing) {
            return existing;
        }
        CustomCampaignEntityAPI wormhole = system.addCustomEntity(wormholeId, Translation.gameText("chart.phasing.wormhole.name"),
                WORMHOLE_TYPE, Factions.NEUTRAL);
        wormhole.setCircularOrbit(focus, startAngle(job), orbitRadius, PhaseDestination.orbitDays(orbitRadius));
        wormhole.addTag(ChartIds.HELD_OPEN_WORMHOLE_TAG);
        wormhole.setRadius(wormholeRadius(job, PhaseGateState.jobDays(market)));
        return wormhole;
    }

    static float startAngle(PhasedPlanetRecord job) {
        return Seeds.random(ANGLE_SEED_PREFIX + job.runId() + "|" + job.planetId()).nextFloat() * 360f;
    }

    static float wormholeRadius(PhasedPlanetRecord job, float progress) {
        float share = Math.max(0f, Math.min(1f, progress / PULL_DAYS));
        float fullRadius = Math.max(WORMHOLE_START_RADIUS, job.radius() * WORMHOLE_PLANET_SHARE);
        return WORMHOLE_START_RADIUS + (fullRadius - WORMHOLE_START_RADIUS) * share;
    }

    static PlanetAPI complete(SectorAPI sector, MarketAPI market, StarSystemAPI system, SectorEntityToken focus, PhasedPlanetRecord job,
                              float orbitRadius, CustomCampaignEntityAPI wormhole) {
        float angle = wormhole == null || wormhole.getLocation() == null || focus.getLocation() == null
                ? startAngle(job) : angleBetween(focus.getLocation(), wormhole.getLocation());
        if (wormhole != null) {
            system.removeEntity(wormhole);
        }
        PlanetAPI planet = PhasedPlanetPlacement.place(sector, system, focus, job, orbitRadius, angle);
        PhaseGateState.clearJob(market);
        market.reapplyIndustries();
        PhaseDisplacementIntel.arrived(sector, market, planet);
        return planet;
    }

    static float angleBetween(Vector2f from, Vector2f to) {
        float degrees = (float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
        return degrees < 0f ? degrees + 360f : degrees;
    }

    public static void abandon(SectorAPI sector, MarketAPI market) {
        if (market == null) {
            return;
        }
        PhaseGateState.clearLock(market);
        PhaseGateState.clearJob(market);
        SectorEntityToken wormhole = sector == null ? null : sector.getEntityById(wormholeId(market));
        if (wormhole != null && wormhole.getContainingLocation() != null) {
            wormhole.getContainingLocation().removeEntity(wormhole);
        }
    }

    public static void started(SectorAPI sector, MarketAPI market) {
        market.reapplyIndustries();
        PhaseDisplacementIntel.started(sector, market);
    }
}
