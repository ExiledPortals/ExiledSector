package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.misc.HypershuntIntel;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.ChartIds;
import exiledsector.charts.phasing.HypershuntPower;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class FoundColonyStep implements HarnessStep {

    static final String COLONY_DATA = "colony";
    static final String MARKET_ID_KEY = "marketId";
    static final String SUPPLY_SOURCE = "exiledSector_harness_supply";
    static final String HYPERSHUNT_USABLE_KEY = "$usable";
    static final String FACTION_SET_UP_KEY = "$shownFactionConfigDialog";
    static final int MIN_SIZE = 3;
    static final int MAX_SIZE = 10;
    static final float SUPPLY_DAYS = 3650f;
    private static final Set<String> HOSTILE_THEME_TAGS = Set.of(Tags.THEME_REMNANT, Tags.THEME_REMNANT_MAIN, Tags.THEME_REMNANT_SECONDARY);

    private final int size;
    private final List<String> industryIds;
    private final Map<String, Integer> supply;
    private final boolean nearHypershunt;

    FoundColonyStep(int size, List<String> industryIds, Map<String, Integer> supply, boolean nearHypershunt) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException("foundColony needs a size from " + MIN_SIZE + " to " + MAX_SIZE + ", not " + size);
        }
        this.size = size;
        this.industryIds = List.copyOf(industryIds);
        this.supply = Map.copyOf(supply);
        this.nearHypershunt = nearHypershunt;
    }

    @Override
    public String name() {
        return "foundColony size " + size + (nearHypershunt ? " near a hypershunt" : "");
    }

    @Override
    public StepOutcome advance(StepContext context) throws JSONException {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("foundColony");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        for (String industryId : industryIds) {
            if (industrySpec(industryId) == null) {
                return StepOutcome.failed("no industry \"" + industryId + "\"");
            }
        }
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        SectorEntityToken hypershunt = nearHypershunt ? nearestHypershunt(sector, playerFleet.getLocationInHyperspace()) : null;
        if (nearHypershunt && hypershunt == null) {
            return StepOutcome.failed("the sector has no coronal hypershunt");
        }
        Vector2f anchor = hypershunt == null ? playerFleet.getLocationInHyperspace() : hypershunt.getLocationInHyperspace();
        float reachLightYears = hypershunt == null ? Float.MAX_VALUE : HypershuntPower.rangeLightYears();
        PlanetAPI planet = colonySite(sector, anchor, reachLightYears);
        if (planet == null) {
            return StepOutcome.failed("found no uncolonised planet" + (hypershunt == null ? "" : " within " + reachLightYears + " ly of " + hypershunt.getName()));
        }
        if (hypershunt != null) {
            makeUsable(hypershunt);
        }
        sector.getMemoryWithoutUpdate().set(FACTION_SET_UP_KEY, true);
        MarketAPI market = found(sector, planet);
        for (String industryId : industryIds) {
            if (!market.hasIndustry(industryId)) {
                market.addIndustry(industryId);
            }
        }
        supply.forEach((commodityId, amount) -> market.getCommodityData(commodityId).getAvailableStat()
                .addTemporaryModFlat(SUPPLY_DAYS, SUPPLY_SOURCE, amount));
        market.reapplyIndustries();
        HarnessCampaign.movePlayerFleetBeside(sector, planet);
        float distanceLightYears = Misc.getDistanceLY(planet.getLocationInHyperspace(), anchor);
        context.results().addData(COLONY_DATA, colonyData(planet, market, hypershunt, distanceLightYears));
        return StepOutcome.passed("founded a size " + size + " colony on " + planet.getName() + " in " + planet.getContainingLocation().getName()
                + (hypershunt == null ? "" : ", " + Math.round(distanceLightYears * 10f) / 10f + " ly from " + hypershunt.getName())
                + ", with " + market.getIndustries().size() + " industries");
    }

    private static IndustrySpecAPI industrySpec(String industryId) {
        for (IndustrySpecAPI spec : Global.getSettings().getAllIndustrySpecs()) {
            if (spec.getId().equals(industryId)) {
                return spec;
            }
        }
        return null;
    }

    static SectorEntityToken nearestHypershunt(SectorAPI sector, Vector2f from) {
        SectorEntityToken nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (SectorEntityToken hypershunt : sector.getCustomEntitiesWithTag(Tags.CORONAL_TAP)) {
            float distance = Misc.getDistanceLY(from, hypershunt.getLocationInHyperspace());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = hypershunt;
            }
        }
        return nearest;
    }

    private static void makeUsable(SectorEntityToken hypershunt) {
        hypershunt.getMemoryWithoutUpdate().set(HYPERSHUNT_USABLE_KEY, true);
        if (HypershuntIntel.getHypershuntIntel(hypershunt) == null) {
            new HypershuntIntel(hypershunt, null);
        }
    }

    static PlanetAPI colonySite(SectorAPI sector, Vector2f anchor, float reachLightYears) {
        PlanetAPI nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (StarSystemAPI system : sector.getStarSystems()) {
            if (system.getStar() == null || system.hasTag(ChartIds.SYSTEM_TAG) || hasHostileTheme(system)) {
                continue;
            }
            float distance = Misc.getDistanceLY(system.getLocation(), anchor);
            if (distance > reachLightYears || distance >= nearestDistance) {
                continue;
            }
            for (PlanetAPI planet : system.getPlanets()) {
                if (isColonySite(planet)) {
                    nearest = planet;
                    nearestDistance = distance;
                    break;
                }
            }
        }
        return nearest;
    }

    private static boolean hasHostileTheme(StarSystemAPI system) {
        for (String tag : HOSTILE_THEME_TAGS) {
            if (system.hasTag(tag)) {
                return true;
            }
        }
        return false;
    }

    static boolean isColonySite(PlanetAPI planet) {
        MarketAPI market = planet.getMarket();
        return !planet.isStar() && market != null && market.isPlanetConditionMarketOnly() && !planet.hasTag(Tags.NON_CLICKABLE)
                && !planet.hasTag(ChartIds.CONTENT_TAG);
    }

    private MarketAPI found(SectorAPI sector, PlanetAPI planet) {
        MarketAPI market = planet.getMarket();
        market.setSize(size);
        market.addCondition("population_" + size);
        market.setFactionId(Factions.PLAYER);
        market.setPlanetConditionMarketOnly(false);
        if (market.hasCondition(Conditions.DECIVILIZED)) {
            market.removeCondition(Conditions.DECIVILIZED);
            market.addCondition(Conditions.DECIVILIZED_SUBPOP);
        }
        market.addIndustry(Industries.POPULATION);
        market.addSubmarket(Submarkets.LOCAL_RESOURCES);
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        Misc.setFullySurveyed(market, null, false);
        sector.getEconomy().addMarket(market, true);
        planet.setFaction(Factions.PLAYER);
        market.setPlayerOwned(true);
        market.addIndustry(Industries.SPACEPORT);
        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
        if (storage != null && storage.getPlugin() instanceof StoragePlugin storagePlugin) {
            storagePlugin.setPlayerPaidToUnlock(true);
        }
        return market;
    }

    private static JSONObject colonyData(PlanetAPI planet, MarketAPI market, SectorEntityToken hypershunt, float distanceLightYears)
            throws JSONException {
        JSONObject colony = new JSONObject();
        colony.put(MARKET_ID_KEY, market.getId());
        colony.put("planet", planet.getName());
        colony.put("system", planet.getContainingLocation().getName());
        colony.put("size", market.getSize());
        JSONArray industries = new JSONArray();
        market.getIndustries().forEach(industry -> industries.put(industry.getId()));
        colony.put("industries", industries);
        JSONArray conditions = new JSONArray();
        for (MarketConditionAPI condition : market.getConditions()) {
            conditions.put(condition.getId());
        }
        colony.put("conditions", conditions);
        if (hypershunt != null) {
            colony.put("hypershunt", hypershunt.getName() + " in " + hypershunt.getContainingLocation().getName());
            colony.put("hypershuntDistanceLy", distanceLightYears);
        }
        return colony;
    }
}
