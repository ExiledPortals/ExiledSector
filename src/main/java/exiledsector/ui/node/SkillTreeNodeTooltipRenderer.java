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
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static exiledsector.ui.SkillTreePanelStyle.NODE_TOOLTIP_MAX_TEXT_WIDTH;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_BODY_COLOR;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_FLAVOUR_FONT_SIZE;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_TITLE_COLOR;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE;

final class SkillTreeNodeTooltipRenderer {

    private static final float TOOLTIP_MAX_TEXT_HEIGHT = 800f;

    private final FleetMemberAPI fleetMember;
    private final SkillTreePanelStyle panelStyle;
    private final Map<String, TextLabel> tooltipTitles = new HashMap<>();
    private final Map<String, TextLabel> tooltipBodies = new HashMap<>();
    private final Map<String, TextLabel> typeTooltipTitles = new HashMap<>();
    private final Map<String, TextLabel> typeTooltipBodies = new HashMap<>();
    private final Map<TextLabel, Boolean> expandableBodies = new HashMap<>();
    private final Map<Boolean, TextLabel> footers = new HashMap<>();
    private final Map<String, TextLabel> flavours = new HashMap<>();
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

        TextLabel title = tooltipTitles.computeIfAbsent(node.getId(), id -> titleLabel()).refresh(signature, label -> {
            if (socketed != null) {
                label.setColor(socketed.rarity().color()).setWrapped(socketed.name(), NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT);
            } else {
                label.setColor(TOOLTIP_TITLE_COLOR).setWrapped(titleText(effectiveType, hidden), NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT);
            }
        });
        TextLabel body = body(tooltipBodies, node.getId(), signature,
                () -> describe(effectiveType, hidden, showOptionalHint, allocatedSocket, socketed, expanded), expanded);

        List<SkillTreeTooltipTable> tables = showOptionalHint || hidden ? List.of() : tablesFor(font, effectiveType);
        TextLabel flavour = hidden || socketed != null ? null : flavourFor(effectiveType);
        panelStyle.drawTitleBodyTooltip(title, flavour, body, tables, footer(body, expanded), mouseX, mouseY, alphaMult);
    }

    void renderTooltipForType(SkillType type, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        boolean expanded = TooltipExpansion.isExpanded();
        TextLabel title = typeTooltipTitles.computeIfAbsent(type.getId(), id -> titleLabel()).refresh(type.getId(),
                label -> label.setWrapped(type.getDisplayName(), NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT));
        TextLabel body = body(typeTooltipBodies, type.getId(), List.of(type.getId(), expanded),
                () -> SkillNode.describeType(type, fleetMember.getHullSpec().getHullSize()), expanded);

        panelStyle.drawTitleBodyTooltip(title, flavourFor(type), body, tablesFor(font, type), footer(body, expanded), mouseX, mouseY, alphaMult);
    }

    private static TextLabel titleLabel() {
        return new TextLabel(TOOLTIP_TITLE_FONT_SIZE, TOOLTIP_TITLE_COLOR);
    }

    private TextLabel body(Map<String, TextLabel> bodies, String key, Object signature, Supplier<NodeDescription> description, boolean expanded) {
        TextLabel bodyLabel = bodies.computeIfAbsent(key, id -> new TextLabel(TOOLTIP_BODY_FONT_SIZE, TOOLTIP_BODY_COLOR));
        bodyLabel.refresh(signature, label -> {
            NodeDescription described = description.get();
            writeBodyText(label, expanded ? described.all() : described.effects());
            expandableBodies.put(label, !described.details().isEmpty());
        });
        return bodyLabel;
    }

    private TextLabel footer(TextLabel body, boolean expanded) {
        if (!expandableBodies.getOrDefault(body, false)) {
            return null;
        }
        return footers.computeIfAbsent(expanded, footerExpanded -> writeBodyText(new TextLabel(TOOLTIP_BODY_FONT_SIZE, TOOLTIP_BODY_COLOR),
                List.of(plainLine(Boolean.TRUE.equals(footerExpanded) ? "ui.tooltip.collapseHint" : "ui.tooltip.expandHint"))));
    }

    private TextLabel flavourFor(SkillType type) {
        String flavour = type.getFlavourText();
        if (flavour == null) {
            return null;
        }
        return flavours.computeIfAbsent(type.getId(), id -> new TextLabel(TOOLTIP_FLAVOUR_FONT_SIZE, Misc.getGrayColor())
                .setWrapped(flavour, NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT));
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
            List<DescriptionLine> lines = socketed.effectLines(expanded, fleetMember.getHullSpec().getHullSize()).stream().map(line -> new DescriptionLine(line, false)).toList();
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

    private TextLabel writeBodyText(TextLabel label, List<DescriptionLine> lines) {
        return panelStyle.writeDescription(label, lines, NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT);
    }
}
