package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.GlScope;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

final class AtlasAbyssShade {

    static final float TILE_CELLS = 0.5f;
    static final float SAMPLE_REACH_CELLS = 0.5f;
    static final int MOST_TILES_ACROSS = 96;
    static final int CACHE_LIMIT = 1 << 16;

    private final Map<Long, Float> shades = new HashMap<>();
    private Predicate<AtlasCell> abyssal = cell -> false;
    private boolean anyAbyss;

    void attach(Predicate<AtlasCell> abyssalCell, Collection<AtlasCell> knownCells) {
        abyssal = abyssalCell;
        shades.clear();
        anyAbyss = knownCells.stream().anyMatch(abyssalCell);
    }

    boolean hasAbyss() {
        return anyAbyss;
    }

    void render(TreeViewport view, float alphaMult) {
        if (!anyAbyss || alphaMult <= 0f) {
            return;
        }
        float tile = TILE_CELLS * AtlasMapCanvas.CELL_PIXELS;
        float zoom = view.zoom();
        int firstX = (int) Math.floor((view.left() - view.centerX()) / zoom / tile);
        int lastX = (int) Math.ceil((view.right() - view.centerX()) / zoom / tile);
        int firstY = (int) Math.floor((view.centerY() - view.top()) / zoom / tile);
        int lastY = (int) Math.ceil((view.centerY() - view.bottom()) / zoom / tile);
        int stride = Math.max(1, (int) Math.ceil(Math.max(lastX - firstX, lastY - firstY) / (float) MOST_TILES_ACROSS));
        firstX = Math.floorDiv(firstX, stride) * stride;
        firstY = Math.floorDiv(firstY, stride) * stride;
        if (shades.size() > CACHE_LIMIT) {
            shades.clear();
        }
        boolean began = false;
        try (GlScope scope = GlScope.flat()) {
            for (int gridX = firstX; gridX < lastX; gridX += stride) {
                for (int gridY = firstY; gridY < lastY; gridY += stride) {
                    float bottomLeft = shade(gridX, gridY);
                    float bottomRight = shade(gridX + stride, gridY);
                    float topRight = shade(gridX + stride, gridY + stride);
                    float topLeft = shade(gridX, gridY + stride);
                    if (bottomLeft + bottomRight + topRight + topLeft <= 0f) {
                        continue;
                    }
                    if (!began) {
                        GL11.glBegin(GL11.GL_QUADS);
                        began = true;
                    }
                    vertex(view, gridX, gridY, tile, bottomLeft * alphaMult);
                    vertex(view, gridX + stride, gridY, tile, bottomRight * alphaMult);
                    vertex(view, gridX + stride, gridY + stride, tile, topRight * alphaMult);
                    vertex(view, gridX, gridY + stride, tile, topLeft * alphaMult);
                }
            }
            if (began) {
                GL11.glEnd();
            }
        }
    }

    float shade(int gridX, int gridY) {
        return shades.computeIfAbsent(((long) gridX << 32) ^ (gridY & 0xffffffffL), key -> sample(gridX, gridY));
    }

    private float sample(int gridX, int gridY) {
        float worldX = gridX * TILE_CELLS * AtlasMapCanvas.CELL_PIXELS;
        float worldY = gridY * TILE_CELLS * AtlasMapCanvas.CELL_PIXELS;
        float reach = SAMPLE_REACH_CELLS * AtlasMapCanvas.CELL_PIXELS;
        int dark = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                dark += abyssal.test(AtlasScenery.cellAt(worldX + dx * reach, worldY + dy * reach)) ? 1 : 0;
            }
        }
        return dark / 9f;
    }

    private static void vertex(TreeViewport view, int gridX, int gridY, float tile, float alpha) {
        GL11.glColor4f(0f, 0f, 0f, alpha);
        GL11.glVertex2f(view.screenX(gridX * tile), view.screenY(gridY * tile));
    }
}
