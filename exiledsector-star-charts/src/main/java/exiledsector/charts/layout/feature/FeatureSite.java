package exiledsector.charts.layout.feature;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public record FeatureSite(StarSystemAPI system, SectorEntityToken centre, float outerRadius, String idPrefix, Random random,
                          Consumer<PlanetAPI> conditions) {

    public String entityId(String part) {
        return idPrefix + "_" + part;
    }

    public float between(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    public float orbitBeyond(float gap, float spread) {
        return outerRadius + gap + between(0f, spread);
    }

    public float angle() {
        return random.nextFloat() * 360f;
    }

    public <T> T pick(List<T> options) {
        return options.get(random.nextInt(options.size()));
    }

    public PlanetAPI addPlanet(String part, SectorEntityToken focus, String type, float radius, float orbitRadius, float orbitDays) {
        PlanetAPI planet = system.addPlanet(entityId(part), focus, part, type, angle(), radius, orbitRadius, orbitDays);
        conditions.accept(planet);
        return planet;
    }

    public static float orbitDays(float orbitRadius) {
        return orbitRadius / 15f;
    }
}
