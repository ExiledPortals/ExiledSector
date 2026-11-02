package exiledsector.items;

import com.fs.starfarer.api.Global;
import exiledsector.ModCsv;
import exiledsector.ModSettings;
import exiledsector.compat.SalvageSiteCompat;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

public final class ItemDrops {

    static final String DATA_PATH = "data/config/exiledSector/item_salvage.csv";
    public static final String TECH_MINING_FIRST_FIND = "techmining_first_find";
    public static final String TECH_MINING_MONTHLY = "techmining_monthly";
    static final int MIN_FEEDSTOCK_PER_FIND = 1;
    static final int MAX_FEEDSTOCK_PER_FIND = 3;
    static final float KERNEL_CHANCE_SHARE = 0.7f;
    static final float FACTION_SOURCE_WEIGHT_MULT = 2f;
    public static final String BATTLE_FEEDSTOCK_FIELD_ID = "exiledSector_socketPartsPer100DeploymentPoints";
    public static final int DEFAULT_BATTLE_FEEDSTOCK_PER_100_DP = 5;
    public static final int MAX_BATTLE_PARTS_PER_100_DP = 200;
    private static final float DEPLOYMENT_POINTS_PER_SETTING_STEP = 100f;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Logger LOG = Logger.getLogger(ItemDrops.class);
    private static final AtomicReference<Map<String, Rule>> RULES = new AtomicReference<>(Map.of());

    record Rule(List<Float> chances, float uniqueChance, int feedstockMin, int feedstockMax, boolean otherMod) {
    }

    private ItemDrops() {
    }

    public static void load() {
        ModCsv.load("site", DATA_PATH, LOG, ItemDrops::register);
        for (SalvageSiteCompat.Source source : SalvageSiteCompat.enabledSources()) {
            try {
                registerOtherMod(SalvageSiteCompat.rows(source));
            } catch (IOException | JSONException | RuntimeException e) {
                LOG.error("Failed to load " + source.file(), e);
            }
        }
    }

    public static void register(JSONArray rows) throws JSONException {
        RULES.set(Map.copyOf(parse(rows, false)));
    }

    public static void registerOtherMod(JSONArray rows) throws JSONException {
        Map<String, Rule> mergedRules = new HashMap<>(parse(rows, true));
        mergedRules.putAll(RULES.get());
        RULES.set(Map.copyOf(mergedRules));
    }

    private static Map<String, Rule> parse(JSONArray rows, boolean otherMod) throws JSONException {
        Map<String, Rule> loaded = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String siteId = ModCsv.text(row, "site");
            if (siteId.isEmpty()) {
                return;
            }
            try {
                List<Float> chances = new ArrayList<>();
                for (String chanceText : row.optString("chances", "").split(";")) {
                    if (!chanceText.isBlank()) {
                        chances.add(Float.parseFloat(chanceText.trim()));
                    }
                }
                String uniqueChanceText = ModCsv.text(row, "uniqueChance");
                String feedstockMinText = ModCsv.text(row, "feedstockMin");
                String feedstockMaxText = ModCsv.text(row, "feedstockMax");
                int feedstockMin = feedstockMinText.isEmpty() ? 0 : Math.max(0, Integer.parseInt(feedstockMinText));
                int feedstockMax = feedstockMaxText.isEmpty() ? feedstockMin : Math.max(feedstockMin, Integer.parseInt(feedstockMaxText));
                loaded.put(siteId, new Rule(List.copyOf(chances), uniqueChanceText.isEmpty() ? 0f : Float.parseFloat(uniqueChanceText),
                        feedstockMin, feedstockMax, otherMod));
            } catch (NumberFormatException e) {
                LOG.error("Skipping the salvage drop row for site " + siteId + ": " + e.getMessage());
            }
        });
        return loaded;
    }

    public static boolean hasRule(String siteId) {
        return activeRule(siteId) != null;
    }

    static Rule activeRule(String siteId) {
        Rule rule = siteId == null ? null : RULES.get().get(siteId);
        return rule == null || rule.otherMod() && !SalvageSiteCompat.dropsEnabled() ? null : rule;
    }

    public static boolean hasUniqueRoll(String siteId) {
        Rule rule = activeRule(siteId);
        return rule != null && rule.uniqueChance() > 0f;
    }

    public static List<ItemData> roll(String siteId, Random random, ItemChances chances) {
        return roll(siteId, random, chances, definition -> ItemUnlock.canDrop(definition, Global.getSector()));
    }

    public static List<ItemData> roll(String siteId, Random random, ItemChances chances, Predicate<ItemDefinition> uniqueAllowed) {
        return rollItems(siteId, random, chances, uniqueAllowed, true);
    }

    private static List<ItemData> rollItems(String siteId, Random random, ItemChances chances, Predicate<ItemDefinition> uniqueAllowed,
                                            boolean withUnique) {
        Rule rule = activeRule(siteId);
        if (rule == null) {
            return new ArrayList<>();
        }
        List<ItemData> droppedItems = rollCascade(rule, random, chances.general(), chances.rarity(), ItemDrops::pickBasic);
        if (withUnique && random.nextFloat() < rule.uniqueChance() * chances.general() * chances.rarity()) {
            ItemDefinition uniqueDefinition = pickUniqueAtSite(siteId, random, uniqueAllowed);
            if (uniqueDefinition != null && ItemTypes.keptFromSalvage(uniqueDefinition, random)) {
                droppedItems.add(ItemData.rolled(uniqueDefinition, random.nextLong()));
            }
        }
        rollTargeted(rule, random, chances.targeted(ItemKind.SUBROUTINE), chances.rarity(), 1, ItemKind.SUBROUTINE, droppedItems);
        if (!rule.chances().isEmpty()) {
            droppedItems.addAll(ItemTypes.rollSalvage(random, rule.chances().get(0) * chances.general()));
        }
        return droppedItems;
    }

    public static Map<String, Integer> rollMaterials(String siteId, Random random, float feedstockMult, float currencyMult) {
        Map<String, Integer> droppedMaterials = new LinkedHashMap<>();
        Rule rule = activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return droppedMaterials;
        }
        int feedstockFound = rule.feedstockMax() > 0 ? rollFeedstock(rule, random, feedstockMult) : rollParts(rule, random, feedstockMult);
        if (feedstockFound > 0) {
            droppedMaterials.put(ItemDisassembly.FEEDSTOCK_ITEM_ID, feedstockFound);
        }
        rollEachCurrency(kernelDrops(), rule.chances().get(0) * KERNEL_CHANCE_SHARE * currencyMult, random, droppedMaterials);
        return droppedMaterials;
    }

    private static int rollFeedstock(Rule rule, Random random, float feedstockMult) {
        int rolled = rule.feedstockMin() + random.nextInt(rule.feedstockMax() - rule.feedstockMin() + 1);
        return wholeFeedstock(rolled * feedstockMult, random);
    }

    private static int rollParts(Rule rule, Random random, float feedstockMult) {
        int feedstockFound = 0;
        for (float chance : rule.chances()) {
            if (random.nextFloat() < chance * feedstockMult) {
                feedstockFound += MIN_FEEDSTOCK_PER_FIND + random.nextInt(MAX_FEEDSTOCK_PER_FIND - MIN_FEEDSTOCK_PER_FIND + 1);
            }
        }
        return feedstockFound;
    }

    public static float battleFeedstockPerDeploymentPoint() {
        return Math.max(0, ModSettings.intOr(BATTLE_FEEDSTOCK_FIELD_ID, DEFAULT_BATTLE_FEEDSTOCK_PER_100_DP)) / DEPLOYMENT_POINTS_PER_SETTING_STEP;
    }

    public static int wholeFeedstock(float exactFeedstock, Random random) {
        if (!(exactFeedstock > 0f)) {
            return 0;
        }
        int wholePartCount = (int) exactFeedstock;
        return wholePartCount + (random.nextFloat() < exactFeedstock - wholePartCount ? 1 : 0);
    }

    static List<CurrencyDrop> kernelDrops() {
        List<CurrencyDrop> kernelDrops = new ArrayList<>();
        for (ItemCurrency currency : ItemCurrencies.all()) {
            if (currency.dropsFromShipLoot()) {
                kernelDrops.add(new CurrencyDrop(currency.itemId(), currency.dropWeight()));
            }
        }
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            kernelDrops.add(new CurrencyDrop(inhibitor.itemId(), inhibitor.dropWeight()));
        }
        return kernelDrops;
    }

    public record CurrencyDrop(String itemId, float weight) {
    }

    public static void rollEachCurrency(List<CurrencyDrop> drops, float expectedDrops, Random random, Map<String, Integer> droppedMaterials) {
        double totalWeight = drops.stream().mapToDouble(drop -> Math.max(0f, drop.weight())).sum();
        if (!(expectedDrops > 0f) || !(totalWeight > 0d)) {
            return;
        }
        for (CurrencyDrop drop : drops) {
            int count = wholeFeedstock((float) (expectedDrops * Math.max(0f, drop.weight()) / totalWeight), random);
            if (count > 0) {
                droppedMaterials.merge(drop.itemId(), count, Integer::sum);
            }
        }
    }

    public static ItemDefinition pickBasic(Random random) {
        return pick(random, ItemDrops::canBeGrown);
    }

    public static boolean canBeGrown(ItemDefinition definition) {
        return definition.type() == ItemType.SUBROUTINE && !definition.unique() && isDroppable(definition);
    }

    public record SiteLoot(List<ItemData> items, List<ShipCurrency> upgrades, Map<String, Integer> materials) {

        public static final SiteLoot NONE = new SiteLoot(List.of(), List.of(), Map.of());

        public SiteLoot {
            items = List.copyOf(items);
            upgrades = List.copyOf(upgrades);
            materials = Collections.unmodifiableMap(new LinkedHashMap<>(materials));
        }

        public boolean isEmpty() {
            return items.isEmpty() && upgrades.isEmpty() && materials.isEmpty();
        }

        public SiteLoot plus(List<ItemData> moreItems, List<ShipCurrency> moreUpgrades, Map<String, Integer> moreMaterials) {
            if (moreItems.isEmpty() && moreUpgrades.isEmpty() && moreMaterials.isEmpty()) {
                return this;
            }
            List<ItemData> allItems = new ArrayList<>(items);
            allItems.addAll(moreItems);
            List<ShipCurrency> allUpgrades = new ArrayList<>(upgrades);
            allUpgrades.addAll(moreUpgrades);
            Map<String, Integer> allMaterials = new LinkedHashMap<>(materials);
            moreMaterials.forEach((itemId, quantity) -> allMaterials.merge(itemId, quantity, Integer::sum));
            return new SiteLoot(allItems, allUpgrades, allMaterials);
        }

        public SiteLoot plus(SiteLoot other) {
            return plus(other.items(), other.upgrades(), other.materials());
        }
    }

    public static SiteLoot rollSite(String siteId, Random random, ItemChances chances, float feedstockMult, float currencyMult) {
        List<ItemData> droppedItems = roll(siteId, random, chances);
        return new SiteLoot(droppedItems, List.of(), rollMaterials(siteId, random, feedstockMult, currencyMult));
    }

    public static SiteLoot rollFleetSalvage(String siteId, Random random, ItemChances chances, float feedstockMult, float currencyMult) {
        List<ItemData> droppedItems = rollItems(siteId, random, chances, definition -> false, false);
        return new SiteLoot(droppedItems, List.of(), rollMaterials(siteId, random, feedstockMult, currencyMult));
    }

    static void rollTargeted(Rule rule, Random random, float extraShare, float rarity, int sharedKinds, ItemKind kind,
                                     List<ItemData> droppedItems) {
        if (!(extraShare > 0f) || !hasDroppableBasic(kind)) {
            return;
        }
        int extraCount = wholeFeedstock(expectedCascadeCount(rule.chances()) * extraShare / sharedKinds, random);
        for (int i = 0; i < extraCount; i++) {
            ItemDefinition definition = pickSectionItemBasic(kind, random);
            if (definition != null) {
                droppedItems.add(ItemData.rolledWithRarity(definition, random.nextLong(), rarity));
            }
        }
    }

    static float expectedCascadeCount(List<Float> chances) {
        float reachChance = 1f;
        float expectedCount = 0f;
        for (float chance : chances) {
            reachChance *= Math.max(0f, Math.min(1f, chance));
            expectedCount += reachChance;
        }
        return expectedCount;
    }

    public static boolean hasDroppableBasic(ItemKind kind) {
        return hasDroppableBasic(kind, definition -> true);
    }

    static boolean hasDroppableBasic(ItemKind kind, Predicate<ItemDefinition> filter) {
        return ItemDefinitions.all().stream().anyMatch(definition -> isDroppableBasic(definition, kind) && filter.test(definition));
    }

    static List<ItemData> rollCascade(Rule rule, Random random, float chanceMult, float rarity,
                                                        Function<Random, ItemDefinition> picker) {
        List<ItemData> droppedItems = new ArrayList<>();
        for (float chance : rule.chances()) {
            if (random.nextFloat() >= chance * chanceMult) {
                break;
            }
            ItemDefinition basicDefinition = picker.apply(random);
            if (basicDefinition != null) {
                droppedItems.add(ItemData.rolledWithRarity(basicDefinition, random.nextLong(), rarity));
            }
        }
        return droppedItems;
    }

    public static ItemDefinition pickSectionItemBasic(ItemKind sectionKind, Random random) {
        return pickSectionItemBasic(sectionKind, random, definition -> true);
    }

    public static ItemDefinition pickSectionItemBasic(ItemKind sectionKind, Random random, Predicate<ItemDefinition> filter) {
        return pick(random, definition -> definition.kind() == sectionKind && !definition.unique() && filter.test(definition));
    }

    private static boolean isDroppableBasic(ItemDefinition definition, ItemKind sectionKind) {
        return definition.kind() == sectionKind && !definition.unique() && isDroppable(definition);
    }

    private static boolean isDroppable(ItemDefinition definition) {
        return ItemTypes.inGenericPools(definition) && definition.rarity() > 0f
                && SAFE_ID.matcher(definition.id()).matches() && ItemVolumes.dropsNow(definition);
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

    static double factionWeight(ItemDefinition definition, String factionId) {
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

    public static void clear() {
        RULES.set(Map.of());
    }
}
