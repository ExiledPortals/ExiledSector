package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.charts.atlas.AtlasAmbience;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDecoration;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.GlScope;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

final class AtlasDecorations {

    static final float CLEARANCE_CELLS = 0.28f;
    static final int PLACEMENT_ATTEMPTS = 6;
    static final float DRIFT_FREQUENCY = 0.15f;
    static final float MIN_PIECE_PIXELS = 1f;
    static final int CACHE_LIMIT = 2048;
    private static final long PLACEMENT_SALT = 0x4445434fL;

    record Piece(AtlasDecoration decoration, String sprite, int frame, float x, float y, float size, float startAngle, float spinRate,
                 float driftPhase, float driftRadius) {
    }

    private final SpriteCache spriteCache = new SpriteCache(AtlasDecorations.class);
    private final Map<Long, List<Piece>> pieces = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, List<Piece>> eldest) {
            return size() > CACHE_LIMIT;
        }
    };
    private final Map<String, List<Piece>> bySprite = new LinkedHashMap<>();
    private final Map<String, List<Piece>> byGlow = new LinkedHashMap<>();
    private AtlasMapCanvas canvas;
    private Set<Long> known = Set.of();
    private float elapsedSeconds;

    void attach(AtlasMapCanvas canvas, Set<Long> known) {
        this.canvas = canvas;
        this.known = known;
        pieces.clear();
    }

    void advance(float amount) {
        elapsedSeconds += amount;
    }

    void render(TreeViewport view, float alphaMult) {
        if (canvas == null || alphaMult <= 0f) {
            return;
        }
        float zoom = view.zoom();
        int firstX = (int) Math.floor((view.left() - view.centerX()) / zoom / AtlasMapCanvas.CELL_PIXELS) - 1;
        int lastX = (int) Math.ceil((view.right() - view.centerX()) / zoom / AtlasMapCanvas.CELL_PIXELS) + 1;
        int firstY = (int) Math.floor((view.centerY() - view.top()) / zoom / AtlasMapCanvas.CELL_PIXELS) - 1;
        int lastY = (int) Math.ceil((view.centerY() - view.bottom()) / zoom / AtlasMapCanvas.CELL_PIXELS) + 1;
        if (lastX - firstX > AtlasMapCanvas.MAX_SCENERY_CELLS || lastY - firstY > AtlasMapCanvas.MAX_SCENERY_CELLS) {
            return;
        }
        bySprite.values().forEach(List::clear);
        byGlow.values().forEach(List::clear);
        for (int gridX = firstX; gridX <= lastX; gridX++) {
            for (int gridY = firstY; gridY <= lastY; gridY++) {
                AtlasCell cell = new AtlasCell(gridX, gridY);
                if (!known.contains(cell.pack())) {
                    continue;
                }
                for (Piece piece : piecesIn(cell)) {
                    bySprite.computeIfAbsent(piece.sprite(), path -> new ArrayList<>()).add(piece);
                    if (!piece.decoration().glow().isEmpty()) {
                        byGlow.computeIfAbsent(piece.decoration().glow(), path -> new ArrayList<>()).add(piece);
                    }
                }
            }
        }
        bySprite.forEach((path, list) -> drawAll(view, path, list, false, alphaMult));
        byGlow.forEach((path, list) -> drawAll(view, path, list, true, alphaMult));
    }

    List<Piece> piecesIn(AtlasCell cell) {
        return pieces.computeIfAbsent(cell.pack(), packed -> place(cell));
    }

    private List<Piece> place(AtlasCell cell) {
        AtlasAmbience ambience = canvas.scenery().ambience(cell);
        if (ambience.decorations().isEmpty() || ambience.decorationDensity() <= 0f) {
            return List.of();
        }
        Random random = new Random(mix(canvas.generator().seed() ^ PLACEMENT_SALT, cell.pack()));
        int placements = (int) ambience.decorationDensity() + (random.nextFloat() < ambience.decorationDensity() % 1f ? 1 : 0);
        List<Piece> placed = new ArrayList<>();
        Map<String, Float> weights = new TreeMap<>(ambience.decorations());
        for (int placement = 0; placement < placements; placement++) {
            String picked = pick(weights, random);
            AtlasDecoration decoration = picked == null ? null : canvas.scenery().decoration(picked);
            float[] spot = spot(cell, random);
            if (decoration == null || spot == null) {
                continue;
            }
            scatter(decoration, spot, random, placed);
            for (String scatterId : decoration.scatter()) {
                AtlasDecoration around = canvas.scenery().decoration(scatterId);
                if (around != null) {
                    scatter(around, spot, random, placed);
                }
            }
        }
        return List.copyOf(placed);
    }

    private static void scatter(AtlasDecoration decoration, float[] spot, Random random, List<Piece> placed) {
        String sprite = decoration.sprites().get(random.nextInt(decoration.sprites().size()));
        for (int index = 0; index < decoration.count(); index++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            float distance = (float) Math.sqrt(random.nextFloat()) * decoration.spread() * AtlasMapCanvas.CELL_PIXELS;
            float size = decoration.size() * (1f + decoration.sizeJitter() * (random.nextFloat() * 2f - 1f)) * AtlasMapCanvas.CELL_PIXELS;
            float spin = decoration.spin() * (random.nextFloat() * 2f - 1f);
            placed.add(new Piece(decoration, sprite, random.nextInt(decoration.columns() * decoration.rows()),
                    spot[0] + (float) Math.cos(angle) * distance, spot[1] + (float) Math.sin(angle) * distance, size, random.nextFloat() * 360f,
                    spin, random.nextFloat() * (float) (Math.PI * 2.0), decoration.drift() * AtlasMapCanvas.CELL_PIXELS));
        }
    }

    private float[] spot(AtlasCell cell, Random random) {
        float clearance = CLEARANCE_CELLS * AtlasMapCanvas.CELL_PIXELS;
        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            float x = (cell.gridX() + random.nextFloat() - 0.5f) * AtlasMapCanvas.CELL_PIXELS;
            float y = (cell.gridY() + random.nextFloat() - 0.5f) * AtlasMapCanvas.CELL_PIXELS;
            if (clearOfSystems(cell, x, y, clearance)) {
                return new float[] {x, y};
            }
        }
        return null;
    }

    private boolean clearOfSystems(AtlasCell cell, float x, float y, float clearance) {
        for (AtlasCell near : cell.around(1)) {
            if (canvas.generator().isVoid(near)) {
                continue;
            }
            float dx = canvas.displayX(near) - x;
            float dy = canvas.displayY(near) - y;
            if (dx * dx + dy * dy < clearance * clearance) {
                return false;
            }
        }
        return true;
    }

    static String pick(Map<String, Float> weights, Random random) {
        float total = 0f;
        for (float weight : weights.values()) {
            total += Math.max(0f, weight);
        }
        float roll = random.nextFloat() * total;
        String last = null;
        for (Map.Entry<String, Float> entry : weights.entrySet()) {
            if (entry.getValue() <= 0f) {
                continue;
            }
            last = entry.getKey();
            roll -= entry.getValue();
            if (roll < 0f) {
                return last;
            }
        }
        return last;
    }

    private void drawAll(TreeViewport view, String path, List<Piece> list, boolean glow, float alphaMult) {
        if (list.isEmpty()) {
            return;
        }
        SpriteAPI texture = spriteCache.texture(path);
        if (texture == null) {
            return;
        }
        float zoom = view.zoom();
        boolean began = false;
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, glow ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA)) {
            for (Piece piece : list) {
                AtlasDecoration decoration = piece.decoration();
                float frameWidth = texture.getWidth() / decoration.columns();
                float frameHeight = texture.getHeight() / decoration.rows();
                float longest = Math.max(frameWidth, frameHeight);
                if (longest <= 0f) {
                    continue;
                }
                float halfWidth = piece.size() * frameWidth / longest / 2f * zoom;
                float halfHeight = piece.size() * frameHeight / longest / 2f * zoom;
                if (Math.max(halfWidth, halfHeight) * 2f < MIN_PIECE_PIXELS) {
                    continue;
                }
                float drift = elapsedSeconds * DRIFT_FREQUENCY * (float) (Math.PI * 2.0) + piece.driftPhase();
                float x = view.screenX(piece.x() + (float) Math.cos(drift) * piece.driftRadius());
                float y = view.screenY(piece.y() + (float) Math.sin(drift) * piece.driftRadius());
                float reach = (halfWidth + halfHeight);
                if (!view.isVisible(x, y, reach)) {
                    continue;
                }
                if (!began) {
                    texture.bindTexture();
                    GL11.glBegin(GL11.GL_QUADS);
                    began = true;
                }
                Color colour = glow ? decoration.glowColour() : decoration.colour();
                float red = colour == null ? 1f : colour.getRed() / 255f;
                float green = colour == null ? 1f : colour.getGreen() / 255f;
                float blue = colour == null ? 1f : colour.getBlue() / 255f;
                GL11.glColor4f(red, green, blue, decoration.alpha() * alphaMult);
                float angle = (float) Math.toRadians(piece.startAngle() + piece.spinRate() * elapsedSeconds);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                float texWidth = texture.getTextureWidth() / decoration.columns();
                float texHeight = texture.getTextureHeight() / decoration.rows();
                float texX = (piece.frame() % decoration.columns()) * texWidth;
                float texY = (piece.frame() / decoration.columns()) * texHeight;
                corner(x, y, -halfWidth, -halfHeight, cos, sin, texX, texY);
                corner(x, y, halfWidth, -halfHeight, cos, sin, texX + texWidth, texY);
                corner(x, y, halfWidth, halfHeight, cos, sin, texX + texWidth, texY + texHeight);
                corner(x, y, -halfWidth, halfHeight, cos, sin, texX, texY + texHeight);
            }
            if (began) {
                GL11.glEnd();
            }
        }
    }

    private static void corner(float x, float y, float localX, float localY, float cos, float sin, float texX, float texY) {
        GL11.glTexCoord2f(texX, texY);
        GL11.glVertex2f(x + localX * cos - localY * sin, y + localX * sin + localY * cos);
    }

    private static long mix(long seed, long value) {
        long mixed = seed + value * 0x9e3779b97f4a7c15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }
}
