package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public enum StarChartCurrency implements ItemCurrency {

    REFORGE("exiledSector_reforge_cipher", "reforge", 30, 5f, MaterialTier.MAJOR, new Color(80, 205, 225)),
    ANNULMENT("exiledSector_annul_cipher", "annul", 40, 3f, MaterialTier.MAJOR, new Color(175, 180, 200)),
    CARTOGRAPHY("exiledSector_cartographer_cipher", "cartographer", 120, 2f, MaterialTier.EXOTIC, new Color(245, 215, 110));

    public static final List<CraftingCurrency> KERNELS_ON_CHARTS = List.of(CraftingCurrency.AUGMENTATION, CraftingCurrency.CORRUPTION,
            CraftingCurrency.TRANSPOSITION, CraftingCurrency.RECALIBRATION, CraftingCurrency.ATTUNEMENT);
    static final int MAX_REFORGE_ATTEMPTS = 16;

    private final String itemId;
    private final String key;
    private final int defaultPartsCost;
    private final float dropWeight;
    private final MaterialTier tier;
    private final Color color;

    StarChartCurrency(String itemId, String key, int defaultPartsCost, float dropWeight, MaterialTier tier, Color color) {
        this.itemId = itemId;
        this.key = key;
        this.defaultPartsCost = defaultPartsCost;
        this.dropWeight = dropWeight;
        this.tier = tier;
        this.color = color;
    }

    public static void registerAll() {
        for (StarChartCurrency currency : values()) {
            if (!ItemCurrencies.isRegistered(currency)) {
                ItemCurrencies.register(currency);
            }
        }
        for (CraftingCurrency kernel : KERNELS_ON_CHARTS) {
            ItemCurrencies.extend(kernel, ItemType.STAR_CHART);
        }
    }

    public static void unregisterAll() {
        for (StarChartCurrency currency : values()) {
            ItemCurrencies.unregister(currency);
        }
        for (CraftingCurrency kernel : KERNELS_ON_CHARTS) {
            ItemCurrencies.retract(kernel, ItemType.STAR_CHART);
        }
    }

    public static StarChartCurrency byItemId(String itemId) {
        for (StarChartCurrency currency : values()) {
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
        return key;
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
        return type == ItemType.STAR_CHART;
    }

    @Override
    public boolean canUse(Item item, CraftingContext context) {
        if (!(item instanceof StarChart chart) || chart.definition() == null) {
            return false;
        }
        return switch (this) {
            case REFORGE -> canReforge(chart, context.guardedSlots());
            case ANNULMENT -> !annullable(chart, context.guardedSlots()).isEmpty();
            case CARTOGRAPHY -> chart.tier() < StarChart.HIGHEST_TIER;
        };
    }

    @Override
    public Item use(Item item, CraftingContext context, Random random) {
        if (!canUse(item, context)) {
            return null;
        }
        StarChart chart = (StarChart) item;
        return switch (this) {
            case REFORGE -> reforge(chart, random, context.guardedSlots());
            case ANNULMENT -> annul(chart, context.guardedSlots());
            case CARTOGRAPHY -> raiseTier(chart);
        };
    }

    @Override
    public boolean respectsWards() {
        return this == REFORGE || this == ANNULMENT;
    }

    @Override
    public boolean wouldChange(Item item, RolledModifier rolled) {
        return respectsWards();
    }

    static boolean canReforge(StarChart chart) {
        return canReforge(chart, Set.of());
    }

    static boolean canReforge(StarChart chart, Set<ModifierSlot> guarded) {
        ItemDefinition definition = chart.definition();
        return !definition.unique() && (rerollable(definition, definition.prefixes(), ModifierSlot.PREFIX, chart.tier(), guarded)
                || rerollable(definition, definition.suffixes(), ModifierSlot.SUFFIX, chart.tier(), guarded));
    }

    private static boolean rerollable(ItemDefinition definition, List<PoolEntry> side, ModifierSlot slot, int tier, Set<ModifierSlot> guarded) {
        return !guarded.contains(slot) && !AffixLayout.rollableAt(definition.type(), side, tier).isEmpty();
    }

    static StarChart reforge(StarChart chart, Random random) {
        return reforge(chart, random, Set.of());
    }

    static StarChart reforge(StarChart chart, Random random, Set<ModifierSlot> guarded) {
        ItemDefinition definition = chart.definition();
        for (int attempt = 0; attempt < MAX_REFORGE_ATTEMPTS; attempt++) {
            List<RolledModifier> rerolled = keepGuarded(chart.explicits(), ItemRoller.rollAtTier(definition, random.nextLong(), chart.tier()), guarded);
            if (!rerolled.equals(chart.explicits())) {
                chart.replaceExplicits(rerolled);
                chart.refreezeName();
                return chart;
            }
        }
        return null;
    }

    private static List<RolledModifier> keepGuarded(List<RolledModifier> previous, List<RolledModifier> rerolled, Set<ModifierSlot> guarded) {
        if (guarded.isEmpty()) {
            return rerolled;
        }
        List<RolledModifier> merged = new ArrayList<>(previous.stream().filter(rolled -> guarded.contains(rolled.slot())).toList());
        for (RolledModifier rolled : rerolled) {
            if (!guarded.contains(rolled.slot())) {
                merged.add(AffixLayout.insertIndex(merged, rolled.slot()), rolled);
            }
        }
        return merged;
    }

    static List<Integer> annullable(StarChart chart) {
        return annullable(chart, Set.of());
    }

    static List<Integer> annullable(StarChart chart, Set<ModifierSlot> guarded) {
        List<Integer> removable = new ArrayList<>();
        if (chart.definition().unique()) {
            return removable;
        }
        List<RolledModifier> explicits = chart.explicits();
        for (int index = 0; index < explicits.size(); index++) {
            RolledModifier rolled = explicits.get(index);
            if (!rolled.locked() && !guarded.contains(rolled.slot())) {
                removable.add(index);
            }
        }
        return removable;
    }

    static StarChart annul(StarChart chart, Set<ModifierSlot> guarded) {
        List<Integer> removable = annullable(chart, guarded);
        List<RolledModifier> remaining = new ArrayList<>();
        List<RolledModifier> explicits = chart.explicits();
        for (int index = 0; index < explicits.size(); index++) {
            if (!removable.contains(index)) {
                remaining.add(explicits.get(index));
            }
        }
        chart.replaceExplicits(remaining);
        chart.refreezeName();
        return chart;
    }

    static StarChart raiseTier(StarChart chart) {
        chart.applyTraits(new ItemTraits(chart.tier() + 1, chart.quality(), null, null, false));
        return chart;
    }
}
