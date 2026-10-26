package exiledsector.charts.run.diagnostics;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.RingBandAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

public record ChartSystemContents(int stars, int planets, int terrain, int ringBands, int asteroids, int customEntities, int jumpPoints,
                                  int fleets, int allEntities) {

    public static ChartSystemContents of(StarSystemAPI system) {
        int stars = 0;
        int planets = 0;
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet.isStar()) {
                stars++;
            } else {
                planets++;
            }
        }
        int ringBands = 0;
        for (SectorEntityToken entity : system.getAllEntities()) {
            if (entity instanceof RingBandAPI) {
                ringBands++;
            }
        }
        return new ChartSystemContents(stars, planets, system.getTerrainCopy().size(), ringBands, system.getAsteroids().size(),
                system.getCustomEntities().size(), system.getJumpPoints().size(), system.getFleets().size(), system.getAllEntities().size());
    }

    public String describe() {
        return stars + " star(s), " + planets + " planet(s) and moon(s), " + terrain + " terrain, " + ringBands + " ring band(s), "
                + asteroids + " asteroid(s), " + customEntities + " custom entit(ies), " + jumpPoints + " jump point(s), " + fleets
                + " fleet(s), " + allEntities + " entities in all";
    }
}
