package exiledsector.items;

import exiledsector.skills.tags.ThemeTags;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public enum InfusedCraft {
    REFORGE_WITH_TAG {
        @Override
        public List<ForgeChoice> choices(Item item, String tag) {
            return wholeItemIf(InfusedCrafting.canReforge(item, tag));
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Random random) {
            return InfusedCrafting.reforgeWithTag(item, tag, random);
        }
    },
    REFORGE_MORE_ALIKE {
        @Override
        public List<ForgeChoice> choices(Item item, String tag) {
            return wholeItemIf(InfusedCrafting.canReforge(item, null));
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Random random) {
            return InfusedCrafting.reforgeAlike(item, InfusedCrafting.MORE_ALIKE_WEIGHT, random);
        }
    },
    REFORGE_LESS_ALIKE {
        @Override
        public List<ForgeChoice> choices(Item item, String tag) {
            return wholeItemIf(InfusedCrafting.canReforge(item, null));
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Random random) {
            return InfusedCrafting.reforgeAlike(item, InfusedCrafting.LESS_ALIKE_WEIGHT, random);
        }
    },
    REROLL_TAG_VALUES {
        @Override
        public List<ForgeChoice> choices(Item item, String tag) {
            return wholeItemIf(!InfusedCrafting.rerollableWithTag(item, tag).isEmpty());
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Random random) {
            return InfusedCrafting.rerollTagValues(item, tag, random);
        }

        @Override
        boolean refusesUniques() {
            return false;
        }
    },
    SWAP_DAMAGE_TYPE {
        @Override
        public List<ForgeChoice> choices(Item item, String tag) {
            return InfusedCrafting.swappable(item, tag, damageTypes()).stream().map(ForgeChoice::modifier).toList();
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Random random) {
            return InfusedCrafting.swapDamageType(item, choice.modifier(), tag, damageTypes());
        }

        @Override
        public boolean picksAChoice() {
            return true;
        }
    },
    ADD_AND_REMOVE {
        @Override
        public List<ForgeChoice> choices(Item item, String tag) {
            return wholeItemIf(!InfusedCrafting.removableForTag(item, tag).isEmpty());
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Random random) {
            return InfusedCrafting.addAndRemove(item, tag, random);
        }
    };

    static final String REFUSAL_PREFIX = "ui.infusedCraft.refused.";

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static InfusedCraft byIdOrNull(String id) {
        for (InfusedCraft craft : values()) {
            if (craft.id().equals(id)) {
                return craft;
            }
        }
        return null;
    }

    public List<String> tags() {
        return InfusedCraftCosts.tags(this);
    }

    public InfusedCraftCosts.Cost cost(String tag) {
        return InfusedCraftCosts.cost(this, tag);
    }

    public abstract List<ForgeChoice> choices(Item item, String tag);

    abstract boolean run(Item item, String tag, ForgeChoice choice, Random random);

    boolean refusesUniques() {
        return true;
    }

    public boolean picksAChoice() {
        return false;
    }

    private static List<ForgeChoice> wholeItemIf(boolean offered) {
        return offered ? List.of(ForgeChoice.WHOLE_ITEM) : List.of();
    }

    private static Set<String> damageTypes() {
        return Set.copyOf(InfusedCraftCosts.tags(SWAP_DAMAGE_TYPE));
    }

    public UseVerdict verdict(Item item, String tag) {
        UseVerdict verdict = refusal(item, tag);
        return verdict.allowed() ? verdict : verdict.with("tag", tag == null ? "" : ThemeTags.displayName(tag));
    }

    private UseVerdict refusal(Item item, String tag) {
        if (item == null) {
            return UseVerdict.refused(REFUSAL_PREFIX + "loadFirst");
        }
        if (item.type() != ItemType.HULL_ITEM || !ItemTypes.takesCraftingItem(item.type())) {
            return UseVerdict.refused(REFUSAL_PREFIX + "itemType");
        }
        if (item.mirrored()) {
            return UseVerdict.refused(REFUSAL_PREFIX + "mirrored");
        }
        if (item.corrupted()) {
            return UseVerdict.refused(REFUSAL_PREFIX + "corrupted");
        }
        if (ItemCrafting.isLockedFromCraftingItem(item.definition())) {
            return UseVerdict.refused(REFUSAL_PREFIX + "locked");
        }
        if (cost(tag) == null) {
            return UseVerdict.refused(REFUSAL_PREFIX + "tag");
        }
        if (refusesUniques() && item.definition() != null && item.definition().unique()) {
            return UseVerdict.refused(REFUSAL_PREFIX + "unique");
        }
        return choices(item, tag).isEmpty() ? UseVerdict.refused(REFUSAL_PREFIX + id()) : UseVerdict.ALLOW;
    }

    public boolean canAfford(String tag, CraftingMaterials materials) {
        InfusedCraftCosts.Cost cost = cost(tag);
        return cost != null && materials.count(cost.feedstockId()) >= cost.amount();
    }

    public boolean apply(Item item, String tag, ForgeChoice choice, Random random) {
        return verdict(item, tag).allowed() && choice != null && choices(item, tag).contains(choice) && run(item, tag, choice, random);
    }

    public boolean use(Item item, String tag, ForgeChoice choice, CraftingMaterials materials, Random random) {
        if (!canAfford(tag, materials) || !apply(item, tag, choice, random)) {
            return false;
        }
        InfusedCraftCosts.Cost cost = cost(tag);
        return materials.take(cost.feedstockId(), cost.amount());
    }
}
