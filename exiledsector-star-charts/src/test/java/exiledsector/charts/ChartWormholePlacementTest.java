package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.EventHorizonPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin.CoronaParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartWormholePlacementTest {

    private StarSystemAPI system;
    private final List<PlanetAPI> planets = new ArrayList<>();
    private final List<CampaignTerrainAPI> terrain = new ArrayList<>();

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        when(system.getPlanets()).thenReturn(planets);
        when(system.getTerrainCopy()).thenReturn(terrain);
    }

    private PlanetAPI body(boolean isStar, float radius, SectorEntityToken focus, float orbitRadius) {
        PlanetAPI body = mock(PlanetAPI.class);
        when(body.isStar()).thenReturn(isStar);
        when(body.getRadius()).thenReturn(radius);
        when(body.getOrbitFocus()).thenReturn(focus);
        when(body.getCircularOrbitRadius()).thenReturn(orbitRadius);
        when(body.getLocation()).thenReturn(new Vector2f(orbitRadius, 0f));
        planets.add(body);
        return body;
    }

    private void ringAround(SectorEntityToken host, BaseRingTerrain plugin, float bandWidth, float middleRadius) {
        plugin.params = new CoronaParams(bandWidth, middleRadius, host, 5f, 0f, 1f);
        CampaignTerrainAPI ring = mock(CampaignTerrainAPI.class);
        when(ring.getPlugin()).thenReturn(plugin);
        when(ring.getOrbitFocus()).thenReturn(host);
        terrain.add(ring);
    }

    private float placedRadius(long seed) {
        return ChartWormholePlacement.choose(system, new Random(seed)).length();
    }

    @Test
    void theWormholeSitsOutsideAStarsCoronaWithRoomForTheArrival() {
        PlanetAPI star = body(true, 800f, null, 0f);
        ringAround(star, mock(StarCoronaTerrainPlugin.class), 3000f, 1500f);

        float radius = placedRadius(1L);

        assertTrue(radius > 3000f + ChartWormholePlacement.TERRAIN_CLEARANCE, "placed at " + radius);
        assertTrue(radius - ChartSystemFactory.ENTRY_OFFSET > 3000f, "the arrival point is clear of the corona");
    }

    @Test
    void theWormholeStaysClearOfABlackHolesEventHorizon() {
        PlanetAPI blackHole = body(true, 300f, null, 0f);
        ringAround(blackHole, mock(EventHorizonPlugin.class), 2400f, 1200f);

        assertTrue(placedRadius(2L) > 2400f + ChartWormholePlacement.TERRAIN_CLEARANCE);
    }

    @Test
    void closeBinaryStarsSweepAnAnnulusThatTheWormholeAvoids() {
        SectorEntityToken centre = mock(SectorEntityToken.class);
        when(centre.getLocation()).thenReturn(new Vector2f());
        PlanetAPI primary = body(true, 700f, centre, 1200f);
        PlanetAPI secondary = body(true, 500f, centre, 1700f);
        ringAround(primary, mock(StarCoronaTerrainPlugin.class), 1600f, 800f);
        ringAround(secondary, mock(StarCoronaTerrainPlugin.class), 1200f, 600f);

        float radius = placedRadius(3L);

        assertTrue(radius > 1700f + 1200f + ChartWormholePlacement.TERRAIN_CLEARANCE, "placed at " + radius);
    }

    @Test
    void theWormholeFindsTheGapBetweenPlanetOrbits() {
        PlanetAPI star = body(true, 500f, null, 0f);
        body(false, 200f, star, 2000f);
        body(false, 200f, star, 6000f);

        float radius = placedRadius(4L);

        assertTrue(radius > 2000f + 200f + ChartWormholePlacement.PLANET_CLEARANCE, "placed at " + radius);
        assertTrue(radius < 6000f - 200f - ChartWormholePlacement.PLANET_CLEARANCE, "placed at " + radius);
    }

    @Test
    void aMoonsSweepCoversItsParentsWholeOrbit() {
        PlanetAPI star = body(true, 500f, null, 0f);
        PlanetAPI giant = body(false, 300f, star, 2600f);
        body(false, 80f, giant, 900f);

        float radius = placedRadius(5L);

        assertTrue(radius > 2600f + 900f + 80f + ChartWormholePlacement.PLANET_CLEARANCE, "placed at " + radius);
    }

    @Test
    void theSameSeedPlacesTheWormholeAtTheSameSpot() {
        body(true, 500f, null, 0f);

        assertEquals(ChartWormholePlacement.choose(system, new Random(9L)), ChartWormholePlacement.choose(system, new Random(9L)));
    }

    @Test
    void anAsteroidFieldIsCentredOnItsOwnOrbitNotOnTheStar() {
        PlanetAPI star = body(true, 500f, null, 0f);
        BaseRingTerrain fieldPlugin = mock(BaseRingTerrain.class);
        fieldPlugin.params = new BaseRingTerrain.RingParams(1200f, 600f, null);
        CampaignTerrainAPI field = mock(CampaignTerrainAPI.class);
        when(field.getPlugin()).thenReturn(fieldPlugin);
        when(field.getOrbitFocus()).thenReturn(star);
        when(field.getCircularOrbitRadius()).thenReturn(2600f);
        terrain.add(field);

        float radius = placedRadius(7L);

        assertTrue(radius > 2600f + 1200f + ChartWormholePlacement.TERRAIN_CLEARANCE, "placed at " + radius);
    }

    @Test
    void coronaFlaresAreKeptClearOfTheWormhole() {
        PlanetAPI star = body(true, 600f, null, 0f);
        StarCoronaTerrainPlugin corona = mock(StarCoronaTerrainPlugin.class);
        when(corona.getFlareExtraLengthMultMax()).thenReturn(1.5f);
        when(corona.getFlareExtraLengthFlatMax()).thenReturn(500f);
        ringAround(star, corona, 1800f, 900f);

        Vector2f placed = ChartWormholePlacement.choose(system, new Random(8L));

        float flareTip = 1800f + 0.5f * 1800f + 500f;
        float arrival = new Vector2f(placed.x + ChartSystemFactory.ENTRY_OFFSET, placed.y).length();
        assertTrue(placed.length() > flareTip + ChartWormholePlacement.TERRAIN_CLEARANCE, "placed at " + placed.length());
        assertTrue(arrival > flareTip + ChartWormholePlacement.TERRAIN_CLEARANCE, "arrival at " + arrival);
    }

    @Test
    void aPulsarBeamThatSweepsEveryAngleDoesNotPushTheWormholeToTheEdge() {
        PlanetAPI neutronStar = body(true, 150f, null, 0f);
        ringAround(neutronStar, mock(PulsarBeamTerrainPlugin.class), 30000f, 15000f);
        ringAround(neutronStar, mock(StarCoronaTerrainPlugin.class), 450f, 225f);

        assertEquals(ChartWormholePlacement.MIN_RADIUS, placedRadius(9L), 0.5f);
    }

    @Test
    void theArrivalPointBesideTheWormholeIsClearToo() {
        List<ChartWormholePlacement.Band> blocked = List.of(new ChartWormholePlacement.Band(1000f, 1790f),
                new ChartWormholePlacement.Band(1810f, 2160f), new ChartWormholePlacement.Band(2400f, 2600f));
        for (long seed = 0; seed < 200; seed++) {
            Random random = new Random(seed);
            Vector2f placed = ChartWormholePlacement.place(blocked, random);
            float arrival = new Vector2f(placed.x + ChartSystemFactory.ENTRY_OFFSET, placed.y).length();
            assertTrue(ChartWormholePlacement.isClear(blocked, placed.length()), "seed " + seed + " wormhole at " + placed.length());
            assertTrue(ChartWormholePlacement.isClear(blocked, arrival), "seed " + seed + " arrival at " + arrival);
        }
    }

    @Test
    void anEmptySystemUsesTheMinimumRadius() {
        assertEquals(ChartWormholePlacement.MIN_RADIUS, placedRadius(6L), 0.5f);
    }
}
