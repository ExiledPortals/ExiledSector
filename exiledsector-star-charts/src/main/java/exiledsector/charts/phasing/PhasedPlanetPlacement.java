package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

public final class PhasedPlanetPlacement {

    public static final String PHASED_TAG = "exiledSector_phased_planet";
    public static final String ORIGIN_MEMORY_KEY = "$exiledSector_phasedOrigin";
    public static final String REGION_MEMORY_KEY = "$exiledSector_phasedRegion";
    public static final String TIER_MEMORY_KEY = "$exiledSector_phasedTier";
    static final String PLANET_ID_PREFIX = "exiledSector_phased_";
    static final String RUINS_EXPLORED_MEMORY_KEY = "$ruinsExplored";

    private PhasedPlanetPlacement() {
    }

    public static PlanetAPI place(SectorAPI sector, StarSystemAPI system, SectorEntityToken focus, PhasedPlanetRecord record,
                                  float orbitRadius, float angle) {
        PlanetAPI planet = system.addPlanet(uniquePlanetId(sector, record), focus, record.name(), record.typeId(), angle, record.radius(),
                orbitRadius, PhaseDestination.orbitDays(orbitRadius));
        applyTexture(planet, record.texture());
        Misc.initConditionMarket(planet);
        MarketAPI market = planet.getMarket();
        for (String conditionId : conditionsFor(record)) {
            market.addCondition(conditionId);
        }
        market.setSurveyLevel(SurveyLevel.FULL);
        for (MarketConditionAPI condition : market.getConditions()) {
            condition.setSurveyed(true);
        }
        if (Misc.hasRuins(market)) {
            market.getMemoryWithoutUpdate().set(RUINS_EXPLORED_MEMORY_KEY, true);
        }
        market.reapplyConditions();
        planet.addTag(PHASED_TAG);
        MemoryAPI memory = planet.getMemoryWithoutUpdate();
        setIfPresent(memory, ORIGIN_MEMORY_KEY, record.originName());
        setIfPresent(memory, REGION_MEMORY_KEY, record.regionId());
        memory.set(TIER_MEMORY_KEY, record.tier());
        return planet;
    }

    static List<String> conditionsFor(PhasedPlanetRecord record) {
        List<String> conditions = new ArrayList<>();
        for (String conditionId : record.conditionIds()) {
            addKnown(conditions, conditionId);
        }
        addKnown(conditions, PhasedPlanetTraits.PHASE_DISPLACED_CONDITION_ID);
        for (String conditionId : record.traitConditionIds()) {
            addKnown(conditions, conditionId);
        }
        return conditions;
    }

    private static void addKnown(List<String> conditions, String conditionId) {
        if (conditionId != null && !conditions.contains(conditionId) && Global.getSettings().getMarketConditionSpec(conditionId) != null) {
            conditions.add(conditionId);
        }
    }

    static String uniquePlanetId(SectorAPI sector, PhasedPlanetRecord record) {
        String base = PLANET_ID_PREFIX + record.runId();
        String candidate = base;
        int suffix = 2;
        while (sector.getEntityById(candidate) != null) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private static void applyTexture(PlanetAPI planet, String texture) {
        if (texture == null || planet.getSpec() == null || texture.equals(planet.getSpec().getTexture())) {
            return;
        }
        planet.getSpec().setTexture(texture);
        planet.applySpecChanges();
    }

    private static void setIfPresent(MemoryAPI memory, String key, String value) {
        if (value != null) {
            memory.set(key, value);
        }
    }
}
