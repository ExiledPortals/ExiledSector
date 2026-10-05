package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.layout.SkillTreeObject;
import exiledsector.skills.skilleffect.WeaponEffectTooltipAggregator;
import exiledsector.skills.tags.SkillTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SkillNode extends SkillTreeObject {

    private final SkillType type;
    private final List<String> connectedNodeIds;
    private final String ringBeltPath;
    private final String ringBeltColor;
    private final Float ringBeltWidth;
    private final String wormholeColor;
    private final String pairedNodeId;

    public SkillNode(String id, SkillType type, List<String> connectedNodeIds, float offsetX, float offsetY) {
        this(id, type, connectedNodeIds, offsetX, offsetY, SkillNodeDecoration.NONE);
    }

    public SkillNode(String id, SkillType type, List<String> connectedNodeIds, float offsetX, float offsetY, SkillNodeDecoration decoration) {
        this(id, type, connectedNodeIds, offsetX, offsetY, decoration, Collections.emptyList());
    }

    public SkillNode(String id, SkillType type, List<String> connectedNodeIds, float offsetX, float offsetY,
                     SkillNodeDecoration decoration, List<String> tags) {
        super(id, offsetX, offsetY, tags);
        this.type = type;
        this.connectedNodeIds = connectedNodeIds == null ? Collections.emptyList() : connectedNodeIds;
        this.ringBeltPath = decoration.ringBeltPath();
        this.ringBeltColor = decoration.ringBeltColor();
        this.ringBeltWidth = decoration.ringBeltWidth();
        this.wormholeColor = decoration.wormholeColor();
        this.pairedNodeId = decoration.pairedNodeId();
    }

    public SkillType getType() {
        return type;
    }

    public String getDisplayName() {
        return type.getDisplayName();
    }

    public String getIconPath() {
        return type.getIconPath();
    }

    public static List<DescriptionLine> describeTypeLines(SkillType type, HullSize hullSize) {
        return describeType(type, hullSize).all();
    }

    public static NodeDescription describeType(SkillType type, HullSize hullSize) {
        List<DescriptionLine> effects = new ArrayList<>();
        addLine(effects, type.getDescriptionText(), false);
        List<SkillTypeEffect> described = hullSize == null ? type.getEffects() : type.effectsFor(hullSize);
        for (SkillTypeEffect effect : WeaponEffectTooltipAggregator.collapse(described)) {
            addLine(effects, effect.effect().description(effect.magnitude(), hullSize), effect.effect().lowerIsBetter());
        }
        addLine(effects, describeTemporaryDuration(type), false);

        List<DescriptionLine> details = new ArrayList<>();
        for (SkillTypeEffect effect : described) {
            addLine(details, effect.effect().deallocationWarning(effect.magnitude()), false);
        }
        for (String tag : type.getTags()) {
            if (SkillTags.isHullRequirement(tag)) {
                addLine(details, Translation.styled("node.requires." + tag), false);
            }
        }
        addLine(details, describeHullSizes(type), false);
        addLine(details, describeItemCost(type), false);
        addLine(details, describeExclusivity(type), false);
        return new NodeDescription(effects, details);
    }

    private static void addLine(List<DescriptionLine> lines, StyledText text, boolean lowerIsBetter) {
        if (text != null) {
            lines.add(new DescriptionLine(text, lowerIsBetter));
        }
    }

    private static StyledText describeTemporaryDuration(SkillType type) {
        Float seconds = type.getTemporaryAfterDeploymentSeconds();
        if (seconds == null) {
            return null;
        }
        return Translation.msg("node.temporary").arg("seconds", seconds).styled();
    }

    private static StyledText describeHullSizes(SkillType type) {
        Set<HullSize> hullSizes = type.getRequiredHullSizes();
        if (hullSizes.isEmpty()) {
            return null;
        }
        List<StyledText> names = new ArrayList<>();
        for (HullSize hullSize : hullSizes) {
            names.add(StyledText.of(Translation.text("hullSize." + hullSize.name())));
        }
        return Translation.msg("node.hullSizes").count(hullSizes.size()).arg("hullSizes", Translation.list(names)).styled();
    }

    private static StyledText describeItemCost(SkillType type) {
        SkillItemCost itemCost = type.getItemCost();
        if (itemCost == null) {
            return null;
        }
        return Translation.msg("node.itemCost").arg("quantity", itemCost.formattedQuantity()).arg("item", itemCost.commodityName()).styled();
    }

    private static StyledText describeExclusivity(SkillType type) {
        Set<String> hullModNames = new LinkedHashSet<>();
        for (String hullModId : type.getExclusiveHullModIds()) {
            String name = HullModNames.loadedDisplayName(hullModId);
            if (name != null) {
                hullModNames.add(name);
            }
        }
        Set<String> nodeNames = new LinkedHashSet<>();
        for (String skillTypeId : type.getExclusiveSkillTypeIds()) {
            if (SkillTree.getType(skillTypeId) == null) {
                nodeNames.add(skillTypeId);
            }
        }
        for (SkillType other : SkillTree.getAllTypes().values()) {
            if (other != type && type.isExclusiveWith(other)) {
                nodeNames.add(other.getDisplayName());
            }
        }

        List<StyledText> lines = new ArrayList<>();
        if (!hullModNames.isEmpty()) {
            lines.add(exclusivityList("node.exclusive.hullmods", Style.HULLMOD, hullModNames));
        }
        if (!nodeNames.isEmpty()) {
            lines.add(exclusivityList("node.exclusive.nodes", Style.NODE, nodeNames));
        }
        return lines.isEmpty() ? null : StyledText.join(StyledText.of("\n\n"), lines);
    }

    private static StyledText exclusivityList(String key, Style style, Set<String> names) {
        List<StyledText> lines = new ArrayList<>();
        lines.add(Translation.msg(key).count(names.size()).styled());
        for (String name : names) {
            lines.add(Translation.msg("node.exclusive.item").arg("name", StyledText.styled(name, style)).styled());
        }
        return StyledText.join(StyledText.of("\n"), lines);
    }

    public SkillType resolveEffectiveType(ShipSkillData data) {
        if (!type.isOptional()) return type;
        String selectedId = data.getOptionalSelection(getId());
        if (selectedId == null || !type.getOptionalOptionIds().contains(selectedId)) return type;
        SkillType chosen = SkillTree.getType(selectedId);
        return chosen != null ? chosen : type;
    }

    public Set<String> effectiveTags(ShipSkillData data) {
        return effectiveTags(data == null ? null : resolveEffectiveType(data));
    }

    public Set<String> effectiveTags(SkillType chosenOption) {
        Set<String> combined = new LinkedHashSet<>(getTags());
        combined.addAll(type.getTags());
        if (chosenOption != null) {
            combined.addAll(chosenOption.getTags());
        }
        return Collections.unmodifiableSet(combined);
    }

    public List<String> getConnectedNodeIds() {
        return connectedNodeIds;
    }

    public SkillNode withoutConnectionsTo(Set<String> removedNodeIds) {
        List<String> kept = connectedNodeIds.stream().filter(id -> !removedNodeIds.contains(id)).toList();
        if (kept.size() == connectedNodeIds.size()) {
            return this;
        }
        return new SkillNode(getId(), type, kept, getX(), getY(),
                new SkillNodeDecoration(ringBeltPath, ringBeltColor, ringBeltWidth, wormholeColor, pairedNodeId), getTags());
    }

    public float getOffsetX() {
        return getX();
    }

    public float getOffsetY() {
        return getY();
    }

    public String getRingBeltPath() {
        return ringBeltPath;
    }

    public String getRingBeltColor() {
        return ringBeltColor;
    }

    public Float getRingBeltWidth() {
        return ringBeltWidth;
    }

    public String getWormholeColor() {
        return wormholeColor;
    }

    public String getPairedNodeId() {
        return pairedNodeId;
    }
}
