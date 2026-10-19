package exiledsector.charts;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemUnlock;

public enum DropType {
    BRIDGE(ItemKind.BRIDGE),
    CREW_QUARTERS(ItemKind.CREW_QUARTERS),
    ENGINE_ROOM(ItemKind.ENGINE_ROOM),
    REACTOR(ItemKind.REACTOR),
    ARMOR_PLATING(ItemKind.ARMOR_PLATING),
    WEAPON_MOUNT(ItemKind.WEAPON_MOUNT),
    SHIELD_GENERATOR(ItemKind.SHIELD_GENERATOR),
    PHASE_COIL(ItemKind.PHASE_COIL),
    FLIGHT_DECK(ItemKind.FLIGHT_DECK),
    SUBROUTINE(ItemKind.SUBROUTINE),
    STAR_CHART(true, true),
    CURRENCY(false, true),
    FEEDSTOCK(false, true),
    WEAPON(false, true),
    RESOURCE(false, true),
    BLUEPRINT(false, true),
    AI_CORE(false, true),
    COLONY_ITEM(false, false);

    private final ItemKind itemKind;
    private final boolean item;
    private final boolean quantityApplies;

    DropType(ItemKind itemKind) {
        this(itemKind, true, true);
    }

    DropType(boolean item, boolean quantityApplies) {
        this(null, item, quantityApplies);
    }

    DropType(ItemKind itemKind, boolean item, boolean quantityApplies) {
        this.itemKind = itemKind;
        this.item = item;
        this.quantityApplies = quantityApplies;
    }

    public ItemKind itemKind() {
        return itemKind;
    }

    public boolean isItem() {
        return item;
    }

    public boolean quantityApplies() {
        return quantityApplies;
    }

    public boolean isSection() {
        return itemKind != null && itemKind.fitsSection();
    }

    public boolean canDrop(SectorAPI sector) {
        if (itemKind == null || sector == null) {
            return true;
        }
        if (isSection() && !ItemUnlock.sectionsOpen(sector)) {
            return false;
        }
        return ItemDrops.hasDroppableBasic(itemKind);
    }

}
