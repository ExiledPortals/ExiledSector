package exiledsector.items;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.NodeReplacements;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;

import java.util.ArrayList;
import java.util.List;

public final class Subroutine extends Item {

    private String rolledNodeId;

    Subroutine(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
        settleCreationRolls();
    }

    Subroutine(Item source) {
        super(source);
        if (source instanceof Subroutine subroutine) {
            rolledNodeId = subroutine.rolledNodeId;
        } else if (source instanceof UnresolvedItem unresolved) {
            rolledNodeId = unresolved.rolledNodeId();
        }
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
        if (kind.rollsNode() && rolledNodeId == null) {
            rolledNodeId = SubroutineNodePools.roll(definition(), seed());
        }
    }

    @Override
    void copyTraitsFrom(Item source) {
        if (source instanceof Subroutine subroutine && subroutine.rolledNodeId != null && subroutine.definitionId().equals(definitionId())) {
            rolledNodeId = subroutine.rolledNodeId;
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

    public String rolledNodeId() {
        return rolledNodeId;
    }

    public String currentNodeId() {
        if (rolledNodeId == null || SkillTree.getDeclared(rolledNodeId) != null) {
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
        if (currentNodeId() != null) {
            lines.add(rolledNodeLine(currentNodeId()));
        }
        lines.addAll(doctrineLines());
        lines.addAll(volumeLines(VolumeGrantStatus.lastKnown(id())));
        return lines;
    }

    public List<StyledText> volumeSocketLines(VolumeGrantStatus status) {
        if (subroutineKind() != SubroutineKind.VOLUME) {
            return List.of();
        }
        List<StyledText> lines = new ArrayList<>();
        if (currentNodeId() != null) {
            lines.add(rolledNodeLine(currentNodeId()));
        }
        lines.addAll(volumeLines(status));
        return lines;
    }

    public List<StyledText> volumeLines(VolumeGrantStatus status) {
        if (subroutineKind() != SubroutineKind.VOLUME) {
            return List.of();
        }
        List<StyledText> lines = new ArrayList<>();
        String volume = VolumeSubroutines.volume(definitionId());
        if (volume != null) {
            lines.add(Translation.msg("socketable.volume.volume").arg("volume", VolumeSubroutines.volumeName(volume)).styled());
        }
        lines.add(Translation.styled("socketable.volume.explain"));
        VolumeGrantStatus shownStatus = status != null ? status : idleStatus();
        if (shownStatus != null) {
            lines.add(volumeStatusLine(shownStatus, volume));
        }
        return lines;
    }

    public String volumeSocketRefusal() {
        VolumeGrantStatus idle = subroutineKind() == SubroutineKind.VOLUME ? idleStatus() : null;
        if (idle == null || idle.state() != VolumeGrantStatus.State.AREA_OFF) {
            return null;
        }
        String volume = VolumeSubroutines.volume(definitionId());
        return Translation.msg("socketable.volume.areaOffRefusal").arg("name", name())
                .arg("volume", volume == null ? "" : VolumeSubroutines.volumeName(volume)).text();
    }

    private VolumeGrantStatus idleStatus() {
        if (currentNodeId() == null || SkillTree.getDeclared(currentNodeId()) == null) {
            return VolumeGrantStatus.of(VolumeGrantStatus.State.UNKNOWN_NODE);
        }
        return SkillTree.get(currentNodeId()) == null ? VolumeGrantStatus.of(VolumeGrantStatus.State.AREA_OFF) : null;
    }

    private StyledText volumeStatusLine(VolumeGrantStatus status, String volume) {
        SkillNode node = currentNodeId() == null ? null : SkillTree.getDeclared(currentNodeId());
        String nodeName = node == null ? "" : node.getDisplayName();
        String reason = status.reason() == null ? "" : status.reason();
        return switch (status.state()) {
            case ACTIVE -> Translation.msg("socketable.volume.active").arg("node", nodeName).styled();
            case READY -> Translation.msg("socketable.volume.ready").arg("node", nodeName).styled();
            case BY_PATH -> Translation.msg("socketable.volume.byPath").arg("node", nodeName).styled();
            case DUPLICATE -> Translation.msg("socketable.volume.duplicate").arg("node", nodeName).styled();
            case INELIGIBLE -> Translation.msg("socketable.volume.ineligible").arg("node", nodeName).arg("reason", reason).styled();
            case WAITING -> Translation.msg("socketable.volume.waiting").arg("node", nodeName).arg("reason", reason).styled();
            case AREA_OFF -> Translation.msg("socketable.volume.areaOff").arg("volume", volume == null ? "" : VolumeSubroutines.volumeName(volume))
                    .styled();
            case UNKNOWN_NODE -> Translation.styled("socketable.volume.unknownNode");
        };
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
        String behaviourKey = archive.transforms() ? "socketable.doctrine.transforms" : "socketable.doctrine.augments";
        lines.add(Translation.msg(behaviourKey).arg("radius", radiusName).styled());
        return lines;
    }

    static StyledText rolledNodeLine(String nodeId) {
        SkillNode node = SkillTree.getDeclared(nodeId);
        if (node == null) {
            return Translation.styled("socketable.tooltip.rolledNodeUnknown");
        }
        return Translation.msg("socketable.tooltip.rolledNode").arg("node", node.getDisplayName()).styled();
    }
}
