package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
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
import exiledsector.ui.socket.BenchMode;
import org.json.JSONArray;
import org.json.JSONObject;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbyssalResearchComplexTest {

    private static final String RUN_ID = "run1";

    private MockSector mockSector;
    private StarSystemAPI system;
    private final List<SectorEntityToken> added = new ArrayList<>();
    private final List<String> addedTypes = new ArrayList<>();
    private final List<String> addedFactions = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        system = mock(StarSystemAPI.class);
        CustomCampaignEntityAPI photosphere = mock(CustomCampaignEntityAPI.class);
        when(photosphere.getCustomEntityType()).thenReturn(ChartPhotospherePlugin.ENTITY_TYPE);
        when(photosphere.getLocation()).thenReturn(new Vector2f(14000f, 0f));
        when(photosphere.getRadius()).thenReturn(600f);
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
        ForgePrograms.register(new JSONArray().put(new JSONObject().put("id", AbyssalResearchComplex.INFUSE_PROGRAM).put("weight", "0")
                .put("min_tier", "1").put("item_types", "hull_item").put("feedstock", "500")));
    }

    @AfterEach
    void tearDown() {
        ForgePrograms.resetForTests();
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
        run.recordContent(new ChartContent(4, "rat_abyssals", null, 3, 2, List.of(AbyssalResearchComplex.ANOMALY_ID)));
        return run;
    }

    @Test
    void theShippedRowIsAnAbyssalRiftLandmarkThatNeverRollsOrInserts() throws Exception {
        ChartTables.loadAnomalies();

        assertSame(AbyssalResearchComplex.INSTANCE, ChartAnomalies.byId(AbyssalResearchComplex.ANOMALY_ID));
        ChartAnomalies.Definition definition = ChartAnomalies.definition(AbyssalResearchComplex.ANOMALY_ID);
        assertEquals(Set.of("abyssal_rift"), definition.biomes());
        assertFalse(definition.insertable());
        assertFalse(definition.icon().isEmpty());
    }

    @Test
    void theComplexIsAGuardedForgeSiteThatOffersOnlyTheInfusionOnce() {
        ChartRun run = run();
        AnomalyContentSite placed = site();
        AbyssalResearchComplex.INSTANCE.generate(placed);
        placed.commitStates(run);

        SectorEntityToken complex = added.get(0);
        assertEquals(AbyssalResearchComplex.ENTITY_TYPE, addedTypes.get(0));
        assertEquals(Factions.NEUTRAL, addedFactions.get(0));
        assertSame(ForgeSite.ABYSSAL_RESEARCH_COMPLEX, ForgeSite.of(complex));
        assertTrue(VanillaDefenders.guarded(complex));
        assertEquals(List.of(ForgePrograms.byId(AbyssalResearchComplex.INFUSE_PROGRAM)), ForgeStates.programs(run, ForgeSite.ABYSSAL_RESEARCH_COMPLEX));
        assertEquals(1, ForgeStates.runsLeft(run, ForgeSite.ABYSSAL_RESEARCH_COMPLEX));
        assertEquals(new ForgeRules.Cost(0, 0, 500),
                ChartForge.costFor(run, ForgeSite.ABYSSAL_RESEARCH_COMPLEX, ForgePrograms.byId(AbyssalResearchComplex.INFUSE_PROGRAM)));
        assertEquals(BenchMode.ABYSSAL_FORGE, ForgeSite.ABYSSAL_RESEARCH_COMPLEX.benchMode());
    }

    @Test
    void theDialogTakesOverTheComplexDuringItsRunAndTheIntelFollowsItsState() {
        ChartRuns.store(run());
        AnomalyContentSite placed = site();
        AbyssalResearchComplex.INSTANCE.generate(placed);
        placed.commitStates(ChartRuns.currentOrNull());
        SectorEntityToken complex = added.get(0);
        when(system.getEntitiesWithTag(AbyssalResearchComplex.COMPLEX_TAG)).thenReturn(List.of(complex));
        AnomalyRun anomalyRun = new AnomalyRun(ChartRuns.currentOrNull(), system);

        assertInstanceOf(ForgeDialog.class, ChartAnomalies.dialogFor(complex));
        assertNotNull(AbyssalResearchComplex.INSTANCE.intelText(anomalyRun));
        ForgeStates.spendRun(ChartRuns.currentOrNull(), ForgeSite.ABYSSAL_RESEARCH_COMPLEX);
        assertNull(AbyssalResearchComplex.INSTANCE.intelText(anomalyRun));
    }

    @Test
    void theGuardsAreAGabrielAndSeraphsGrowingWithTier() {
        FakeChartContentEngine engine = FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());

        CampaignFleetAPI guards = ComplexGuards.create(ChartTiers.HIGHEST_TIER, ChartGenContext.none(), 7L);

        assertNotNull(guards);
        assertEquals("rat_gabriel_Attack", engine.flagships.get(0).variantId());
        assertEquals(ComplexGuards.SERAPH_CORE, engine.flagships.get(0).aiCoreId());
        assertEquals(ComplexGuards.LINEUP.escorts().size(), engine.coreShips.size());
        assertTrue(engine.coreShips.stream().allMatch(ship -> ComplexGuards.SERAPH_CORE.equals(ship.aiCoreId())));
        assertEquals(3, ComplexGuards.LINEUP.escortCount(ChartTiers.LOWEST_TIER));
    }
}
