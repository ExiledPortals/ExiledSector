package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledlib.i18n.Style;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledlib.skills.AffinitySteps;
import exiledlib.skills.DescriptionLine;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.skills.NodeReplacements;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.radius.RadiusSize;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

public final class Subroutine extends Item implements NodeSocketable {

    public record AllocatedNodeEffects(String nodeName, List<StyledText> effectLines) {
    }

    public static final ItemTypeSupport SUPPORT = new BasicItemTypeSupport(ItemType.SUBROUTINE, Subroutine.class, "exiledSector.Subroutine",
            Subroutine::new, Subroutine::new) {
        @Override
        public String definitionsPath() {
            return SubroutineDefinitions.dataPath();
        }

        @Override
        public ItemDefinition parseDefinition(JSONObject row) {
            return SubroutineDefinitions.parse(row);
        }

        @Override
        public int uniqueMinPlayerLevel() {
            return UniqueSubroutinesSetting.unlockLevel();
        }

        @Override
        public boolean ownsItemId(String itemId) {
            return NpcItems.isNpcId(itemId);
        }

        @Override
        public Item resolveItem(String itemId) {
            return NpcItems.resolve(itemId);
        }

        @Override
        public boolean inGenericPools(ItemDefinition definition) {
            return SkillTreeSetting.enabled() && SubroutineKind.inGenericPools(definition);
        }

        @Override
        public List<ItemData> rollSalvage(Random random, float chance) {
            return SkillTreeSetting.enabled() ? ModspecCaches.rollSalvage(random, chance) : List.of();
        }

        @Override
        public boolean uniqueRarity(ItemDefinition definition) {
            SubroutineKind kind = SubroutineKind.of(definition);
            return kind != null && kind.uniqueRarity();
        }

        @Override
        public boolean lockedFromCurrency(ItemDefinition definition) {
            return SubroutineKind.isUnique(definition);
        }
    };

    private List<String> rolledNodeIds;

    Subroutine(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
        settleCreationRolls();
    }

    Subroutine(Item source) {
        super(source);
        List<String> sourceNodeIds = null;
        if (source instanceof Subroutine subroutine) {
            sourceNodeIds = subroutine.rolledNodeIds;
        } else if (source instanceof UnresolvedItem unresolved) {
            sourceNodeIds = unresolved.rolledNodeIds();
        }
        rolledNodeIds = sourceNodeIds == null || sourceNodeIds.isEmpty() ? null : new ArrayList<>(sourceNodeIds);
        settleCreationRolls();
    }

    @Override
    protected Object readResolve() {
        Object resolved = super.readResolve();
        if (resolved == this) {
            settleCreationRolls();
        }
        return resolved;
    }

    private void settleCreationRolls() {
        SubroutineKind kind = subroutineKind();
        if (kind == null) {
            return;
        }
        if ((kind.bornCorrupted() || EmptyShells.hollows(definition())) && corruption() == null) {
            corrupt(CorruptionOutcome.INNATE);
        }
        if (kind.rollsNode() && rolledNodeIds == null) {
            List<String> rolled = SubroutineNodePools.roll(definition(), seed());
            rolledNodeIds = rolled.isEmpty() ? null : new ArrayList<>(rolled);
        }
    }

    @Override
    public void copyTraitsFrom(Item source) {
        if (source instanceof Subroutine subroutine && subroutine.rolledNodeIds != null && subroutine.definitionId().equals(definitionId())) {
            rolledNodeIds = new ArrayList<>(subroutine.rolledNodeIds);
        }
    }

    @Override
    public void keepWhenUnresolved(UnresolvedItem unresolved) {
        unresolved.keepRolledNodeIds(rolledNodeIds());
    }

    @Override
    public void writeTooltipSections(TooltipMakerAPI tooltip, HullSize hullSize, boolean showRollRanges) {
        if (showRollRanges) {
            for (AllocatedNodeEffects allocated : allocatedNodeEffects(hullSize)) {
                ItemTooltip.addSection(tooltip, allocated.nodeName(), allocated.effectLines());
            }
        }
    }

    @Override
    public ItemType type() {
        return ItemType.SUBROUTINE;
    }

    @Override
    public Modifier implicitModifier(String modifierId) {
        Modifier corruptionImplicit = SubroutineImplicits.byId(modifierId);
        return corruptionImplicit != null ? corruptionImplicit : super.implicitModifier(modifierId);
    }

    @Override
    public ModifierLine implicitLine(String effectName) {
        return SubroutineImplicits.line(effectName);
    }

    @Override
    public List<Modifier> implicitCorruptionPool() {
        return SubroutineImplicits.corruptionPool();
    }

    public SubroutineKind subroutineKind() {
        ItemDefinition definition = definition();
        return SubroutineKind.of(definition);
    }

    public List<String> rolledNodeIds() {
        return rolledNodeIds == null ? List.of() : List.copyOf(rolledNodeIds);
    }

    public List<String> currentNodeIds() {
        if (rolledNodeIds == null) {
            return List.of();
        }
        List<String> currentIds = new ArrayList<>(rolledNodeIds.size());
        for (String rolledNodeId : rolledNodeIds) {
            String currentId = NodeReplacements.current(rolledNodeId);
            if (!currentIds.contains(currentId)) {
                currentIds.add(currentId);
            }
        }
        return currentIds;
    }

    @Override
    public boolean canSocketInto(SkillNode socket) {
        return socket != null && socket.getType().getTier() == SkillTier.SOCKET && subroutineKind() != null;
    }

    @Override
    public List<StyledText> headerLines() {
        List<StyledText> lines = new ArrayList<>(super.headerLines());
        lines.addAll(allocationLines(NodeGrantStatus.lastKnown(id())));
        lines.addAll(kindLines());
        return lines;
    }

    public List<StyledText> allocationLines(List<NodeGrantStatus> statuses) {
        return allocationLines(statuses, false, null);
    }

    public List<StyledText> allocationLines(List<NodeGrantStatus> statuses, boolean withNodeEffects, HullSize hullSize) {
        if (subroutineKind() != SubroutineKind.MODSPEC_CACHE) {
            return List.of();
        }
        List<StyledText> lines = new ArrayList<>();
        for (String nodeId : currentNodeIds()) {
            NodeGrantStatus status = NodeGrantStatus.find(statuses, nodeId);
            addAllocationLines(lines, nodeId, status != null ? status : idleStatus(nodeId));
            if (withNodeEffects) {
                for (StyledText effectLine : nodeEffectLines(nodeId, hullSize)) {
                    lines.add(Translation.msg("socketable.cache.effectLine").arg("line", effectLine).styled());
                }
            }
        }
        return lines;
    }

    public List<AllocatedNodeEffects> allocatedNodeEffects(HullSize hullSize) {
        if (subroutineKind() != SubroutineKind.MODSPEC_CACHE) {
            return List.of();
        }
        List<AllocatedNodeEffects> allocated = new ArrayList<>();
        for (String nodeId : currentNodeIds()) {
            SkillNode node = SkillTree.getDeclared(nodeId);
            List<StyledText> effectLines = nodeEffectLines(nodeId, hullSize);
            if (node != null && !effectLines.isEmpty()) {
                allocated.add(new AllocatedNodeEffects(node.getDisplayName(), effectLines));
            }
        }
        return allocated;
    }

    private static List<StyledText> nodeEffectLines(String nodeId, HullSize hullSize) {
        SkillNode node = SkillTree.getDeclared(nodeId);
        if (node == null) {
            return List.of();
        }
        List<StyledText> effectLines = new ArrayList<>();
        for (DescriptionLine effectLine : SkillNode.describeEffects(node.getType(), hullSize)) {
            effectLines.add(effectLine.display());
        }
        return effectLines;
    }

    private static NodeGrantStatus idleStatus(String nodeId) {
        if (SkillTree.getDeclared(nodeId) == null) {
            return NodeGrantStatus.of(nodeId, NodeGrantStatus.State.UNKNOWN_NODE);
        }
        return SkillTree.get(nodeId) == null ? NodeGrantStatus.of(nodeId, NodeGrantStatus.State.AREA_OFF) : null;
    }

    private static void addAllocationLines(List<StyledText> lines, String nodeId, NodeGrantStatus status) {
        SkillNode node = SkillTree.getDeclared(nodeId);
        String nodeName = node == null ? "" : node.getDisplayName();
        NodeGrantStatus.State state = status == null ? NodeGrantStatus.State.ACTIVE : status.state();
        switch (state) {
            case ACTIVE, READY -> lines.add(Translation.msg("socketable.cache.allocates").arg("node", nodeName).styled());
            case BY_PATH -> lines.add(Translation.msg("socketable.cache.byPath").arg("node", nodeName).styled());
            case DUPLICATE -> lines.add(Translation.msg("socketable.cache.duplicate").arg("node", nodeName).styled());
            case INELIGIBLE -> {
                lines.add(Translation.msg("socketable.cache.ineligible").arg("node", nodeName).styled());
                if (status.reason() != null && !status.reason().isEmpty()) {
                    lines.add(StyledText.styled(status.reason(), Style.BAD));
                }
            }
            case AREA_OFF -> lines.add(Translation.msg("socketable.cache.areaOff").arg("node", nodeName).styled());
            case UNKNOWN_NODE -> lines.add(Translation.styled("socketable.cache.unknownNode"));
        }
    }

    public int firmwareRevision() {
        return DoctrineRewriter.revision(seed());
    }

    public SkillEffect conduitAffinity() {
        List<SkillEffect> affinities = AffinitySteps.affinities();
        return affinities.get(new SplittableRandom(seed()).nextInt(affinities.size()));
    }

    public List<StyledText> kindLines() {
        if (subroutineKind() == SubroutineKind.ROOT_CONDUIT) {
            return List.of(Translation.msg("socketable.rootConduit.line").arg("value", Math.round(RootConduits.AFFINITY_PER_STEP))
                    .arg("affinity", Translation.text("affinity." + conduitAffinity().name() + ".name")).arg("nodes", RootConduits.NODES_PER_STEP).styled());
        }
        if (subroutineKind() == SubroutineKind.UNIQUE) {
            RadiusSize radiusSize = UniqueSubroutineRadii.sizeOf(definitionId());
            return radiusSize == null ? List.of() : List.of(SubroutineRadius.radiusLine(radiusSize));
        }
        if (subroutineKind() != SubroutineKind.DOCTRINE) {
            return List.of();
        }
        List<StyledText> lines = new ArrayList<>();
        lines.add(Translation.msg("socketable.doctrine.revision").arg("revision", String.valueOf(firmwareRevision())).styled());
        DoctrineArchive archive = DoctrineArchives.get(definitionId());
        if (archive == null) {
            lines.add(Translation.styled("socketable.doctrine.inert"));
            return lines;
        }
        lines.add(SubroutineRadius.radiusLine(archive.radiusSize()));
        return lines;
    }

    @Override
    public StyledText limitLine() {
        return subroutineKind() == SubroutineKind.DOCTRINE ? DoctrineText.limitLine() : null;
    }
}
