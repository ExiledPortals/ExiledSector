package exiledsector.items;

import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.ThemeTags;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public enum InfusedCraft {
    REFORGE_WITH_TAG {
        @Override
        List<ForgeChoice> offeredChoices(Item item, String tag, Set<ModifierSlot> inhibited) {
            return wholeItemIf(InfusedCrafting.canReforge(item, tag, inhibited));
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Set<ModifierSlot> inhibited, Random random) {
            return InfusedCrafting.reforgeWithTag(item, tag, inhibited, random);
        }

        @Override
        List<RolledModifier> changeable(Item item, String tag) {
            return InfusedCrafting.changedByReforge(item);
        }
    },
    REFORGE_MORE_ALIKE {
        @Override
        List<ForgeChoice> offeredChoices(Item item, String tag, Set<ModifierSlot> inhibited) {
            return wholeItemIf(InfusedCrafting.canReforge(item, null, inhibited));
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Set<ModifierSlot> inhibited, Random random) {
            return InfusedCrafting.reforgeAlike(item, InfusedCrafting.MORE_ALIKE_WEIGHT, inhibited, random);
        }

        @Override
        List<RolledModifier> changeable(Item item, String tag) {
            return InfusedCrafting.changedByReforge(item);
        }
    },
    REFORGE_LESS_ALIKE {
        @Override
        List<ForgeChoice> offeredChoices(Item item, String tag, Set<ModifierSlot> inhibited) {
            return wholeItemIf(InfusedCrafting.canReforge(item, null, inhibited));
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Set<ModifierSlot> inhibited, Random random) {
            return InfusedCrafting.reforgeAlike(item, InfusedCrafting.LESS_ALIKE_WEIGHT, inhibited, random);
        }

        @Override
        List<RolledModifier> changeable(Item item, String tag) {
            return InfusedCrafting.changedByReforge(item);
        }
    },
    REROLL_TAG_VALUES {
        @Override
        List<ForgeChoice> offeredChoices(Item item, String tag, Set<ModifierSlot> inhibited) {
            return wholeItemIf(!InfusedCrafting.rerollableWithTag(item, tag, inhibited).isEmpty());
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Set<ModifierSlot> inhibited, Random random) {
            return InfusedCrafting.rerollTagValues(item, tag, inhibited, random);
        }

        @Override
        List<RolledModifier> changeable(Item item, String tag) {
            return InfusedCrafting.rerollableWithTag(item, tag, Set.of());
        }

        @Override
        boolean refusesUniques() {
            return false;
        }
    },
    SWAP_DAMAGE_TYPE {
        @Override
        List<ForgeChoice> offeredChoices(Item item, String tag, Set<ModifierSlot> inhibited) {
            return InfusedCrafting.swappable(item, tag, damageTypes(), inhibited).stream().map(ForgeChoice::modifier).toList();
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Set<ModifierSlot> inhibited, Random random) {
            return InfusedCrafting.swapDamageType(item, choice.modifier(), tag, damageTypes(), inhibited);
        }

        @Override
        List<RolledModifier> changeable(Item item, String tag) {
            return InfusedCrafting.swappable(item, tag, damageTypes(), Set.of());
        }

        @Override
        public boolean picksAChoice() {
            return true;
        }
    },
    ADD_AND_REMOVE {
        @Override
        List<ForgeChoice> offeredChoices(Item item, String tag, Set<ModifierSlot> inhibited) {
            return wholeItemIf(!InfusedCrafting.removableForTag(item, tag, inhibited).isEmpty());
        }

        @Override
        boolean run(Item item, String tag, ForgeChoice choice, Set<ModifierSlot> inhibited, Random random) {
            return InfusedCrafting.addAndRemove(item, tag, inhibited, random);
        }

        @Override
        List<RolledModifier> changeable(Item item, String tag) {
            return InfusedCrafting.removableForTag(item, tag, Set.of());
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

    public List<ForgeChoice> choices(Item item, String tag, Collection<CraftingInhibitor> inhibitors) {
        return offeredChoices(item, tag, CraftingInhibitor.inhibitedSlots(inhibitors));
    }

    abstract List<ForgeChoice> offeredChoices(Item item, String tag, Set<ModifierSlot> inhibited);

    abstract boolean run(Item item, String tag, ForgeChoice choice, Set<ModifierSlot> inhibited, Random random);

    abstract List<RolledModifier> changeable(Item item, String tag);

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

    public UseVerdict verdict(Item item, String tag, Collection<CraftingInhibitor> inhibitors) {
        UseVerdict verdict = refusal(item, tag, CraftingInhibitor.inhibitedSlots(inhibitors));
        return verdict.allowed() ? verdict : verdict.with("tag", tag == null ? "" : ThemeTags.displayName(tag)).with("article", article(tag));
    }

    public static String article(String tag) {
        String tagName = tag == null ? "" : ThemeTags.displayName(tag);
        boolean vowel = !tagName.isEmpty() && "aeiou".indexOf(Character.toLowerCase(tagName.charAt(0))) >= 0;
        return Translation.text(vowel ? "ui.infusedCraft.article.an" : "ui.infusedCraft.article.a");
    }

    private UseVerdict refusal(Item item, String tag, Set<ModifierSlot> inhibited) {
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
        if (!offeredChoices(item, tag, inhibited).isEmpty()) {
            return UseVerdict.ALLOW;
        }
        return offeredChoices(item, tag, Set.of()).isEmpty() ? UseVerdict.refused(REFUSAL_PREFIX + id())
                : UseVerdict.refused(REFUSAL_PREFIX + "inhibited");
    }

    public Set<CraftingInhibitor> inhibitorsSpent(Item item, String tag, Collection<CraftingInhibitor> inhibitors) {
        Set<CraftingInhibitor> spent = EnumSet.noneOf(CraftingInhibitor.class);
        if (item == null || item.definition() == null || inhibitors.isEmpty()) {
            return spent;
        }
        List<RolledModifier> changeable = changeable(item, tag);
        for (CraftingInhibitor inhibitor : inhibitors) {
            if (changeable.stream().anyMatch(rolled -> rolled.slot() == inhibitor.inhibitedSlot())) {
                spent.add(inhibitor);
            }
        }
        return spent;
    }

    public boolean canAfford(String tag, CraftingMaterials materials) {
        InfusedCraftCosts.Cost cost = cost(tag);
        return cost != null && materials.count(cost.feedstockId()) >= cost.amount();
    }

    public boolean apply(Item item, String tag, ForgeChoice choice, Collection<CraftingInhibitor> inhibitors, Random random) {
        Set<ModifierSlot> inhibited = CraftingInhibitor.inhibitedSlots(inhibitors);
        return verdict(item, tag, inhibitors).allowed() && choice != null && offeredChoices(item, tag, inhibited).contains(choice)
                && run(item, tag, choice, inhibited, random);
    }

    public Optional<Set<CraftingInhibitor>> use(Item item, String tag, ForgeChoice choice, CraftingMaterials materials,
                                                Collection<CraftingInhibitor> inhibitors, Random random) {
        if (!canAfford(tag, materials) || inhibitors.stream().anyMatch(inhibitor -> materials.count(inhibitor.itemId()) < 1)) {
            return Optional.empty();
        }
        Set<CraftingInhibitor> spent = inhibitorsSpent(item, tag, inhibitors);
        if (!apply(item, tag, choice, inhibitors, random)) {
            return Optional.empty();
        }
        InfusedCraftCosts.Cost cost = cost(tag);
        materials.take(cost.feedstockId(), cost.amount());
        spent.forEach(inhibitor -> materials.take(inhibitor.itemId(), 1));
        return Optional.of(spent);
    }
}
