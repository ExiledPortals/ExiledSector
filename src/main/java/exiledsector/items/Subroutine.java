package exiledsector.items;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;

import java.util.List;

public final class Subroutine extends Item {

    Subroutine(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    Subroutine(Item source) {
        super(source);
    }

    @Override
    public ItemType type() {
        return ItemType.SUBROUTINE;
    }

    @Override
    public boolean canSocketInto(SkillNode socket) {
        return socket != null && socket.getType().getTier() == SkillTier.SOCKET;
    }
}
