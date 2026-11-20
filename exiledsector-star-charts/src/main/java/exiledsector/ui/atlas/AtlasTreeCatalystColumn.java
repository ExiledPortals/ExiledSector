package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.items.starchart.AtlasCatalystItems;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CatalystCargo;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.Rects;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class AtlasTreeCatalystColumn {

    static final float MAX_CELL_SIZE = 48f;
    static final float PADDING = 8f;
    private static final float ICON_SHARE = 0.84f;
    private static final float HOVER_FILL_ALPHA = 0.25f;
    private static final float SELECTED_FILL_ALPHA = 0.45f;
    private static final float HOVER_BORDER_ALPHA = 0.9f;
    private static final float HOVER_BORDER_WIDTH = 1.5f;

    private final SpriteCache icons = new SpriteCache(AtlasTreeCatalystColumn.class);
    private final List<AtlasCatalysts.Catalyst> catalysts;
    private final Set<AtlasCatalysts.Catalyst> toggled = new LinkedHashSet<>();
    private float columnLeft;
    private float columnTop;
    private float cellSize;

    AtlasTreeCatalystColumn(List<AtlasCatalysts.Catalyst> catalysts) {
        this.catalysts = List.copyOf(catalysts);
    }

    static List<AtlasCatalysts.Catalyst> shown() {
        return AtlasCatalysts.all().stream().filter(catalyst -> catalyst.weight() > 0f).toList();
    }

    static float cellSizeFor(int count, float availableHeight) {
        if (count <= 0) {
            return 0f;
        }
        return Math.max(0f, Math.min(MAX_CELL_SIZE, (availableHeight - PADDING * 2f) / count));
    }

    void place(float left, float top, float bottom) {
        columnLeft = left;
        columnTop = top;
        cellSize = cellSizeFor(catalysts.size(), top - bottom);
    }

    private float columnWidth() {
        return cellSize + PADDING * 2f;
    }

    private float columnHeight() {
        return cellSize * catalysts.size() + PADDING * 2f;
    }

    private float columnBottom() {
        return columnTop - columnHeight();
    }

    boolean contains(float x, float y) {
        return !catalysts.isEmpty() && cellSize > 0f && Rects.contains(columnLeft, columnBottom(), columnWidth(), columnHeight(), x, y);
    }

    AtlasCatalysts.Catalyst catalystAt(float x, float y) {
        if (!contains(x, y)) {
            return null;
        }
        int index = (int) ((columnTop - PADDING - y) / cellSize);
        return index >= 0 && index < catalysts.size() && x >= columnLeft + PADDING && x <= columnLeft + PADDING + cellSize ? catalysts.get(index) : null;
    }

    void toggle(AtlasCatalysts.Catalyst catalyst) {
        if (catalyst != null && !toggled.remove(catalyst)) {
            toggled.add(catalyst);
        }
    }

    boolean isToggled(AtlasCatalysts.Catalyst catalyst) {
        return toggled.contains(catalyst);
    }

    Set<String> focusedAnomalies(AtlasCatalysts.Catalyst hovered) {
        Set<String> anomalies = new LinkedHashSet<>();
        toggled.forEach(catalyst -> anomalies.add(catalyst.anomalyId()));
        if (hovered != null) {
            anomalies.add(hovered.anomalyId());
        }
        return anomalies;
    }

    void render(AtlasCatalysts.Catalyst hovered, float alpha) {
        if (catalysts.isEmpty() || cellSize <= 0f || alpha <= 0f) {
            return;
        }
        BorderedPanel.draw(columnLeft, columnBottom(), columnWidth(), columnHeight(), alpha);
        float iconSize = cellSize * ICON_SHARE;
        for (int index = 0; index < catalysts.size(); index++) {
            AtlasCatalysts.Catalyst catalyst = catalysts.get(index);
            float cellLeft = columnLeft + PADDING;
            float cellBottom = columnTop - PADDING - cellSize * (index + 1);
            boolean selected = toggled.contains(catalyst);
            if (selected || catalyst == hovered) {
                float fillAlpha = selected ? SELECTED_FILL_ALPHA : HOVER_FILL_ALPHA;
                GLDraw.fillQuad(cellLeft, cellBottom, cellSize, cellSize, AtlasCatalystItems.color(catalyst.family()), fillAlpha * alpha);
                GLDraw.strokeQuad(cellLeft, cellBottom, cellSize, cellSize, AtlasCatalystItems.color(catalyst.family()), HOVER_BORDER_WIDTH,
                        HOVER_BORDER_ALPHA * alpha);
            }
            float inset = (cellSize - iconSize) / 2f;
            CargoIconFx.render(icons, catalyst.iconPath(), CatalystCargo.look(catalyst.family()), cellLeft + inset, cellBottom + inset, iconSize, iconSize,
                    alpha, 1f, catalyst.itemId().hashCode(), -1f);
        }
    }
}
