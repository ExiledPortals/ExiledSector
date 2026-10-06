package exiledsector.ui.node;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.ReusableText;
import exiledsector.ui.util.Rects;
import org.lazywizard.lazylib.ui.LazyFont;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static exiledsector.ui.SkillTreePanelStyle.FONT_LINE_HEIGHT_FACTOR;
import static exiledsector.ui.SkillTreePanelStyle.GLOW_COLOR;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_BODY_COLOR;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;

final class SkillTreeNodeDropdownRenderer {

    private static final float DROPDOWN_FONT_SIZE = TOOLTIP_BODY_FONT_SIZE;
    private static final float DROPDOWN_ROW_PADDING = 8f;
    private static final float DROPDOWN_ROW_GAP = 2f;
    private static final float DROPDOWN_TOP_OFFSET = 24f;
    private static final float DROPDOWN_HOVER_ALPHA = 0.35f;

    private final SkillTreePanelStyle style;
    private final Map<String, ReusableText> dropdownRowText = new HashMap<>();
    private SkillNode openNode;

    SkillTreeNodeDropdownRenderer(SkillTreePanelStyle style) {
        this.style = style;
    }

    boolean isOpen() {
        return openNode != null;
    }

    void open(SkillNode node) {
        openNode = node;
    }

    SkillNode getOpenNode() {
        return openNode;
    }

    void close() {
        openNode = null;
    }

    SkillType findOptionAt(TreeViewport viewport, float x, float y) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return null;
        for (DropdownRow row : computeRows(viewport, font)) {
            if (row.contains(x, y)) {
                return row.option;
            }
        }
        return null;
    }

    void render(TreeViewport viewport, float mouseX, float mouseY, boolean mouseKnown, float alphaMult) {
        if (openNode == null) return;
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        List<DropdownRow> rows = computeRows(viewport, font);
        if (rows.isEmpty()) return;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (DropdownRow row : rows) {
            minX = Math.min(minX, row.x);
            minY = Math.min(minY, row.y);
            maxX = Math.max(maxX, row.x + row.width);
            maxY = Math.max(maxY, row.y + row.height);
        }
        style.drawTooltipBackground(minX, minY, maxX - minX, maxY - minY, alphaMult, style.getAccentColor());

        for (DropdownRow row : rows) {
            if (mouseKnown && row.contains(mouseX, mouseY)) {
                drawDropdownRowHighlight(row, alphaMult);
            }
            float textY = row.y + row.height / 2f + (DROPDOWN_FONT_SIZE * FONT_LINE_HEIGHT_FACTOR) / 2f;
            dropdownRowText(row.option).setAlpha(alphaMult).draw(row.x + DROPDOWN_ROW_PADDING, textY);
        }
    }

    private List<DropdownRow> computeRows(TreeViewport viewport, LazyFont font) {
        List<DropdownRow> rows = new ArrayList<>();
        if (openNode == null) return rows;

        List<SkillType> options = new ArrayList<>();
        for (String optionId : openNode.getType().getOptionalOptionIds()) {
            SkillType option = SkillTree.getType(optionId);
            if (option != null) options.add(option);
        }
        if (options.isEmpty()) return rows;

        float nodeX = viewport.screenX(openNode.getOffsetX());
        float nodeY = viewport.screenY(openNode.getOffsetY());

        float width = 0f;
        for (SkillType option : options) {
            width = Math.max(width, font.calcWidth(option.getDisplayName(), DROPDOWN_FONT_SIZE));
        }
        width += DROPDOWN_ROW_PADDING * 2f;

        float rowHeight = DROPDOWN_FONT_SIZE * FONT_LINE_HEIGHT_FACTOR + DROPDOWN_ROW_PADDING * 2f;
        float x = nodeX - width / 2f;
        float topY = nodeY - DROPDOWN_TOP_OFFSET;

        for (int i = 0; i < options.size(); i++) {
            float rowTop = topY - i * (rowHeight + DROPDOWN_ROW_GAP);
            rows.add(new DropdownRow(options.get(i), x, rowTop - rowHeight, width, rowHeight));
        }
        return rows;
    }

    private void drawDropdownRowHighlight(DropdownRow row, float alphaMult) {
        GLDraw.fillQuad(row.x, row.y, row.width, row.height, GLOW_COLOR, DROPDOWN_HOVER_ALPHA * alphaMult);
    }

    private ReusableText dropdownRowText(SkillType option) {
        return dropdownRowText.computeIfAbsent(option.getId(),
                id -> new ReusableText(DROPDOWN_FONT_SIZE, TOOLTIP_BODY_COLOR).set(option.getDisplayName()));
    }

    private static final class DropdownRow {
        final SkillType option;
        final float x;
        final float y;
        final float width;
        final float height;

        DropdownRow(SkillType option, float x, float y, float width, float height) {
            this.option = option;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean contains(float px, float py) {
            return Rects.contains(x, y, width, height, px, py);
        }
    }
}
