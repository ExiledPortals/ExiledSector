package exiledsector.charts;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledlib.i18n.Translation;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class ChartSystemFactory {

    static final List<String> ISOLATION_TAGS = List.of(Tags.THEME_HIDDEN, Tags.THEME_SPECIAL, Tags.TEMPORARY_LOCATION,
            Tags.SYSTEM_CUT_OFF_FROM_HYPER, Tags.DO_NOT_SHOW_STRANDED_DIALOG, Tags.NOT_RANDOM_MISSION_TARGET, Tags.NO_TOPOGRAPHY_SCANS,
            Tags.SYSTEM_ALREADY_USED_FOR_STORY);
    static final String STAR_TYPE = "star_white";
    static final float STAR_RADIUS = 600f;
    static final float CORONA_SIZE = 300f;
    static final Color SYSTEM_LIGHT = new Color(205, 215, 255);
    static final String SYSTEM_BACKGROUND = "graphics/backgrounds/background4.jpg";
    static final float INNER_WORMHOLE_X = 2600f;
    static final float INNER_WORMHOLE_Y = 0f;
    static final float ENTRY_OFFSET = 350f;
    static final float OUTER_WORMHOLE_OFFSET = 250f;
    static final float OUTER_ORBIT_DAYS = 40f;
    static final float FAR_AWAY_CORNER = -320000f;
    static final float FAR_AWAY_SPACING = 4000f;
    static final int FAR_AWAY_CELLS = 40;

    private ChartSystemFactory() {
    }

    public static ChartSystem createSystem(SectorAPI sector, String runId, ChartLayoutRequest request) {
        StarSystemAPI system = createEmptySystem(sector, runId);
        ChartLayout layout = ChartLayoutGenerator.generate(sector, system, runId, request);
        Vector2f farAway = systemLocation(runId);
        system.getLocation().set(farAway.x, farAway.y);
        localiseName(system);
        addInnerWormhole(system, runId, layout.innerWormholeLocation());
        ChartContent content = ChartContentGenerator.generate(sector, system, runId, request.contentSeed(), request.tier(),
                innerWormholeSpot(layout.innerWormholeLocation()), request.chartContext(), layout.featureAnchor(), layout.anomalyIds());
        tagEveryEntity(system, runId);
        request.chartContext().afterGeneration(new ChartGenerated(system, runId, content, request.chartContext()));
        return new ChartSystem(system, layout, content);
    }

    static StarSystemAPI createEmptySystem(SectorAPI sector, String runId) {
        StarSystemAPI system = sector.createStarSystem(ChartIds.SYSTEM_BASE_NAME);
        system.setOptionalUniqueId(ChartIds.systemId(runId));
        system.addTag(ChartIds.SYSTEM_TAG);
        system.addTag(ChartIds.runTag(runId));
        ISOLATION_TAGS.forEach(system::addTag);
        system.getMemoryWithoutUpdate().set(ChartIds.NEX_DO_NOT_COLONIZE, true);
        system.setDoNotShowIntelFromThisLocationOnMap(true);
        Vector2f farAway = systemLocation(runId);
        system.getLocation().set(farAway.x, farAway.y);
        return system;
    }

    public static void ensureIsolationTags(StarSystemAPI system) {
        for (String tag : ISOLATION_TAGS) {
            if (!system.hasTag(tag)) {
                system.addTag(tag);
            }
        }
    }

    static PlanetAPI addFallbackStar(StarSystemAPI system, String runId) {
        system.setBackgroundTextureFilename(SYSTEM_BACKGROUND);
        system.setLightColor(SYSTEM_LIGHT);
        return system.initStar(ChartIds.starId(runId), STAR_TYPE, STAR_RADIUS, CORONA_SIZE);
    }

    public static CustomCampaignEntityAPI addInnerWormhole(StarSystemAPI system, String runId, Vector2f location) {
        CustomCampaignEntityAPI inner = system.addCustomEntity(ChartIds.innerWormholeId(runId),
                Translation.gameText("chart.wormhole.inner.name"), ChartIds.WORMHOLE_TYPE, Factions.NEUTRAL);
        Vector2f placed = innerWormholeSpot(location);
        inner.setFixedLocation(placed.x, placed.y);
        tag(inner, runId);
        inner.addTag(ChartIds.INNER_WORMHOLE_TAG);
        return inner;
    }

    static Vector2f innerWormholeSpot(Vector2f location) {
        return location == null ? new Vector2f(INNER_WORMHOLE_X, INNER_WORMHOLE_Y) : location;
    }

    public static CustomCampaignEntityAPI addOuterWormhole(LocationAPI origin, Vector2f besideHere, String runId) {
        return addOuterWormhole(origin, besideHere, null, runId);
    }

    public static CustomCampaignEntityAPI addOuterWormhole(LocationAPI origin, Vector2f besideHere, SectorEntityToken anchor, String runId) {
        CustomCampaignEntityAPI outer = origin.addCustomEntity(ChartIds.outerWormholeId(runId),
                Translation.gameText("chart.wormhole.outer.name"), ChartIds.WORMHOLE_TYPE, Factions.NEUTRAL);
        if (anchor == null) {
            outer.setFixedLocation(besideHere.x + OUTER_WORMHOLE_OFFSET, besideHere.y);
        } else {
            outer.setCircularOrbit(anchor, angleFrom(anchor.getLocation(), besideHere), anchor.getRadius() + OUTER_WORMHOLE_OFFSET, OUTER_ORBIT_DAYS);
        }
        tag(outer, runId);
        outer.addTag(ChartIds.OUTER_WORMHOLE_TAG);
        return outer;
    }

    static float angleFrom(Vector2f centre, Vector2f point) {
        return (float) Math.toDegrees(Math.atan2(point.y - centre.y, point.x - centre.x));
    }

    public static SectorEntityToken entryPoint(StarSystemAPI system, SectorEntityToken innerWormhole) {
        if (innerWormhole == null) {
            return system.createToken(INNER_WORMHOLE_X + ENTRY_OFFSET, INNER_WORMHOLE_Y);
        }
        Vector2f innerLocation = innerWormhole.getLocation();
        return system.createToken(innerLocation.x + ENTRY_OFFSET, innerLocation.y);
    }

    static Vector2f systemLocation(String runId) {
        int spread = runId.hashCode() & Integer.MAX_VALUE;
        float column = spread % FAR_AWAY_CELLS;
        float row = (spread / FAR_AWAY_CELLS) % FAR_AWAY_CELLS;
        return new Vector2f(FAR_AWAY_CORNER - column * FAR_AWAY_SPACING, FAR_AWAY_CORNER - row * FAR_AWAY_SPACING);
    }

    private static void localiseName(StarSystemAPI system) {
        String localisedName = Translation.gameText("chart.system.name");
        if (localisedName == null || localisedName.isEmpty() || localisedName.equals(ChartIds.SYSTEM_BASE_NAME)) {
            return;
        }
        system.setBaseName(localisedName);
    }

    public record ChartSystem(StarSystemAPI system, ChartLayout layout, ChartContent content) {
    }

    private static void tagEveryEntity(StarSystemAPI system, String runId) {
        for (SectorEntityToken entity : new ArrayList<>(system.getAllEntities())) {
            tag(entity, runId);
        }
    }

    private static void tag(SectorEntityToken entity, String runId) {
        entity.addTag(ChartIds.ENTITY_TAG);
        entity.addTag(ChartIds.runTag(runId));
    }
}
