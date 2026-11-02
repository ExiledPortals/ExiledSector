package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;

import java.awt.Color;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public enum CraftingInhibitor implements GrowableMaterial {

    PREFIX("exiledSector_prefix_inhibitor", ModifierSlot.PREFIX, 100, 0.5f, new Color(80, 225, 205)),
    SUFFIX("exiledSector_suffix_inhibitor", ModifierSlot.SUFFIX, 100, 0.5f, new Color(235, 110, 225));

    private final String itemId;
    private final ModifierSlot inhibitedSlot;
    private final int defaultPartsCost;
    private final float dropWeight;
    private final Color color;

    CraftingInhibitor(String itemId, ModifierSlot inhibitedSlot, int defaultPartsCost, float dropWeight, Color color) {
        this.itemId = itemId;
        this.inhibitedSlot = inhibitedSlot;
        this.defaultPartsCost = defaultPartsCost;
        this.dropWeight = dropWeight;
        this.color = color;
    }

    public static CraftingInhibitor byItemId(String itemId) {
        for (CraftingInhibitor inhibitor : values()) {
            if (inhibitor.itemId.equals(itemId)) {
                return inhibitor;
            }
        }
        return null;
    }

    public static Set<ModifierSlot> inhibitedSlots(Collection<CraftingInhibitor> inhibitors) {
        Set<ModifierSlot> slots = EnumSet.noneOf(ModifierSlot.class);
        inhibitors.forEach(inhibitor -> slots.add(inhibitor.inhibitedSlot));
        return slots;
    }

    @Override
    public String itemId() {
        return itemId;
    }

    @Override
    public boolean synthesisable() {
        return defaultPartsCost > 0;
    }

    @Override
    public int partsCost() {
        return ItemCraftingCosts.cost(itemId, defaultPartsCost);
    }

    public ModifierSlot inhibitedSlot() {
        return inhibitedSlot;
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
