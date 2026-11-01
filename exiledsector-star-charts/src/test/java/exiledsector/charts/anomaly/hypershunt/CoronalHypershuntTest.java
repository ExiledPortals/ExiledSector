package exiledsector.charts.anomaly.hypershunt;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.StarAnomalies;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.objective.ChartObjective;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class CoronalHypershuntTest {

    private static final String RUN_ID = "run1";
    private static final float STAR_RADIUS = 1200f;

    private MockSector mockSector;
    private StarSystemAPI system;
    private PlanetAPI star;
    private final List<SectorEntityToken> added = new ArrayList<>();
    private final List<String> addedNames = new ArrayList<>();
    private final List<String> addedTypes = new ArrayList<>();
    private final List<String> addedFactions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        system = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        when(star.getLocation()).thenReturn(new Vector2f());
        when(star.getRadius()).thenReturn(STAR_RADIUS);
        when(star.getName()).thenReturn("Blue Star");
        when(system.getStar()).thenReturn(star);
        when(system.getId()).thenReturn("charted space");
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(RUN_ID));
        when(system.addCustomEntity(anyString(), any(), anyString(), anyString())).thenAnswer(invocation -> {
            SectorEntityToken entity = entity();
            added.add(entity);
            addedNames.add(invocation.getArgument(1));
            addedTypes.add(invocation.getArgument(2));
            addedFactions.add(invocation.getArgument(3));
            return entity;
        });
    }

    @AfterEach
    void tearDown() {
        ChartTables.resetAnomalies();
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
        when(entity.getOrbitFocus()).thenReturn(star);
        return entity;
    }

    private AnomalyContentSite site(Predicate<ChartSpot> clear) {
        return new AnomalyContentSite(system, RUN_ID, 4, "pirates", ChartGenContext.none(), List.of(), null, clear, salt -> new Random(salt));
    }

    private static ChartRun run() {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(4, "pirates", ChartObjective.CLEAR, List.of(), null, 3, 2, List.of(CoronalHypershunt.ANOMALY_ID)));
        return run;
    }

    private static AtlasTraits traitsAt(float distance) {
        return new AtlasTraits(new AtlasCell(1, 1), distance, "normal", "pirates", "t1", "compact", Set.of(), List.of(), null, null);
    }

    @Test
    void theShippedRowIsARareStarAnomalyThatNoOrdinaryCatalystInserts() throws Exception {
        ChartTables.loadAnomalies();

        assertSame(CoronalHypershunt.INSTANCE, ChartAnomalies.byId(CoronalHypershunt.ANOMALY_ID));
        ChartAnomalies.Definition definition = ChartAnomalies.definition(CoronalHypershunt.ANOMALY_ID);
        assertTrue(definition.replacesStar());
        assertFalse(definition.insertable());
        assertTrue(definition.weight() < ChartAnomalies.definition(StarAnomalies.BLACK_HOLE.id()).weight());
        assertTrue(ChartAnomalies.conflicts(CoronalHypershunt.ANOMALY_ID, StarAnomalies.BLACK_HOLE.id()));
        assertFalse(definition.icon().isEmpty());
    }

    @Test
    void theStarIsASingleBlueSupergiant() {
        assertEquals(StarTypes.BLUE_SUPERGIANT, CoronalHypershunt.INSTANCE.primaryStar());
        assertEquals(StarSystemType.SINGLE, CoronalHypershunt.INSTANCE.systemType());
    }

    @Test
    void itOnlyFormsFifteenOrMoreCellsFromTheGateway() {
        assertFalse(CoronalHypershunt.INSTANCE.fitsAt(traitsAt(CoronalHypershunt.MIN_DISTANCE - 0.1f)));
        assertTrue(CoronalHypershunt.INSTANCE.fitsAt(traitsAt(CoronalHypershunt.MIN_DISTANCE)));
        assertTrue(CoronalHypershunt.INSTANCE.fitsAt(traitsAt(40f)));
    }

    @Test
    void theHypershuntOrbitsInTheStarsCoronaAsAGuardedNeutralEntity() {
        CoronalHypershunt.INSTANCE.generate(site(spot -> true));

        assertEquals(1, added.size());
        assertNull(addedNames.get(0));
        assertEquals(Entities.CORONAL_TAP, addedTypes.get(0));
        assertEquals(Factions.NEUTRAL, addedFactions.get(0));
        SectorEntityToken hypershunt = added.get(0);
        assertTrue(CoronalHypershunt.isHypershunt(hypershunt));
        assertTrue(hypershunt.hasTag(ChartIds.runTag(RUN_ID)));
        verify(hypershunt).setCircularOrbitPointingDown(eq(star), anyFloat(), eq(STAR_RADIUS + CoronalHypershunt.ORBIT_GAP), anyFloat());
        assertTrue(VanillaDefenders.guarded(hypershunt));
        assertTrue(hypershunt.getMemoryWithoutUpdate().get(MemFlags.SALVAGE_SEED) instanceof Long);
    }

    @Test
    void aCrowdedCoronaStillTakesTheHypershunt() {
        CoronalHypershunt.INSTANCE.generate(site(spot -> false));

        verify(added.get(0)).setCircularOrbitPointingDown(eq(star), anyFloat(), eq(STAR_RADIUS + CoronalHypershunt.ORBIT_GAP), anyFloat());
    }

    @Test
    void aSystemWithoutAStarFails() {
        when(system.getStar()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> CoronalHypershunt.INSTANCE.generate(site(spot -> true)));
    }

    @Test
    void theDialogTakesOverTheHypershuntOnlyDuringItsRun() {
        CoronalHypershunt.INSTANCE.generate(site(spot -> true));
        SectorEntityToken hypershunt = added.get(0);
        when(system.getEntitiesWithTag(CoronalHypershunt.HYPERSHUNT_TAG)).thenReturn(List.of(hypershunt));

        assertNull(ChartAnomalies.dialogFor(hypershunt));
        ChartRuns.store(run());
        AnomalyRun anomalyRun = new AnomalyRun(ChartRuns.currentOrNull(), system);

        assertInstanceOf(HypershuntDialog.class, ChartAnomalies.dialogFor(hypershunt));
        assertNull(CoronalHypershunt.INSTANCE.dialogFor(anomalyRun, star));
        String intel = CoronalHypershunt.INSTANCE.intelText(anomalyRun);
        assertTrue(intel.contains("Blue Star"), intel);

        hypershunt.getMemoryWithoutUpdate().set(VanillaDefenders.DEFEATED_KEY, true);
        assertNull(CoronalHypershunt.INSTANCE.intelText(anomalyRun));
        assertInstanceOf(HypershuntDialog.class, ChartAnomalies.dialogFor(hypershunt));
    }
}
