package exiledsector.charts.layout.feature;

import exiledsector.charts.layout.feature.FeaturePlan.AsteroidBelt;
import exiledsector.charts.layout.feature.FeaturePlan.RingBand;
import exiledsector.charts.layout.feature.FeaturePlan.World;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeaturePlanTest {

    private static final float OUTER = 8000f;

    @Test
    void theReachCoversCentreWorldsWithEverythingOrbitingThem() {
        FeaturePlan plan = new FeaturePlan(
                List.of(new World("giant", FeaturePlan.CENTRE, "gas_giant", 300f, 10000f, 600f, 0f),
                        new World("moon", 0, "barren", 60f, 1500f, 40f, 0f),
                        new World("moonlet", 1, "barren", 20f, 200f, 10f, 0f)),
                List.of(new RingBand(0, "rings_ice0", 0, 256f, 800f, 40f, true)),
                List.of());

        assertEquals(1500f + 200f + 20f, plan.extent(0));
        assertEquals(10000f + 1720f, plan.reach());
    }

    @Test
    void centreBandsAndBeltsCountTowardsTheReach() {
        FeaturePlan plan = new FeaturePlan(List.of(),
                List.of(new RingBand(FeaturePlan.CENTRE, "rings_asteroids0", 0, 256f, 9000f, 400f, false)),
                List.of(new AsteroidBelt(100, 9500f, 400f, 300f)));

        assertEquals(9700f, plan.reach());
    }

    @Test
    void everyFeaturePlansTheSameLayoutForTheSameRollsAndReachesPastTheOrbits() {
        for (ChartFeature feature : List.of(RingedGiant.INSTANCE, RoguePlanet.INSTANCE, TwinWorlds.INSTANCE, DebrisRing.INSTANCE)) {
            for (long seed = 0; seed < 20; seed++) {
                FeaturePlan plan = feature.plan(new FeatureRolls(new Random(seed), OUTER));

                assertEquals(plan, feature.plan(new FeatureRolls(new Random(seed), OUTER)));
                assertTrue(plan.reach() > OUTER);
                for (World world : plan.worlds()) {
                    if (world.focus() == FeaturePlan.CENTRE) {
                        assertTrue(world.orbitRadius() - plan.extent(plan.worlds().indexOf(world)) > OUTER);
                    }
                }
            }
        }
    }

    @Test
    void aRingedGiantKeepsItsBandsAndMoonsOnTheGiant() {
        FeaturePlan plan = RingedGiant.INSTANCE.plan(new FeatureRolls(new Random(7L), OUTER));

        assertEquals(FeaturePlan.CENTRE, plan.worlds().get(0).focus());
        assertEquals(RingedGiant.BANDS, plan.ringBands().size());
        assertTrue(plan.ringBands().stream().allMatch(band -> band.focus() == 0));
        assertTrue(plan.worlds().subList(1, plan.worlds().size()).stream().allMatch(moon -> moon.focus() == 0));
        assertEquals(plan.worlds().get(0).orbitRadius() + plan.extent(0), plan.reach());
    }
}
