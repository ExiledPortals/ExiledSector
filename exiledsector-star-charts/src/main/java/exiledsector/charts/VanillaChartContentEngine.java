package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictType;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageEntityGeneratorOld;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.ShipRecoverySpecialCreator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.SpecialCreationContext;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
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
    static final float STATION_DETECTION_BONUS = 1000f;

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

    @Override
    public CampaignFleetAPI createStation(String factionId, String variantId, int officerLevel, Random random) {
        ShipVariantAPI variant = Global.getSettings().doesVariantExist(variantId) ? Global.getSettings().getVariant(variantId) : null;
        if (variant == null) {
            return null;
        }
        CampaignFleetAPI station = FleetFactoryV3.createEmptyFleet(factionId, FleetTypes.BATTLESTATION, null);
        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        station.getFleetData().addFleetMember(member);
        station.setStationMode(true);
        RemnantThemeGenerator.addRemnantStationInteractionConfig(station);
        station.clearAbilities();
        station.addAbility(Abilities.TRANSPONDER);
        station.getAbility(Abilities.TRANSPONDER).activate();
        station.getDetectedRangeMod().modifyFlat(DETECTION_MOD_ID, STATION_DETECTION_BONUS);
        PersonAPI commander = stationCommander(station.getFaction(), member, officerLevel, random);
        station.setCommander(commander);
        member.setCaptain(commander);
        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        return station;
    }

    private static PersonAPI stationCommander(FactionAPI faction, FleetMemberAPI station, int officerLevel, Random random) {
        if (Misc.isAutomated(station)) {
            AICoreOfficerPlugin core = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE);
            return core.createPerson(Commodities.ALPHA_CORE, faction.getId(), random);
        }
        return OfficerManagerEvent.createOfficer(faction, officerLevel, SkillPickPreference.ANY, false, null, true, true, -1, random);
    }

    @Override
    public ChartVanillaLoot.SiteDrops siteDrops(String salvageSpec) {
        SalvageEntityGenDataSpec spec = SalvageEntityGeneratorOld.getSalvageSpec(salvageSpec);
        return spec == null ? ChartVanillaLoot.SiteDrops.NONE : new ChartVanillaLoot.SiteDrops(spec.getDropRandom(), spec.getDropValue());
    }

    @Override
    public void addColonyItem(SectorEntityToken holder, String itemId) {
        CargoAPI colonyItem = Global.getFactory().createCargo(true);
        colonyItem.addSpecial(new SpecialItemData(itemId, null), 1f);
        BaseSalvageSpecial.addExtraSalvage(holder, colonyItem);
    }

    @Override
    public String planetCategory(PlanetAPI planet) {
        Object spec = planet == null ? null : Global.getSettings().getSpec(PlanetGenDataSpec.class, planet.getTypeId(), true);
        return spec instanceof PlanetGenDataSpec data ? data.getCategory() : "";
    }
}
