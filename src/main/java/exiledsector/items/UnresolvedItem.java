package exiledsector.items;

import java.util.List;

public final class UnresolvedItem extends Item {

    UnresolvedItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    UnresolvedItem(Item source) {
        super(source);
    }

    @Override
    public ItemType type() {
        return null;
    }
}
