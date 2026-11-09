package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.IntegrationLines;
import exiledsector.skills.ShipFacts;
import exiledsector.effects.ModuleShareSetting;
import exiledsector.effects.PlayerShipFacts;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.skills.AllocationCheck;
import exiledsector.skills.CostItems;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.ItemCostLedger;
import exiledsector.skills.KnownHullMods;
import exiledsector.skills.LogisticsNodes;
import exiledsector.skills.ModuleShareExtent;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.TypeAffinity;
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
import exiledsector.ui.TooltipText;
import exiledsector.ui.inspect.ItemCodex;
import exiledsector.ui.util.TextLabel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_BODY_COLOR;
import static exiledsector.ui.SkillTreePanelStyle.TOOLTIP_TITLE_COLOR;

final class SkillTreeNodeTooltipRenderer {

    private static final float TOOLTIP_MAX_TEXT_HEIGHT = 800f;

    private final FleetMemberAPI fleetMember;
    private final SkillTreePanelStyle panelStyle;
    private final BooleanSupplier planning;
    private final Function<SkillNode, IntegrationLines.TemplateMark> templateMark;
    private final TooltipText tooltipText = SkillTreePanelStyle.tooltipText();
    private final Map<String, TextLabel> tooltipTitles = new HashMap<>();
    private final Map<String, TextLabel> tooltipBodies = new HashMap<>();
    private final Map<String, TextLabel> typeTooltipTitles = new HashMap<>();
    private final Map<String, TextLabel> typeTooltipBodies = new HashMap<>();
    private final Map<String, TextLabel> affinitySections = new HashMap<>();
    private final Map<String, TextLabel> integrationSections = new HashMap<>();
    private final Map<String, TextLabel> detailSections = new HashMap<>();
    private final Map<TextLabel, List<TextLabel>> bodySections = new HashMap<>();
    private final Map<TextLabel, Boolean> expandableBodies = new HashMap<>();
    private final Map<Boolean, TextLabel> footers = new HashMap<>();
    private final Map<String, TextLabel> flavours = new HashMap<>();
    private final Map<String, List<SkillTreeTooltipTable>> tablesByType = new HashMap<>();
    private final Map<String, List<SkillTreeTooltipTable>> riftTablesByType = new HashMap<>();
    private int riftTablesStatsRevision = -1;
    private String hoveredCostKey;
    private float hoveredItemsHeld;

    record ItemCostLine(SkillItemCost cost, Style style) {

        StyledText text() {
            return SkillNode.describeItemCost(cost, style);
        }
    }

    record LogisticsStatus(int used, int cap, boolean allocated, boolean changesAllowed) {

        static LogisticsStatus of(SkillType effectiveType, boolean allocated, AllocationCheck gate) {
            NodeEligibility.LogisticsRule rule = gate.logisticsRule();
            if (rule == null || !LogisticsNodes.isLogistics(effectiveType)) {
                return null;
            }
            return new LogisticsStatus(gate.allocatedLogisticsNodes(), rule.cap(), allocated, rule.changesAllowed());
        }

        List<StyledText> lines() {
            StyledText countLine = Translation.msg("node.logistics.count").arg("used", used).arg("cap", cap).styled();
            if (!allocated || changesAllowed) {
                return List.of(countLine);
            }
            return List.of(countLine, StyledText.styled(Translation.text("node.logistics.removeUndocked"), Style.BAD));
        }
    }

    SkillTreeNodeTooltipRenderer(FleetMemberAPI fleetMember, SkillTreePanelStyle panelStyle, BooleanSupplier planning,
                                 Function<SkillNode, IntegrationLines.TemplateMark> templateMark) {
        this.fleetMember = fleetMember;
        this.panelStyle = panelStyle;
        this.planning = planning;
        this.templateMark = templateMark;
    }

    void renderTooltip(SkillNode node, NodeAllocator.Snapshot allocation, String tooltipWarning, float mouseX, float mouseY, float alphaMult) {
        if (!tooltipText.fontsLoaded()) return;

        ShipSkillData skillData = allocation.skillData();
        SkillType effectiveType = node.resolveEffectiveType(skillData);
        boolean hidden = allocation.isHidden(node);
        boolean showChoiceHint = effectiveType == node.getType() && effectiveType.isChoice()
                && effectiveType.getFlavourText() == null;
        boolean expanded = TooltipExpansion.isExpanded();
        boolean allocatedSocket = node.getType().getTier() == SkillTier.SOCKET && skillData.isAllocated(node.getId());
        Item socketed = allocatedSocket ? ItemStore.lookup(skillData.getSocketedItem(node.getId())) : null;
        ItemCostLine costLine = hidden || socketed != null ? null : itemCostLine(node, null, allocation);
        String refusalReason = withoutItemCostRefusal(allocation.refusalReason(node), costLine, () -> allocation.gate().allocation(node));
        List<NodeGrantStatus> socketGrantStatuses = socketed instanceof Subroutine ? allocation.nodeGrantStatuses(node.getId()) : List.of();
        boolean affinityTooltip = TypeAffinity.of(effectiveType) != null;
        boolean shipStateTooltip = socketed instanceof WormholeAnchor || affinityTooltip;
        Object shipState = shipStateTooltip ? skillData : "";
        int shipStateRevision = shipStateTooltip ? skillData.revision() : 0;
        Item grantingItem = hidden ? null : grantingItem(node, allocation);
        DoctrineRewrite rewrite = hidden ? null : DoctrineOverrides.rewrite(skillData, node.getId());
        LogisticsStatus logistics = hidden || socketed != null ? null : LogisticsStatus.of(effectiveType, skillData.isAllocated(node.getId()),
                allocation.gate());
        IntegrationLines.State integration = integrationState(skillData, node);
        boolean previewsIntegration = !hidden && !showChoiceHint && socketed == null;
        List<Object> signature = List.of(effectiveType.getId(), hidden, showChoiceHint, expanded, allocatedSocket,
                socketed == null ? "" : socketed.id(), refusalReason == null ? "" : refusalReason, shipState, shipStateRevision,
                rewrite == null ? "" : rewrite, tooltipWarning == null ? "" : tooltipWarning,
                socketGrantStatuses, grantingItem == null ? "" : grantingItem.id(), costLine == null ? "" : costLine, logistics == null ? "" : logistics,
                integration);

        TextLabel title = tooltipTitles.computeIfAbsent(node.getId(), id -> titleLabel()).refresh(signature, label -> {
            if (socketed != null) {
                label.setColor(socketed.rarity().color()).setWrapped(socketed.name(), tooltipText.nodeMaxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT);
            } else {
                label.setColor(TOOLTIP_TITLE_COLOR).setWrapped(titleText(effectiveType, hidden), tooltipText.nodeMaxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT);
            }
        });
        TextLabel body = body(tooltipBodies, node.getId(), signature,
                () -> withLeadingLines(withCostLine(withGrantLine(withDoctrineLines(describe(node, skillData, effectiveType, hidden, showChoiceHint,
                        socketed, socketGrantStatuses, expanded), rewrite), grantingItem), costLine), logistics == null ? List.of() : logistics.lines()),
                () -> previewsIntegration ? IntegrationLines.preview(effectiveType, fleetMember.getHullSpec().getHullSize(), integration) : List.of(),
                expanded, tooltipWarning != null ? tooltipWarning : refusalReason);
        TextLabel affinitySection = showsAffinity(effectiveType, hidden, showChoiceHint, socketed)
                ? affinitySections.computeIfAbsent(node.getId(), id -> new TextLabel(tooltipText.body(), TOOLTIP_BODY_COLOR))
                        .refresh(signature, label -> writeBodyText(label, affinityLines(effectiveType, skillData).details()))
                : null;

        List<SkillTreeTooltipTable> tables = showChoiceHint || hidden ? List.of() : tablesFor(effectiveType, allocation.statsRevision());
        TextLabel flavour = hidden || socketed != null ? null : flavourFor(effectiveType);
        boolean expandable = expandableBodies.getOrDefault(body, false) || affinitySection != null;
        panelStyle.drawTitleBodyTooltip(title, flavour, body, bodySections.getOrDefault(body, List.of()), tables, expanded ? affinitySection : null,
                footer(expandable, expanded),
                mouseX, mouseY, alphaMult);
    }

    void renderTooltipForType(SkillNode choicePickerNode, SkillType type, NodeAllocator.Snapshot allocation, String optionRefusalReason,
                              float mouseX, float mouseY, float alphaMult) {
        if (!tooltipText.fontsLoaded()) return;

        boolean expanded = TooltipExpansion.isExpanded();
        ItemCostLine costLine = itemCostLine(choicePickerNode, type, allocation);
        String refusalReason = withoutItemCostRefusal(optionRefusalReason, costLine,
                () -> TreeAllocationSession.optionVerdict(choicePickerNode, type, allocation));
        TextLabel title = typeTooltipTitles.computeIfAbsent(type.getId(), id -> titleLabel()).refresh(type.getId(),
                label -> label.setWrapped(type.getDisplayName(), tooltipText.nodeMaxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT));
        TextLabel body = body(typeTooltipBodies, type.getId(), List.of(type.getId(), expanded, refusalReason == null ? "" : refusalReason,
                        costLine == null ? "" : costLine),
                () -> withCostLine(withModuleShareLine(SkillNode.describeMechanics(type, fleetMember.getHullSpec().getHullSize(), false,
                        this::listsConflictingHullMod), type), costLine),
                List::of, expanded, refusalReason);

        panelStyle.drawTitleBodyTooltip(title, flavourFor(type), body, List.of(), tablesFor(type, allocation.statsRevision()), null,
                footer(expandableBodies.getOrDefault(body, false), expanded), mouseX, mouseY, alphaMult);
    }

    private TextLabel titleLabel() {
        return new TextLabel(tooltipText.title(), TOOLTIP_TITLE_COLOR);
    }

    private TextLabel body(Map<String, TextLabel> bodies, String key, Object signature, Supplier<NodeDescription> description,
                           Supplier<List<DescriptionLine>> integrationPreview, boolean expanded, String refusalReason) {
        TextLabel bodyLabel = bodies.computeIfAbsent(key, id -> sectionLabel());
        bodyLabel.refresh(signature, label -> {
            NodeDescription described = description.get();
            List<DescriptionLine> preview = integrationPreview.get();
            boolean sectioned = expanded && !preview.isEmpty();
            List<DescriptionLine> lines = new ArrayList<>(expanded && !sectioned ? described.all() : described.effects());
            if (refusalReason != null) {
                lines.add(new DescriptionLine(StyledText.styled(refusalReason, Style.BAD), false));
            }
            writeBodyText(label, lines);
            expandableBodies.put(label, !described.details().isEmpty() || !preview.isEmpty());
            bodySections.put(label, sectioned ? sections(key, preview, described.details()) : List.of());
        });
        return bodyLabel;
    }

    private List<TextLabel> sections(String key, List<DescriptionLine> integrationPreview, List<DescriptionLine> details) {
        TextLabel integrationSection = writeBodyText(integrationSections.computeIfAbsent(key, id -> sectionLabel()), integrationPreview);
        if (details.isEmpty()) {
            return List.of(integrationSection);
        }
        return List.of(integrationSection, writeBodyText(detailSections.computeIfAbsent(key, id -> sectionLabel()), details));
    }

    private TextLabel sectionLabel() {
        return new TextLabel(tooltipText.body(), TOOLTIP_BODY_COLOR);
    }

    private TextLabel footer(boolean expandable, boolean expanded) {
        if (!expandable) {
            return null;
        }
        return footers.computeIfAbsent(expanded, footerExpanded -> writeBodyText(new TextLabel(tooltipText.hint(), TOOLTIP_BODY_COLOR),
                List.of(plainLine(Boolean.TRUE.equals(footerExpanded) ? "ui.tooltip.collapseHint" : "ui.tooltip.expandHint"))));
    }

    private TextLabel flavourFor(SkillType type) {
        if (flavours.containsKey(type.getId())) {
            return flavours.get(type.getId());
        }
        String flavourText = flavourText(type, fleetMember);
        TextLabel flavour = flavourText == null ? null : new TextLabel(tooltipText.flavour(), Misc.getGrayColor())
                .setWrapped(flavourText, tooltipText.nodeMaxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT);
        flavours.put(type.getId(), flavour);
        return flavour;
    }

    static String flavourText(SkillType type, FleetMemberAPI fleetMember) {
        return type.getFlavourText(Misc.isAutomated(fleetMember));
    }

    private List<SkillTreeTooltipTable> tablesFor(SkillType type, int statsRevision) {
        if (statsRevision != riftTablesStatsRevision) {
            riftTablesByType.clear();
            riftTablesStatsRevision = statsRevision;
        }
        List<SkillTreeTooltipTable> cached = tablesByType.get(type.getId());
        if (cached == null) {
            cached = riftTablesByType.get(type.getId());
        }
        if (cached != null) {
            return cached;
        }
        boolean riftStrike = RiftStrikeTooltipTable.appliesTo(type);
        List<TooltipTable> tables = riftStrike ? RiftStrikeTooltipTable.forType(type, fleetMember)
                : HullModTooltipTables.forType(type, fleetMember.getHullSpec());
        List<SkillTreeTooltipTable> measured = new ArrayList<>();
        for (TooltipTable table : tables) {
            measured.add(SkillTreeTooltipTable.measure(tooltipText, table));
        }
        (riftStrike ? riftTablesByType : tablesByType).put(type.getId(), measured);
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
        effects.add(new DescriptionLine(Translation.msg("subroutine.grant.grantedBy").arg("item", grantingItem.name()).styled(), false));
        return new NodeDescription(effects, description.details());
    }

    void endHover() {
        hoveredCostKey = null;
    }

    private ItemCostLine itemCostLine(SkillNode node, SkillType option, NodeAllocator.Snapshot allocation) {
        ShipSkillData skillData = allocation.skillData();
        boolean allocated = skillData.isAllocated(node.getId());
        SkillItemCost charged = allocated ? ItemCostLedger.paidItemCost(skillData, node.getId()) : null;
        if (allocated && option == null) {
            return paidCostLine(charged);
        }
        SkillItemCost cost = allocation.gate().itemCost(node, option);
        if (cost == null) {
            return null;
        }
        float refund = charged != null && charged.itemId().equals(cost.itemId()) ? charged.quantity() : 0f;
        String hoverKey = node.getId() + '/' + (option == null ? "" : option.getId()) + '/' + skillData.revision();
        return unpaidCostLine(cost, heldOnHover(hoverKey, cost.itemId()) + refund);
    }

    private float heldOnHover(String hoverKey, String itemId) {
        if (!hoverKey.equals(hoveredCostKey)) {
            hoveredCostKey = hoverKey;
            hoveredItemsHeld = CostItems.held(PlayerFleet.cargoOrNull(), itemId);
        }
        return hoveredItemsHeld;
    }

    static ItemCostLine paidCostLine(SkillItemCost charged) {
        return charged == null ? null : new ItemCostLine(charged, null);
    }

    static ItemCostLine unpaidCostLine(SkillItemCost cost, float available) {
        return new ItemCostLine(cost, available >= cost.quantity() ? Style.GOOD : Style.HIGHLIGHT);
    }

    static String withoutItemCostRefusal(String refusalReason, ItemCostLine costLine, Supplier<AllocationCheck.Verdict> verdict) {
        if (refusalReason == null || costLine == null) {
            return refusalReason;
        }
        NodeEligibility.Block block = verdict.get().block();
        return block != null && block.kind() == NodeEligibility.Kind.ITEM_COST ? null : refusalReason;
    }

    private static NodeDescription withLeadingLines(NodeDescription description, List<StyledText> leadingLines) {
        if (leadingLines.isEmpty()) {
            return description;
        }
        List<DescriptionLine> effects = new ArrayList<>(description.effects().size() + leadingLines.size());
        leadingLines.forEach(line -> effects.add(new DescriptionLine(line, false)));
        effects.addAll(description.effects());
        return new NodeDescription(effects, description.details());
    }

    private static NodeDescription withCostLine(NodeDescription description, ItemCostLine costLine) {
        if (costLine == null) {
            return description;
        }
        List<DescriptionLine> effects = new ArrayList<>(description.effects().size() + 1);
        effects.add(new DescriptionLine(costLine.text(), false));
        effects.addAll(description.effects());
        return new NodeDescription(effects, description.details());
    }

    private NodeDescription withModuleShareLine(NodeDescription description, SkillType type) {
        StyledText moduleShareLine = moduleShareLine(type, false);
        if (moduleShareLine == null) {
            return description;
        }
        List<DescriptionLine> details = new ArrayList<>(description.details());
        details.add(new DescriptionLine(moduleShareLine, false));
        return new NodeDescription(description.effects(), details);
    }

    private IntegrationLines.State integrationState(ShipSkillData skillData, SkillNode node) {
        return IntegrationLines.State.of(skillData, node, !planning.getAsBoolean(), templateMark.apply(node));
    }

    private StyledText moduleShareLine(SkillType type, boolean integrated) {
        ShipHullSpecAPI hullSpec = fleetMember.getHullSpec();
        if (!ShipFacts.hasModuleSlots(hullSpec)) {
            return null;
        }
        return moduleShareLine(type.moduleShareExtent(hullSpec.getHullSize(), integrated), ModuleShareSetting.percentFor(fleetMember.isStation()));
    }

    static StyledText moduleShareLine(ModuleShareExtent extent, int sharePercent) {
        if (extent == null) {
            return null;
        }
        return switch (sharePercent == 0 ? ModuleShareExtent.NONE : extent) {
            case ALL -> Translation.msg("node.moduleShare.all").arg("percent", sharePercent).styled();
            case SOME -> Translation.msg("node.moduleShare.some").arg("percent", sharePercent).styled();
            case NONE -> Translation.styled("node.moduleShare.none");
        };
    }

    private NodeDescription describe(SkillNode node, ShipSkillData skillData, SkillType effectiveType, boolean hidden, boolean showChoiceHint,
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
                subroutine.kindLines().forEach(kindLine -> lines.add(new DescriptionLine(kindLine, false)));
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
        if (showChoiceHint) {
            effects.add(plainLine("ui.node.choiceHint"));
        } else {
            IntegrationLines.State integration = integrationState(skillData, node);
            NodeDescription description = IntegrationLines.describe(effectiveType, fleetMember.getHullSpec().getHullSize(), integration,
                    this::listsConflictingHullMod);
            effects.addAll(description.effects());
            details.addAll(description.details());
            if (showsAffinity(effectiveType, false, false, null)) {
                effects.addAll(affinityLines(effectiveType, skillData).effects());
            }
            StyledText moduleShareLine = moduleShareLine(effectiveType, integration.integrated());
            if (moduleShareLine != null) {
                details.add(new DescriptionLine(moduleShareLine, false));
            }
        }
        if (allocatedSocket) {
            effects.add(plainLine("ui.node.socket.installHint"));
        }
        return new NodeDescription(effects, details);
    }

    private boolean listsConflictingHullMod(String hullModId) {
        return KnownHullMods.knownByPlayer(hullModId) || PlayerShipFacts.hasHullMod(fleetMember.getVariant(), hullModId);
    }

    private static boolean showsAffinity(SkillType effectiveType, boolean hidden, boolean showChoiceHint, Item socketed) {
        return !hidden && !showChoiceHint && socketed == null && TypeAffinity.of(effectiveType) != null;
    }

    private NodeDescription affinityLines(SkillType effectiveType, ShipSkillData skillData) {
        List<EffectContribution> shipContributions = SkillTreeHullMod.contributions(skillData, fleetMember, fleetMember.getVariant());
        return AffinityTooltipLines.forType(effectiveType, shipContributions, fleetMember.getHullSpec().getHullSize());
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
        if (rewrite.rewritten()) {
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
        return panelStyle.writeDescription(label, lines, tooltipText.nodeMaxTextWidth(), TOOLTIP_MAX_TEXT_HEIGHT);
    }
}
