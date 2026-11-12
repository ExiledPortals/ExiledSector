package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.TagWeights;

import java.awt.Color;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public enum CraftingInhibitor implements GrowableMaterial {

    PREFIX("exiledSector_prefix_inhibitor", ModifierSlot.PREFIX, null, 100, 0.5f, new Color(80, 225, 205)),
    SUFFIX("exiledSector_suffix_inhibitor", ModifierSlot.SUFFIX, null, 100, 0.5f, new Color(235, 110, 225)),
    BEAM("exiledSector_beam_inhibitor", null, "beam", 0, 0f, new Color(255, 100, 190)),
    NON_BEAM_ENERGY("exiledSector_non_beam_energy_inhibitor", null, "non_beam_energy", 0, 0f, new Color(120, 160, 255)),
    BALLISTIC("exiledSector_ballistic_inhibitor", null, "ballistic", 0, 0f, new Color(235, 200, 90)),
    MISSILE("exiledSector_missile_inhibitor", null, "missile", 0, 0f, new Color(140, 230, 110)),
    SHIELD("exiledSector_shield_inhibitor", null, "shield", 0, 0f, new Color(110, 215, 255)),
    ARMOUR("exiledSector_armour_inhibitor", null, "armour", 0, 0f, new Color(200, 160, 120)),
    PHASE("exiledSector_phase_inhibitor", null, "phase", 0, 0f, new Color(190, 130, 255));

    public static final float INHIBITED_TAG_MULTIPLIER = 0.2f;

    private final String itemId;
    private final ModifierSlot inhibitedSlot;
    private final String inhibitedTag;
    private final int defaultFeedstockCost;
    private final float dropWeight;
    private final Color color;

    CraftingInhibitor(String itemId, ModifierSlot inhibitedSlot, String inhibitedTag, int defaultFeedstockCost, float dropWeight, Color color) {
        this.itemId = itemId;
        this.inhibitedSlot = inhibitedSlot;
        this.inhibitedTag = inhibitedTag;
        this.defaultFeedstockCost = defaultFeedstockCost;
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
        inhibitors.stream().filter(inhibitor -> inhibitor.inhibitedSlot != null).forEach(inhibitor -> slots.add(inhibitor.inhibitedSlot));
        return slots;
    }

    public static TagWeights tagWeights(Collection<CraftingInhibitor> inhibitors) {
        Map<String, Float> multipliersByTag = new LinkedHashMap<>();
        inhibitors.stream().filter(CraftingInhibitor::inhibitsTag).forEach(inhibitor -> multipliersByTag.put(inhibitor.inhibitedTag, INHIBITED_TAG_MULTIPLIER));
        return multipliersByTag.isEmpty() ? TagWeights.NONE : TagWeights.NONE.with(multipliersByTag);
    }

    public static Set<CraftingInhibitor> tagInhibitors(Collection<CraftingInhibitor> inhibitors) {
        Set<CraftingInhibitor> tagInhibitors = EnumSet.noneOf(CraftingInhibitor.class);
        inhibitors.stream().filter(CraftingInhibitor::inhibitsTag).forEach(tagInhibitors::add);
        return tagInhibitors;
    }

    public static Set<CraftingInhibitor> tagInhibitors() {
        return tagInhibitors(EnumSet.allOf(CraftingInhibitor.class));
    }

    @Override
    public String itemId() {
        return itemId;
    }

    @Override
    public boolean growable() {
        return defaultFeedstockCost > 0;
    }

    @Override
    public int feedstockCost() {
        return ItemCraftingCosts.cost(itemId, defaultFeedstockCost);
    }

    public ModifierSlot inhibitedSlot() {
        return inhibitedSlot;
    }

    public String inhibitedTag() {
        return inhibitedTag;
    }

    public boolean inhibitsTag() {
        return inhibitedTag != null;
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
