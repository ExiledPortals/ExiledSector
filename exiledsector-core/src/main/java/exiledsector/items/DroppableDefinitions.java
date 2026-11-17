package exiledsector.items;

import exiledsector.data.WeightedPick;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

public final class DroppableDefinitions {

    static final float FACTION_SOURCE_WEIGHT_MULT = 2f;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_.-]+");

    private DroppableDefinitions() {
    }

    public static boolean isDroppable(ItemDefinition definition) {
        return ItemTypes.inGenericPools(definition) && definition.rarity() > 0f
                && SAFE_ID.matcher(definition.id()).matches() && ItemVolumes.dropsNow(definition);
    }

    public static boolean hasDroppable(ItemKind kind) {
        return ItemDefinitions.all().stream().anyMatch(definition -> definition.kind() == kind && isDroppable(definition));
    }

    public static boolean hasDroppableBasic(ItemKind kind) {
        return hasDroppableBasic(kind, definition -> true);
    }

    private static boolean hasDroppableBasic(ItemKind kind, Predicate<ItemDefinition> filter) {
        return ItemDefinitions.all().stream().anyMatch(definition -> isDroppableBasic(definition, kind) && filter.test(definition));
    }

    private static boolean isDroppableBasic(ItemDefinition definition, ItemKind kind) {
        return definition.kind() == kind && !definition.unique() && isDroppable(definition);
    }

    public static ItemDefinition pickBasicOfKind(ItemKind kind, Random random) {
        return pickBasicOfKind(kind, random, definition -> true);
    }

    public static ItemDefinition pickBasicOfKind(ItemKind kind, Random random, Predicate<ItemDefinition> filter) {
        return pick(random, definition -> definition.kind() == kind && !definition.unique() && filter.test(definition));
    }

    public static ItemDefinition pickHullItemBasic(Random random) {
        return pickHullItemBasic(random, definition -> true);
    }

    public static ItemDefinition pickHullItemBasic(Random random, Predicate<ItemDefinition> filter) {
        List<ItemKind> kindsWithBasics = hullItemKindsWithBasics(filter);
        return kindsWithBasics.isEmpty() ? null : pickBasicOfKind(kindsWithBasics.get(random.nextInt(kindsWithBasics.size())), random, filter);
    }

    public static List<ItemKind> hullItemKindsWithBasics() {
        return hullItemKindsWithBasics(definition -> true);
    }

    private static List<ItemKind> hullItemKindsWithBasics(Predicate<ItemDefinition> filter) {
        List<ItemKind> kindsWithBasics = new ArrayList<>();
        for (ItemKind kind : ItemKind.sectionItemTypes()) {
            if (hasDroppableBasic(kind, filter)) {
                kindsWithBasics.add(kind);
            }
        }
        return kindsWithBasics;
    }

    public static ItemDefinition pickUnique(Random random, Predicate<ItemDefinition> allowedUniques) {
        return pickUniqueAtSite(null, random, allowedUniques);
    }

    public static ItemDefinition pickUniqueAtSite(String siteId, Random random, Predicate<ItemDefinition> allowedUniques) {
        return pick(random, definition -> definition.unique() && UniqueSource.allowsSite(definition.uniqueSources(), siteId)
                && allowedUniques.test(definition));
    }

    public static ItemDefinition pickUniqueFromAnySource(Random random, Predicate<ItemDefinition> allowedUniques) {
        return pick(random, definition -> definition.unique() && allowedUniques.test(definition));
    }

    public static ItemDefinition pickUniqueFromFaction(String factionId, Random random, Predicate<ItemDefinition> allowedUniques) {
        return pick(random, definition -> definition.unique() && UniqueSource.allowsSite(definition.uniqueSources(), null)
                && allowedUniques.test(definition), definition -> factionWeight(definition, factionId));
    }

    public static double factionWeight(ItemDefinition definition, String factionId) {
        float mult = UniqueSource.favoursFaction(definition.uniqueSources(), factionId) ? FACTION_SOURCE_WEIGHT_MULT : 1f;
        return definition.rarity() * mult;
    }

    public static ItemDefinition pickOfType(Random random, ItemType type, Predicate<ItemDefinition> filter) {
        List<ItemDefinition> candidates = new ArrayList<>();
        for (ItemDefinition definition : ItemDefinitions.ofType(type)) {
            if (filter.test(definition) && isDroppable(definition)) {
                candidates.add(definition);
            }
        }
        return WeightedPick.pick(candidates, ItemDefinition::rarity, random);
    }

    private static ItemDefinition pick(Random random, Predicate<ItemDefinition> filter) {
        return pick(random, filter, ItemDefinition::rarity);
    }

    private static ItemDefinition pick(Random random, Predicate<ItemDefinition> filter, ToDoubleFunction<ItemDefinition> weightOf) {
        List<ItemDefinition> candidates = new ArrayList<>();
        for (ItemDefinition definition : ItemDefinitions.all()) {
            if (filter.test(definition) && isDroppable(definition)) {
                candidates.add(definition);
            }
        }
        return WeightedPick.pick(candidates, weightOf, random);
    }
}
