package exiledsector.items;

import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.NodeReplacements;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;

import java.util.ArrayList;
import java.util.List;

public final class Subroutine extends Item {

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
        if (kind.bornCorrupted() && corruption() == null) {
            corrupt(CorruptionOutcome.INNATE);
        }
        if (kind.rollsNode() && rolledNodeIds == null) {
            List<String> rolled = SubroutineNodePools.roll(definition(), seed());
            rolledNodeIds = rolled.isEmpty() ? null : new ArrayList<>(rolled);
        }
    }

    @Override
    void copyTraitsFrom(Item source) {
        if (source instanceof Subroutine subroutine && subroutine.rolledNodeIds != null && subroutine.definitionId().equals(definitionId())) {
            rolledNodeIds = new ArrayList<>(subroutine.rolledNodeIds);
        }
    }

    @Override
    public ItemType type() {
        return ItemType.SUBROUTINE;
    }

    public SubroutineKind subroutineKind() {
        ItemDefinition definition = definition();
        return definition == null ? null : definition.subroutineKind();
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
            String currentId = currentNodeId(rolledNodeId);
            if (!currentIds.contains(currentId)) {
                currentIds.add(currentId);
            }
        }
        return currentIds;
    }

    private static String currentNodeId(String rolledNodeId) {
        if (SkillTree.getDeclared(rolledNodeId) != null) {
            return rolledNodeId;
        }
        String replacementId = NodeReplacements.all().get(rolledNodeId);
        return replacementId != null ? replacementId : rolledNodeId;
    }

    @Override
    public boolean canSocketInto(SkillNode socket) {
        return socket != null && socket.getType().getTier() == SkillTier.SOCKET && subroutineKind() != null;
    }

    @Override
    public List<StyledText> headerLines() {
        List<StyledText> lines = new ArrayList<>(super.headerLines());
        lines.addAll(allocationLines(NodeGrantStatus.lastKnown(id())));
        lines.addAll(doctrineLines());
        return lines;
    }

    public List<StyledText> allocationLines(List<NodeGrantStatus> statuses) {
        if (subroutineKind() != SubroutineKind.MODSPEC_CACHE) {
            return List.of();
        }
        List<StyledText> lines = new ArrayList<>();
        for (String nodeId : currentNodeIds()) {
            NodeGrantStatus status = NodeGrantStatus.find(statuses, nodeId);
            addAllocationLines(lines, nodeId, status != null ? status : idleStatus(nodeId));
        }
        return lines;
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

    public List<StyledText> doctrineLines() {
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
        String radiusName = Translation.text("socketable.doctrine.radius." + archive.radiusSize().dataKey());
        lines.add(Translation.msg("socketable.doctrine.radiusLine").arg("radius", radiusName).styled());
        return lines;
    }

    @Override
    public StyledText limitLine() {
        SubroutineKind kind = subroutineKind();
        if (kind != SubroutineKind.DOCTRINE) {
            return null;
        }
        int limit = kind.socketLimitPerShip();
        return Translation.msg("socketable.doctrine.limit").count(limit).arg("limit", limit).styled();
    }
}
