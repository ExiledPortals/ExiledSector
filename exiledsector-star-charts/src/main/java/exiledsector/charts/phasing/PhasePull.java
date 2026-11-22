package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import exiledsector.Seeds;
import exiledsector.charts.StarChartSettings;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import org.lwjgl.util.vector.Vector2f;

public final class PhasePull {

    public static final float PULL_DAYS = 120f;
    public static final int TRANSPLUTONICS_DEMAND = 10;
    public static final int VOLATILES_DEMAND = 8;
    public static final String GATE_TYPE = "exiledSector_phase_gate";
    public static final String GATE_ID_PREFIX = "exiledSector_phase_gate_";
    static final String LEGACY_WORMHOLE_ID_PREFIX = "exiledSector_phase_wormhole_";
    static final String GATE_MARKET_KEY = "$exiledSector_phaseGateMarket";
    static final float CHECK_DAYS = 1f;
    static final String ANGLE_SEED_PREFIX = "phaseAngle|";

    public enum Pause {
        DOMAIN_OFF, NOT_PLAYER_OWNED, NOT_FUNCTIONAL, NO_GATEWAY, NO_HYPERSHUNT, SHORTAGE
    }

    private PhasePull() {
    }

    public static Pause pauseFor(PhaseDisplacementGate gate) {
        MarketAPI market = gate.getMarket();
        if (!StarChartSettings.enabled()) {
            return Pause.DOMAIN_OFF;
        }
        if (market == null || !PhaseDisplacementGate.isPlayers(market)) {
            return Pause.NOT_PLAYER_OWNED;
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

    public static Pause pauseFor(MarketAPI market) {
        Industry industry = market == null ? null : market.getIndustry(PhaseDisplacementGate.INDUSTRY_ID);
        return industry instanceof PhaseDisplacementGate gate ? pauseFor(gate) : Pause.NOT_FUNCTIONAL;
    }

    public static String pauseKey(Pause pause) {
        return switch (pause) {
            case DOMAIN_OFF -> "chart.phasing.pull.paused.domainOff";
            case NOT_PLAYER_OWNED -> "chart.phasing.pull.paused.notPlayerOwned";
            case NOT_FUNCTIONAL -> "chart.phasing.pull.paused.notFunctional";
            case NO_GATEWAY -> "chart.phasing.pull.paused.gateway";
            case NO_HYPERSHUNT -> "chart.phasing.pull.paused.hypershunt";
            case SHORTAGE -> "chart.phasing.pull.paused.shortage";
        };
    }

    public static int daysLeft(MarketAPI market) {
        return (int) Math.ceil(Math.max(0f, PULL_DAYS - PhaseGateState.jobDays(market)));
    }

    public static String statusLine(MarketAPI market) {
        PhasedPlanetRecord job = PhaseGateState.job(market);
        if (job == null) {
            return null;
        }
        Pause pause = pauseFor(market);
        if (pause != null) {
            return I18n.forGameText(() -> Translation.msg("chart.phasing.tooltip.paused").arg("planet", job.name())
                    .arg("reason", Translation.gameText(pauseKey(pause))).text());
        }
        int days = daysLeft(market);
        return I18n.forGameText(() -> Translation.msg("chart.phasing.tooltip.pulling").arg("planet", job.name()).arg("days", days).text());
    }

    public static PlanetAPI advance(SectorAPI sector, PhaseDisplacementGate gate, float days) {
        MarketAPI market = gate.getMarket();
        CustomCampaignEntityAPI phaseGate = placeGate(sector, market);
        if (phaseGate == null || pauseFor(gate) != null) {
            return null;
        }
        float progress = PhaseGateState.jobDays(market) + days;
        if (progress < PULL_DAYS) {
            PhaseGateState.setJobDays(market, progress);
            showProgress(phaseGate, progress);
            return null;
        }
        PhaseGateState.setJobDays(market, PULL_DAYS);
        showProgress(phaseGate, PULL_DAYS);
        if (phaseGate.getCustomPlugin() instanceof PhaseGateEntityPlugin plugin && phaseGate.isInCurrentLocation()) {
            if (!plugin.isArriving()) {
                plugin.beginArrival();
            }
            return null;
        }
        PlanetAPI planet = arrive(sector, market, phaseGate);
        removeEntity(phaseGate);
        return planet;
    }

    public static PlanetAPI finishArrival(SectorAPI sector, SectorEntityToken phaseGate) {
        MarketAPI market = gateMarketOf(phaseGate);
        return market == null || PhaseGateState.job(market) == null ? null : arrive(sector, market, phaseGate);
    }

    static CustomCampaignEntityAPI placeGate(SectorAPI sector, MarketAPI market) {
        PhasedPlanetRecord job = PhaseGateState.job(market);
        StarSystemAPI system = market == null ? null : market.getStarSystem();
        SectorEntityToken focus = system == null ? null : PhaseDestination.focusOf(system);
        if (job == null || focus == null) {
            return null;
        }
        removeLegacyWormhole(sector, market);
        return ensureGate(sector, market, system, focus, job, orbitFor(market, system, focus, job));
    }

    static float orbitFor(MarketAPI market, StarSystemAPI system, SectorEntityToken focus, PhasedPlanetRecord job) {
        float orbitRadius = PhaseGateState.jobOrbit(market);
        if (orbitRadius <= 0f) {
            float clearance = PhaseGateEntityPlugin.frameRadiusFor(job.radius());
            orbitRadius = PhaseDestination.orbitRadiusNear(system, focus, market.getPrimaryEntity(), clearance);
            PhaseGateState.setJobOrbit(market, orbitRadius);
        }
        return orbitRadius;
    }

    public static String gateId(MarketAPI market) {
        return GATE_ID_PREFIX + market.getId();
    }

    public static SectorEntityToken gateOf(SectorAPI sector, MarketAPI market) {
        return sector == null || market == null ? null : sector.getEntityById(gateId(market));
    }

    public static MarketAPI gateMarketOf(SectorEntityToken phaseGate) {
        String marketId = phaseGate == null ? null : phaseGate.getMemoryWithoutUpdate().getString(GATE_MARKET_KEY);
        return marketId == null ? null : PhaseDisplacementGate.marketById(marketId);
    }

    public static boolean isPhaseGate(SectorEntityToken entity) {
        return entity != null && GATE_TYPE.equals(entity.getCustomEntityType());
    }

    static CustomCampaignEntityAPI ensureGate(SectorAPI sector, MarketAPI market, StarSystemAPI system, SectorEntityToken focus,
                                              PhasedPlanetRecord job, float orbitRadius) {
        if (gateOf(sector, market) instanceof CustomCampaignEntityAPI existing) {
            configureGate(existing, job);
            return existing;
        }
        CustomCampaignEntityAPI phaseGate = system.addCustomEntity(gateId(market), Translation.gameText("chart.phasing.gate.name"),
                GATE_TYPE, Factions.NEUTRAL);
        phaseGate.setCircularOrbit(focus, startAngle(job), orbitRadius, PhaseDestination.orbitDays(orbitRadius));
        phaseGate.getMemoryWithoutUpdate().set(GATE_MARKET_KEY, market.getId());
        configureGate(phaseGate, job);
        showProgress(phaseGate, PhaseGateState.jobDays(market));
        return phaseGate;
    }

    static void configureGate(CustomCampaignEntityAPI phaseGate, PhasedPlanetRecord job) {
        float radius = PhaseGateEntityPlugin.frameRadiusFor(job.radius());
        if (phaseGate.getRadius() != radius) {
            phaseGate.setRadius(radius);
        }
        if (phaseGate.getCustomPlugin() instanceof PhaseGateEntityPlugin plugin) {
            plugin.configure(textureFor(job), job.radius());
        }
    }

    static String textureFor(PhasedPlanetRecord job) {
        if (job.texture() != null) {
            return job.texture();
        }
        for (PlanetSpecAPI spec : Global.getSettings().getAllPlanetSpecs()) {
            if (spec.getPlanetType().equals(job.typeId())) {
                return spec.getTexture();
            }
        }
        return null;
    }

    static void showProgress(SectorEntityToken phaseGate, float progress) {
        if (phaseGate != null && phaseGate.getCustomPlugin() instanceof PhaseGateEntityPlugin plugin) {
            plugin.setProgressShare(progressShare(progress));
        }
    }

    static float progressShare(float progress) {
        return Math.max(0f, Math.min(1f, progress / PULL_DAYS));
    }

    static void removeLegacyWormhole(SectorAPI sector, MarketAPI market) {
        removeEntity(sector == null ? null : sector.getEntityById(LEGACY_WORMHOLE_ID_PREFIX + market.getId()));
    }

    private static void removeEntity(SectorEntityToken entity) {
        if (entity != null && entity.getContainingLocation() != null) {
            entity.getContainingLocation().removeEntity(entity);
        }
    }

    static float startAngle(PhasedPlanetRecord job) {
        return Seeds.random(ANGLE_SEED_PREFIX + job.runId() + "|" + job.planetId()).nextFloat() * 360f;
    }

    static PlanetAPI arrive(SectorAPI sector, MarketAPI market, SectorEntityToken phaseGate) {
        StarSystemAPI system = market.getStarSystem();
        SectorEntityToken focus = system == null ? null : PhaseDestination.focusOf(system);
        PhasedPlanetRecord job = PhaseGateState.job(market);
        if (focus == null || job == null) {
            return null;
        }
        float orbitRadius = orbitFor(market, system, focus, job);
        float angle = phaseGate == null || phaseGate.getLocation() == null || focus.getLocation() == null
                ? startAngle(job) : angleBetween(focus.getLocation(), phaseGate.getLocation());
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

    public static boolean cancel(SectorAPI sector, MarketAPI market) {
        if (!PhaseGateState.hasJobEntry(market)) {
            return false;
        }
        PhaseGateState.clearJob(market);
        SectorEntityToken phaseGate = gateOf(sector, market);
        if (phaseGate != null) {
            Misc.fadeAndExpire(phaseGate);
        }
        removeLegacyWormhole(sector, market);
        market.reapplyIndustries();
        return true;
    }

    public static void abandon(SectorAPI sector, MarketAPI market) {
        if (market == null) {
            return;
        }
        PhaseGateState.clearLock(market);
        PhaseGateState.clearJob(market);
        removeEntity(gateOf(sector, market));
        removeLegacyWormhole(sector, market);
    }

    public static void started(SectorAPI sector, MarketAPI market) {
        market.reapplyIndustries();
        placeGate(sector, market);
        PhaseDisplacementIntel.started(sector, market);
    }
}
