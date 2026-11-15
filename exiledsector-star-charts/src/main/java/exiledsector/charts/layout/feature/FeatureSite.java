package exiledsector.charts.layout.feature;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import exiledsector.charts.layout.feature.FeaturePlan.AsteroidBelt;
import exiledsector.charts.layout.feature.FeaturePlan.RingBand;
import exiledsector.charts.layout.feature.FeaturePlan.World;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public record FeatureSite(StarSystemAPI system, SectorEntityToken centre, float outerRadius, String idPrefix, Random random,
                          Consumer<PlanetAPI> conditions) {

    static final float BELT_FASTEST_DAYS = 0.75f;
    static final float BELT_SLOWEST_DAYS = 1.5f;

    public String entityId(String part) {
        return idPrefix + "_" + part;
    }

    public FeatureRolls rolls() {
        return new FeatureRolls(random, outerRadius);
    }

    public SectorEntityToken place(FeaturePlan plan) {
        for (AsteroidBelt belt : plan.asteroidBelts()) {
            system.addAsteroidBelt(centre, belt.asteroids(), belt.orbitRadius(), belt.width(), belt.orbitDays() * BELT_FASTEST_DAYS,
                    belt.orbitDays() * BELT_SLOWEST_DAYS, Terrain.ASTEROID_BELT, null);
        }
        addRingBands(plan, FeaturePlan.CENTRE, centre);
        List<SectorEntityToken> placed = new ArrayList<>();
        for (int index = 0; index < plan.worlds().size(); index++) {
            World world = plan.worlds().get(index);
            SectorEntityToken focus = world.focus() == FeaturePlan.CENTRE ? centre : placed.get(world.focus());
            PlanetAPI planet = system.addPlanet(entityId(world.part()), focus, world.part(), world.typeId(), world.startAngle(), world.radius(),
                    world.orbitRadius(), world.orbitDays());
            conditions.accept(planet);
            placed.add(planet);
            addRingBands(plan, index, planet);
        }
        return placed.isEmpty() ? null : placed.get(0);
    }

    private void addRingBands(FeaturePlan plan, int focusIndex, SectorEntityToken focus) {
        for (RingBand band : plan.ringBands()) {
            if (band.focus() == focusIndex) {
                system.addRingBand(focus, "misc", band.texture(), band.width(), band.variant(), Color.white, band.width(), band.middleRadius(),
                        band.orbitDays(), band.terrain() ? Terrain.RING : null, null);
            }
        }
    }
}
