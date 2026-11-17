package exiledsector.loot;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.DroppableDefinitions;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemType;
import exiledsector.items.ItemUnlock;

import java.util.ArrayList;
import java.util.List;

public enum DropType {
    ITEM(null, null),
    HULL_ITEM(ITEM, null),
    BRIDGE(HULL_ITEM, ItemKind.BRIDGE),
    CREW_QUARTERS(HULL_ITEM, ItemKind.CREW_QUARTERS),
    ENGINE_ROOM(HULL_ITEM, ItemKind.ENGINE_ROOM),
    REACTOR(HULL_ITEM, ItemKind.REACTOR),
    ARMOR_PLATING(HULL_ITEM, ItemKind.ARMOR_PLATING),
    WEAPON_MOUNT(HULL_ITEM, ItemKind.WEAPON_MOUNT),
    SHIELD_GENERATOR(HULL_ITEM, ItemKind.SHIELD_GENERATOR),
    PHASE_COIL(HULL_ITEM, ItemKind.PHASE_COIL),
    FLIGHT_DECK(HULL_ITEM, ItemKind.FLIGHT_DECK),
    SUBROUTINE(ITEM, ItemKind.SUBROUTINE),
    STAR_CHART(ITEM, null),
    SCHEMATIC(ITEM, null),
    CRAFTING_ITEM(ITEM, null),
    BASIC_CRAFTING_ITEM(CRAFTING_ITEM, null),
    ENGRAM(CRAFTING_ITEM, null),
    INHIBITOR(CRAFTING_ITEM, null),
    FEEDSTOCK(ITEM, null),
    CATALYST(ITEM, null),
    WEAPON(ITEM, null),
    RESOURCE(ITEM, null),
    BLUEPRINT(ITEM, null),
    AI_CORE(ITEM, null),
    COLONY_ITEM(ITEM, null);

    private final DropType parent;
    private final ItemKind itemKind;
    private final List<DropType> chain;

    DropType(DropType parent, ItemKind itemKind) {
        this.parent = parent;
        this.itemKind = itemKind;
        List<DropType> selfToRoot = new ArrayList<>();
        selfToRoot.add(this);
        for (DropType ancestor = parent; ancestor != null; ancestor = ancestor.parent) {
            selfToRoot.add(ancestor);
        }
        this.chain = List.copyOf(selfToRoot);
    }

    public static DropType forItemKind(ItemKind kind) {
        for (DropType type : values()) {
            if (type.itemKind != null && type.itemKind == kind) {
                return type;
            }
        }
        return null;
    }

    public static DropType forDefinition(ItemDefinition definition) {
        DropType type = forItemKind(definition.kind());
        if (type != null) {
            return type;
        }
        return definition.isHullItem() ? HULL_ITEM : ITEM;
    }

    public static DropType forCraftingItem(String itemId) {
        return CraftingInhibitor.byItemId(itemId) != null ? INHIBITOR : BASIC_CRAFTING_ITEM;
    }

    public DropType parent() {
        return parent;
    }

    public List<DropType> chain() {
        return chain;
    }

    public ItemKind itemKind() {
        return itemKind;
    }

    public boolean isVanilla() {
        return this == WEAPON || this == RESOURCE || this == BLUEPRINT || this == AI_CORE;
    }

    public boolean isSection() {
        return itemKind != null && itemKind.fitsSection();
    }

    public boolean canDrop(SectorAPI sector) {
        if (sector == null) {
            return true;
        }
        if ((this == HULL_ITEM || isSection()) && !ItemUnlock.available(ItemType.HULL_ITEM, sector)) {
            return false;
        }
        if (itemKind == null) {
            return true;
        }
        return this == SUBROUTINE ? DroppableDefinitions.hasDroppable(itemKind) : DroppableDefinitions.hasDroppableBasic(itemKind);
    }
}
