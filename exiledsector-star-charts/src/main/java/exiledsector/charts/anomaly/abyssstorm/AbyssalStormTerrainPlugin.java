package exiledsector.charts.anomaly.abyssstorm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTerrain;
import exiledsector.i18n.Translation;
import exiledsector.charts.layout.abyss.AbyssArt;
import exiledsector.charts.layout.abyss.AbyssalFogTerrainPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class AbyssalStormTerrainPlugin extends BaseTerrain {

    public static final String TERRAIN_ID = "exiledSector_chart_abyssal_storm";
    static final String MOD_ID = "exiledSector_abyssalStorm";
    static final String THUNDER_SOUND = "terrain_hyperspace_lightning";
    static final float DETECTED_RANGE_MULT_PER_BRIGHTNESS = 1.5f;
    static final float DETECTED_RANGE_FLAT_PER_BRIGHTNESS = 150f;
    static final float MOD_DAYS = 0.1f;
    static final float APPLY_INTERVAL_SECONDS = 0.25f;
    static final float RENDER_RANGE = 100000f;
    static final float FLASH_OVERHANG = 200f;
    static final float TINTED_FLASH_ALPHA = 0.175f;
    static final float WHITE_FLASH_ALPHA = 0.05f;
    static final Color WHITE_FLASH = new Color(255, 220, 220);
    private static final EnumSet<CampaignEngineLayers> LAYERS = EnumSet.of(CampaignEngineLayers.ABOVE);

    public record Params(Color colour) {
    }

    private Color colour;
    private StormCycle cycle;
    private transient Random random;
    private transient float sinceApplied;

    @Override
    public void init(String terrainId, SectorEntityToken entity, Object param) {
        super.init(terrainId, entity, param);
        colour = param instanceof Params params && params.colour() != null ? params.colour() : Color.WHITE;
    }

    private Random random() {
        if (random == null) {
            random = new Random();
        }
        return random;
    }

    @Override
    protected boolean shouldCheckFleetsToApplyEffect() {
        return false;
    }

    @Override
    public void advance(float amount) {
        LocationAPI location = entity == null ? null : entity.getContainingLocation();
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (location == null || player == null || player.getContainingLocation() != location) {
            return;
        }
        if (cycle == null) {
            cycle = new StormCycle(random());
        }
        cycle.advance(amount, random());
        if (cycle.struckThisFrame()) {
            Global.getSoundPlayer().playSound(THUNDER_SOUND, 1f, 1f, player.getLocation(), new Vector2f());
        }
        sinceApplied += amount;
        if (sinceApplied >= APPLY_INTERVAL_SECONDS && cycle.brightness() > 0f) {
            sinceApplied = 0f;
            expose(location, cycle.brightness());
        }
    }

    static void expose(LocationAPI location, float brightness) {
        List<AbyssalFogTerrainPlugin> fog = fogIn(location);
        String description = Translation.gameText("chart.anomaly.abyssal_storm.detected");
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (hiddenInFog(fleet, fog)) {
                continue;
            }
            MutableFleetStatsAPI stats = fleet.getStats();
            stats.addTemporaryModMult(MOD_DAYS, MOD_ID, description, detectedRangeMult(brightness), stats.getDetectedRangeMod());
            stats.addTemporaryModFlat(MOD_DAYS, MOD_ID, description, DETECTED_RANGE_FLAT_PER_BRIGHTNESS * brightness, stats.getDetectedRangeMod());
        }
    }

    static float detectedRangeMult(float brightness) {
        return 1f + DETECTED_RANGE_MULT_PER_BRIGHTNESS * brightness;
    }

    static List<AbyssalFogTerrainPlugin> fogIn(LocationAPI location) {
        List<AbyssalFogTerrainPlugin> fog = new ArrayList<>();
        for (CampaignTerrainAPI terrain : location.getTerrainCopy()) {
            if (terrain.getPlugin() instanceof AbyssalFogTerrainPlugin plugin) {
                fog.add(plugin);
            }
        }
        return fog;
    }

    static boolean hiddenInFog(CampaignFleetAPI fleet, List<AbyssalFogTerrainPlugin> fog) {
        for (AbyssalFogTerrainPlugin plugin : fog) {
            if (plugin.containsEntity(fleet)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return LAYERS;
    }

    @Override
    public float getRenderRange() {
        return RENDER_RANGE;
    }

    @Override
    public boolean containsPoint(Vector2f point, float radius) {
        return false;
    }

    @Override
    public boolean hasTooltip() {
        return false;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (cycle == null || cycle.brightness() <= 0f || layer != CampaignEngineLayers.ABOVE || player == null
                || player.getContainingLocation() != entity.getContainingLocation()) {
            return;
        }
        SpriteAPI thunder = AbyssArt.thunder();
        if (thunder == null) {
            return;
        }
        float left = viewport.getLLX() - FLASH_OVERHANG / 2f;
        float bottom = viewport.getLLY() - FLASH_OVERHANG / 2f;
        thunder.setSize(viewport.getVisibleWidth() + FLASH_OVERHANG, viewport.getVisibleHeight() + FLASH_OVERHANG);
        thunder.setAngle(0f);
        thunder.setNormalBlend();
        thunder.setColor(colour);
        thunder.setAlphaMult(TINTED_FLASH_ALPHA * cycle.brightness() * viewport.getAlphaMult());
        thunder.render(left, bottom);
        thunder.setColor(WHITE_FLASH);
        thunder.setAlphaMult(WHITE_FLASH_ALPHA * cycle.brightness() * viewport.getAlphaMult());
        thunder.render(left, bottom);
    }
}
