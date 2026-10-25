package exiledsector.charts;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartSystemFactoryTest {

    private static final String RUN_ID = "abc";
    private static final ChartLayoutRequest REQUEST = new ChartLayoutRequest(42L, StarSystemType.BINARY_FAR);

    private SectorAPI sector;
    private StarSystemAPI system;
    private PlanetAPI star;
    private CustomCampaignEntityAPI inner;
    private MemoryAPI systemMemory;
    private FakeChartLayoutEngine engine;
    private final Vector2f systemLocation = new Vector2f();

    @BeforeEach
    void setUp() {
        sector = mock(SectorAPI.class);
        system = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        inner = mock(CustomCampaignEntityAPI.class);
        systemMemory = mock(MemoryAPI.class);
        when(sector.createStarSystem(anyString())).thenReturn(system);
        when(system.getLocation()).thenReturn(systemLocation);
        when(system.getMemoryWithoutUpdate()).thenReturn(systemMemory);
        when(system.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenReturn(star);
        when(system.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(inner);
        when(system.getAllEntities()).thenReturn(List.of(star, inner));
        when(system.getStar()).thenReturn(star);
        when(star.isStar()).thenReturn(true);
        when(star.getRadius()).thenReturn(500f);
        when(star.getLocation()).thenReturn(new Vector2f());
        engine = FakeChartLayoutEngine.install(FakeChartLayoutEngine.doingNothing());
        FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
    }

    @AfterEach
    void tearDown() {
        ChartLayoutGenerator.resetForTests();
        ChartContentGenerator.resetForTests();
    }

    @Test
    void theSystemHasAConstantBaseNameAndARunUniqueId() {
        ChartSystemFactory.createSystem(sector, RUN_ID, REQUEST);

        verify(sector).createStarSystem(ChartIds.SYSTEM_BASE_NAME);
        verify(system).setOptionalUniqueId("exiledSector_chart_abc");
        verify(system, never()).setBaseName(anyString());
    }

    @Test
    void theSystemCarriesEveryIsolationTagAndOurOwnTags() {
        ChartSystemFactory.createSystem(sector, RUN_ID, REQUEST);

        for (String tag : List.of(Tags.THEME_HIDDEN, Tags.THEME_SPECIAL, Tags.TEMPORARY_LOCATION, Tags.SYSTEM_CUT_OFF_FROM_HYPER,
                Tags.DO_NOT_SHOW_STRANDED_DIALOG, Tags.NO_TOPOGRAPHY_SCANS, Tags.SYSTEM_ALREADY_USED_FOR_STORY, ChartIds.SYSTEM_TAG,
                ChartIds.runTag(RUN_ID))) {
            verify(system).addTag(tag);
        }
        verify(systemMemory).set(ChartIds.NEX_DO_NOT_COLONIZE, true);
        verify(system).setDoNotShowIntelFromThisLocationOnMap(true);
    }

    @Test
    void theLayoutIsBuiltAroundTheRunsStarIdWithTheRequestedTypeAndRecordsIt() {
        ChartSystemFactory.ChartSystem created = ChartSystemFactory.createSystem(sector, RUN_ID, REQUEST);

        assertEquals(1, engine.builds.size());
        assertSame(system, engine.builds.get(0).system());
        assertEquals(ChartIds.starId(RUN_ID), engine.builds.get(0).starId());
        assertEquals(StarSystemType.BINARY_FAR, engine.builds.get(0).systemType());
        assertSame(system, created.system());
        assertEquals(42L, created.layout().seed());
        assertEquals(StarSystemType.BINARY_FAR, created.layout().systemType());
        assertTrue(created.layout().generated());
        verify(system, never()).initStar(anyString(), anyString(), anyFloat(), anyFloat());
    }

    @Test
    void aFailedLayoutFallsBackToTheSingleStarAndNeverAddsHyperspacePresence() {
        FakeChartLayoutEngine.install(FakeChartLayoutEngine.failing());

        ChartSystemFactory.ChartSystem created = ChartSystemFactory.createSystem(sector, RUN_ID, REQUEST);

        assertFalse(created.layout().generated());
        verify(system).initStar(eq(ChartIds.starId(RUN_ID)), eq(ChartSystemFactory.STAR_TYPE), anyFloat(), anyFloat());
        verify(system, never()).initStar(anyString(), anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
        verify(system, never()).generateAnchorIfNeeded();
        verify(system, never()).autogenerateHyperspaceJumpPoints();
        verify(system, never()).autogenerateHyperspaceJumpPoints(anyBoolean(), anyBoolean());
        verify(system, never()).autogenerateHyperspaceJumpPoints(anyBoolean(), anyBoolean(), anyBoolean());
        verify(system, never()).setHyperspaceAnchor(any());
        verify(system).setLightColor(ChartSystemFactory.SYSTEM_LIGHT);
        verify(system).setBackgroundTextureFilename(ChartSystemFactory.SYSTEM_BACKGROUND);
    }

    @Test
    void theSystemsitsFarOutsideTheSector() {
        ChartSystemFactory.createSystem(sector, RUN_ID, REQUEST);

        assertTrue(systemLocation.x <= ChartSystemFactory.FAR_AWAY_CORNER && systemLocation.y <= ChartSystemFactory.FAR_AWAY_CORNER,
                "system at " + systemLocation);
    }

    @Test
    void theSystemLocationIsDeterministicPerRunAndDiffersBetweenRuns() {
        assertEquals(ChartSystemFactory.systemLocation("abc"), ChartSystemFactory.systemLocation("abc"));
        assertNotEquals(ChartSystemFactory.systemLocation("abc"), ChartSystemFactory.systemLocation("abd"));
    }

    @Test
    void everyEntityInTheSystemIsTaggedWithTheRun() {
        ChartSystemFactory.createSystem(sector, RUN_ID, REQUEST);

        verify(star).addTag(ChartIds.ENTITY_TAG);
        verify(star).addTag(ChartIds.runTag(RUN_ID));
        verify(inner).addTag(ChartIds.INNER_WORMHOLE_TAG);
        verify(inner, atLeastOnce()).addTag(ChartIds.runTag(RUN_ID));
    }

    @Test
    void theInnerWormholeIsOurCustomEntityPlacedClearOfTheStar() {
        ChartSystemFactory.ChartSystem created = ChartSystemFactory.createSystem(sector, RUN_ID, REQUEST);

        Vector2f placed = created.layout().innerWormholeLocation();
        verify(system).addCustomEntity(eq(ChartIds.innerWormholeId(RUN_ID)), anyString(), eq(ChartIds.WORMHOLE_TYPE), anyString());
        verify(inner).setFixedLocation(placed.x, placed.y);
        assertTrue(placed.length() > 500f + ChartWormholePlacement.STAR_CLEARANCE, "wormhole at " + placed);
    }

    @Test
    void theOuterWormholeOpensBesideThePlayerAndIsTagged() {
        LocationAPI hyperspace = mock(LocationAPI.class);
        CustomCampaignEntityAPI outer = mock(CustomCampaignEntityAPI.class);
        when(hyperspace.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(outer);

        ChartSystemFactory.addOuterWormhole(hyperspace, new Vector2f(1000f, 2000f), RUN_ID);

        verify(hyperspace).addCustomEntity(eq(ChartIds.outerWormholeId(RUN_ID)), anyString(), eq(ChartIds.WORMHOLE_TYPE), anyString());
        verify(outer).setFixedLocation(1000f + ChartSystemFactory.OUTER_WORMHOLE_OFFSET, 2000f);
        verify(outer).addTag(ChartIds.ENTITY_TAG);
        verify(outer).addTag(ChartIds.runTag(RUN_ID));
        verify(outer).addTag(ChartIds.OUTER_WORMHOLE_TAG);
    }

    @Test
    void theOuterWormholeOrbitsTheMarketItOpenedBesideOnThePlayersSide() {
        LocationAPI system = mock(LocationAPI.class);
        CustomCampaignEntityAPI outer = mock(CustomCampaignEntityAPI.class);
        PlanetAPI market = mock(PlanetAPI.class);
        when(system.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(outer);
        when(market.getLocation()).thenReturn(new Vector2f(500f, 500f));
        when(market.getRadius()).thenReturn(150f);

        ChartSystemFactory.addOuterWormhole(system, new Vector2f(500f, 900f), market, RUN_ID);

        verify(outer).setCircularOrbit(market, 90f, 150f + ChartSystemFactory.OUTER_WORMHOLE_OFFSET, ChartSystemFactory.OUTER_ORBIT_DAYS);
        verify(outer, never()).setFixedLocation(anyFloat(), anyFloat());
        verify(outer).addTag(ChartIds.OUTER_WORMHOLE_TAG);
    }

    @Test
    void theRunIdCanBeReadBackFromTheSystem() {
        when(system.getTags()).thenReturn(List.of(Tags.THEME_HIDDEN, ChartIds.runTag(RUN_ID)));

        assertEquals(RUN_ID, ChartIds.runIdOf(system));
    }
}
