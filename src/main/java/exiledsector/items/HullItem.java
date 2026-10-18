package exiledsector.items;

import java.util.List;

public final class HullItem extends Item {

    HullItem(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        super(id, definitionId, seed, explicitModifiers);
    }

    HullItem(Item source) {
        super(source);
    }

    @Override
    public ItemType type() {
        return ItemType.HULL_ITEM;
    }

    public ShipSection section() {
        ItemKind kind = kind();
        return kind == null ? null : kind.section();
    }

    @Override
    public boolean canSocketInto(ShipSection section) {
        return section != null && section == section();
    }
}
