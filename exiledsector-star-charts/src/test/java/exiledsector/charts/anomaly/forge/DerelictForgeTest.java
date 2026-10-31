package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
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
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DerelictForgeTest {

    private static final String RUN_ID = "run1";

    private MockSector mockSector;
    private StarSystemAPI system;
    private PlanetAPI star;
    private final List<SectorEntityToken> added = new ArrayList<>();
    private final List<String> addedTypes = new ArrayList<>();
    private final List<String> addedFactions = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        ForgeProgramsTest.registerShipped();
        system = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        when(star.getLocation()).thenReturn(new Vector2f());
        when(system.getCenter()).thenReturn(star);
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
        ForgePrograms.resetForTests();
        mockSector.close();
    }

    private static SectorEntityToken entity() {
        Set<String> tags = new HashSet<>();
        CustomCampaignEntityAPI entity = mock(CustomCampaignEntityAPI.class);
        doAnswer(invocation -> tags.add(invocation.getArgument(0))).when(entity).addTag(anyString());
        when(entity.hasTag(anyString())).thenAnswer(invocation -> tags.contains((String) invocation.getArgument(0)));
        MemoryAPI memory = FakeChartContentEngine.memory();
        when(entity.getMemoryWithoutUpdate()).thenReturn(memory);
        return entity;
    }

    private AnomalyContentSite site(ChartGenContext context, Predicate<ChartSpot> clear) {
        return new AnomalyContentSite(system, RUN_ID, 4, "pirates", context, List.of(), null, clear, salt -> new Random(salt));
    }

    private static void generateInto(ChartRun run, AnomalyContentSite site) {
        DerelictForge.INSTANCE.generate(site);
        site.commitStates(run);
    }

    private static ChartRun run() {
        return new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
    }

    @Test
    void theForgeIsANeutralStationOnAClearOrbitWithItsProgramsAndOneRun() {
        ChartRun run = run();

        generateInto(run, site(ChartGenContext.none(), spot -> true));

        assertEquals(1, added.size());
        assertEquals(DerelictForge.ENTITY_TYPE, addedTypes.get(0));
        assertEquals(Factions.NEUTRAL, addedFactions.get(0));
        SectorEntityToken forge = added.get(0);
        assertTrue(DerelictForge.isForge(forge));
        assertTrue(forge.hasTag(ChartIds.runTag(RUN_ID)));
        verify(forge).setCircularOrbitPointingDown(eq(star), anyFloat(), anyFloat(), anyFloat());
        assertEquals(ForgeRules.BASE_PROGRAMS, ForgeStates.programs(run).size());
        assertEquals(ForgeRules.BASE_RUNS, ForgeStates.runsLeft(run));
    }

    @Test
    void theForgeIsPlacedGuardedWithASeedForItsDefenders() {
        ChartRuns.store(run());

        DerelictForge.INSTANCE.generate(site(ChartGenContext.none(), spot -> true));

        MemoryAPI memory = added.get(0).getMemoryWithoutUpdate();
        assertTrue(VanillaDefenders.guarded(added.get(0)));
        assertTrue(memory.get(MemFlags.SALVAGE_SEED) instanceof Long);
    }

    @Test
    void chartLinesAddProgramsAndRuns() {
        ChartRun run = run();
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_FORGE_PROGRAMS, 1f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_FORGE_RUNS, 2f)));

        generateInto(run, site(context, spot -> true));

        assertEquals(ForgeRules.BASE_PROGRAMS + 1, ForgeStates.programs(run).size());
        assertEquals(ForgeRules.BASE_RUNS + 2, ForgeStates.runsLeft(run));
    }

    @Test
    void theForgeLooksFurtherOutWhenTheInnerOrbitsAreTaken() {
        ChartRuns.store(run());
        float blockedRadius = ForgeRules.ORBIT_MIN + ForgeRules.ORBIT_BAND_WIDTH * 2f;

        DerelictForge.INSTANCE.generate(site(ChartGenContext.none(), spot -> spot.position().length() > blockedRadius));

        assertEquals(1, added.size());
    }

    @Test
    void generatingWithoutAClearOrbitFails() {
        assertThrows(IllegalStateException.class, () -> DerelictForge.INSTANCE.generate(site(ChartGenContext.none(), spot -> false)));
    }

    @Test
    void spendingTheLastRunDarkensTheForge() {
        ChartRun run = run();
        ForgeStates.record(ForgeStates.of(run), List.of(ForgePrograms.byId("graft")), 1);

        assertTrue(ForgeStates.spendRun(run));
        assertEquals(0, ForgeStates.runsLeft(run));
        assertFalse(ForgeStates.spendRun(run));
        assertEquals(List.of(ForgePrograms.byId("graft")), ForgeStates.programs(run));
    }

    @Test
    void theForgeIsABuiltInAnomaly() {
        assertSame(DerelictForge.INSTANCE, ChartAnomalies.byId(DerelictForge.ANOMALY_ID));
    }
}
