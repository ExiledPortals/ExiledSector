package exiledsector.items;


import java.awt.Color;
import java.util.Locale;
import java.util.Random;

public enum CraftingCurrency implements ItemCurrency {

    AUGMENTATION("exiledSector_augmentation_kernel", 10, 6, MaterialTier.MINOR, new Color(90, 170, 255)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canAugment(item);
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.augment(item, random) == null ? null : item;
        }
    },
    ATTUNEMENT("exiledSector_attunement_kernel", 15, 4, MaterialTier.MINOR, new Color(185, 125, 255)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canAttune(item);
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.attune(item, random);
        }
    },
    SUBSTITUTION("exiledSector_substitution_kernel", 20, 3, MaterialTier.MAJOR, new Color(255, 195, 80)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canSubstitute(item, item.definition(), context.guardedSlots());
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.substitute(item, random, context.guardedSlots());
        }

        @Override
        public boolean respectsWards() {
            return true;
        }

        @Override
        public boolean wouldChange(Item item, RolledModifier rolled) {
            return ItemCrafting.canSubstituteAway(item, item.definition(), rolled);
        }
    },
    CORRUPTION("exiledSector_corruption_kernel", 50, 1.5f, MaterialTier.EXOTIC, Item.CORRUPTED_FRAME_COLOR) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return true;
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.corrupt(context.store(), item, random, context.allowedUniques());
        }

        @Override
        public boolean mayChangeDefinition() {
            return true;
        }
    },
    TRANSPOSITION("exiledSector_transposition_kernel", 0, 0.4f, MaterialTier.MYTHIC, new Color(255, 140, 50)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return !ItemCrafting.wardBlocksTransposition(item, context.guardedSlots())
                    && ItemCrafting.canTranspose(item.definition(), item.tier(), context.allowedUniques());
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.transpose(context.store(), item, random, context.allowedUniques());
        }

        @Override
        public boolean mayChangeDefinition() {
            return true;
        }
    },
    ANNULMENT("exiledSector_annulment_kernel", 0, 0.3f, MaterialTier.MYTHIC, new Color(80, 230, 200)) {
        @Override
        public boolean appliesTo(ItemType type) {
            return type == ItemType.HULL_ITEM;
        }

        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canAnnul(item, context.guardedSlots());
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.annul(item, random, context.guardedSlots());
        }

        @Override
        public boolean respectsWards() {
            return true;
        }

        @Override
        public boolean wouldChange(Item item, RolledModifier rolled) {
            return true;
        }
    },
    RECALIBRATION("exiledSector_recalibration_kernel", 0, 0.3f, MaterialTier.MYTHIC, new Color(110, 235, 140)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canRecalibrate(item, item.definition(), context.guardedSlots());
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.recalibrate(item, random, context.guardedSlots());
        }

        @Override
        public boolean respectsWards() {
            return true;
        }

        @Override
        public boolean wouldChange(Item item, RolledModifier rolled) {
            return ItemCrafting.canVary(rolled, item.definition());
        }
    },
    FIXATION("exiledSector_fixation_kernel", 0, 0.2f, MaterialTier.MYTHIC, new Color(230, 80, 175)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canFixate(item, context.guardedSlots());
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.fixate(item, random, context.guardedSlots());
        }

        @Override
        public boolean respectsWards() {
            return true;
        }

        @Override
        public boolean wouldChange(Item item, RolledModifier rolled) {
            return true;
        }
    },
    REPLICATION("exiledSector_replication_kernel", 0, 0.035f, MaterialTier.MYTHIC, new Color(225, 240, 255)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return true;
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return context.store().addMirrorOf(item);
        }

        @Override
        public boolean createsCopy() {
            return true;
        }
    };

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
        return type == ItemType.HULL_ITEM || type == ItemType.SUBROUTINE;
    }
}
