package exiledsector.charts;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.RingBandAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

public record ChartPocketContents(int stars, int planets, int terrain, int ringBands, int asteroids, int customEntities, int jumpPoints,
                                  int fleets, int allEntities) {

    public static ChartPocketContents of(StarSystemAPI pocket) {
        int stars = 0;
        int planets = 0;
        for (PlanetAPI planet : pocket.getPlanets()) {
            if (planet.isStar()) {
                stars++;
            } else {
                planets++;
            }
        }
        int ringBands = 0;
        for (SectorEntityToken entity : pocket.getAllEntities()) {
            if (entity instanceof RingBandAPI) {
                ringBands++;
            }
        }
        return new ChartPocketContents(stars, planets, pocket.getTerrainCopy().size(), ringBands, pocket.getAsteroids().size(),
                pocket.getCustomEntities().size(), pocket.getJumpPoints().size(), pocket.getFleets().size(), pocket.getAllEntities().size());
    }

    public String describe() {
        return stars + " star(s), " + planets + " planet(s) and moon(s), " + terrain + " terrain, " + ringBands + " ring band(s), "
                + asteroids + " asteroid(s), " + customEntities + " custom entit(ies), " + jumpPoints + " jump point(s), " + fleets
                + " fleet(s), " + allEntities + " entities in all";
    }
}
