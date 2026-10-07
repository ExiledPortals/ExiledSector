package exiledsector.ui.node;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.Rects;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
    private static final float REFUSED_OPTION_ALPHA = 0.4f;
    private static final float ROW_HEIGHT = DROPDOWN_FONT_SIZE * FONT_LINE_HEIGHT_FACTOR + DROPDOWN_ROW_PADDING * 2f;

    private final SkillTreePanelStyle panelStyle;
    private final Map<String, TextLabel> dropdownRowText = new HashMap<>();
    private final List<DropdownRow> rows = new ArrayList<>();
    private SkillNode rowsNode;
    private float rowsWidth;
    private float rowsNodeX = Float.NaN;
    private float rowsNodeY = Float.NaN;

    SkillTreeNodeDropdownRenderer(SkillTreePanelStyle panelStyle) {
        this.panelStyle = panelStyle;
    }

    SkillType findOptionAt(SkillNode openNode, TreeViewport viewport, float screenX, float screenY) {
        List<DropdownRow> openRows = rowsFor(openNode, viewport);
        for (int i = 0; i < openRows.size(); i++) {
            DropdownRow row = openRows.get(i);
            if (row.contains(screenX, screenY)) {
                return row.option;
            }
        }
        return null;
    }

    void render(SkillNode openNode, TreeViewport viewport, float mouseX, float mouseY, boolean hovered, float alphaMult,
                Predicate<SkillType> optionUsable) {
        if (openNode == null) return;
        List<DropdownRow> openRows = rowsFor(openNode, viewport);
        if (openRows.isEmpty()) return;

        DropdownRow topRow = openRows.get(0);
        DropdownRow bottomRow = openRows.get(openRows.size() - 1);
        panelStyle.drawTooltipBackground(topRow.rowX, bottomRow.rowY, rowsWidth, topRow.rowY + ROW_HEIGHT - bottomRow.rowY, alphaMult,
                panelStyle.getAccentColor());

        for (int i = 0; i < openRows.size(); i++) {
            DropdownRow row = openRows.get(i);
            if (hovered && row.contains(mouseX, mouseY)) {
                GLDraw.fillQuad(row.rowX, row.rowY, rowsWidth, ROW_HEIGHT, GLOW_COLOR, DROPDOWN_HOVER_ALPHA * alphaMult);
            }
            float textY = row.rowY + ROW_HEIGHT / 2f + (DROPDOWN_FONT_SIZE * FONT_LINE_HEIGHT_FACTOR) / 2f;
            float rowAlpha = optionUsable.test(row.option) ? alphaMult : alphaMult * REFUSED_OPTION_ALPHA;
            dropdownRowText(row.option).setAlpha(rowAlpha).draw(row.rowX + DROPDOWN_ROW_PADDING, textY);
        }
    }

    private List<DropdownRow> rowsFor(SkillNode openNode, TreeViewport viewport) {
        if (openNode != rowsNode && !rebuildRows(openNode)) {
            return rows;
        }
        float nodeX = viewport.screenX(openNode.getOffsetX());
        float nodeY = viewport.screenY(openNode.getOffsetY());
        if (nodeX != rowsNodeX || nodeY != rowsNodeY) {
            rowsNodeX = nodeX;
            rowsNodeY = nodeY;
            float dropdownX = nodeX - rowsWidth / 2f;
            float topY = nodeY - DROPDOWN_TOP_OFFSET;
            for (int i = 0; i < rows.size(); i++) {
                DropdownRow row = rows.get(i);
                row.rowX = dropdownX;
                row.rowY = topY - i * (ROW_HEIGHT + DROPDOWN_ROW_GAP) - ROW_HEIGHT;
            }
        }
        return rows;
    }

    private boolean rebuildRows(SkillNode openNode) {
        rows.clear();
        rowsNode = null;
        rowsNodeX = Float.NaN;
        rowsNodeY = Float.NaN;
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return false;
        rowsNode = openNode;
        float widestOption = 0f;
        for (String optionId : openNode.getType().getOptionalOptionIds()) {
            SkillType option = SkillTree.getType(optionId);
            if (option != null) {
                rows.add(new DropdownRow(option));
                widestOption = Math.max(widestOption, font.calcWidth(option.getDisplayName(), DROPDOWN_FONT_SIZE));
            }
        }
        rowsWidth = widestOption + DROPDOWN_ROW_PADDING * 2f;
        return true;
    }

    private TextLabel dropdownRowText(SkillType option) {
        return dropdownRowText.computeIfAbsent(option.getId(),
                id -> new TextLabel(DROPDOWN_FONT_SIZE, TOOLTIP_BODY_COLOR).set(option.getDisplayName()));
    }

    private final class DropdownRow {
        final SkillType option;
        float rowX;
        float rowY;

        DropdownRow(SkillType option) {
            this.option = option;
        }

        boolean contains(float px, float py) {
            return Rects.contains(rowX, rowY, rowsWidth, ROW_HEIGHT, px, py);
        }
    }
}
