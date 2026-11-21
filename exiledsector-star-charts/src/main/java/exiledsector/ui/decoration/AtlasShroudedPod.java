package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class AtlasShroudedPod {

    private static final String SHEET_PATH = ShroudedMawRenderer.SHROUD_SHEET_PATH;
    private static final String HALO_PATH = "graphics/fx/hit_glow.png";
    private static final int SHEET_SIDE = ShroudedMawRenderer.SHEET_SIDE;
    private static final float BODY_SCALE = 0.7f;
    private static final float MIN_LOOP_REACH = 0.35f;
    private static final float LOOP_REACH_SPREAD = 0.45f;
    private static final float MIN_LOOP_RAD_PER_SECOND = 0.05f;
    private static final float LOOP_RATE_SPREAD = 0.04f;
    private static final float SWAY_DEGREES = 5f;
    private static final float SWAY_RAD_PER_SECOND = 1.4f;
    private static final int CLOUD_PUFFS = 30;
    private static final int DARK_PUFFS = 12;
    private static final float CLOUD_RADIUS = 0.55f;
    private static final float CLOUD_PUFF_SIZE = 0.6f;
    private static final float DARK_PUFF_SIZE = 0.45f;
    private static final float DARK_RADIUS = 0.75f;
    private static final float MIN_PUFF_SECONDS = 2f;
    private static final float PUFF_SECONDS_SPREAD = 3f;
    private static final float MIN_DARK_SECONDS = 2f;
    private static final float DARK_SECONDS_SPREAD = 2f;
    private static final float MAX_SPIN_DEGREES_PER_SECOND = 90f;
    private static final float MIN_CLOUD_LAG_SECONDS = 0.6f;
    private static final float CLOUD_LAG_SECONDS = 2.4f;
    private static final float MIN_DARK_LAG_SECONDS = 1.5f;
    private static final float DARK_LAG_SECONDS = 3f;
    private static final float FLASH_RISE_SECONDS = 1f;
    private static final float FLASH_FALL_SECONDS = 4f;
    private static final float FLASH_REST_SECONDS = 3f;
    private static final float HALO_SIZE = 1.6f;
    private static final float HALO_ALPHA = 0.5f;
    private static final float CLOUD_ALPHA = 0.85f;
    private static final float DARK_ALPHA = 0.8f;
    private static final float BODY_ALPHA = 0.95f;
    private static final int WARP_VERTICES = 5;
    private static final float WARP_SHARE = 0.04f;
    private static final float MIN_WARP_RATE = 0.9f;
    private static final float WARP_RATE_SPREAD = 1.2f;
    private static final Color SHROUD = new Color(100, 0, 25);
    private static final Color FLASH = new Color(255, 0, 64);
    private static final Color HALO = new Color(150, 0, 30);
    private static final Color DARKNESS = new Color(6, 0, 3);
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private final SpriteCache spriteCache;
    private final TexturedQuadBatch quadBatch = new TexturedQuadBatch();
    private final List<Swimmer> swimmers = new ArrayList<>();
    private final Pose pose = new Pose();

    private record Swimmer(int index, String spritePath, float width, float height, float bodySize, float loopReach, float loopRate,
                           float loopPhase, float loopTilt, float loopTurn) {
    }

    private static final class Pose {
        private float x;
        private float y;
        private float facingDeg;
    }

    AtlasShroudedPod(SpriteCache spriteCache, List<ShipHullSpecAPI> hulls, float radius, Random random) {
        this.spriteCache = spriteCache;
        for (ShipHullSpecAPI hull : hulls) {
            String path = hull.getSpriteName();
            SpriteAPI texture = path == null ? null : spriteCache.texture(path);
            if (texture == null || texture.getWidth() <= 0f || texture.getHeight() <= 0f) {
                continue;
            }
            float scale = FleetShipDrift.HullMotion.of(hull.getHullSize()).scaleMult() * AtlasShipGroup.SHIP_SCALE * BODY_SCALE;
            float bodySize = Math.max(texture.getWidth(), texture.getHeight()) * scale;
            swimmers.add(new Swimmer(swimmers.size(), path, texture.getWidth() * scale, texture.getHeight() * scale, bodySize,
                    radius * (MIN_LOOP_REACH + LOOP_REACH_SPREAD * random.nextFloat()), MIN_LOOP_RAD_PER_SECOND + LOOP_RATE_SPREAD * random.nextFloat(),
                    TWO_PI * random.nextFloat(), TWO_PI * random.nextFloat(), random.nextBoolean() ? 1f : -1f));
        }
    }

    void render(float screenX, float screenY, float zoom, float seconds, float alpha) {
        if (alpha <= 0f || swimmers.isEmpty()) {
            return;
        }
        SpriteAPI sheet = spriteCache.texture(SHEET_PATH);
        if (sheet != null) {
            try (TexturedQuadBatch quads = quadBatch.beginNormal(sheet)) {
                for (Swimmer swimmer : swimmers) {
                    drawCloud(quads, swimmer, screenX, screenY, zoom, seconds, alpha, false);
                }
            }
            try (TexturedQuadBatch quads = quadBatch.beginAdditive(sheet)) {
                for (Swimmer swimmer : swimmers) {
                    drawCloud(quads, swimmer, screenX, screenY, zoom, seconds, alpha, true);
                }
            }
        }
        SpriteAPI halo = spriteCache.texture(HALO_PATH);
        if (halo != null) {
            try (TexturedQuadBatch quads = quadBatch.beginAdditive(halo)) {
                for (Swimmer swimmer : swimmers) {
                    drawHalos(quads, swimmer, screenX, screenY, zoom, seconds, alpha);
                }
            }
        }
        for (Swimmer swimmer : swimmers) {
            SpriteAPI body = spriteCache.texture(swimmer.spritePath());
            if (body != null) {
                poseAt(swimmer, seconds);
                try (TexturedQuadBatch quads = quadBatch.beginNormal(body)) {
                    drawBody(quads, swimmer, screenX + pose.x * zoom, screenY - pose.y * zoom, zoom, pose.facingDeg, seconds, alpha * BODY_ALPHA);
                }
            }
        }
        if (sheet != null) {
            try (TexturedQuadBatch quads = quadBatch.beginNormal(sheet)) {
                for (Swimmer swimmer : swimmers) {
                    drawDark(quads, swimmer, screenX, screenY, zoom, seconds, alpha);
                }
            }
        }
    }

    private void poseAt(Swimmer swimmer, float seconds) {
        float phase = swimmer.loopPhase() + swimmer.loopTurn() * swimmer.loopRate() * seconds;
        float localX = (float) Math.cos(phase) * swimmer.loopReach();
        float localY = (float) Math.sin(phase * 2f) * swimmer.loopReach() * 0.5f;
        float velocityX = -(float) Math.sin(phase) * swimmer.loopReach() * swimmer.loopTurn();
        float velocityY = (float) Math.cos(phase * 2f) * swimmer.loopReach() * swimmer.loopTurn();
        float tiltCos = (float) Math.cos(swimmer.loopTilt());
        float tiltSin = (float) Math.sin(swimmer.loopTilt());
        pose.x = localX * tiltCos - localY * tiltSin;
        pose.y = localX * tiltSin + localY * tiltCos;
        float headingX = velocityX * tiltCos - velocityY * tiltSin;
        float headingY = velocityX * tiltSin + velocityY * tiltCos;
        float sway = SWAY_DEGREES * (float) Math.sin(seconds * SWAY_RAD_PER_SECOND + swimmer.loopPhase());
        pose.facingDeg = (float) Math.toDegrees(Math.atan2(headingY, headingX)) + sway;
    }

    private void drawCloud(TexturedQuadBatch quads, Swimmer swimmer, float screenX, float screenY, float zoom, float seconds, float alpha,
                           boolean flashes) {
        for (int index = 0; index < CLOUD_PUFFS; index++) {
            float lifeSeconds = MIN_PUFF_SECONDS + PUFF_SECONDS_SPREAD * hash(swimmer, index, 0, 1);
            float aged = seconds / lifeSeconds + hash(swimmer, index, 0, 2);
            int life = (int) Math.floor(aged);
            float age = aged - life;
            float fade = (float) Math.sin(Math.PI * age);
            float flash = flashes ? flash(swimmer, index, seconds) : 1f;
            if (fade <= 0f || flash <= 0f) {
                continue;
            }
            poseAt(swimmer, seconds - (MIN_CLOUD_LAG_SECONDS + CLOUD_LAG_SECONDS * age));
            float reach = swimmer.bodySize() * CLOUD_RADIUS * (float) Math.sqrt(hash(swimmer, index, life, 3)) * (0.6f + 0.4f * age);
            float angle = TWO_PI * hash(swimmer, index, life, 4);
            float size = swimmer.bodySize() * CLOUD_PUFF_SIZE * (0.5f + 0.5f * reach / (swimmer.bodySize() * CLOUD_RADIUS)) * (0.7f + 0.3f * age);
            float spin = 360f * hash(swimmer, index, life, 5) + (hash(swimmer, index, life, 6) * 2f - 1f) * MAX_SPIN_DEGREES_PER_SECOND * age * lifeSeconds;
            float x = screenX + (pose.x + reach * (float) Math.cos(angle)) * zoom;
            float y = screenY - (pose.y + reach * (float) Math.sin(angle)) * zoom;
            quads.cellAtCenter(SHEET_SIDE, SHEET_SIDE, (int) (hash(swimmer, index, life, 7) * SHEET_SIDE * SHEET_SIDE), x, y, size * zoom, size * zoom,
                    spin, flashes ? FLASH : SHROUD, alpha * fade * (flashes ? flash : CLOUD_ALPHA));
        }
    }

    private void drawHalos(TexturedQuadBatch quads, Swimmer swimmer, float screenX, float screenY, float zoom, float seconds, float alpha) {
        for (int index = 0; index < CLOUD_PUFFS; index += 3) {
            float glow = flash(swimmer, index, seconds);
            if (glow <= 0f) {
                continue;
            }
            float lifeSeconds = MIN_PUFF_SECONDS + PUFF_SECONDS_SPREAD * hash(swimmer, index, 0, 1);
            float aged = seconds / lifeSeconds + hash(swimmer, index, 0, 2);
            int life = (int) Math.floor(aged);
            float age = aged - life;
            poseAt(swimmer, seconds - (MIN_CLOUD_LAG_SECONDS + CLOUD_LAG_SECONDS * age));
            float reach = swimmer.bodySize() * CLOUD_RADIUS * (float) Math.sqrt(hash(swimmer, index, life, 3)) * (0.6f + 0.4f * age);
            float angle = TWO_PI * hash(swimmer, index, life, 4);
            float size = swimmer.bodySize() * HALO_SIZE * (0.5f + 0.5f * glow) * zoom;
            quads.quadAtCenter(screenX + (pose.x + reach * (float) Math.cos(angle)) * zoom, screenY - (pose.y + reach * (float) Math.sin(angle)) * zoom,
                    size, size, 0f, HALO, alpha * glow * HALO_ALPHA * (float) Math.sin(Math.PI * age));
        }
    }

    private void drawDark(TexturedQuadBatch quads, Swimmer swimmer, float screenX, float screenY, float zoom, float seconds, float alpha) {
        for (int index = 0; index < DARK_PUFFS; index++) {
            float lifeSeconds = MIN_DARK_SECONDS + DARK_SECONDS_SPREAD * hash(swimmer, index, 0, 8);
            float aged = seconds / lifeSeconds + hash(swimmer, index, 0, 9);
            int life = (int) Math.floor(aged);
            float age = aged - life;
            float fade = (float) Math.sin(Math.PI * age);
            if (fade <= 0f) {
                continue;
            }
            poseAt(swimmer, seconds - (MIN_DARK_LAG_SECONDS + DARK_LAG_SECONDS * age));
            float reach = swimmer.bodySize() * DARK_RADIUS * (float) Math.sqrt(hash(swimmer, index, life, 10));
            float angle = TWO_PI * hash(swimmer, index, life, 11);
            float size = swimmer.bodySize() * DARK_PUFF_SIZE * (1f + age) * zoom;
            quads.cellAtCenter(SHEET_SIDE, SHEET_SIDE, (int) (hash(swimmer, index, life, 12) * SHEET_SIDE * SHEET_SIDE),
                    screenX + (pose.x + reach * (float) Math.cos(angle)) * zoom, screenY - (pose.y + reach * (float) Math.sin(angle)) * zoom, size, size,
                    360f * hash(swimmer, index, life, 13), DARKNESS, alpha * DARK_ALPHA * fade * 0.5f);
        }
    }

    private void drawBody(TexturedQuadBatch quads, Swimmer swimmer, float centreX, float centreY, float zoom, float facingDeg, float seconds,
                          float alpha) {
        double angle = Math.toRadians(-facingDeg - 90f);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float width = swimmer.width() * zoom;
        float height = swimmer.height() * zoom;
        float warp = Math.max(width, height) * WARP_SHARE;
        quads.color(Color.WHITE, alpha);
        for (int column = 0; column < WARP_VERTICES - 1; column++) {
            for (int row = 0; row < WARP_VERTICES - 1; row++) {
                bodyVertex(quads, swimmer, column, row, centreX, centreY, width, height, cos, sin, warp, seconds);
                bodyVertex(quads, swimmer, column, row + 1, centreX, centreY, width, height, cos, sin, warp, seconds);
                bodyVertex(quads, swimmer, column + 1, row + 1, centreX, centreY, width, height, cos, sin, warp, seconds);
                bodyVertex(quads, swimmer, column + 1, row, centreX, centreY, width, height, cos, sin, warp, seconds);
            }
        }
    }

    private void bodyVertex(TexturedQuadBatch quads, Swimmer swimmer, int column, int row, float centreX, float centreY, float width, float height,
                            float cos, float sin, float warp, float seconds) {
        float fractionX = column / (float) (WARP_VERTICES - 1);
        float fractionY = row / (float) (WARP_VERTICES - 1);
        float side = (fractionX - 0.5f) * width;
        float forward = (fractionY - 0.5f) * height;
        boolean edge = column == 0 || row == 0 || column == WARP_VERTICES - 1 || row == WARP_VERTICES - 1;
        if (!edge) {
            int vertex = column * WARP_VERTICES + row;
            float rate = MIN_WARP_RATE + WARP_RATE_SPREAD * hash(swimmer, vertex, 0, 20);
            float turn = seconds * rate + TWO_PI * hash(swimmer, vertex, 0, 21);
            side += warp * (float) Math.cos(turn);
            forward += warp * (float) Math.sin(turn);
        }
        quads.meshVertex(fractionX, fractionY, centreX + side * cos - forward * sin, centreY + side * sin + forward * cos);
    }

    private float flash(Swimmer swimmer, int index, float seconds) {
        float cycle = FLASH_RISE_SECONDS + FLASH_FALL_SECONDS + FLASH_REST_SECONDS * hash(swimmer, index, 0, 14);
        float into = (seconds + cycle * hash(swimmer, index, 0, 15)) % cycle;
        float brightness;
        if (into < FLASH_RISE_SECONDS) {
            brightness = into / FLASH_RISE_SECONDS;
        } else if (into < FLASH_RISE_SECONDS + FLASH_FALL_SECONDS) {
            brightness = 1f - (into - FLASH_RISE_SECONDS) / FLASH_FALL_SECONDS;
        } else {
            return 0f;
        }
        return brightness * brightness;
    }

    private static float hash(Swimmer swimmer, int index, int life, int salt) {
        return ShroudedMawRenderer.hash(index + swimmer.index() * 997, life, salt + 60);
    }
}
