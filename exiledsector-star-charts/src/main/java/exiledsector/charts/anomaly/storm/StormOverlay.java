package exiledsector.charts.anomaly.storm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

final class StormOverlay extends BaseCombatLayeredRenderingPlugin {

    static final String HIT_GLOW_TEXTURE = "graphics/fx/hit_glow.png";
    static final int CLOUD_COUNT = 10;
    static final float MIN_CLOUD_SIZE = 1500f;
    static final float MAX_CLOUD_SIZE = 2500f;
    static final float CLOUD_SPEED = 20f;
    static final float CLOUD_ALPHA = 0.12f;
    static final float FLASH_SECONDS = 0.4f;
    static final float FLASH_SIZE = 500f;
    static final float GLOW_SIZE = 200f;
    static final float RENDER_RADIUS = 1_000_000f;
    static final Color CLOUD_COLOUR = StormFrontTerrainPlugin.CLOUD_COLOUR;
    static final Color FLASH_COLOUR = StormFrontTerrainPlugin.FLASH_COLOUR;
    private static final Logger LOG = Logger.getLogger(StormOverlay.class);

    static final class Cloud {
        final float size;
        final int cell;
        final float facing;
        float x;
        float y;

        Cloud(float x, float y, float size, int cell, float facing) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.cell = cell;
            this.facing = facing;
        }
    }

    static final class Flash {
        final Vector2f point;
        float age;

        Flash(Vector2f point) {
            this.point = point;
        }
    }

    private final float mapWidth;
    private final float mapHeight;
    private final float driftX;
    private final float driftY;
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Flash> flashes = new ArrayList<>();
    private final Vector2f scratchPosition = new Vector2f();
    private SpriteAPI[] cloudCells;
    private SpriteAPI flashSprite;
    private SpriteAPI glowSprite;
    private boolean spritesFailed;

    StormOverlay(float mapWidth, float mapHeight, long seed) {
        this.mapWidth = Math.max(1f, mapWidth);
        this.mapHeight = Math.max(1f, mapHeight);
        Random random = new Random(seed);
        double angle = random.nextDouble() * Math.PI * 2d;
        driftX = (float) Math.cos(angle) * CLOUD_SPEED;
        driftY = (float) Math.sin(angle) * CLOUD_SPEED;
        for (int index = 0; index < CLOUD_COUNT; index++) {
            clouds.add(new Cloud((random.nextFloat() - 0.5f) * this.mapWidth, (random.nextFloat() - 0.5f) * this.mapHeight,
                    MIN_CLOUD_SIZE + random.nextFloat() * (MAX_CLOUD_SIZE - MIN_CLOUD_SIZE),
                    random.nextInt(StormFrontTerrainPlugin.SHEET_COLUMNS * StormFrontTerrainPlugin.SHEET_ROWS), random.nextFloat() * 360f));
        }
    }

    void flash(Vector2f point) {
        flashes.add(new Flash(new Vector2f(point)));
    }

    int flashCount() {
        return flashes.size();
    }

    List<Cloud> clouds() {
        return clouds;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER, CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
    }

    @Override
    public float getRenderRadius() {
        return RENDER_RADIUS;
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && engine.isPaused() || amount <= 0f) {
            return;
        }
        drift(amount);
        for (Iterator<Flash> iterator = flashes.iterator(); iterator.hasNext(); ) {
            Flash flash = iterator.next();
            flash.age += amount;
            if (flash.age >= FLASH_SECONDS) {
                iterator.remove();
            }
        }
    }

    void drift(float seconds) {
        float halfWidth = mapWidth / 2f;
        float halfHeight = mapHeight / 2f;
        for (Cloud cloud : clouds) {
            cloud.x = wrap(cloud.x + driftX * seconds, halfWidth + cloud.size / 2f);
            cloud.y = wrap(cloud.y + driftY * seconds, halfHeight + cloud.size / 2f);
        }
    }

    static float wrap(float value, float halfSpan) {
        float span = halfSpan * 2f;
        float shifted = (value + halfSpan) % span;
        return (shifted < 0f ? shifted + span : shifted) - halfSpan;
    }

    private boolean ensureSprites() {
        if (cloudCells != null) {
            return true;
        }
        if (spritesFailed) {
            return false;
        }
        try {
            int cellCount = StormFrontTerrainPlugin.SHEET_COLUMNS * StormFrontTerrainPlugin.SHEET_ROWS;
            SpriteAPI[] cells = new SpriteAPI[cellCount];
            for (int cell = 0; cell < cellCount; cell++) {
                SpriteAPI sheet = Global.getSettings().getSprite(StormFrontTerrainPlugin.CLOUD_TEXTURE);
                float cellWidth = sheet.getTextureWidth() / StormFrontTerrainPlugin.SHEET_COLUMNS;
                float cellHeight = sheet.getTextureHeight() / StormFrontTerrainPlugin.SHEET_ROWS;
                sheet.setTexX(cell % StormFrontTerrainPlugin.SHEET_COLUMNS * cellWidth);
                sheet.setTexY(cell / StormFrontTerrainPlugin.SHEET_COLUMNS * cellHeight);
                sheet.setTexWidth(cellWidth);
                sheet.setTexHeight(cellHeight);
                cells[cell] = sheet;
            }
            flashSprite = Global.getSettings().getSprite(StormFrontTerrainPlugin.FLASH_TEXTURE);
            glowSprite = Global.getSettings().getSprite(HIT_GLOW_TEXTURE);
            cloudCells = cells;
            return true;
        } catch (RuntimeException e) {
            spritesFailed = true;
            LOG.error("[ExiledSector] The Storm Front overlay could not load its sprites", e);
            return false;
        }
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        float alpha = viewport.getAlphaMult();
        if (alpha <= 0f || !ensureSprites()) {
            return;
        }
        if (layer == CombatEngineLayers.BELOW_SHIPS_LAYER) {
            renderClouds(viewport, alpha);
        } else if (layer == CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER) {
            renderFlashes(viewport, alpha);
        }
    }

    private void renderClouds(ViewportAPI viewport, float alpha) {
        for (Cloud cloud : clouds) {
            scratchPosition.set(cloud.x, cloud.y);
            if (!viewport.isNearViewport(scratchPosition, cloud.size)) {
                continue;
            }
            SpriteAPI sprite = cloudCells[cloud.cell];
            sprite.setSize(cloud.size, cloud.size);
            sprite.setAngle(cloud.facing);
            sprite.setColor(CLOUD_COLOUR);
            sprite.setAdditiveBlend();
            sprite.setAlphaMult(alpha * CLOUD_ALPHA);
            sprite.renderAtCenter(cloud.x, cloud.y);
        }
    }

    private void renderFlashes(ViewportAPI viewport, float alpha) {
        for (Flash flash : flashes) {
            if (!viewport.isNearViewport(flash.point, FLASH_SIZE)) {
                continue;
            }
            float strength = 1f - flash.age / FLASH_SECONDS;
            flashSprite.setSize(FLASH_SIZE, FLASH_SIZE);
            flashSprite.setColor(FLASH_COLOUR);
            flashSprite.setAdditiveBlend();
            flashSprite.setAlphaMult(alpha * strength);
            flashSprite.renderAtCenter(flash.point.x, flash.point.y);
            glowSprite.setSize(GLOW_SIZE, GLOW_SIZE);
            glowSprite.setColor(FLASH_COLOUR);
            glowSprite.setAdditiveBlend();
            glowSprite.setAlphaMult(alpha * strength);
            glowSprite.renderAtCenter(flash.point.x, flash.point.y);
        }
    }
}
