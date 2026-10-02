package exiledsector.ui.node;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillType;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeTooltipTable;
import exiledsector.ui.TooltipTable;
import exiledsector.ui.util.CachedText;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static exiledsector.ui.SkillTreePanelStyle.NODE_TOOLTIP_MAX_TEXT_WIDTH;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_BODY_COLOR;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_TITLE_COLOR;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE;

final class SkillTreeNodeTooltipRenderer {

    private static final float TOOLTIP_MAX_TEXT_HEIGHT = 800f;

    private final FleetMemberAPI member;
    private final SkillTreePanelStyle style;
    private final CachedText<String, SkillTreePanelStyle.TooltipText> tooltipTitles = new CachedText<>();
    private final CachedText<String, SkillTreePanelStyle.TooltipText> tooltipBodies = new CachedText<>();
    private final CachedText<String, SkillTreePanelStyle.TooltipText> typeTooltipTitles = new CachedText<>();
    private final CachedText<String, SkillTreePanelStyle.TooltipText> typeTooltipBodies = new CachedText<>();
    private final Map<String, List<SkillTreeTooltipTable>> tablesByType = new HashMap<>();

    SkillTreeNodeTooltipRenderer(FleetMemberAPI member, SkillTreePanelStyle style) {
        this.member = member;
        this.style = style;
    }

    void renderTooltip(SkillNode node, NodeAllocator.Snapshot tree, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        ShipSkillData data = tree.data();
        SkillType effectiveType = node.resolveEffectiveType(data);
        boolean hidden = tree.isHidden(node);
        boolean free = data.isFreeNode(node.getId());
        boolean showOptionalHint = effectiveType == node.getType() && effectiveType.isOptional()
                && effectiveType.getDescriptionOverride() == null;
        List<Object> signature = List.of(effectiveType.getId(), hidden, free, showOptionalHint);

        SkillTreePanelStyle.TooltipText title = tooltipTitles.get(node.getId(), signature,
                id -> buildTooltipText(font, titleText(effectiveType, hidden), TOOLTIP_TITLE_FONT_SIZE, TOOLTIP_TITLE_COLOR));
        SkillTreePanelStyle.TooltipText body = tooltipBodies.get(node.getId(), signature,
                id -> buildBodyText(font, bodyLines(effectiveType, hidden, free, showOptionalHint)));

        List<SkillTreeTooltipTable> tables = showOptionalHint || hidden ? List.of() : tablesFor(font, effectiveType);
        style.drawTitleBodyTooltip(title, body, tables, mouseX, mouseY, alphaMult);
    }

    void renderTooltipForType(SkillType type, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        SkillTreePanelStyle.TooltipText title = typeTooltipTitles.get(type.getId(), type.getId(),
                id -> buildTooltipText(font, type.getDisplayName(), TOOLTIP_TITLE_FONT_SIZE, TOOLTIP_TITLE_COLOR));
        SkillTreePanelStyle.TooltipText body = typeTooltipBodies.get(type.getId(), type.getId(),
                id -> buildBodyText(font, SkillNode.describeTypeLines(type, member.getHullSpec().getHullSize())));

        style.drawTitleBodyTooltip(title, body, tablesFor(font, type), mouseX, mouseY, alphaMult);
    }

    private List<SkillTreeTooltipTable> tablesFor(LazyFont font, SkillType type) {
        List<SkillTreeTooltipTable> cached = tablesByType.get(type.getId());
        if (cached != null) {
            return cached;
        }
        List<SkillTreeTooltipTable> measured = new ArrayList<>();
        for (TooltipTable table : HullModTooltipTables.forType(type, member.getHullSpec())) {
            measured.add(SkillTreeTooltipTable.measure(font, table));
        }
        tablesByType.put(type.getId(), measured);
        return measured;
    }

    private static String titleText(SkillType effectiveType, boolean hidden) {
        return hidden ? Translation.text("ui.node.lockedTitle") : effectiveType.getDisplayName();
    }

    private List<DescriptionLine> bodyLines(SkillType effectiveType, boolean hidden, boolean free, boolean showOptionalHint) {
        if (hidden) {
            return List.of(plainLine("ui.node.lockedBody"), plainLine("ui.node.lockedHint"));
        }
        List<DescriptionLine> lines = new ArrayList<>();
        if (showOptionalHint) {
            lines.add(plainLine("ui.node.optionalHint"));
        } else {
            lines.addAll(SkillNode.describeTypeLines(effectiveType, member.getHullSpec().getHullSize()));
        }
        if (free) {
            lines.add(plainLine("ui.node.freeNote"));
        }
        return lines;
    }

    private static DescriptionLine plainLine(String key) {
        return new DescriptionLine(Translation.styled(key), false);
    }

    private SkillTreePanelStyle.TooltipText buildBodyText(LazyFont font, List<DescriptionLine> lines) {
        return style.buildHighlightedWrappedText(font, lines, TOOLTIP_BODY_FONT_SIZE, NODE_TOOLTIP_MAX_TEXT_WIDTH,
                TOOLTIP_MAX_TEXT_HEIGHT, TOOLTIP_BODY_COLOR);
    }

    private SkillTreePanelStyle.TooltipText buildTooltipText(LazyFont font, String rawText, float fontSize, Color color) {
        return SkillTreePanelStyle.buildWrappedText(font, rawText, fontSize, NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT, color);
    }
}
