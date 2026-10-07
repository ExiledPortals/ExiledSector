package exiledsector.ui.node;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeTooltipTable;
import exiledsector.ui.TooltipExpansion;
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
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_FLAVOUR_FONT_SIZE;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_TITLE_COLOR;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE;

final class SkillTreeNodeTooltipRenderer {

    private static final float TOOLTIP_MAX_TEXT_HEIGHT = 800f;

    private record Body(SkillTreePanelStyle.TooltipText bodyText, boolean expandable) {
    }

    private final FleetMemberAPI fleetMember;
    private final SkillTreePanelStyle panelStyle;
    private final CachedText<String, SkillTreePanelStyle.TooltipText> tooltipTitles = new CachedText<>();
    private final CachedText<String, Body> tooltipBodies = new CachedText<>();
    private final CachedText<String, SkillTreePanelStyle.TooltipText> typeTooltipTitles = new CachedText<>();
    private final CachedText<String, Body> typeTooltipBodies = new CachedText<>();
    private final CachedText<Boolean, SkillTreePanelStyle.TooltipText> footers = new CachedText<>();
    private final CachedText<String, SkillTreePanelStyle.TooltipText> flavours = new CachedText<>();
    private final Map<String, List<SkillTreeTooltipTable>> tablesByType = new HashMap<>();

    SkillTreeNodeTooltipRenderer(FleetMemberAPI fleetMember, SkillTreePanelStyle panelStyle) {
        this.fleetMember = fleetMember;
        this.panelStyle = panelStyle;
    }

    void renderTooltip(SkillNode node, NodeAllocator.Snapshot allocation, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        ShipSkillData skillData = allocation.skillData();
        SkillType effectiveType = node.resolveEffectiveType(skillData);
        boolean hidden = allocation.isHidden(node);
        boolean showOptionalHint = effectiveType == node.getType() && effectiveType.isOptional()
                && effectiveType.getDescriptionOverride() == null;
        boolean expanded = TooltipExpansion.isExpanded();
        boolean allocatedSocket = node.getType().getTier() == SkillTier.SOCKET && skillData.isAllocated(node.getId());
        Socketable socketed = allocatedSocket ? SocketableStore.lookup(skillData.getSocketedItem(node.getId())) : null;
        List<Object> signature = List.of(effectiveType.getId(), hidden, showOptionalHint, expanded, allocatedSocket,
                socketed == null ? "" : socketed.id());

        SkillTreePanelStyle.TooltipText title = tooltipTitles.get(node.getId(), signature,
                id -> socketed != null
                        ? buildTooltipText(font, socketed.name(), TOOLTIP_TITLE_FONT_SIZE, socketed.rarity().color())
                        : buildTooltipText(font, titleText(effectiveType, hidden), TOOLTIP_TITLE_FONT_SIZE, TOOLTIP_TITLE_COLOR));
        Body body = tooltipBodies.get(node.getId(), signature,
                id -> buildBody(font, describe(effectiveType, hidden, showOptionalHint, allocatedSocket, socketed, expanded), expanded));

        List<SkillTreeTooltipTable> tables = showOptionalHint || hidden ? List.of() : tablesFor(font, effectiveType);
        SkillTreePanelStyle.TooltipText flavour = hidden || socketed != null ? null : flavourFor(font, effectiveType);
        panelStyle.drawTitleBodyTooltip(title, flavour, body.bodyText(), tables, footer(font, body, expanded), mouseX, mouseY, alphaMult);
    }

    void renderTooltipForType(SkillType type, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        boolean expanded = TooltipExpansion.isExpanded();
        SkillTreePanelStyle.TooltipText title = typeTooltipTitles.get(type.getId(), type.getId(),
                id -> buildTooltipText(font, type.getDisplayName(), TOOLTIP_TITLE_FONT_SIZE, TOOLTIP_TITLE_COLOR));
        Body body = typeTooltipBodies.get(type.getId(), List.of(type.getId(), expanded),
                id -> buildBody(font, SkillNode.describeType(type, fleetMember.getHullSpec().getHullSize()), expanded));

        panelStyle.drawTitleBodyTooltip(title, flavourFor(font, type), body.bodyText(), tablesFor(font, type), footer(font, body, expanded), mouseX, mouseY, alphaMult);
    }

    private Body buildBody(LazyFont font, NodeDescription description, boolean expanded) {
        List<DescriptionLine> lines = expanded ? description.all() : description.effects();
        return new Body(buildBodyText(font, lines), !description.details().isEmpty());
    }

    private SkillTreePanelStyle.TooltipText footer(LazyFont font, Body body, boolean expanded) {
        if (!body.expandable()) {
            return null;
        }
        return footers.get(expanded, expanded,
                footerExpanded -> buildBodyText(font, List.of(plainLine(footerExpanded ? "ui.tooltip.collapseHint" : "ui.tooltip.expandHint"))));
    }

    private SkillTreePanelStyle.TooltipText flavourFor(LazyFont font, SkillType type) {
        return flavours.get(type.getId(), type.getId(), id -> {
            String flavour = type.getFlavourText();
            return flavour == null ? null : SkillTreePanelStyle.buildWrappedText(font, flavour, TOOLTIP_FLAVOUR_FONT_SIZE,
                    NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT, Misc.getGrayColor());
        });
    }

    private List<SkillTreeTooltipTable> tablesFor(LazyFont font, SkillType type) {
        List<SkillTreeTooltipTable> cached = tablesByType.get(type.getId());
        if (cached != null) {
            return cached;
        }
        List<SkillTreeTooltipTable> measured = new ArrayList<>();
        for (TooltipTable table : HullModTooltipTables.forType(type, fleetMember.getHullSpec())) {
            measured.add(SkillTreeTooltipTable.measure(font, table));
        }
        tablesByType.put(type.getId(), measured);
        return measured;
    }

    private static String titleText(SkillType effectiveType, boolean hidden) {
        return hidden ? Translation.text("ui.node.lockedTitle") : effectiveType.getDisplayName();
    }

    private NodeDescription describe(SkillType effectiveType, boolean hidden, boolean showOptionalHint, boolean allocatedSocket,
                                     Socketable socketed, boolean expanded) {
        if (hidden) {
            return new NodeDescription(List.of(plainLine("ui.node.lockedBody"), plainLine("ui.node.lockedHint")), List.of());
        }
        if (socketed != null) {
            List<DescriptionLine> lines = socketed.effectLines(expanded).stream().map(line -> new DescriptionLine(line, false)).toList();
            return new NodeDescription(lines, List.of());
        }
        List<DescriptionLine> effects = new ArrayList<>();
        List<DescriptionLine> details = new ArrayList<>();
        if (showOptionalHint) {
            effects.add(plainLine("ui.node.optionalHint"));
        } else {
            NodeDescription description = SkillNode.describeType(effectiveType, fleetMember.getHullSpec().getHullSize());
            effects.addAll(description.effects());
            details.addAll(description.details());
        }
        if (allocatedSocket) {
            effects.add(plainLine("ui.node.socket.installHint"));
        }
        return new NodeDescription(effects, details);
    }

    private static DescriptionLine plainLine(String key) {
        return new DescriptionLine(Translation.styled(key), false);
    }

    private SkillTreePanelStyle.TooltipText buildBodyText(LazyFont font, List<DescriptionLine> lines) {
        return panelStyle.buildHighlightedWrappedText(font, lines, TOOLTIP_BODY_FONT_SIZE, NODE_TOOLTIP_MAX_TEXT_WIDTH,
                TOOLTIP_MAX_TEXT_HEIGHT, TOOLTIP_BODY_COLOR);
    }

    private SkillTreePanelStyle.TooltipText buildTooltipText(LazyFont font, String rawText, float fontSize, Color color) {
        return SkillTreePanelStyle.buildWrappedText(font, rawText, fontSize, NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT, color);
    }
}
