package exiledsector.charts;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.i18n.Translation;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class ChartPocketFactory {

    static final List<String> ISOLATION_TAGS = List.of(Tags.THEME_HIDDEN, Tags.THEME_SPECIAL, Tags.TEMPORARY_LOCATION,
            Tags.SYSTEM_CUT_OFF_FROM_HYPER, Tags.DO_NOT_SHOW_STRANDED_DIALOG, Tags.NOT_RANDOM_MISSION_TARGET);
    static final String STAR_TYPE = "star_white";
    static final float STAR_RADIUS = 600f;
    static final float CORONA_SIZE = 300f;
    static final Color POCKET_LIGHT = new Color(205, 215, 255);
    static final String POCKET_BACKGROUND = "graphics/backgrounds/background4.jpg";
    static final float INNER_WORMHOLE_X = 2600f;
    static final float INNER_WORMHOLE_Y = 0f;
    static final float ENTRY_OFFSET = 350f;
    static final float OUTER_WORMHOLE_OFFSET = 250f;
    static final float FAR_AWAY_CORNER = -320000f;
    static final float FAR_AWAY_SPACING = 4000f;
    static final int FAR_AWAY_CELLS = 40;

    private ChartPocketFactory() {
    }

    public static StarSystemAPI createPocket(SectorAPI sector, String runId) {
        StarSystemAPI pocket = sector.createStarSystem(ChartIds.POCKET_BASE_NAME);
        pocket.setOptionalUniqueId(ChartIds.pocketSystemId(runId));
        pocket.addTag(ChartIds.POCKET_TAG);
        pocket.addTag(ChartIds.runTag(runId));
        ISOLATION_TAGS.forEach(pocket::addTag);
        pocket.getMemoryWithoutUpdate().set(ChartIds.NEX_DO_NOT_COLONIZE, true);
        pocket.setDoNotShowIntelFromThisLocationOnMap(true);
        pocket.setBackgroundTextureFilename(POCKET_BACKGROUND);
        pocket.setLightColor(POCKET_LIGHT);
        PlanetAPI star = pocket.initStar(ChartIds.starId(runId), STAR_TYPE, STAR_RADIUS, CORONA_SIZE);
        Vector2f farAway = pocketLocation(runId);
        pocket.getLocation().set(farAway.x, farAway.y);
        localiseName(pocket, star);
        addInnerWormhole(pocket, runId);
        tagEveryEntity(pocket, runId);
        return pocket;
    }

    public static CustomCampaignEntityAPI addInnerWormhole(StarSystemAPI pocket, String runId) {
        CustomCampaignEntityAPI inner = pocket.addCustomEntity(ChartIds.innerWormholeId(runId),
                Translation.gameText("chart.wormhole.inner.name"), ChartIds.WORMHOLE_TYPE, Factions.NEUTRAL);
        inner.setFixedLocation(INNER_WORMHOLE_X, INNER_WORMHOLE_Y);
        tag(inner, runId);
        inner.addTag(ChartIds.INNER_WORMHOLE_TAG);
        return inner;
    }

    public static CustomCampaignEntityAPI addOuterWormhole(LocationAPI origin, Vector2f besideHere, String runId) {
        CustomCampaignEntityAPI outer = origin.addCustomEntity(ChartIds.outerWormholeId(runId),
                Translation.gameText("chart.wormhole.outer.name"), ChartIds.WORMHOLE_TYPE, Factions.NEUTRAL);
        outer.setFixedLocation(besideHere.x + OUTER_WORMHOLE_OFFSET, besideHere.y);
        tag(outer, runId);
        outer.addTag(ChartIds.OUTER_WORMHOLE_TAG);
        return outer;
    }

    public static SectorEntityToken entryPoint(StarSystemAPI pocket, SectorEntityToken innerWormhole) {
        if (innerWormhole == null) {
            return pocket.createToken(INNER_WORMHOLE_X + ENTRY_OFFSET, INNER_WORMHOLE_Y);
        }
        Vector2f innerLocation = innerWormhole.getLocation();
        return pocket.createToken(innerLocation.x + ENTRY_OFFSET, innerLocation.y);
    }

    static Vector2f pocketLocation(String runId) {
        int spread = runId.hashCode() & Integer.MAX_VALUE;
        float column = spread % FAR_AWAY_CELLS;
        float row = (spread / FAR_AWAY_CELLS) % FAR_AWAY_CELLS;
        return new Vector2f(FAR_AWAY_CORNER - column * FAR_AWAY_SPACING, FAR_AWAY_CORNER - row * FAR_AWAY_SPACING);
    }

    private static void localiseName(StarSystemAPI pocket, PlanetAPI star) {
        String localisedName = Translation.gameText("chart.pocket.name");
        if (localisedName == null || localisedName.isEmpty() || localisedName.equals(ChartIds.POCKET_BASE_NAME)) {
            return;
        }
        pocket.setBaseName(localisedName);
        if (star != null) {
            star.setName(localisedName);
        }
    }

    private static void tagEveryEntity(StarSystemAPI pocket, String runId) {
        for (SectorEntityToken entity : new ArrayList<>(pocket.getAllEntities())) {
            tag(entity, runId);
        }
    }

    private static void tag(SectorEntityToken entity, String runId) {
        entity.addTag(ChartIds.ENTITY_TAG);
        entity.addTag(ChartIds.runTag(runId));
    }
}
