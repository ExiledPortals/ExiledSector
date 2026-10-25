package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasAmbience;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasDecoration;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.HyperspaceClouds;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

final class AtlasScenery {

    static final float CLOUD_TILE = AtlasMapCanvas.CELL_PIXELS / 4f;
    static final float CLOUD_ALPHA = 0.35f;
    static final float KNOWN_SAMPLE_REACH = 0.5f;

    private final Function<String, AtlasAmbience> ambiences;
    private final Function<String, AtlasDecoration> decorationLookup;
    private final HyperspaceClouds clouds;
    private final AtlasDecorations decorations = new AtlasDecorations();
    private final AtlasFleetTraffic fleets = new AtlasFleetTraffic();
    private final Set<Long> known = new HashSet<>();
    private AtlasMapCanvas canvas;

    AtlasScenery(long seed) {
        this(seed, AtlasData::ambience, AtlasData::decoration);
    }

    AtlasScenery(long seed, Function<String, AtlasAmbience> ambiences, Function<String, AtlasDecoration> decorationLookup) {
        this.ambiences = ambiences;
        this.decorationLookup = decorationLookup;
        clouds = new HyperspaceClouds(CLOUD_TILE, seed, CLOUD_ALPHA);
    }

    AtlasDecorations decorations() {
        return decorations;
    }

    AtlasDecoration decoration(String decorationId) {
        return decorationLookup.apply(decorationId);
    }

    void attach(AtlasMapCanvas canvas) {
        this.canvas = canvas;
        known.clear();
        for (AtlasCell cell : canvas.visibleCells()) {
            for (AtlasCell around : cell.around(1)) {
                known.add(around.pack());
            }
        }
        clouds.setCover(new Cover());
        decorations.attach(canvas, known);
        fleets.attach(canvas);
    }

    void advance(float amount) {
        clouds.advance(amount);
        decorations.advance(amount);
        fleets.advance(amount);
    }

    void renderBackdrop(TreeViewport view, float alphaMult) {
        clouds.render(view, alphaMult);
        decorations.render(view, alphaMult);
    }

    void renderAbove(TreeViewport view, float alphaMult) {
        fleets.render(view, alphaMult);
        clouds.renderFlashOver(view, alphaMult);
    }

    boolean isKnown(AtlasCell cell) {
        return known.contains(cell.pack());
    }

    AtlasAmbience ambience(AtlasCell cell) {
        AtlasAmbience ambience = ambiences.apply(regionAt(cell));
        return ambience == null ? AtlasAmbience.BARE : ambience;
    }

    String regionAt(AtlasCell cell) {
        return cell.equals(AtlasCell.ORIGIN) ? AtlasRegion.NORMAL : canvas.generator().biomeOf(cell).regionId();
    }

    static AtlasCell cellAt(float worldX, float worldY) {
        return new AtlasCell(Math.round(worldX / AtlasMapCanvas.CELL_PIXELS), Math.round(worldY / AtlasMapCanvas.CELL_PIXELS));
    }

    private final class Cover implements HyperspaceClouds.Cover {

        @Override
        public float cover(float worldX, float worldY) {
            return canvas.generator().cloudAt(worldX / AtlasMapCanvas.CELL_PIXELS, worldY / AtlasMapCanvas.CELL_PIXELS) ? 1f : 0f;
        }

        @Override
        public float storms(float worldX, float worldY) {
            return ambience(cellAt(worldX, worldY)).storms();
        }

        @Override
        public Color colour(float worldX, float worldY) {
            return ambience(cellAt(worldX, worldY)).cloudColour();
        }

        @Override
        public float alpha(float worldX, float worldY) {
            float reach = AtlasMapCanvas.CELL_PIXELS * KNOWN_SAMPLE_REACH;
            int knownSamples = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    knownSamples += isKnown(cellAt(worldX + dx * reach, worldY + dy * reach)) ? 1 : 0;
                }
            }
            return knownSamples / 9f;
        }
    }
}
