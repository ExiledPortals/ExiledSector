package exiledsector.items;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
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

    @Override
    public boolean canSocketInto(SkillNode socket) {
        return socket != null && socket.getType().getTier() == SkillTier.SOCKET && subroutineKind() != null;
    }

    @Override
    public List<StyledText> headerLines() {
        List<StyledText> lines = new ArrayList<>(super.headerLines());
        if (rolledNodeId != null) {
            lines.add(rolledNodeLine(rolledNodeId));
        }
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
