package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageEntityGeneratorOld;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.ShipRecoverySpecialCreator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.SpecialCreationContext;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class VanillaChartContentEngine implements ChartContentEngine {

    static final float PLANET_ORBIT_WEIGHT = 10f;
    static final float STAR_ORBIT_WEIGHT = 6f;
    static final float HIDDEN_WEIGHT = 5f;
    static final String DETECTION_MOD_ID = "gen";

    static LinkedHashMap<LocationType, Float> locationWeights() {
        LinkedHashMap<LocationType, Float> weights = new LinkedHashMap<>();
        weights.put(LocationType.PLANET_ORBIT, PLANET_ORBIT_WEIGHT);
        weights.put(LocationType.STAR_ORBIT, STAR_ORBIT_WEIGHT);
        weights.put(LocationType.GAS_GIANT_ORBIT, HIDDEN_WEIGHT);
        weights.put(LocationType.IN_ASTEROID_BELT, HIDDEN_WEIGHT);
        weights.put(LocationType.IN_ASTEROID_FIELD, HIDDEN_WEIGHT);
        weights.put(LocationType.IN_RING, HIDDEN_WEIGHT);
        weights.put(LocationType.IN_SMALL_NEBULA, HIDDEN_WEIGHT);
        weights.put(LocationType.L_POINT, HIDDEN_WEIGHT);
        weights.put(LocationType.OUTER_SYSTEM, HIDDEN_WEIGHT);
        return weights;
    }

    @Override
    public List<ChartSpot> spots(StarSystemAPI system, Random random, Set<SectorEntityToken> excludedFoci, float gap) {
        WeightedRandomPicker<EntityLocation> picker = BaseThemeGenerator.getLocations(random, system, excludedFoci, gap, locationWeights());
        List<ChartSpot> spots = new ArrayList<>();
        List<EntityLocation> locations = picker.getItems();
        for (int i = 0; i < locations.size(); i++) {
            EntityLocation location = locations.get(i);
            Vector2f position = location.orbit != null ? location.orbit.computeCurrentLocation() : location.location;
            SectorEntityToken focus = location.orbit != null ? location.orbit.getFocus() : null;
            if (position != null) {
                spots.add(new ChartSpot(location, new Vector2f(position), focus, picker.getWeight(i)));
            }
        }
        return spots;
    }

    @Override
    public SectorEntityToken addSalvageEntity(StarSystemAPI system, String entityType, String salvageSpec, Random random) {
        SectorEntityToken entity = BaseThemeGenerator.addSalvageEntity(random, system, entityType, Factions.NEUTRAL);
        if (salvageSpec == null || salvageSpec.equals(entityType)) {
            return entity;
        }
        try {
            SalvageEntityGenDataSpec spec = SalvageEntityGeneratorOld.getSalvageSpec(salvageSpec);
            entity.setDiscoveryXP(spec.getXpDiscover());
            entity.getDetectedRangeMod().modifyFlat(DETECTION_MOD_ID, spec.getDetectionRange());
            return entity;
        } catch (RuntimeException | LinkageError e) {
            system.removeEntity(entity);
            throw e;
        }
    }

    @Override
    public SectorEntityToken addDerelict(StarSystemAPI system, String factionId, String derelictType, boolean recoverable, Random random) {
        DerelictShipData shipData = DerelictShipEntityPlugin.createRandom(factionId, DerelictType.valueOf(derelictType), random,
                DerelictShipEntityPlugin.getDefaultSModProb());
        if (shipData == null || shipData.ship == null) {
            return null;
        }
        SectorEntityToken wreck = BaseThemeGenerator.addSalvageEntity(random, system, Entities.WRECK, Factions.NEUTRAL, shipData);
        if (wreck instanceof CustomCampaignEntityAPI customWreck) {
            customWreck.setDiscoverable(true);
        }
        if (recoverable) {
            try {
                Object recovery = new ShipRecoverySpecialCreator(random, 0, 0, false, null, null).createSpecial(wreck, new SpecialCreationContext());
                if (recovery != null) {
                    Misc.setSalvageSpecial(wreck, recovery);
                }
            } catch (RuntimeException | LinkageError e) {
                system.removeEntity(wreck);
                throw e;
            }
        }
        return wreck;
    }

    @Override
    public void place(SectorEntityToken entity, ChartSpot spot) {
        if (spot.vanillaLocation() instanceof EntityLocation location) {
            BaseThemeGenerator.setEntityLocation(entity, location, entity.getCustomEntityType());
        } else {
            entity.setFixedLocation(spot.position().x, spot.position().y);
        }
    }

    @Override
    public CampaignFleetAPI createFleet(FleetParamsV3 params) {
        return FleetFactoryV3.createFleet(params);
    }
}
