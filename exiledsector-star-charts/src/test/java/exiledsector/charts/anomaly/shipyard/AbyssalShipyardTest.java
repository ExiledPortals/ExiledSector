package exiledsector.charts.anomaly.shipyard;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.layout.abyss.ChartPhotospherePlugin;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.floatThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbyssalShipyardTest {

    private static final String RUN_ID = "run1";
    private static final float CORE_RADIUS = 600f;

    private MockSector mockSector;
    private StarSystemAPI system;
    private CustomCampaignEntityAPI photosphere;
    private final List<SectorEntityToken> added = new ArrayList<>();
    private final List<String> addedTypes = new ArrayList<>();
    private final List<String> addedFactions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        system = mock(StarSystemAPI.class);
        photosphere = mock(CustomCampaignEntityAPI.class);
        when(photosphere.getCustomEntityType()).thenReturn(ChartPhotospherePlugin.ENTITY_TYPE);
        when(photosphere.getLocation()).thenReturn(new Vector2f(14000f, 0f));
        when(photosphere.getRadius()).thenReturn(CORE_RADIUS);
        when(system.getCustomEntities()).thenReturn(List.of(photosphere));
        when(system.getId()).thenReturn("charted space");
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(RUN_ID));
        when(system.addCustomEntity(anyString(), any(), anyString(), anyString())).thenAnswer(invocation -> {
            SectorEntityToken entity = entity();
            added.add(entity);
            addedTypes.add(invocation.getArgument(2));
            addedFactions.add(invocation.getArgument(3));
            return entity;
        });
    }

    @AfterEach
    void tearDown() {
        ChartTables.resetAnomalies();
        ChartContentGenerator.resetForTests();
        mockSector.close();
    }

    private SectorEntityToken entity() {
        Set<String> tags = new HashSet<>();
        CustomCampaignEntityAPI entity = mock(CustomCampaignEntityAPI.class);
        doAnswer(invocation -> tags.add(invocation.getArgument(0))).when(entity).addTag(anyString());
        when(entity.hasTag(anyString())).thenAnswer(invocation -> tags.contains((String) invocation.getArgument(0)));
        MemoryAPI memory = FakeChartContentEngine.memory();
        when(entity.getMemoryWithoutUpdate()).thenReturn(memory);
        when(entity.getContainingLocation()).thenReturn(system);
        return entity;
    }

    private AnomalyContentSite site() {
        return new AnomalyContentSite(system, RUN_ID, 4, "rat_abyssals", ChartGenContext.none(), List.of(), null, spot -> true, salt -> new Random(salt));
    }

    private static ChartRun run() {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(4, "rat_abyssals", null, 3, 2, List.of(AbyssalShipyard.ANOMALY_ID)));
        return run;
    }

    @Test
    void theShippedRowIsAnAbyssalRiftLandmarkThatNeverRollsOrInserts() throws Exception {
        ChartTables.loadAnomalies();

        assertSame(AbyssalShipyard.INSTANCE, ChartAnomalies.byId(AbyssalShipyard.ANOMALY_ID));
        ChartAnomalies.Definition definition = ChartAnomalies.definition(AbyssalShipyard.ANOMALY_ID);
        assertEquals(Set.of("abyssal_rift"), definition.biomes());
        assertFalse(definition.insertable());
        assertFalse(definition.icon().isEmpty());
    }

    @Test
    void theShipyardOrbitsAPhotosphereAsAGuardedNeutralEntity() {
        AbyssalShipyard.INSTANCE.generate(site());

        assertEquals(1, added.size());
        assertEquals(AbyssalShipyard.ENTITY_TYPE, addedTypes.get(0));
        assertEquals(Factions.NEUTRAL, addedFactions.get(0));
        SectorEntityToken shipyard = added.get(0);
        assertTrue(shipyard.hasTag(ChartIds.runTag(RUN_ID)));
        float minRadius = CORE_RADIUS + AbyssalShipyard.MIN_ORBIT_GAP - 0.5f;
        float maxRadius = CORE_RADIUS + AbyssalShipyard.MAX_ORBIT_GAP + 0.5f;
        verify(shipyard).setCircularOrbitPointingDown(eq(photosphere), anyFloat(), floatThat(radius -> radius >= minRadius && radius <= maxRadius),
                anyFloat());
        assertTrue(VanillaDefenders.guarded(shipyard));
        assertTrue(shipyard.getMemoryWithoutUpdate().get(MemFlags.SALVAGE_SEED) instanceof Long);
    }

    @Test
    void theDialogTakesOverTheShipyardDuringItsRunAndTheIntelFollowsItsState() {
        AbyssalShipyard.INSTANCE.generate(site());
        SectorEntityToken shipyard = added.get(0);
        when(system.getEntitiesWithTag(AbyssalShipyard.SHIPYARD_TAG)).thenReturn(List.of(shipyard));

        assertNull(ChartAnomalies.dialogFor(shipyard));
        ChartRuns.store(run());
        AnomalyRun anomalyRun = new AnomalyRun(ChartRuns.currentOrNull(), system);

        assertInstanceOf(ShipyardDialog.class, ChartAnomalies.dialogFor(shipyard));
        String guarded = AbyssalShipyard.INSTANCE.intelText(anomalyRun);
        shipyard.getMemoryWithoutUpdate().set(VanillaDefenders.DEFEATED_KEY, true);
        String open = AbyssalShipyard.INSTANCE.intelText(anomalyRun);
        shipyard.getMemoryWithoutUpdate().set(AbyssalShipyard.SALVAGED_KEY, true);

        assertNotNull(guarded);
        assertNotNull(open);
        assertNotEquals(guarded, open);
        assertNull(AbyssalShipyard.INSTANCE.intelText(anomalyRun));
    }

    @Test
    void theGuardsGrowFromThreeEscortsToTheFullLineupWithTier() {
        assertEquals(ShipyardGuards.ESCORTS_AT_LOWEST_TIER, ShipyardGuards.escortCount(ChartTiers.LOWEST_TIER));
        assertEquals(ShipyardGuards.ESCORTS.size(), ShipyardGuards.escortCount(ChartTiers.HIGHEST_TIER));
        for (int tier = ChartTiers.LOWEST_TIER; tier < ChartTiers.HIGHEST_TIER; tier++) {
            assertTrue(ShipyardGuards.escortCount(tier + 1) >= ShipyardGuards.escortCount(tier));
        }
    }

    @Test
    void theGuardsAreAChronosCoredMorkothAndItsEscorts() {
        FakeChartContentEngine engine = FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());

        CampaignFleetAPI guards = ShipyardGuards.create(1, ChartGenContext.none(), 7L);

        assertNotNull(guards);
        assertEquals(1, engine.flagships.size());
        assertEquals(ShipyardGuards.FLAGSHIP_VARIANT, engine.flagships.get(0).variantId());
        assertEquals(ShipyardGuards.CHRONOS_CORE, engine.flagships.get(0).aiCoreId());
        assertEquals(ShipyardGuards.escortCount(1), engine.coreShips.size());
        assertEquals(ShipyardGuards.ESCORTS.get(0).variantId(), engine.coreShips.get(0).variantId());
    }
}
