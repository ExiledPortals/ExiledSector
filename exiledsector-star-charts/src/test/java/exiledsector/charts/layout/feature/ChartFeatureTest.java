package exiledsector.charts.layout.feature;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartFeatureTest {

    private static final float OUTER = 8000f;

    private record Added(String id, SectorEntityToken focus, String type, float radius, float orbitRadius, PlanetAPI planet) {
    }

    private final List<Added> added = new ArrayList<>();
    private final List<PlanetAPI> conditioned = new ArrayList<>();
    private StarSystemAPI system;
    private SectorEntityToken star;

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        star = mock(SectorEntityToken.class);
        when(system.addPlanet(anyString(), any(), anyString(), anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenAnswer(invocation -> {
            PlanetAPI planet = mock(PlanetAPI.class);
            float radius = invocation.getArgument(5);
            when(planet.getRadius()).thenReturn(radius);
            added.add(new Added(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(3), radius,
                    invocation.getArgument(6), planet));
            return planet;
        });
    }

    private FeatureSite site(long seed) {
        return new FeatureSite(system, star, OUTER, "chart_star_feature", new Random(seed), conditioned::add);
    }

    private Added first() {
        return added.get(0);
    }

    @Test
    void aRingedGiantSitsPastTheLastOrbitWithRingsAndMoonsAndHoldsTheBoss() {
        SectorEntityToken anchor = RingedGiant.INSTANCE.build(site(3L));

        assertSame(first().planet(), anchor);
        assertSame(star, first().focus());
        assertTrue(RingedGiant.GIANT_TYPES.contains(first().type()));
        List<Added> moons = added.subList(1, added.size());
        for (Added moon : moons) {
            assertTrue(first().orbitRadius() - moon.orbitRadius() - moon.radius() >= OUTER + RingedGiant.GAP, "a moon swings inside the last orbit");
        }
        assertTrue(moons.size() >= 3 && moons.size() <= 4);
        moons.forEach(moon -> assertSame(anchor, moon.focus()));
        verify(system, atLeast(3)).addRingBand(eq(anchor), eq("misc"), anyString(), anyFloat(), anyInt(), any(), anyFloat(), anyFloat(), anyFloat(),
                anyString(), isNull());
        assertEquals(added.size(), conditioned.size());
    }

    @Test
    void twinWorldsShareATypeAndTheSecondCirclesTheFirst() {
        SectorEntityToken anchor = TwinWorlds.INSTANCE.build(site(5L));

        assertEquals(2, added.size());
        assertSame(first().planet(), anchor);
        assertEquals(first().type(), added.get(1).type());
        assertSame(anchor, added.get(1).focus());
        assertTrue(added.get(1).orbitRadius() > first().radius() + added.get(1).radius());
        assertTrue(first().orbitRadius() >= OUTER + TwinWorlds.GAP);
    }

    @Test
    void aRoguePlanetDriftsFarOutOnASlowOrbit() {
        SectorEntityToken anchor = RoguePlanet.INSTANCE.build(site(7L));

        assertEquals(1, added.size());
        assertSame(first().planet(), anchor);
        assertTrue(first().orbitRadius() >= OUTER + RoguePlanet.GAP);
        assertTrue(RoguePlanet.ROGUE_TYPES.contains(first().type()));
    }

    @Test
    void theSameSeedBuildsTheSameFeature() {
        RingedGiant.INSTANCE.build(site(9L));
        List<Added> firstBuild = new ArrayList<>(added);
        added.clear();
        RingedGiant.INSTANCE.build(site(9L));

        assertEquals(firstBuild.size(), added.size());
        for (int index = 0; index < added.size(); index++) {
            assertEquals(firstBuild.get(index).type(), added.get(index).type());
            assertEquals(firstBuild.get(index).orbitRadius(), added.get(index).orbitRadius());
        }
    }
}
