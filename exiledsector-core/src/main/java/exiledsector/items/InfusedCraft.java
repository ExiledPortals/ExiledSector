package exiledsector.items;

import exiledsector.i18n.Translation;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.TagWeights;
import exiledsector.skills.tags.ThemeTags;

import java.util.Collection;
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
        boolean run(Item item, String tag, Set<ModifierSlot> inhibited, TagWeights tagWeights, Random random) {
            return InfusedCrafting.reforgeWithTag(item, tag, inhibited, tagWeights, random);
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
        boolean run(Item item, String tag, Set<ModifierSlot> inhibited, TagWeights tagWeights, Random random) {
            return InfusedCrafting.addAndRemove(item, tag, inhibited, tagWeights, random);
        }

        @Override
        List<RolledModifier> changeable(Item item, String tag) {
            return InfusedCrafting.removableForTag(item, tag, Set.of());
        }
    };

    static final String REFUSAL_PREFIX = HullItemCraftRefusals.PREFIX;

    private final int reshapingCost;

    InfusedCraft(int reshapingCost) {
        this.reshapingCost = reshapingCost;
    }

    public int reshapingCost() {
        return reshapingCost;
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

    public CraftCost cost(String tag) {
        return InfusedCraftCosts.cost(this, tag);
    }

    public HullItemCraft forTag(String tag) {
        return new TaggedCraft(this, tag);
    }

    abstract boolean offered(Item item, String tag, Set<ModifierSlot> inhibited);

    abstract boolean run(Item item, String tag, Set<ModifierSlot> inhibited, TagWeights tagWeights, Random random);

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
        UseVerdict itemRefusal = HullItemCraftRefusals.ofItem(item);
        if (!itemRefusal.allowed()) {
            return itemRefusal;
        }
        if (cost(tag) == null) {
            return UseVerdict.refused(REFUSAL_PREFIX + "tag");
        }
        if (item.definition().unique()) {
            return UseVerdict.refused(REFUSAL_PREFIX + "unique");
        }
        return HullItemCraftRefusals.ofInhibitors(slots -> offered(item, tag, slots), inhibited, REFUSAL_PREFIX + id());
    }

    public Set<CraftingInhibitor> inhibitorsSpent(Item item, String tag, Collection<CraftingInhibitor> inhibitors) {
        return CraftingInhibitor.spentOn(item, inhibitors, true, rolled -> changeable(item, tag).contains(rolled));
    }

    public boolean canAfford(String tag, CraftingMaterials materials) {
        return forTag(tag).canAfford(materials);
    }

    public boolean apply(Item item, String tag, Collection<CraftingInhibitor> inhibitors, Random random) {
        return verdict(item, tag, inhibitors).allowed()
                && run(item, tag, CraftingInhibitor.inhibitedSlots(inhibitors), CraftingInhibitor.tagWeights(inhibitors), random);
    }

    public Optional<Set<CraftingInhibitor>> use(Item item, String tag, CraftingMaterials materials,
                                                Collection<CraftingInhibitor> inhibitors, Random random) {
        return forTag(tag).use(item, materials, inhibitors, random);
    }

    private record TaggedCraft(InfusedCraft craft, String tag) implements HullItemCraft {

        @Override
        public String id() {
            return craft.id();
        }

        @Override
        public CraftCost cost() {
            return craft.cost(tag);
        }

        @Override
        public UseVerdict verdict(Item item, Collection<CraftingInhibitor> inhibitors) {
            return craft.verdict(item, tag, inhibitors);
        }

        @Override
        public Set<CraftingInhibitor> inhibitorsSpent(Item item, Collection<CraftingInhibitor> inhibitors) {
            return craft.inhibitorsSpent(item, tag, inhibitors);
        }

        @Override
        public boolean changeModifiers(Item item, Collection<CraftingInhibitor> inhibitors, Random random) {
            return craft.run(item, tag, CraftingInhibitor.inhibitedSlots(inhibitors), CraftingInhibitor.tagWeights(inhibitors), random);
        }
    }
}
