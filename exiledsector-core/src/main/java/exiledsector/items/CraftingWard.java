package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;

import java.awt.Color;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public enum CraftingWard {

    PREFIX("exiledSector_prefix_ward", ModifierSlot.PREFIX, 0.5f, new Color(80, 225, 205)),
    SUFFIX("exiledSector_suffix_ward", ModifierSlot.SUFFIX, 0.5f, new Color(235, 110, 225));

    private final String itemId;
    private final ModifierSlot guardedSlot;
    private final float dropWeight;
    private final Color color;

    CraftingWard(String itemId, ModifierSlot guardedSlot, float dropWeight, Color color) {
        this.itemId = itemId;
        this.guardedSlot = guardedSlot;
        this.dropWeight = dropWeight;
        this.color = color;
    }

    public static CraftingWard byItemId(String itemId) {
        for (CraftingWard ward : values()) {
            if (ward.itemId.equals(itemId)) {
                return ward;
            }
        }
        return null;
    }

    public static Set<ModifierSlot> guardedSlots(Collection<CraftingWard> wards) {
        Set<ModifierSlot> slots = EnumSet.noneOf(ModifierSlot.class);
        wards.forEach(ward -> slots.add(ward.guardedSlot));
        return slots;
    }

    public String itemId() {
        return itemId;
    }

    public ModifierSlot guardedSlot() {
        return guardedSlot;
    }

    public float dropWeight() {
        return dropWeight;
    }

    public MaterialTier tier() {
        return MaterialTier.EXOTIC;
    }

    public Color color() {
        return color;
    }
}
