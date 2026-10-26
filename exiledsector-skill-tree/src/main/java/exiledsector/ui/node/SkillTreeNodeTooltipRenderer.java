package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.Style;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.DescriptionLine;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledlib.skills.SkillTypeEffect;
import exiledsector.items.DoctrineOverrides;
import exiledsector.items.DoctrineRewrite;
import exiledsector.items.DoctrineText;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.Subroutine;
import exiledsector.items.NodeGrantStatus;
import exiledsector.items.WormholeAnchor;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeTooltipTable;
import exiledsector.ui.TooltipExpansion;
import exiledsector.ui.TooltipTable;
import exiledsector.ui.inspect.ItemCodex;
import exiledsector.ui.util.ItalicTextLabel;
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
    private final Map<String, ItalicTextLabel> flavours = new HashMap<>();
    private final Map<String, List<SkillTreeTooltipTable>> tablesByType = new HashMap<>();

    SkillTreeNodeTooltipRenderer(FleetMemberAPI fleetMember, SkillTreePanelStyle panelStyle) {
        this.fleetMember = fleetMember;
        this.panelStyle = panelStyle;
    }

    void renderTooltip(SkillNode node, NodeAllocator.Snapshot allocation, String tooltipWarning, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        ShipSkillData skillData = allocation.skillData();
        SkillType effectiveType = node.resolveEffectiveType(skillData);
        boolean hidden = allocation.isHidden(node);
        boolean showOptionalHint = effectiveType == node.getType() && effectiveType.isOptional()
                && effectiveType.getFlavourText() == null;
        boolean expanded = TooltipExpansion.isExpanded();
        boolean allocatedSocket = node.getType().getTier() == SkillTier.SOCKET && skillData.isAllocated(node.getId());
        Item socketed = allocatedSocket ? ItemStore.lookup(skillData.getSocketedItem(node.getId())) : null;
        String refusalReason = allocation.refusalReason(node);
        List<NodeGrantStatus> socketGrantStatuses = socketed instanceof Subroutine ? allocation.nodeGrantStatuses(node.getId()) : List.of();
        boolean affinityTooltip = node.getType().getTier() == SkillTier.ROOT || RootAffinityNumber.affinityOf(effectiveType) != null;
        boolean shipStateTooltip = socketed instanceof WormholeAnchor || affinityTooltip;
        Object shipState = shipStateTooltip ? skillData : "";
        int shipStateRevision = shipStateTooltip ? skillData.revision() : 0;
        Item grantingItem = hidden ? null : grantingItem(node, allocation);
        DoctrineRewrite rewrite = hidden ? null : DoctrineOverrides.rewrite(skillData, node.getId());
        List<Object> signature = List.of(effectiveType.getId(), hidden, showOptionalHint, expanded, allocatedSocket,
                socketed == null ? "" : socketed.id(), refusalReason == null ? "" : refusalReason, shipState, shipStateRevision,
                rewrite == null ? "" : rewrite, tooltipWarning == null ? "" : tooltipWarning,
                socketGrantStatuses, grantingItem == null ? "" : grantingItem.id());

        TextLabel title = tooltipTitles.computeIfAbsent(node.getId(), id -> titleLabel()).refresh(signature, label -> {
            if (socketed != null) {
                label.setColor(socketed.rarity().color()).setWrapped(socketed.name(), NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT);
            } else {
                label.setColor(TOOLTIP_TITLE_COLOR).setWrapped(titleText(effectiveType, hidden), NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT);
            }
        });
        TextLabel body = body(tooltipBodies, node.getId(), signature,
                () -> withGrantLine(withDoctrineLines(describe(node, skillData, effectiveType, hidden, showOptionalHint, socketed, socketGrantStatuses,
                        expanded), rewrite), grantingItem),
                expanded, tooltipWarning != null ? tooltipWarning : refusalReason);

        List<SkillTreeTooltipTable> tables = showOptionalHint || hidden ? List.of() : tablesFor(font, effectiveType);
        ItalicTextLabel flavour = hidden || socketed != null ? null : flavourFor(effectiveType);
        panelStyle.drawTitleBodyTooltip(title, flavour, body, tables, footer(body, expanded), mouseX, mouseY, alphaMult);
    }

    void renderTooltipForType(SkillType type, String refusalReason, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;

        boolean expanded = TooltipExpansion.isExpanded();
        TextLabel title = typeTooltipTitles.computeIfAbsent(type.getId(), id -> titleLabel()).refresh(type.getId(),
                label -> label.setWrapped(type.getDisplayName(), NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT));
        TextLabel body = body(typeTooltipBodies, type.getId(), List.of(type.getId(), expanded, refusalReason == null ? "" : refusalReason),
                () -> SkillNode.describeMechanics(type, fleetMember.getHullSpec().getHullSize()), expanded, refusalReason);

        panelStyle.drawTitleBodyTooltip(title, flavourFor(type), body, tablesFor(font, type), footer(body, expanded), mouseX, mouseY, alphaMult);
    }

    private static TextLabel titleLabel() {
        return new TextLabel(TOOLTIP_TITLE_FONT_SIZE, TOOLTIP_TITLE_COLOR);
    }

    private TextLabel body(Map<String, TextLabel> bodies, String key, Object signature, Supplier<NodeDescription> description, boolean expanded,
                           String refusalReason) {
        TextLabel bodyLabel = bodies.computeIfAbsent(key, id -> new TextLabel(TOOLTIP_BODY_FONT_SIZE, TOOLTIP_BODY_COLOR));
        bodyLabel.refresh(signature, label -> {
            NodeDescription described = description.get();
            List<DescriptionLine> lines = new ArrayList<>(expanded ? described.all() : described.effects());
            if (refusalReason != null) {
                lines.add(new DescriptionLine(StyledText.styled(refusalReason, Style.BAD), false));
            }
            writeBodyText(label, lines);
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

    private ItalicTextLabel flavourFor(SkillType type) {
        if (flavours.containsKey(type.getId())) {
            return flavours.get(type.getId());
        }
        String flavourText = type.getFlavourText();
        ItalicTextLabel flavour = flavourText == null ? null : new ItalicTextLabel(TOOLTIP_FLAVOUR_FONT_SIZE, Misc.getGrayColor())
                .setWrapped(flavourText, NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT);
        flavours.put(type.getId(), flavour);
        return flavour;
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

    private static Item grantingItem(SkillNode node, NodeAllocator.Snapshot allocation) {
        ShipSkillData skillData = allocation.skillData();
        if (node.getType().getTier() == SkillTier.SOCKET || !skillData.isAllocated(node.getId())) {
            return null;
        }
        String grantingSocketId = ShipTreeLinks.grantingSocket(skillData, node.getId());
        NodeGrantStatus grantStatus = grantingSocketId == null ? null : allocation.nodeGrantStatus(grantingSocketId, node.getId());
        if (grantStatus == null || grantStatus.state() != NodeGrantStatus.State.ACTIVE) {
            return null;
        }
        return ItemStore.lookup(skillData.getSocketedItem(grantingSocketId));
    }

    private static NodeDescription withGrantLine(NodeDescription description, Item grantingItem) {
        if (grantingItem == null) {
            return description;
        }
        List<DescriptionLine> effects = new ArrayList<>(description.effects());
        effects.add(new DescriptionLine(Translation.msg("socketable.cache.grantedBy").arg("item", grantingItem.name()).styled(), false));
        return new NodeDescription(effects, description.details());
    }

    private NodeDescription describe(SkillNode node, ShipSkillData skillData, SkillType effectiveType, boolean hidden, boolean showOptionalHint,
                                     Item socketed, List<NodeGrantStatus> socketGrantStatuses, boolean expanded) {
        boolean allocatedSocket = node.getType().getTier() == SkillTier.SOCKET && skillData.isAllocated(node.getId());
        if (hidden) {
            return new NodeDescription(List.of(plainLine("ui.node.lockedBody"), plainLine("ui.node.lockedHint")), List.of());
        }
        if (socketed != null) {
            List<DescriptionLine> lines = new ArrayList<>();
            if (socketed instanceof WormholeAnchor anchor) {
                anchor.socketStatusLines(skillData, node.getId()).forEach(statusLine -> lines.add(new DescriptionLine(statusLine, false)));
            }
            if (socketed instanceof Subroutine subroutine) {
                StyledText limitLine = subroutine.limitLine();
                if (limitLine != null) {
                    lines.add(new DescriptionLine(limitLine, false));
                }
                subroutine.doctrineLines().forEach(doctrineLine -> lines.add(new DescriptionLine(doctrineLine, false)));
                subroutine.allocationLines(socketGrantStatuses, expanded, fleetMember.getHullSpec().getHullSize()).forEach(allocationLine -> lines.add(new DescriptionLine(allocationLine, false)));
            }
            for (StyledText effectLine : socketed.effectLines(expanded, fleetMember.getHullSpec().getHullSize())) {
                lines.add(new DescriptionLine(effectLine, false));
            }
            if (ItemCodex.existingEntryId(socketed.definitionId()) != null) {
                lines.add(plainLine("socketable.tooltip.codexHint"));
            }
            return new NodeDescription(lines, List.of());
        }
        List<DescriptionLine> effects = new ArrayList<>();
        List<DescriptionLine> details = new ArrayList<>();
        if (showOptionalHint) {
            effects.add(plainLine("ui.node.optionalHint"));
        } else {
            NodeDescription description = SkillNode.describeMechanics(effectiveType, fleetMember.getHullSpec().getHullSize());
            effects.addAll(description.effects());
            details.addAll(description.details());
            HullSize hullSize = fleetMember.getHullSpec().getHullSize();
            NodeDescription affinity = node.getType().getTier() == SkillTier.ROOT
                    ? AffinityTooltipLines.forRoot(node.getType(), skillData, hullSize)
                    : AffinityTooltipLines.forNode(node, effectiveType, skillData, hullSize);
            effects.addAll(affinity.effects());
            details.addAll(affinity.details());
        }
        if (allocatedSocket) {
            effects.add(plainLine("ui.node.socket.installHint"));
        }
        return new NodeDescription(effects, details);
    }

    private NodeDescription withDoctrineLines(NodeDescription description, DoctrineRewrite rewrite) {
        if (rewrite == null) {
            return description;
        }
        List<DescriptionLine> effects = new ArrayList<>(description.effects());
        HullSize hullSize = fleetMember.getHullSpec().getHullSize();
        for (SkillTypeEffect effect : rewrite.addedEffects(hullSize)) {
            StyledText line = effect.effect().description(effect.magnitude(), hullSize);
            if (line != null) {
                effects.add(new DescriptionLine(line, effect.effect().invertsDescriptionColours()));
            }
        }
        if (rewrite.transformed()) {
            effects.add(new DescriptionLine(Translation.msg("socketable.doctrine.originally").arg("node", rewrite.originalType().getDisplayName())
                    .styled(), false));
        }
        effects.add(new DescriptionLine(DoctrineText.rewrittenBy(rewrite), false));
        return new NodeDescription(effects, description.details());
    }

    private static DescriptionLine plainLine(String key) {
        return new DescriptionLine(Translation.styled(key), false);
    }

    private TextLabel writeBodyText(TextLabel label, List<DescriptionLine> lines) {
        return panelStyle.writeDescription(label, lines, NODE_TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT);
    }
}
