package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public enum StarChartCipher implements ItemCurrency {

    REFORGE("exiledSector_reforge_cipher", "reforge", 30, 5f, MaterialTier.MAJOR, new Color(80, 205, 225)),
    ERASURE("exiledSector_erasure_cipher", "erasure", 40, 3f, MaterialTier.MAJOR, new Color(175, 180, 200)),
    CARTOGRAPHY("exiledSector_cartographer_cipher", "cartographer", 120, 2f, MaterialTier.EXOTIC, new Color(245, 215, 110));

    public static final List<Nanoswarm> KERNELS_ON_CHARTS = List.of(Nanoswarm.AUGMENTATION, Nanoswarm.CORRUPTION,
            Nanoswarm.ANNULMENT, Nanoswarm.TUNING);
    static final int MAX_REFORGE_ATTEMPTS = 16;

    private final String itemId;
    private final String key;
    private final int defaultFeedstockCost;
    private final float dropWeight;
    private final MaterialTier tier;
    private final Color color;

    StarChartCipher(String itemId, String key, int defaultFeedstockCost, float dropWeight, MaterialTier tier, Color color) {
        this.itemId = itemId;
        this.key = key;
        this.defaultFeedstockCost = defaultFeedstockCost;
        this.dropWeight = dropWeight;
        this.tier = tier;
        this.color = color;
    }

    public static void registerAll() {
        for (StarChartCipher currency : values()) {
            if (!ItemCurrencies.isRegistered(currency)) {
                ItemCurrencies.register(currency);
            }
        }
        for (Nanoswarm kernel : KERNELS_ON_CHARTS) {
            ItemCurrencies.extend(kernel, ItemType.STAR_CHART);
        }
    }

    public static StarChartCipher byItemId(String itemId) {
        for (StarChartCipher currency : values()) {
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
    public boolean growable() {
        return defaultFeedstockCost > 0;
    }

    @Override
    public int feedstockCost() {
        return ItemCraftingCosts.cost(itemId, defaultFeedstockCost);
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
            case REFORGE -> canReforge(chart, context.inhibitedSlots());
            case ERASURE -> !erasable(chart, context.inhibitedSlots()).isEmpty();
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
            case REFORGE -> reforge(chart, random, context.inhibitedSlots());
            case ERASURE -> erase(chart, context.inhibitedSlots());
            case CARTOGRAPHY -> raiseTier(chart);
        };
    }

    @Override
    public boolean respectsInhibitors() {
        return this == REFORGE || this == ERASURE;
    }

    @Override
    public boolean wouldChange(Item item, RolledModifier rolled) {
        return respectsInhibitors();
    }

    static boolean canReforge(StarChart chart) {
        return canReforge(chart, Set.of());
    }

    static boolean canReforge(StarChart chart, Set<ModifierSlot> inhibited) {
        ItemDefinition definition = chart.definition();
        return definition != null && (rerollable(definition, definition.prefixes(), ModifierSlot.PREFIX, chart.tier(), inhibited)
                || rerollable(definition, definition.suffixes(), ModifierSlot.SUFFIX, chart.tier(), inhibited));
    }

    private static boolean rerollable(ItemDefinition definition, List<PoolEntry> side, ModifierSlot slot, int tier, Set<ModifierSlot> inhibited) {
        return !inhibited.contains(slot) && !AffixLayout.rollableAt(definition.type(), side, tier).isEmpty();
    }

    static StarChart reforge(StarChart chart, Random random) {
        return reforge(chart, random, Set.of());
    }

    static StarChart reforge(StarChart chart, Random random, Set<ModifierSlot> inhibited) {
        ItemDefinition definition = chart.definition();
        if (definition == null) {
            return null;
        }
        for (int attempt = 0; attempt < MAX_REFORGE_ATTEMPTS; attempt++) {
            List<RolledModifier> rerolled = keepInhibited(chart.explicits(), ItemRoller.rollAtTier(definition, random.nextLong(), chart.tier()), inhibited);
            if (!rerolled.equals(chart.explicits())) {
                chart.replaceExplicits(rerolled);
                chart.refreezeName();
                return chart;
            }
        }
        return null;
    }

    private static List<RolledModifier> keepInhibited(List<RolledModifier> previous, List<RolledModifier> rerolled, Set<ModifierSlot> inhibited) {
        if (inhibited.isEmpty()) {
            return rerolled;
        }
        List<RolledModifier> merged = new ArrayList<>(previous.stream().filter(rolled -> inhibited.contains(rolled.slot())).toList());
        for (RolledModifier rolled : rerolled) {
            if (!inhibited.contains(rolled.slot()) && !ModifierExclusivity.clashesWithRolled(rolled.modifierId(), merged)) {
                merged.add(AffixLayout.insertIndex(merged, rolled.slot()), rolled);
            }
        }
        return merged;
    }

    static List<Integer> erasable(StarChart chart) {
        return erasable(chart, Set.of());
    }

    static List<Integer> erasable(StarChart chart, Set<ModifierSlot> inhibited) {
        List<Integer> removable = new ArrayList<>();
        ItemDefinition definition = chart.definition();
        if (definition == null) {
            return removable;
        }
        List<RolledModifier> explicits = chart.explicits();
        for (int index = 0; index < explicits.size(); index++) {
            RolledModifier rolled = explicits.get(index);
            if (!rolled.immutable() && !inhibited.contains(rolled.slot())) {
                removable.add(index);
            }
        }
        return removable;
    }

    static StarChart erase(StarChart chart, Set<ModifierSlot> inhibited) {
        List<Integer> removable = erasable(chart, inhibited);
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
