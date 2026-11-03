package exiledsector.charts.layout.abyss;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain.TileParams;
import exiledsector.charts.content.ChartSites;
import exiledsector.i18n.Translation;
import exiledsector.charts.layout.ChartDeepSpace;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ChartAbyss {

    public static final float CENTRE_CLEARANCE = 2500f;
    static final float CENTRE_CORE_RADIUS = 1000f;
    static final float SATELLITE_CORE_RADIUS = 600f;
    static final float MIN_LIGHT_REACH = 41500f;
    static final float MAX_LIGHT_REACH = 43000f;
    static final float CENTRE_EXTRA_LIGHT = 4000f;
    static final int MIN_SATELLITES = 1;
    static final int MAX_SATELLITES = 3;
    static final float MIN_REACH = 9000f;
    static final float REACH_MARGIN = 3000f;
    static final float SATELLITE_SPACING = 14000f;
    static final float SATELLITE_SPREAD = 4000f;
    static final int PLACEMENT_ATTEMPTS = 30;
    static final float FOG_KEEP = 0.45f;
    static final float MIN_FOG_CLEARING = 200f;
    static final float MAX_FOG_CLEARING = 1200f;
    static final float FOG_TILE_SIZE = 400f;
    static final int MIN_FOG_TILES = 32;
    static final int MAX_FOG_TILES = 128;
    static final int FOG_TEXTURE_TILES = 4;
    static final float LIGHT_TINT = 0.35f;

    record Light(Vector2f at, float coreRadius, float lightRadius) {
    }

    private ChartAbyss() {
    }

    public static Color light(AbyssPalette palette) {
        Color from = ChartDeepSpace.AMBIENT_LIGHT;
        Color to = palette.colour();
        return new Color(blend(from.getRed(), to.getRed()), blend(from.getGreen(), to.getGreen()), blend(from.getBlue(), to.getBlue()));
    }

    private static int blend(int from, int to) {
        return Math.round(from + (to - from) * LIGHT_TINT);
    }

    public static void dress(StarSystemAPI system, AbyssPalette palette, boolean litCentre, float outermost, String idPrefix, Random random) {
        system.setLightColor(light(palette));
        List<Light> lights = new ArrayList<>();
        if (litCentre) {
            Light centre = rollLight(new Vector2f(), CENTRE_CORE_RADIUS, CENTRE_EXTRA_LIGHT, random);
            addPhotosphere(system, palette, centre, idPrefix + "_photosphere_centre");
            lights.add(centre);
        }
        int satellites = MIN_SATELLITES + random.nextInt(MAX_SATELLITES - MIN_SATELLITES + 1);
        List<Vector2f> spots = satelliteSpots(satellites, random);
        for (int i = 0; i < spots.size(); i++) {
            Light satellite = rollLight(spots.get(i), SATELLITE_CORE_RADIUS, 0f, random);
            addPhotosphere(system, palette, satellite, idPrefix + "_photosphere_" + i);
            lights.add(satellite);
        }
        addFog(system, palette, fogReach(reach(outermost), spots), lights, random);
        system.addTerrain(AbyssalDarknessTerrainPlugin.TERRAIN_ID, new AbyssalDarknessTerrainPlugin.Params(palette));
    }

    static float reach(float outermost) {
        return Math.max(MIN_REACH, outermost + REACH_MARGIN);
    }

    static float fogReach(float reach, List<Vector2f> satellites) {
        float farthest = 0f;
        for (Vector2f satellite : satellites) {
            farthest = Math.max(farthest, satellite.length() + SATELLITE_CORE_RADIUS + REACH_MARGIN);
        }
        return Math.max(reach, farthest);
    }

    static Light rollLight(Vector2f at, float coreRadius, float extraLight, Random random) {
        return new Light(at, coreRadius, coreRadius + MIN_LIGHT_REACH + (MAX_LIGHT_REACH - MIN_LIGHT_REACH) * random.nextFloat() + extraLight);
    }

    static List<Vector2f> satelliteSpots(int count, Random random) {
        List<Vector2f> spots = new ArrayList<>();
        for (int attempt = 0; attempt < count * PLACEMENT_ATTEMPTS && spots.size() < count; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2d;
            float distance = SATELLITE_SPACING + SATELLITE_SPREAD * random.nextFloat();
            Vector2f spot = ChartSites.pointAround(new Vector2f(), angle, distance);
            if (spots.stream().allMatch(taken -> Vector2f.sub(taken, spot, null).length() >= SATELLITE_SPACING)) {
                spots.add(spot);
            }
        }
        return spots;
    }

    static int fogTiles(float reach) {
        int needed = (int) Math.ceil(reach * 2f / FOG_TILE_SIZE);
        int tiles = MIN_FOG_TILES;
        while (tiles < needed && tiles < MAX_FOG_TILES) {
            tiles *= 2;
        }
        return tiles;
    }

    private static void addPhotosphere(StarSystemAPI system, AbyssPalette palette, Light light, String id) {
        CustomCampaignEntityAPI photosphere = system.addCustomEntity(id, Translation.gameText("chart.abyss.photosphere.name"),
                ChartPhotospherePlugin.ENTITY_TYPE, Factions.NEUTRAL, new ChartPhotospherePlugin.Params(palette.colour(), light.lightRadius()));
        photosphere.setRadius(light.coreRadius());
        photosphere.setFixedLocation(light.at().x, light.at().y);
    }

    private static void addFog(StarSystemAPI system, AbyssPalette palette, float reach, List<Light> lights, Random random) {
        int tiles = fogTiles(reach);
        SectorEntityToken fog = system.addTerrain(AbyssalFogTerrainPlugin.TERRAIN_ID, new TileParams("x".repeat(tiles * tiles), tiles, tiles,
                AbyssalFogTerrainPlugin.FALLBACK_CATEGORY, AbyssalFogTerrainPlugin.FALLBACK_KEY, FOG_TEXTURE_TILES, FOG_TEXTURE_TILES, null));
        if (!(fog instanceof CampaignTerrainAPI terrain) || !(terrain.getPlugin() instanceof AbyssalFogTerrainPlugin plugin)) {
            return;
        }
        plugin.setColour(palette.darkColour());
        NebulaEditor editor = new NebulaEditor(plugin);
        editor.noisePrune(FOG_KEEP);
        for (Light light : lights) {
            float clearing = light.coreRadius() + MIN_FOG_CLEARING + (MAX_FOG_CLEARING - MIN_FOG_CLEARING) * random.nextFloat();
            editor.clearArc(light.at().x, light.at().y, 0f, clearing, 0f, 360f);
        }
    }
}
