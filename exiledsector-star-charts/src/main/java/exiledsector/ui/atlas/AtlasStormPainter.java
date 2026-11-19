package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.anomaly.stormfront.StormFrontTerrainPlugin;
import exiledsector.charts.tree.AtlasBodyArt;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import exiledsector.ui.atlas.AtlasBodyPainter.Placement;
import exiledsector.ui.util.ColorUtil;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

final class AtlasStormPainter {

    private static final String CLOUD_SHEET = StormFrontTerrainPlugin.CLOUD_TEXTURE;
    private static final String FLASH_PATH = StormFrontTerrainPlugin.FLASH_TEXTURE;
    private static final String GLOW_PATH = "graphics/fx/hit_glow.png";
    private static final int SHEET_SIDE = AtlasSystemArt.CLOUD_SHEET_SIDE;
    private static final float ANOMALY_SHARE_OF_CLOUDS = 0.4f;
    private static final float SOLAR_ANOMALY_SHARE = 0.25f;
    private static final Color STORM_CLOUD = AtlasSystemArt.STORM_TINT;
    private static final Color SOLAR_CLOUD = new Color(255, 110, 60);
    private static final Color LIGHTNING = AtlasSystemArt.LIGHTNING_COLOUR;
    private static final Color GOLD_LIGHTNING = new Color(255, 205, 100);
    private static final Color CALM_GLOW = new Color(200, 225, 255);
    private static final float CLOUD_ALPHA = 0.6f;
    private static final float PLANET_CLOUD_ALPHA = 0.5f;
    private static final float FLASH_SIZE = 1.8f;
    private static final float FLASH_GLOW_SHARE = 0.5f;

    private static final int CELL_PUFFS = 5;
    private static final float CELL_REACH = 0.45f;
    private static final float CELL_REACH_WOBBLE = 0.15f;
    private static final float CELL_PUFF_SIZE = 1.15f;
    private static final float CELL_TURN_DEGREES_PER_SECOND = 18f;
    private static final float CELL_FLASH_SECONDS = 1.1f;

    private static final int SCATTERED_PUFFS = 6;
    private static final float SCATTERED_INNER = 0.2f;
    private static final float SCATTERED_SPREAD = 0.42f;
    private static final float SCATTERED_PUFF_SIZE = 0.62f;
    private static final float SCATTERED_TURN_DEGREES_PER_SECOND = 4f;
    private static final float SCATTERED_FLASH_SECONDS = 1.4f;
    private static final float SCATTERED_FLASH_SIZE = 0.9f;

    private static final float ROD_REACH = 0.38f;
    private static final float ROD_STRIKE_SECONDS = 0.7f;
    private static final float ROD_CHANCE_MULT = 1.6f;
    private static final float ROD_BOLT_REACH = 0.6f;
    private static final int ROD_BOLT_POINTS = 7;
    private static final float ROD_BOLT_JITTER = 0.07f;
    private static final float ROD_BOLT_WIDTH = 0.025f;
    private static final float ROD_STRIKE_SIZE = 0.7f;
    private static final float ROD_EMBER_SIZE = 0.35f;
    private static final float ROD_EMBER_ALPHA = 0.3f;

    private static final int SUPERCELL_BACKGROUND_PUFFS = 4;
    private static final int SUPERCELL_MASS_PUFFS = 3;
    private static final float SUPERCELL_MASS_REACH = 0.28f;
    private static final float SUPERCELL_MASS_SPREAD = 0.18f;
    private static final float SUPERCELL_MASS_PUFF_SIZE = 0.85f;
    private static final float SUPERCELL_TURN_DEGREES_PER_SECOND = 30f;
    private static final float SUPERCELL_SPIN_DEGREES_PER_SECOND = 45f;
    private static final float SUPERCELL_FLASH_SECONDS = 0.9f;
    private static final float SUPERCELL_CHANCE_MULT = 1.3f;

    private static final int EYE_PUFFS = 9;
    private static final float EYE_OFFSET = 0.18f;
    private static final float EYE_RING = 0.45f;
    private static final float EYE_PUFF_SIZE = 0.42f;
    private static final float EYE_TURN_DEGREES_PER_SECOND = 6f;
    private static final float EYE_FLASH_SECONDS = 1.6f;
    private static final float EYE_CHANCE_MULT = 0.8f;
    private static final float EYE_CALM_SIZE = 0.45f;
    private static final float EYE_CALM_ALPHA = 0.2f;

    private static final float CAPTURED_ORBIT = 1.45f;
    private static final float CAPTURED_CELL = 0.3f;
    private static final float CAPTURED_ORBIT_DEGREES_PER_SECOND = 14f;

    private static final int SOLAR_OUTER_PUFFS = 8;
    private static final int SOLAR_INNER_PUFFS = 3;
    private static final float SOLAR_OUTER_REACH = 1.05f;
    private static final float SOLAR_INNER_REACH = 0.5f;
    private static final float SOLAR_REACH_WOBBLE = 0.12f;
    private static final float SOLAR_PUFF_SIZE = 0.6f;
    private static final float SOLAR_INNER_ALPHA = 0.55f;
    private static final float SOLAR_SWIRL_DEGREES_PER_SECOND = 20f;
    private static final float SOLAR_FLASH_SECONDS = 1f;

    private static final float DUEL_REACH = 0.5f;
    private static final float DUEL_CELL = 0.38f;
    private static final float DUEL_TURN_DEGREES_PER_SECOND = 3f;
    private static final float DUEL_FLASH_SECONDS = 0.8f;
    private static final float DUEL_CHANCE_MULT = 1.4f;

    private final SpriteCache sprites = new SpriteCache(AtlasStormPainter.class);
    private final Map<Color, Color> stormCloudByColour = new HashMap<>();
    private final Map<Color, Color> solarCloudByColour = new HashMap<>();
    private SpriteAPI cloudSheet;
    private Color cloudTint;
    private Color flashColour;
    private float cloudAlpha;
    private float flashAlpha;
    private float flashChance;
    private float stormSeconds;
    private float seconds;
    private int seed;
    private float flashX;
    private float flashY;
    private boolean flashPlaced;

    void paintCell(Color colour, Placement placement, Lighting lighting, float realSeconds, float alpha) {
        if (!begin(AtlasBodyArt.Storm.GENERIC, colour, placement, lighting, realSeconds, alpha)) {
            return;
        }
        cloudAlpha = alpha * CLOUD_ALPHA * StormWeather.cloudShare(lighting);
        paintOwnCell(placement.screenX(), placement.screenY(), placement.screenRadius(), seed);
    }

    void paintOnPlanet(AtlasBodyArt.Storm storm, Color colour, Placement placement, Lighting lighting, float realSeconds, float alpha) {
        if (storm == AtlasBodyArt.Storm.NONE || !begin(storm, colour, placement, lighting, realSeconds, alpha)) {
            return;
        }
        float centreX = placement.screenX();
        float centreY = placement.screenY();
        float radius = placement.screenRadius();
        switch (storm) {
            case LIGHTNING_ROD -> paintLightningRod(centreX, centreY, radius);
            case SUPERCELL -> paintSupercell(centreX, centreY, radius);
            case EYE -> paintEye(centreX, centreY, radius);
            case CAPTURED -> paintCaptured(centreX, centreY, radius);
            case SOLAR -> paintSolar(centreX, centreY, radius);
            case DUEL -> paintDuel(centreX, centreY, radius);
            default -> paintScattered(centreX, centreY, radius, SCATTERED_PUFFS, true);
        }
    }

    private boolean begin(AtlasBodyArt.Storm storm, Color colour, Placement placement, Lighting lighting, float realSeconds, float alpha) {
        cloudSheet = sprites.sprite(CLOUD_SHEET);
        if (cloudSheet == null || alpha <= 0f) {
            return false;
        }
        boolean solar = storm == AtlasBodyArt.Storm.SOLAR;
        Color litCloud = solar ? solarCloudByColour.computeIfAbsent(colour, AtlasStormPainter::solarCloudOf)
                : stormCloudByColour.computeIfAbsent(colour, AtlasStormPainter::stormCloudOf);
        cloudTint = AtlasBodyPainter.tintFor(lighting, litCloud);
        cloudAlpha = alpha * PLANET_CLOUD_ALPHA * StormWeather.cloudShare(lighting);
        flashColour = solar ? GOLD_LIGHTNING : LIGHTNING;
        flashAlpha = alpha * StormWeather.flashShare(lighting);
        flashChance = StormWeather.flashChance(lighting);
        stormSeconds = placement.stormSeconds();
        seconds = realSeconds;
        seed = (int) placement.seedDegrees();
        return true;
    }

    private static Color stormCloudOf(Color colour) {
        return ColorUtil.mix(STORM_CLOUD, colour, ANOMALY_SHARE_OF_CLOUDS);
    }

    private static Color solarCloudOf(Color colour) {
        return ColorUtil.mix(SOLAR_CLOUD, colour, SOLAR_ANOMALY_SHARE);
    }

    private void paintOwnCell(float centreX, float centreY, float cellRadius, int cellSeed) {
        int slot = StormWeather.slotOf(cellSeed, seconds, CELL_FLASH_SECONDS);
        paintCellPuffs(centreX, centreY, cellRadius, cellSeed, StormWeather.flash(cellSeed, seconds, CELL_FLASH_SECONDS, flashChance), slot);
    }

    private void paintCellPuffs(float centreX, float centreY, float cellRadius, int cellSeed, float flashStrength, int flashSlot) {
        int flashing = flashStrength > 0f ? StormWeather.pick(cellSeed, flashSlot, CELL_PUFFS) : -1;
        flashPlaced = false;
        for (int puff = 0; puff < CELL_PUFFS; puff++) {
            double angle = Math.toRadians(cellSeed + puff * 360f / CELL_PUFFS + stormSeconds * CELL_TURN_DEGREES_PER_SECOND);
            float reach = cellRadius * (CELL_REACH + CELL_REACH_WOBBLE * (float) Math.sin(puff * 1.9f + stormSeconds * 0.7f));
            float puffX = centreX + (float) Math.cos(angle) * reach;
            float puffY = centreY + (float) Math.sin(angle) * reach;
            puff(cellSeed + puff, puffX, puffY, cellRadius * CELL_PUFF_SIZE, puff * 53f + stormSeconds * 10f, cloudAlpha);
            if (puff == flashing) {
                markFlash(puffX, puffY);
            }
        }
        if (flashPlaced) {
            flash(flashX, flashY, cellRadius * FLASH_SIZE, flashStrength);
        }
    }

    private void paintScattered(float centreX, float centreY, float radius, int puffs, boolean flashes) {
        float strength = flashes ? StormWeather.flash(seed, seconds, SCATTERED_FLASH_SECONDS, flashChance) : 0f;
        int flashing = strength > 0f ? StormWeather.pick(seed, StormWeather.slotOf(seed, seconds, SCATTERED_FLASH_SECONDS), puffs) : -1;
        flashPlaced = false;
        for (int puff = 0; puff < puffs; puff++) {
            float reach = radius * (SCATTERED_INNER + SCATTERED_SPREAD * StormWeather.unit(seed, puff, 2));
            double angle = Math.toRadians(seed * 7f + puff * 360f / puffs + StormWeather.signedUnit(seed, puff, 3) * 25f
                    + stormSeconds * SCATTERED_TURN_DEGREES_PER_SECOND);
            float puffX = centreX + (float) Math.cos(angle) * reach;
            float puffY = centreY + (float) Math.sin(angle) * reach;
            puff(seed + puff, puffX, puffY, radius * SCATTERED_PUFF_SIZE, puff * 61f + stormSeconds * 3f, cloudAlpha);
            if (puff == flashing) {
                markFlash(puffX, puffY);
            }
        }
        if (flashPlaced) {
            flash(flashX, flashY, radius * SCATTERED_FLASH_SIZE, strength);
        }
    }

    private void paintLightningRod(float centreX, float centreY, float radius) {
        paintScattered(centreX, centreY, radius, SCATTERED_PUFFS - 1, false);
        double rodAngle = Math.toRadians(seed * 3f + 40f);
        float rodX = centreX + (float) Math.cos(rodAngle) * radius * ROD_REACH;
        float rodY = centreY + (float) Math.sin(rodAngle) * radius * ROD_REACH;
        float ember = ROD_EMBER_ALPHA * (0.8f + 0.2f * (float) Math.sin(seconds * 3f));
        float emberSize = radius * ROD_EMBER_SIZE;
        SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_PATH, rodX, rodY, emberSize, emberSize, flashColour, flashAlpha * ember);
        float strength = StormWeather.flash(seed, seconds, ROD_STRIKE_SECONDS, Math.min(1f, flashChance * ROD_CHANCE_MULT));
        if (strength <= 0f) {
            return;
        }
        int slot = StormWeather.slotOf(seed, seconds, ROD_STRIKE_SECONDS);
        double fromAngle = StormWeather.unit(seed, slot, 4) * Math.PI * 2.0;
        float fromX = rodX + (float) Math.cos(fromAngle) * radius * ROD_BOLT_REACH;
        float fromY = rodY + (float) Math.sin(fromAngle) * radius * ROD_BOLT_REACH;
        bolt(fromX, fromY, rodX, rodY, radius, slot, strength);
        flash(rodX, rodY, radius * ROD_STRIKE_SIZE, strength);
    }

    private void bolt(float fromX, float fromY, float toX, float toY, float radius, int slot, float strength) {
        float acrossX = -(toY - fromY);
        float acrossY = toX - fromX;
        try (GlScope scope = GlScope.flat().additive()) {
            GL11.glLineWidth(Math.max(1f, radius * ROD_BOLT_WIDTH));
            Misc.setColor(flashColour, flashAlpha * strength);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int point = 0; point < ROD_BOLT_POINTS; point++) {
                float share = point / (float) (ROD_BOLT_POINTS - 1);
                boolean end = point == 0 || point == ROD_BOLT_POINTS - 1;
                float jitter = end ? 0f : StormWeather.signedUnit(seed, slot, 10 + point) * ROD_BOLT_JITTER / ROD_BOLT_REACH;
                GL11.glVertex2f(fromX + (toX - fromX) * share + acrossX * jitter, fromY + (toY - fromY) * share + acrossY * jitter);
            }
            GL11.glEnd();
        }
    }

    private void paintSupercell(float centreX, float centreY, float radius) {
        paintScattered(centreX, centreY, radius, SUPERCELL_BACKGROUND_PUFFS, false);
        double massAngle = Math.toRadians(seed + stormSeconds * SUPERCELL_TURN_DEGREES_PER_SECOND);
        float massX = centreX + (float) Math.cos(massAngle) * radius * SUPERCELL_MASS_REACH;
        float massY = centreY + (float) Math.sin(massAngle) * radius * SUPERCELL_MASS_REACH;
        float strength = StormWeather.flash(seed, seconds, SUPERCELL_FLASH_SECONDS, Math.min(1f, flashChance * SUPERCELL_CHANCE_MULT));
        int slot = StormWeather.slotOf(seed, seconds, SUPERCELL_FLASH_SECONDS);
        int flashing = strength > 0f ? StormWeather.pick(seed, slot, SUPERCELL_MASS_PUFFS) : -1;
        float massSpin = stormSeconds * SUPERCELL_SPIN_DEGREES_PER_SECOND;
        flashPlaced = false;
        for (int puff = 0; puff < SUPERCELL_MASS_PUFFS; puff++) {
            double angle = massAngle * 2.0 + Math.toRadians(puff * 360f / SUPERCELL_MASS_PUFFS);
            float puffX = massX + (float) Math.cos(angle) * radius * SUPERCELL_MASS_SPREAD;
            float puffY = massY + (float) Math.sin(angle) * radius * SUPERCELL_MASS_SPREAD;
            puff(seed + puff + 1, puffX, puffY, radius * SUPERCELL_MASS_PUFF_SIZE, puff * 120f + massSpin, cloudAlpha);
            if (puff == flashing) {
                markFlash(puffX, puffY);
            }
        }
        if (flashPlaced) {
            flash(flashX, flashY, radius * SCATTERED_FLASH_SIZE, strength);
        }
    }

    private void paintEye(float centreX, float centreY, float radius) {
        double eyeAngle = Math.toRadians(seed * 5f);
        float eyeX = centreX + (float) Math.cos(eyeAngle) * radius * EYE_OFFSET;
        float eyeY = centreY + (float) Math.sin(eyeAngle) * radius * EYE_OFFSET;
        float calm = EYE_CALM_ALPHA * (0.85f + 0.15f * (float) Math.sin(stormSeconds * 0.8f));
        float calmSize = radius * EYE_CALM_SIZE;
        SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_PATH, eyeX, eyeY, calmSize, calmSize, CALM_GLOW, flashAlpha * calm);
        float strength = StormWeather.flash(seed, seconds, EYE_FLASH_SECONDS, flashChance * EYE_CHANCE_MULT);
        int flashing = strength > 0f ? StormWeather.pick(seed, StormWeather.slotOf(seed, seconds, EYE_FLASH_SECONDS), EYE_PUFFS) : -1;
        flashPlaced = false;
        for (int puff = 0; puff < EYE_PUFFS; puff++) {
            double angle = Math.toRadians(puff * 360f / EYE_PUFFS + stormSeconds * EYE_TURN_DEGREES_PER_SECOND);
            float reach = radius * EYE_RING * (1f + 0.06f * StormWeather.signedUnit(seed, puff, 5));
            float puffX = eyeX + (float) Math.cos(angle) * reach;
            float puffY = eyeY + (float) Math.sin(angle) * reach;
            puff(seed + puff, puffX, puffY, radius * EYE_PUFF_SIZE, puff * 41f + stormSeconds * 5f, cloudAlpha);
            if (puff == flashing) {
                markFlash(puffX, puffY);
            }
        }
        if (flashPlaced) {
            flash(flashX, flashY, radius * EYE_PUFF_SIZE * FLASH_SIZE, strength);
        }
    }

    private void paintCaptured(float centreX, float centreY, float radius) {
        double orbitAngle = Math.toRadians(seed + stormSeconds * CAPTURED_ORBIT_DEGREES_PER_SECOND);
        float cellX = centreX + (float) Math.cos(orbitAngle) * radius * CAPTURED_ORBIT;
        float cellY = centreY + (float) Math.sin(orbitAngle) * radius * CAPTURED_ORBIT;
        paintOwnCell(cellX, cellY, radius * CAPTURED_CELL, seed);
    }

    private void paintSolar(float centreX, float centreY, float radius) {
        float strength = StormWeather.flash(seed, seconds, SOLAR_FLASH_SECONDS, flashChance);
        int flashing = strength > 0f ? StormWeather.pick(seed, StormWeather.slotOf(seed, seconds, SOLAR_FLASH_SECONDS), SOLAR_OUTER_PUFFS) : -1;
        float swirl = stormSeconds * SOLAR_SWIRL_DEGREES_PER_SECOND;
        for (int puff = 0; puff < SOLAR_INNER_PUFFS; puff++) {
            double angle = Math.toRadians(seed - puff * 360f / SOLAR_INNER_PUFFS - swirl * 0.6f);
            float reach = radius * SOLAR_INNER_REACH;
            puff(seed + puff + SOLAR_OUTER_PUFFS, centreX + (float) Math.cos(angle) * reach, centreY + (float) Math.sin(angle) * reach,
                    radius * SOLAR_PUFF_SIZE, puff * 77f - swirl, cloudAlpha * SOLAR_INNER_ALPHA);
        }
        flashPlaced = false;
        for (int puff = 0; puff < SOLAR_OUTER_PUFFS; puff++) {
            double angle = Math.toRadians(seed + puff * 360f / SOLAR_OUTER_PUFFS + swirl);
            float reach = radius * (SOLAR_OUTER_REACH + SOLAR_REACH_WOBBLE * (float) Math.sin(puff * 2.3f + stormSeconds));
            float puffX = centreX + (float) Math.cos(angle) * reach;
            float puffY = centreY + (float) Math.sin(angle) * reach;
            puff(seed + puff, puffX, puffY, radius * SOLAR_PUFF_SIZE, puff * 45f + swirl, cloudAlpha);
            if (puff == flashing) {
                markFlash(puffX, puffY);
            }
        }
        if (flashPlaced) {
            flash(flashX, flashY, radius * SOLAR_PUFF_SIZE * FLASH_SIZE, strength);
        }
    }

    private void paintDuel(float centreX, float centreY, float radius) {
        double axis = Math.toRadians(seed + stormSeconds * DUEL_TURN_DEGREES_PER_SECOND);
        float offsetX = (float) Math.cos(axis) * radius * DUEL_REACH;
        float offsetY = (float) Math.sin(axis) * radius * DUEL_REACH;
        int slot = StormWeather.slotOf(seed, seconds, DUEL_FLASH_SECONDS);
        float strength = StormWeather.flash(seed, seconds, DUEL_FLASH_SECONDS, Math.min(1f, flashChance * DUEL_CHANCE_MULT));
        int striking = Math.floorMod(slot, 2);
        for (int side = 0; side < 2; side++) {
            float direction = side == 0 ? 1f : -1f;
            paintCellPuffs(centreX + offsetX * direction, centreY + offsetY * direction, radius * DUEL_CELL, seed + side * 97,
                    side == striking ? strength : 0f, slot);
        }
    }

    private void puff(int cell, float puffX, float puffY, float size, float angleDegrees, float puffAlpha) {
        SpriteDraw.drawCellAtCenter(cloudSheet, SHEET_SIDE, SHEET_SIDE, cell, puffX, puffY, size, size, angleDegrees, cloudTint, puffAlpha, false);
    }

    private void markFlash(float atX, float atY) {
        flashX = atX;
        flashY = atY;
        flashPlaced = true;
    }

    private void flash(float atX, float atY, float size, float strength) {
        float flashStrength = flashAlpha * strength;
        if (flashStrength <= 0f) {
            return;
        }
        SpriteDraw.drawAdditiveAtCenter(sprites, FLASH_PATH, atX, atY, size, size, flashColour, flashStrength);
        float glow = size * FLASH_GLOW_SHARE;
        SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_PATH, atX, atY, glow, glow, flashColour, flashStrength);
    }
}
