package exiledsector.items;

import exiledlib.skills.SocketedItem;
import exiledsector.skills.ShipSections;

public record HullItemOrigin(ShipSections.UnlockedSection unlocked) implements SocketedItem {

    @Override
    public String category() {
        return HULL_ITEM;
    }

    @Override
    public String slotId() {
        return unlocked.section().id();
    }

    @Override
    public String targetSlotId() {
        ShipSection target = ShipSections.targetSection(unlocked);
        return target == null ? null : target.id();
    }
}
