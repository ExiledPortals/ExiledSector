package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceAutomaton;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin.CellStateTracker;
import exiledsector.Seeds;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.GlScope;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class HyperspaceClouds {

    public interface Cover {

        float cover(float worldX, float worldY);

        float storms(float worldX, float worldY);

        Color colour(float worldX, float worldY);

        float alpha(float worldX, float worldY);
    }

    public static final String TEXTURE_CATEGORY = "terrain";
    public static final String CLOUD_KEY = "deep_hyperspace";
    public static final String FLASH_KEY = "deep_hyperspace_glow";
    static final int SHEET_CELLS = 4;
    static final float RENDER_SIZE_MULT = 2.5f;
    static final float OFFSET_SHARE = 0.25f;
    static final float WOBBLE_SHARE = 0.05f;
    static final float WOBBLE_SPEED = 0.5f;
    static final float RADIUS_PERIOD_SECONDS = 2f / (0.5f * WOBBLE_SPEED);
    static final float FLASH_SIZE_MULT = 1.5f;
    static final float FLASH_JITTER_SHARE = 0.3f;
    static final float FLASH_OVER_ALPHA = 0.25f;
    static final int AUTOMATON_SIZE = 48;
    static final float AUTOMATON_MIN_DAYS = 1.5f;
    static final float AUTOMATON_MAX_DAYS = 2.5f;
    static final float SECONDS_PER_DAY = 10f;
    static final int TILE_CACHE_LIMIT = 8192;
    private static final long COVER_SALT = 0x434c4f5544L;
    private static final long STORM_SALT = 0x53544f524dL;
    private static final long SHAPE_SALT = 0x5348415045L;
    private static final Logger LOG = Logger.getLogger(HyperspaceClouds.class);

    private static final class Tile {
        private final int tileX;
        private final int tileY;
        private final boolean present;
        private final boolean stormProne;
        private final float centreX;
        private final float centreY;
        private final float cos;
        private final float sin;
        private final float texX;
        private final float texY;
        private final float[] wobble = new float[4 * 6];
        private final Color colour;
        private final float alpha;
        private final float[] screen = new float[8];

        private Tile(int tileX, int tileY, boolean present, boolean stormProne, float centreX, float centreY, float angle, int sheetCell,
                     Color colour, float alpha) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.present = present;
            this.stormProne = stormProne;
            this.centreX = centreX;
            this.centreY = centreY;
            this.cos = (float) Math.cos(angle);
            this.sin = (float) Math.sin(angle);
            this.texX = (sheetCell % SHEET_CELLS) / (float) SHEET_CELLS;
            this.texY = (sheetCell / SHEET_CELLS) / (float) SHEET_CELLS;
            this.colour = colour;
            this.alpha = alpha;
        }
    }

    private final float tileSize;
    private final float renderSize;
    private final long seed;
    private final float cloudAlpha;
    private final SpriteCache spriteCache = new SpriteCache(HyperspaceClouds.class);
    private final Map<Long, Tile> tiles = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Tile> eldest) {
            return size() > TILE_CACHE_LIMIT;
        }
    };
    private final Map<Long, CellStateTracker> trackers = new HashMap<>();
    private final List<Tile> drawn = new ArrayList<>();
    private final List<Tile> stormCandidates = new ArrayList<>();
    private final Random jitter = new Random();
    private final Random flashJitter = new Random();
    private final HyperspaceAutomaton automaton = new HyperspaceAutomaton(AUTOMATON_SIZE, AUTOMATON_SIZE, AUTOMATON_MIN_DAYS, AUTOMATON_MAX_DAYS);
    private Cover cover;
    private float elapsedSeconds;
    private boolean primed;
    private String cloudTexture;
    private String flashTexture;

    public HyperspaceClouds(float tileSize, long seed, float cloudAlpha) {
        this.tileSize = tileSize;
        this.renderSize = tileSize * RENDER_SIZE_MULT;
        this.seed = seed;
        this.cloudAlpha = cloudAlpha;
    }

    public void setCover(Cover cover) {
        this.cover = cover;
        tiles.clear();
        drawn.clear();
        stormCandidates.clear();
    }

    public void advance(float amount) {
        elapsedSeconds += amount;
        float days = amount / SECONDS_PER_DAY;
        automaton.advance(days);
        int[][] cells = automaton.getCells();
        float interval = automaton.getInterval().getIntervalDuration();
        for (Tile tile : stormCandidates) {
            long key = key(tile.tileX, tile.tileY);
            if (!trackers.containsKey(key) && live(cells, tile)) {
                CellStateTracker tracker = new CellStateTracker(tile.tileX, tile.tileY, interval * 1.5f * jitter.nextFloat(),
                        interval * 0.5f + interval * 0.5f * jitter.nextFloat());
                if (!primed) {
                    tracker.advance(jitter.nextFloat() * interval * 2.5f);
                }
                trackers.put(key, tracker);
            }
        }
        primed |= !stormCandidates.isEmpty();
        for (Iterator<CellStateTracker> iterator = trackers.values().iterator(); iterator.hasNext(); ) {
            CellStateTracker tracker = iterator.next();
            if (!live(cells, tracker.i, tracker.j) && tracker.isStorming() && !tracker.isWaning()) {
                tracker.wane(interval * 0.5f + interval * 0.5f * jitter.nextFloat());
            }
            tracker.advance(days);
            if (tracker.isOff()) {
                iterator.remove();
            }
        }
    }

    public void render(TreeViewport view, float alphaMult) {
        if (cover == null || alphaMult <= 0f) {
            return;
        }
        collect(view);
        if (drawn.isEmpty()) {
            return;
        }
        SpriteAPI clouds = texture(CLOUD_KEY, true);
        SpriteAPI flash = texture(FLASH_KEY, false);
        if (clouds == null) {
            return;
        }
        drawClouds(view, clouds, GL11.GL_ONE, cloudAlpha * alphaMult, false);
        drawClouds(view, clouds, GL11.GL_ONE_MINUS_SRC_ALPHA, cloudAlpha * alphaMult, false);
        drawClouds(view, clouds, GL11.GL_ONE, alphaMult, true);
        if (flash != null) {
            drawFlashes(view, flash, alphaMult);
        }
    }

    public void renderFlashOver(TreeViewport view, float alphaMult) {
        SpriteAPI flash = texture(FLASH_KEY, false);
        if (flash != null && !drawn.isEmpty() && alphaMult > 0f) {
            drawFlashes(view, flash, alphaMult * FLASH_OVER_ALPHA);
        }
    }

    static boolean present(long seed, int tileX, int tileY, float cover) {
        return cover > 0f && unit(hash(seed, COVER_SALT, tileX, tileY)) < cover;
    }

    static int wrap(int index) {
        return Math.floorMod(index, AUTOMATON_SIZE);
    }

    private boolean live(int[][] cells, Tile tile) {
        return live(cells, tile.tileX, tile.tileY);
    }

    private static boolean live(int[][] cells, int tileX, int tileY) {
        return cells[wrap(tileX)][wrap(tileY)] == 1;
    }

    private void collect(TreeViewport view) {
        drawn.clear();
        stormCandidates.clear();
        float zoom = view.zoom();
        float margin = renderSize * FLASH_SIZE_MULT;
        float minX = (view.left() - view.centerX()) / zoom - margin;
        float maxX = (view.right() - view.centerX()) / zoom + margin;
        float minY = (view.centerY() - view.top()) / zoom - margin;
        float maxY = (view.centerY() - view.bottom()) / zoom + margin;
        if (Float.isInfinite(minX) || Float.isInfinite(maxX) || Float.isInfinite(minY) || Float.isInfinite(maxY)) {
            return;
        }
        int firstX = (int) Math.floor(minX / tileSize);
        int lastX = (int) Math.floor(maxX / tileSize);
        int firstY = (int) Math.floor(minY / tileSize);
        int lastY = (int) Math.floor(maxY / tileSize);
        for (int tileX = firstX; tileX <= lastX; tileX++) {
            for (int tileY = firstY; tileY <= lastY; tileY++) {
                Tile tile = tile(tileX, tileY);
                if (!tile.present || tile.alpha <= 0f) {
                    continue;
                }
                drawn.add(tile);
                if (tile.stormProne) {
                    stormCandidates.add(tile);
                }
                placeCorners(view, tile);
            }
        }
    }

    private Tile tile(int tileX, int tileY) {
        long key = key(tileX, tileY);
        Tile tile = tiles.get(key);
        if (tile == null) {
            tile = buildTile(tileX, tileY);
            tiles.put(key, tile);
        }
        return tile;
    }

    private Tile buildTile(int tileX, int tileY) {
        float baseX = (tileX + 0.5f) * tileSize;
        float baseY = (tileY + 0.5f) * tileSize;
        float coverage = cover.cover(baseX, baseY);
        boolean present = present(seed, tileX, tileY, coverage);
        if (!present) {
            return new Tile(tileX, tileY, false, false, baseX, baseY, 0f, 0, Color.WHITE, 0f);
        }
        Random shape = new Random(hash(seed, SHAPE_SALT, tileX, tileY));
        float offRange = renderSize * OFFSET_SHARE;
        float centreX = baseX - offRange / 2f + offRange * shape.nextFloat();
        float centreY = baseY - offRange / 2f + offRange * shape.nextFloat();
        float angle = shape.nextFloat() * (float) (Math.PI * 2.0);
        int sheetCell = shape.nextInt(SHEET_CELLS * SHEET_CELLS);
        float storms = cover.storms(baseX, baseY);
        boolean stormProne = storms > 0f && unit(hash(seed, STORM_SALT, tileX, tileY)) < storms;
        Color colour = cover.colour(baseX, baseY);
        Tile tile = new Tile(tileX, tileY, true, stormProne, centreX, centreY, angle, sheetCell, colour == null ? Color.WHITE : colour,
                Math.max(0f, Math.min(1f, cover.alpha(baseX, baseY))));
        for (int corner = 0; corner < 4; corner++) {
            float spin = (shape.nextFloat() * 3f + 1f) * (float) Math.toRadians(360.0);
            float rate = (30f + 70f * shape.nextFloat()) * (float) Math.toRadians(1.0) * WOBBLE_SPEED;
            int at = corner * 6;
            tile.wobble[at] = -spin;
            tile.wobble[at + 1] = spin;
            tile.wobble[at + 2] = 2f * (2f * spin) / rate;
            tile.wobble[at + 3] = shape.nextFloat();
            tile.wobble[at + 4] = RADIUS_PERIOD_SECONDS;
            tile.wobble[at + 5] = shape.nextFloat();
        }
        return tile;
    }

    private void placeCorners(TreeViewport view, Tile tile) {
        float half = renderSize / 2f;
        float maxRadius = renderSize * WOBBLE_SHARE;
        for (int corner = 0; corner < 4; corner++) {
            float localX = corner < 2 ? -half : half;
            float localY = corner == 0 || corner == 3 ? -half : half;
            int at = corner * 6;
            float theta = tile.wobble[at] + (tile.wobble[at + 1] - tile.wobble[at]) * triangle(tile.wobble[at + 3] + elapsedSeconds / tile.wobble[at + 2]);
            float radius = maxRadius * triangle(tile.wobble[at + 5] + elapsedSeconds / tile.wobble[at + 4]);
            float worldX = tile.centreX + localX * tile.cos - localY * tile.sin + (float) Math.sin(theta) * radius;
            float worldY = tile.centreY + localX * tile.sin + localY * tile.cos + (float) Math.cos(theta) * radius;
            tile.screen[corner * 2] = view.screenX(worldX);
            tile.screen[corner * 2 + 1] = view.screenY(worldY);
        }
    }

    private void drawClouds(TreeViewport view, SpriteAPI texture, int destinationFactor, float alpha, boolean glowOnly) {
        float cellWidth = texture.getTextureWidth() / SHEET_CELLS;
        float cellHeight = texture.getTextureHeight() / SHEET_CELLS;
        boolean began = false;
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, destinationFactor)) {
            for (Tile tile : drawn) {
                float shade = alpha * tile.alpha;
                if (glowOnly) {
                    CellStateTracker tracker = trackers.get(key(tile.tileX, tile.tileY));
                    shade *= tracker == null ? 0f : tracker.getSignalBrightness();
                }
                if (shade <= 0f || !view.overlaps(minOf(tile.screen, 0), minOf(tile.screen, 1), maxOf(tile.screen, 0), maxOf(tile.screen, 1))) {
                    continue;
                }
                if (!began) {
                    texture.bindTexture();
                    GL11.glBegin(GL11.GL_QUADS);
                    began = true;
                }
                colour(tile.colour, shade);
                quad(tile.screen, tile.texX * texture.getTextureWidth(), tile.texY * texture.getTextureHeight(), cellWidth, cellHeight);
            }
            if (began) {
                GL11.glEnd();
            }
        }
    }

    private void drawFlashes(TreeViewport view, SpriteAPI texture, float alpha) {
        float cellWidth = texture.getTextureWidth() / SHEET_CELLS;
        float cellHeight = texture.getTextureHeight() / SHEET_CELLS;
        float maxJitter = renderSize * FLASH_JITTER_SHARE * view.zoom();
        float[] corners = new float[8];
        boolean began = false;
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
            for (Tile tile : drawn) {
                CellStateTracker tracker = trackers.get(key(tile.tileX, tile.tileY));
                if (tracker == null || tracker.flicker == null || tracker.flicker.getBrightness() <= 0f) {
                    continue;
                }
                float centreX = (tile.screen[0] + tile.screen[2] + tile.screen[4] + tile.screen[6]) / 4f;
                float centreY = (tile.screen[1] + tile.screen[3] + tile.screen[5] + tile.screen[7]) / 4f;
                flashJitter.setSeed(key(tile.tileX, tile.tileY) * 1000003L + (long) (tracker.flicker.getAngle() * 1000f));
                for (int corner = 0; corner < 4; corner++) {
                    corners[corner * 2] = centreX + (tile.screen[corner * 2] - centreX) * FLASH_SIZE_MULT + (flashJitter.nextFloat() - 0.5f) * maxJitter;
                    corners[corner * 2 + 1] = centreY + (tile.screen[corner * 2 + 1] - centreY) * FLASH_SIZE_MULT + (flashJitter.nextFloat() - 0.5f) * maxJitter;
                }
                if (!began) {
                    texture.bindTexture();
                    GL11.glBegin(GL11.GL_QUADS);
                    began = true;
                }
                colour(tile.colour, alpha * tile.alpha * tracker.flicker.getBrightness());
                quad(corners, tile.texX * texture.getTextureWidth(), tile.texY * texture.getTextureHeight(), cellWidth, cellHeight);
            }
            if (began) {
                GL11.glEnd();
            }
        }
    }

    private static void colour(Color colour, float alpha) {
        GL11.glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, Math.min(1f, alpha));
    }

    private static void quad(float[] corners, float texX, float texY, float texWidth, float texHeight) {
        GL11.glTexCoord2f(texX, texY);
        GL11.glVertex2f(corners[0], corners[1]);
        GL11.glTexCoord2f(texX, texY + texHeight);
        GL11.glVertex2f(corners[2], corners[3]);
        GL11.glTexCoord2f(texX + texWidth, texY + texHeight);
        GL11.glVertex2f(corners[4], corners[5]);
        GL11.glTexCoord2f(texX + texWidth, texY);
        GL11.glVertex2f(corners[6], corners[7]);
    }

    private SpriteAPI texture(String key, boolean cloud) {
        String path = cloud ? cloudTexture : flashTexture;
        if (path == null) {
            try {
                path = Global.getSettings().getSpriteName(TEXTURE_CATEGORY, key);
            } catch (RuntimeException e) {
                LOG.warn("No " + TEXTURE_CATEGORY + "/" + key + " sprite for the hyperspace clouds: " + e.getMessage());
                path = "";
            }
            if (cloud) {
                cloudTexture = path;
            } else {
                flashTexture = path;
            }
        }
        return path.isEmpty() ? null : spriteCache.texture(path);
    }

    static float triangle(float progress) {
        float fraction = progress - (float) Math.floor(progress);
        return fraction < 0.5f ? fraction * 2f : (1f - fraction) * 2f;
    }

    private static float minOf(float[] corners, int axis) {
        return Math.min(Math.min(corners[axis], corners[axis + 2]), Math.min(corners[axis + 4], corners[axis + 6]));
    }

    private static float maxOf(float[] corners, int axis) {
        return Math.max(Math.max(corners[axis], corners[axis + 2]), Math.max(corners[axis + 4], corners[axis + 6]));
    }

    private static long key(int tileX, int tileY) {
        return ((long) tileX << 32) ^ (tileY & 0xffffffffL);
    }

    static long hash(long seed, long salt, int x, int y) {
        long mixed = seed ^ salt * Seeds.GOLDEN_GAMMA;
        mixed = Seeds.mix(mixed + x * 0xc2b2ae3d27d4eb4fL);
        mixed = Seeds.mix(mixed + y * 0x165667b19e3779f9L);
        return mixed;
    }

    static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }
}
