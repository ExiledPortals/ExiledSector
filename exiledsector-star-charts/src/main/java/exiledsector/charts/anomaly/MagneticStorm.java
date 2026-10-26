package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin.MagneticFieldParams;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class MagneticStorm implements ChartAnomaly {

    public static final String ID = "magnetic_storm";
    public static final MagneticStorm INSTANCE = new MagneticStorm();
    static final float STAR_WIDTH = 400f;
    static final float PLANET_WIDTH = 200f;
    static final int MAX_WORLDS = 3;
    static final Color BASE = new Color(50, 20, 110, 130);
    static final Color[] AURORA = {new Color(150, 30, 120, 150), new Color(200, 50, 130, 190), new Color(250, 70, 150, 240),
        new Color(200, 80, 130, 255), new Color(75, 0, 160), new Color(127, 0, 255)};

    private MagneticStorm() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void shape(AnomalyLayoutSite site) {
        float starEdge = site.centre().getRadius() + site.between(500f, 900f);
        addField(site, site.centre(), STAR_WIDTH, starEdge + STAR_WIDTH / 2f, starEdge, starEdge + STAR_WIDTH, 1f);
        List<PlanetAPI> worlds = new ArrayList<>(site.worlds());
        for (int count = 0; count < MAX_WORLDS && !worlds.isEmpty(); count++) {
            PlanetAPI world = worlds.remove(site.random().nextInt(worlds.size()));
            float width = world.getRadius() + PLANET_WIDTH;
            addField(site, world, width, width / 2f, world.getRadius(), width + 50f, site.between(0.25f, 1f));
        }
    }

    private static void addField(AnomalyLayoutSite site, SectorEntityToken parent, float bandWidth, float middleRadius, float visualStart,
                                 float visualEnd, float auroraChance) {
        SectorEntityToken field = site.system().addTerrain(Terrain.MAGNETIC_FIELD,
                new MagneticFieldParams(bandWidth, middleRadius, parent, visualStart, visualEnd, BASE, auroraChance, AURORA));
        field.setCircularOrbit(parent, 0, 0, 100);
    }
}
