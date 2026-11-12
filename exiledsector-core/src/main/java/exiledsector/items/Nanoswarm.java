package exiledsector.items;


import java.awt.Color;
import java.util.Locale;
import java.util.Random;

public enum Nanoswarm implements CraftingMaterialFamily {

    AUGMENTATION("exiledSector_augmentation_nanoswarm", 10, 6, MaterialTier.MINOR, new Color(90, 170, 255)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canAugment(item);
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.augment(item, random, context.favouredTags()) == null ? null : item;
        }

        @Override
        public boolean addsModifiers() {
            return true;
        }
    },
    FUNDAMENTAL("exiledSector_fundamental_nanoswarm", 15, 4, MaterialTier.MINOR, new Color(185, 125, 255)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canRerollFundamentals(item);
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.rerollFundamentals(item, random);
        }
    },
    RESHAPING("exiledSector_reshaping_nanoswarm", 20, 3, MaterialTier.MAJOR, new Color(255, 195, 80)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canReshape(item, item.definition(), context.inhibitedSlots());
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.reshape(item, random, context.inhibitedSlots(), context.favouredTags());
        }

        @Override
        public boolean addsModifiers() {
            return true;
        }

        @Override
        public boolean respectsInhibitors() {
            return true;
        }

        @Override
        public boolean wouldChange(Item item, RolledModifier rolled) {
            return ItemCrafting.canReshapeAway(item, item.definition(), rolled);
        }
    },
    CORRUPTION("exiledSector_corrupted_nanoswarm", 50, 1.5f, MaterialTier.EXOTIC, Item.CORRUPTED_FRAME_COLOR) {
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
    METAMORPH("exiledSector_metamorph_nanoswarm", 150, 0.4f, MaterialTier.MYTHIC, new Color(255, 140, 50)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return !ItemCrafting.inhibitorBlocksMetamorph(item, context.inhibitedSlots())
                    && ItemCrafting.canMetamorph(item.definition(), item.tier(), context.allowedUniques());
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.metamorph(context.store(), item, random, context.allowedUniques());
        }

        @Override
        public boolean mayChangeDefinition() {
            return true;
        }
    },
    ANNULMENT("exiledSector_annulment_nanoswarm", 200, 0.3f, MaterialTier.MYTHIC, new Color(80, 230, 200)) {
        @Override
        public boolean appliesTo(ItemType type) {
            return type == ItemType.HULL_ITEM;
        }

        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canAnnul(item, context.inhibitedSlots());
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.annul(item, random, context.inhibitedSlots());
        }

        @Override
        public boolean respectsInhibitors() {
            return true;
        }

        @Override
        public boolean wouldChange(Item item, RolledModifier rolled) {
            return true;
        }
    },
    TUNING("exiledSector_tuning_nanoswarm", 200, 0.3f, MaterialTier.MYTHIC, new Color(110, 235, 140)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canTune(item, item.definition(), context.inhibitedSlots());
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.tune(item, random, context.inhibitedSlots());
        }

        @Override
        public boolean respectsInhibitors() {
            return true;
        }

        @Override
        public boolean wouldChange(Item item, RolledModifier rolled) {
            return ItemCrafting.canVary(item, rolled);
        }
    },
    IMMUTABLE("exiledSector_immutable_nanoswarm", 300, 0.2f, MaterialTier.MYTHIC, new Color(230, 80, 175)) {
        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return ItemCrafting.canMakeImmutable(item, context.inhibitedSlots());
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return ItemCrafting.makeImmutable(item, random, context.inhibitedSlots());
        }

        @Override
        public boolean respectsInhibitors() {
            return true;
        }

        @Override
        public boolean wouldChange(Item item, RolledModifier rolled) {
            return true;
        }
    },
    REPLICATION("exiledSector_replicator_nanoseed", 0, 0.035f, MaterialTier.MYTHIC, new Color(225, 240, 255)) {
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
    private final int defaultFeedstockCost;
    private final float dropWeight;
    private final MaterialTier tier;
    private final Color color;

    Nanoswarm(String itemId, int defaultFeedstockCost, float dropWeight, MaterialTier tier, Color color) {
        this.itemId = itemId;
        this.defaultFeedstockCost = defaultFeedstockCost;
        this.dropWeight = dropWeight;
        this.tier = tier;
        this.color = color;
    }

    public static Nanoswarm byItemId(String itemId) {
        for (Nanoswarm craftingItem : values()) {
            if (craftingItem.itemId.equals(itemId)) {
                return craftingItem;
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
    public boolean growable() {
        return defaultFeedstockCost > 0;
    }

    @Override
    public int feedstockCost() {
        return ItemCraftingCosts.cost(itemId, defaultFeedstockCost);
    }

    @Override
    public boolean appliesTo(ItemType type) {
        return type == ItemType.HULL_ITEM || type == ItemType.SUBROUTINE;
    }
}
