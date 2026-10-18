package exiledsector.charts;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartPocketFactoryTest {

    private static final String RUN_ID = "abc";

    private SectorAPI sector;
    private StarSystemAPI pocket;
    private PlanetAPI star;
    private CustomCampaignEntityAPI inner;
    private MemoryAPI pocketMemory;
    private final Vector2f pocketLocation = new Vector2f();

    @BeforeEach
    void setUp() {
        sector = mock(SectorAPI.class);
        pocket = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        inner = mock(CustomCampaignEntityAPI.class);
        pocketMemory = mock(MemoryAPI.class);
        when(sector.createStarSystem(anyString())).thenReturn(pocket);
        when(pocket.getLocation()).thenReturn(pocketLocation);
        when(pocket.getMemoryWithoutUpdate()).thenReturn(pocketMemory);
        when(pocket.initStar(anyString(), anyString(), anyFloat(), anyFloat())).thenReturn(star);
        when(pocket.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(inner);
        when(pocket.getAllEntities()).thenReturn(List.of(star, inner));
    }

    @Test
    void thePocketHasAConstantBaseNameAndARunUniqueId() {
        ChartPocketFactory.createPocket(sector, RUN_ID);

        verify(sector).createStarSystem(ChartIds.POCKET_BASE_NAME);
        verify(pocket).setOptionalUniqueId("exiledSector_chart_abc");
        verify(pocket, never()).setBaseName(anyString());
    }

    @Test
    void thePocketCarriesEveryIsolationTagAndOurOwnTags() {
        ChartPocketFactory.createPocket(sector, RUN_ID);

        for (String tag : List.of(Tags.THEME_HIDDEN, Tags.THEME_SPECIAL, Tags.TEMPORARY_LOCATION, Tags.SYSTEM_CUT_OFF_FROM_HYPER,
                Tags.DO_NOT_SHOW_STRANDED_DIALOG, ChartIds.POCKET_TAG, ChartIds.runTag(RUN_ID))) {
            verify(pocket).addTag(tag);
        }
        verify(pocketMemory).set(ChartIds.NEX_DO_NOT_COLONIZE, true);
        verify(pocket).setDoNotShowIntelFromThisLocationOnMap(true);
    }

    @Test
    void thePocketHasOneStarAndNoHyperspacePresence() {
        ChartPocketFactory.createPocket(sector, RUN_ID);

        verify(pocket).initStar(eq(ChartIds.starId(RUN_ID)), eq(ChartPocketFactory.STAR_TYPE), anyFloat(), anyFloat());
        verify(pocket, never()).initStar(anyString(), anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
        verify(pocket, never()).initStar(anyString(), anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
        verify(pocket, never()).generateAnchorIfNeeded();
        verify(pocket, never()).autogenerateHyperspaceJumpPoints();
        verify(pocket, never()).autogenerateHyperspaceJumpPoints(org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyBoolean());
        verify(pocket, never()).setHyperspaceAnchor(org.mockito.ArgumentMatchers.any());
        verify(pocket).setLightColor(ChartPocketFactory.POCKET_LIGHT);
        verify(pocket).setBackgroundTextureFilename(ChartPocketFactory.POCKET_BACKGROUND);
    }

    @Test
    void thePocketSitsFarOutsideTheSector() {
        ChartPocketFactory.createPocket(sector, RUN_ID);

        assertTrue(pocketLocation.x <= ChartPocketFactory.FAR_AWAY_CORNER && pocketLocation.y <= ChartPocketFactory.FAR_AWAY_CORNER,
                "pocket at " + pocketLocation);
    }

    @Test
    void thePocketLocationIsDeterministicPerRunAndDiffersBetweenRuns() {
        assertEquals(ChartPocketFactory.pocketLocation("abc"), ChartPocketFactory.pocketLocation("abc"));
        assertNotEquals(ChartPocketFactory.pocketLocation("abc"), ChartPocketFactory.pocketLocation("abd"));
    }

    @Test
    void everyEntityInThePocketIsTaggedWithTheRun() {
        ChartPocketFactory.createPocket(sector, RUN_ID);

        verify(star).addTag(ChartIds.ENTITY_TAG);
        verify(star).addTag(ChartIds.runTag(RUN_ID));
        verify(inner).addTag(ChartIds.INNER_WORMHOLE_TAG);
        verify(inner, org.mockito.Mockito.atLeastOnce()).addTag(ChartIds.runTag(RUN_ID));
    }

    @Test
    void theInnerWormholeIsOurCustomEntityWithARunUniqueId() {
        ChartPocketFactory.createPocket(sector, RUN_ID);

        verify(pocket).addCustomEntity(eq(ChartIds.innerWormholeId(RUN_ID)), anyString(), eq(ChartIds.WORMHOLE_TYPE), anyString());
        verify(inner).setFixedLocation(ChartPocketFactory.INNER_WORMHOLE_X, ChartPocketFactory.INNER_WORMHOLE_Y);
    }

    @Test
    void theOuterWormholeOpensBesideThePlayerAndIsTagged() {
        LocationAPI hyperspace = mock(LocationAPI.class);
        CustomCampaignEntityAPI outer = mock(CustomCampaignEntityAPI.class);
        when(hyperspace.addCustomEntity(anyString(), anyString(), anyString(), anyString())).thenReturn(outer);

        ChartPocketFactory.addOuterWormhole(hyperspace, new Vector2f(1000f, 2000f), RUN_ID);

        verify(hyperspace).addCustomEntity(eq(ChartIds.outerWormholeId(RUN_ID)), anyString(), eq(ChartIds.WORMHOLE_TYPE), anyString());
        verify(outer).setFixedLocation(1000f + ChartPocketFactory.OUTER_WORMHOLE_OFFSET, 2000f);
        verify(outer).addTag(ChartIds.ENTITY_TAG);
        verify(outer).addTag(ChartIds.runTag(RUN_ID));
        verify(outer).addTag(ChartIds.OUTER_WORMHOLE_TAG);
    }

    @Test
    void theRunIdCanBeReadBackFromThePocket() {
        when(pocket.getTags()).thenReturn(List.of(Tags.THEME_HIDDEN, ChartIds.runTag(RUN_ID)));

        assertEquals(RUN_ID, ChartIds.runIdOf(pocket));
    }
}
