package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import exiledsector.charts.loot.ChartVanillaLoot;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class FakeChartContentEngine implements ChartContentEngine {

    public record Assignment(FleetAssignment assignment, SectorEntityToken target) {
    }

    public record ColonyItem(SectorEntityToken holder, String itemId) {
    }

    public final List<Vector2f> spotPositions = new ArrayList<>();
    public final List<ChartSpot> focusedSpots = new ArrayList<>();
    public final List<SectorEntityToken> salvageEntities = new ArrayList<>();
    public final List<String> salvageTypes = new ArrayList<>();
    public final List<Boolean> derelictRecoverable = new ArrayList<>();
    public final List<FleetParamsV3> fleetParams = new ArrayList<>();
    public final List<CampaignFleetAPI> fleets = new ArrayList<>();
    public final Map<CampaignFleetAPI, List<Assignment>> assignments = new HashMap<>();
    public final Map<SectorEntityToken, Set<String>> tags = new HashMap<>();
    public final Map<String, ChartVanillaLoot.SiteDrops> siteDropsBySpec = new HashMap<>();
    public final Map<PlanetAPI, String> planetCategories = new HashMap<>();
    public final List<ColonyItem> colonyItems = new ArrayList<>();
    public final List<String> stationVariants = new ArrayList<>();
    public final List<CampaignFleetAPI> stations = new ArrayList<>();
    public final List<String> dressedFactions = new ArrayList<>();
    public String alliesFactionId = "allies";
    boolean missingStations;
    boolean failFleets;
    boolean emptyFleets;
    int spotCalls;

    public static FakeChartContentEngine install(FakeChartContentEngine engine) {
        ChartContentGenerator.useEngineForTests(engine);
        return engine;
    }

    public static FakeChartContentEngine offeringNothing() {
        return new FakeChartContentEngine();
    }

    public FakeChartContentEngine offering(float... coordinates) {
        for (int i = 0; i + 1 < coordinates.length; i += 2) {
            spotPositions.add(new Vector2f(coordinates[i], coordinates[i + 1]));
        }
        return this;
    }

    public FakeChartContentEngine offeringAt(SectorEntityToken focus, float x, float y) {
        focusedSpots.add(new ChartSpot(null, new Vector2f(x, y), focus, 1f));
        return this;
    }

    public static MemoryAPI memory() {
        Map<String, Object> stored = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        doAnswer(invocation -> stored.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any(), anyFloat());
        doAnswer(invocation -> stored.remove((String) invocation.getArgument(0))).when(memory).unset(anyString());
        when(memory.get(anyString())).thenAnswer(invocation -> stored.get((String) invocation.getArgument(0)));
        when(memory.contains(anyString())).thenAnswer(invocation -> stored.containsKey((String) invocation.getArgument(0)));
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(stored.get((String) invocation.getArgument(0))));
        when(memory.getFloat(anyString())).thenAnswer(invocation -> stored.get((String) invocation.getArgument(0)) instanceof Number number
                ? number.floatValue() : 0f);
        when(memory.getString(anyString())).thenAnswer(invocation -> {
            Object value = stored.get((String) invocation.getArgument(0));
            return value == null ? null : value.toString();
        });
        when(memory.getKeys()).thenAnswer(invocation -> new ArrayList<>(stored.keySet()));
        return memory;
    }

    public <T extends SectorEntityToken> T tagged(T entity) {
        Set<String> entityTags = tags.computeIfAbsent(entity, key -> new LinkedHashSet<>());
        doAnswer(invocation -> entityTags.add(invocation.getArgument(0))).when(entity).addTag(anyString());
        doAnswer(invocation -> entityTags.remove((String) invocation.getArgument(0))).when(entity).removeTag(anyString());
        when(entity.hasTag(anyString())).thenAnswer(invocation -> entityTags.contains((String) invocation.getArgument(0)));
        when(entity.getTags()).thenAnswer(invocation -> new ArrayList<>(entityTags));
        return entity;
    }

    @Override
    public List<ChartSpot> spots(StarSystemAPI system, Random random, Set<SectorEntityToken> excludedFoci, float gap) {
        spotCalls++;
        List<ChartSpot> spots = new ArrayList<>();
        for (Vector2f position : spotPositions) {
            spots.add(new ChartSpot(null, new Vector2f(position), null, 1f));
        }
        for (ChartSpot focused : focusedSpots) {
            if (excludedFoci == null || !excludedFoci.contains(focused.focus())) {
                spots.add(new ChartSpot(null, new Vector2f(focused.position()), focused.focus(), focused.weight()));
            }
        }
        return spots;
    }

    @Override
    public SectorEntityToken addSalvageEntity(StarSystemAPI system, String entityType, String salvageSpec, Random random) {
        return newEntity(entityType);
    }

    @Override
    public SectorEntityToken addDerelict(StarSystemAPI system, String factionId, String derelictType, boolean recoverable, Random random) {
        derelictRecoverable.add(recoverable);
        return newEntity("wreck");
    }

    private SectorEntityToken newEntity(String entityType) {
        CustomCampaignEntityAPI entity = tagged(mock(CustomCampaignEntityAPI.class));
        String id = "poi-" + salvageEntities.size();
        Vector2f location = new Vector2f();
        when(entity.getId()).thenReturn(id);
        when(entity.getCustomEntityType()).thenReturn(entityType);
        when(entity.getLocation()).thenReturn(location);
        MemoryAPI memory = memory();
        when(entity.getMemoryWithoutUpdate()).thenReturn(memory);
        salvageEntities.add(entity);
        salvageTypes.add(entityType);
        return entity;
    }

    @Override
    public void place(SectorEntityToken entity, ChartSpot spot) {
        entity.getLocation().set(spot.position());
    }

    @Override
    public ChartVanillaLoot.SiteDrops siteDrops(String salvageSpec) {
        return siteDropsBySpec.getOrDefault(salvageSpec, ChartVanillaLoot.SiteDrops.NONE);
    }

    @Override
    public String planetCategory(PlanetAPI planet) {
        return planetCategories.getOrDefault(planet, "");
    }

    @Override
    public String dressAllies(String factionId) {
        dressedFactions.add(factionId);
        return alliesFactionId;
    }

    @Override
    public void addColonyItem(SectorEntityToken holder, String itemId) {
        colonyItems.add(new ColonyItem(holder, itemId));
    }

    @Override
    public CampaignFleetAPI createFleet(FleetParamsV3 params) {
        fleetParams.add(params);
        if (failFleets) {
            throw new IllegalStateException("vanilla fleet factory changed");
        }
        CampaignFleetAPI fleet = newFleet("fleet-" + fleets.size());
        fleets.add(fleet);
        return fleet;
    }

    @Override
    public CampaignFleetAPI createStation(String factionId, String variantId, int officerLevel, Random random) {
        stationVariants.add(variantId);
        if (missingStations) {
            return null;
        }
        CampaignFleetAPI station = newFleet("station-" + stations.size());
        stations.add(station);
        return station;
    }

    private CampaignFleetAPI newFleet(String id) {
        CampaignFleetAPI fleet = tagged(mock(CampaignFleetAPI.class));
        when(fleet.getId()).thenReturn(id);
        when(fleet.isEmpty()).thenReturn(emptyFleets);
        when(fleet.hasAbility(anyString())).thenReturn(true);
        MemoryAPI memory = memory();
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        Vector2f location = new Vector2f();
        when(fleet.getLocation()).thenReturn(location);
        doAnswer(invocation -> {
            location.set(invocation.<Float>getArgument(0), invocation.<Float>getArgument(1));
            return null;
        }).when(fleet).setLocation(anyFloat(), anyFloat());
        doAnswer(invocation -> {
            location.set(invocation.<Float>getArgument(0), invocation.<Float>getArgument(1));
            return null;
        }).when(fleet).setFixedLocation(anyFloat(), anyFloat());
        List<Assignment> fleetAssignments = new ArrayList<>();
        assignments.put(fleet, fleetAssignments);
        doAnswer(invocation -> fleetAssignments.add(new Assignment(invocation.getArgument(0), invocation.getArgument(1))))
                .when(fleet).addAssignment(any(FleetAssignment.class), any(), anyFloat());
        return fleet;
    }
}
