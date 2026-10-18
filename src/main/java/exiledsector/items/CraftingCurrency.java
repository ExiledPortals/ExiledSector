package exiledsector.items;

import java.awt.Color;
import java.util.Locale;
import java.util.Random;

public enum CraftingCurrency implements ItemCurrency {

    AUGMENTATION("exiledSector_augmentation_kernel", 10, 6, MaterialTier.MINOR, new Color(90, 170, 255)),
    ATTUNEMENT("exiledSector_attunement_kernel", 10, 4, MaterialTier.MINOR, new Color(185, 125, 255)),
    RECALIBRATION("exiledSector_recalibration_kernel", 20, 3, MaterialTier.MAJOR, new Color(110, 235, 140)),
    SUBSTITUTION("exiledSector_substitution_kernel", 20, 3, MaterialTier.MAJOR, new Color(255, 195, 80)),
    TRANSPOSITION("exiledSector_transposition_kernel", 100, 1, MaterialTier.EXOTIC, new Color(255, 140, 50)),
    FIXATION("exiledSector_fixation_kernel", 100, 1, MaterialTier.EXOTIC, new Color(255, 75, 70)),
    CORRUPTION("exiledSector_corruption_kernel", 50, 1.5f, MaterialTier.EXOTIC, new Color(215, 35, 70)),
    REPLICATION("exiledSector_replication_kernel", 0, 0.035f, MaterialTier.MYTHIC, new Color(225, 240, 255));

    private final String itemId;
    private final int defaultPartsCost;
    private final float dropWeight;
    private final MaterialTier tier;
    private final Color color;

    CraftingCurrency(String itemId, int defaultPartsCost, float dropWeight, MaterialTier tier, Color color) {
        this.itemId = itemId;
        this.defaultPartsCost = defaultPartsCost;
        this.dropWeight = dropWeight;
        this.tier = tier;
        this.color = color;
    }

    public static CraftingCurrency byItemId(String itemId) {
        for (CraftingCurrency currency : values()) {
            if (currency.itemId.equals(itemId)) {
                return currency;
            }
        }
        return null;
    }

    @Override
    public String itemId() {
        return itemId;
    }

    @Override
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public float dropWeight() {
        return dropWeight;
    }

    @Override
    public MaterialTier tier() {
        return tier;
    }

    @Override
    public Color color() {
        return color;
    }

    @Override
    public boolean synthesisable() {
        return defaultPartsCost > 0;
    }

    @Override
    public int partsCost() {
        return ItemCraftingCosts.cost(itemId, defaultPartsCost);
    }

    @Override
    public boolean appliesTo(ItemType type) {
        return type == ItemType.SUBROUTINE || type == ItemType.HULL_ITEM;
    }

    @Override
    public boolean canUse(Item item, CraftingContext context) {
        return switch (this) {
            case AUGMENTATION -> ItemCrafting.canAugment(item);
            case ATTUNEMENT -> ItemCrafting.canAttune(item);
            case RECALIBRATION -> ItemCrafting.canRecalibrate(item, item.definition(), context.guardedSlots());
            case SUBSTITUTION -> ItemCrafting.canSubstitute(item, item.definition(), context.guardedSlots());
            case TRANSPOSITION -> !ItemCrafting.wardBlocksTransposition(item, context.guardedSlots())
                    && ItemCrafting.canTranspose(item.definition(), item.tier(), context.allowedUniques());
            case FIXATION -> ItemCrafting.canFixate(item, context.guardedSlots());
            case CORRUPTION, REPLICATION -> true;
        };
    }

    @Override
    public Item use(Item item, CraftingContext context, Random random) {
        return switch (this) {
            case AUGMENTATION -> ItemCrafting.augment(item, random) == null ? null : item;
            case ATTUNEMENT -> ItemCrafting.attune(item, random);
            case RECALIBRATION -> ItemCrafting.recalibrate(item, random, context.guardedSlots());
            case SUBSTITUTION -> ItemCrafting.substitute(item, random, context.guardedSlots());
            case TRANSPOSITION -> ItemCrafting.transpose(context.store(), item, random, context.allowedUniques());
            case FIXATION -> ItemCrafting.fixate(item, random, context.guardedSlots());
            case CORRUPTION -> ItemCrafting.corrupt(context.store(), item, random, context.allowedUniques());
            case REPLICATION -> context.store().addMirrorOf(item);
        };
    }

    @Override
    public boolean respectsWards() {
        return this == RECALIBRATION || this == SUBSTITUTION || this == FIXATION;
    }

    @Override
    public boolean wouldChange(Item item, RolledModifier rolled) {
        return switch (this) {
            case RECALIBRATION -> ItemCrafting.canVary(rolled, item.definition());
            case SUBSTITUTION -> ItemCrafting.canSubstituteAway(item, item.definition(), rolled);
            case FIXATION -> true;
            default -> false;
        };
    }

    @Override
    public boolean createsCopy() {
        return this == REPLICATION;
    }

    @Override
    public boolean mayChangeDefinition() {
        return this == TRANSPOSITION || this == CORRUPTION;
    }
}
