package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import java.util.List;
import java.util.Random;

public record AnomalyLayoutSite(StarSystemAPI system, SectorEntityToken centre, List<PlanetAPI> stars, Random random) {

    public AnomalyLayoutSite {
        stars = List.copyOf(stars);
    }

    public float between(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    public List<PlanetAPI> worlds() {
        return system.getPlanets().stream().filter(planet -> !planet.isStar()).toList();
    }
}
