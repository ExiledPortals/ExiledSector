package exiledsector.items;

import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.TagWeights;
import exiledsector.skills.tags.ThemeTags;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public enum InfusedCraft {
    REFORGE_WITH_TAG(1) {
        @Override
        boolean offered(Item item, String tag, Set<ModifierSlot> inhibited) {
            return InfusedCrafting.canReforge(item, tag, inhibited);
        }

        @Override
        boolean run(Item item, String tag, Set<ModifierSlot> inhibited, TagWeights favouredTags, Random random) {
            return InfusedCrafting.reforgeWithTag(item, tag, inhibited, favouredTags, random);
        }

        @Override
        List<RolledModifier> changeable(Item item, String tag) {
            return InfusedCrafting.changedByReforge(item);
        }
    },
    ADD_AND_REMOVE(2) {
        @Override
        boolean offered(Item item, String tag, Set<ModifierSlot> inhibited) {
            return !InfusedCrafting.removableForTag(item, tag, inhibited).isEmpty();
        }

        @Override
        boolean run(Item item, String tag, Set<ModifierSlot> inhibited, TagWeights favouredTags, Random random) {
            return InfusedCrafting.addAndRemove(item, tag, inhibited, favouredTags, random);
        }

        @Override
        List<RolledModifier> changeable(Item item, String tag) {
            return InfusedCrafting.removableForTag(item, tag, Set.of());
        }
    };

    static final String REFUSAL_PREFIX = "ui.infusedCraft.refused.";

    private final int reshapingCost;

    InfusedCraft(int reshapingCost) {
        this.reshapingCost = reshapingCost;
    }

    public int reshapingCost() {
        return reshapingCost;
    }

    public static String reshapingId() {
        return Nanoswarm.RESHAPING.itemId();
    }

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

    abstract boolean offered(Item item, String tag, Set<ModifierSlot> inhibited);

    abstract boolean run(Item item, String tag, Set<ModifierSlot> inhibited, TagWeights favouredTags, Random random);

    abstract List<RolledModifier> changeable(Item item, String tag);

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
        if (item.definition() != null && item.definition().unique()) {
            return UseVerdict.refused(REFUSAL_PREFIX + "unique");
        }
        if (offered(item, tag, inhibited)) {
            return UseVerdict.ALLOW;
        }
        return offered(item, tag, Set.of()) ? UseVerdict.refused(REFUSAL_PREFIX + "inhibited") : UseVerdict.refused(REFUSAL_PREFIX + id());
    }

    public Set<CraftingInhibitor> inhibitorsSpent(Item item, String tag, Collection<CraftingInhibitor> inhibitors) {
        Set<CraftingInhibitor> spent = EnumSet.noneOf(CraftingInhibitor.class);
        if (item == null || item.definition() == null || inhibitors.isEmpty()) {
            return spent;
        }
        spent.addAll(CraftingInhibitor.favouring(inhibitors));
        List<RolledModifier> changeable = changeable(item, tag);
        for (CraftingInhibitor inhibitor : inhibitors) {
            if (inhibitor.inhibitedSlot() != null && changeable.stream().anyMatch(rolled -> rolled.slot() == inhibitor.inhibitedSlot())) {
                spent.add(inhibitor);
            }
        }
        return spent;
    }

    public boolean canAfford(String tag, CraftingMaterials materials) {
        InfusedCraftCosts.Cost cost = cost(tag);
        return cost != null && cost.feedstockIds().stream().allMatch(feedstockId -> materials.count(feedstockId) >= cost.amount())
                && materials.count(reshapingId()) >= reshapingCost;
    }

    public boolean apply(Item item, String tag, Collection<CraftingInhibitor> inhibitors, Random random) {
        return verdict(item, tag, inhibitors).allowed()
                && run(item, tag, CraftingInhibitor.inhibitedSlots(inhibitors), CraftingInhibitor.favouredTags(inhibitors), random);
    }

    public Optional<Set<CraftingInhibitor>> use(Item item, String tag, CraftingMaterials materials,
                                                Collection<CraftingInhibitor> inhibitors, Random random) {
        if (!canAfford(tag, materials) || inhibitors.stream().anyMatch(inhibitor -> materials.count(inhibitor.itemId()) < 1)) {
            return Optional.empty();
        }
        Set<CraftingInhibitor> spent = inhibitorsSpent(item, tag, inhibitors);
        if (!apply(item, tag, inhibitors, random)) {
            return Optional.empty();
        }
        InfusedCraftCosts.Cost cost = cost(tag);
        cost.feedstockIds().forEach(feedstockId -> materials.take(feedstockId, cost.amount()));
        if (reshapingCost > 0) {
            materials.take(reshapingId(), reshapingCost);
        }
        spent.forEach(inhibitor -> materials.take(inhibitor.itemId(), 1));
        return Optional.of(spent);
    }
}
