package exiledsector.items;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
            case REFORGE -> canReforge(chart);
            case ANNULMENT -> !annullable(chart).isEmpty();
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
            case REFORGE -> reforge(chart, random);
            case ANNULMENT -> annul(chart, random);
            case CARTOGRAPHY -> raiseTier(chart);
        };
    }

    static boolean canReforge(StarChart chart) {
        ItemDefinition definition = chart.definition();
        return !definition.unique() && (!AffixLayout.rollableAt(definition.prefixes(), chart.tier()).isEmpty()
                || !AffixLayout.rollableAt(definition.suffixes(), chart.tier()).isEmpty());
    }

    static StarChart reforge(StarChart chart, Random random) {
        ItemDefinition definition = chart.definition();
        for (int attempt = 0; attempt < MAX_REFORGE_ATTEMPTS; attempt++) {
            List<RolledModifier> rerolled = ItemRoller.rollAtTier(definition, random.nextLong(), chart.tier());
            if (!rerolled.equals(chart.explicits())) {
                chart.replaceExplicits(rerolled);
                chart.refreezeName();
                return chart;
            }
        }
        return null;
    }

    static List<Integer> annullable(StarChart chart) {
        List<Integer> removable = new ArrayList<>();
        if (chart.definition().unique()) {
            return removable;
        }
        List<RolledModifier> explicits = chart.explicits();
        for (int index = 0; index < explicits.size(); index++) {
            if (!explicits.get(index).locked()) {
                removable.add(index);
            }
        }
        return removable;
    }

    static StarChart annul(StarChart chart, Random random) {
        List<Integer> removable = annullable(chart);
        List<RolledModifier> remaining = new ArrayList<>(chart.explicits());
        remaining.remove((int) removable.get(random.nextInt(removable.size())));
        chart.replaceExplicits(remaining);
        chart.refreezeName();
        return chart;
    }

    static StarChart raiseTier(StarChart chart) {
        chart.applyTraits(new ItemTraits(chart.tier() + 1, chart.quality(), null, null, false));
        return chart;
    }
}
