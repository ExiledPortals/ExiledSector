package exiledsector.items;

import com.fs.starfarer.api.Global;
import exiledlib.ModCsv;
import exiledlib.ModSettings;
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
import java.util.regex.Pattern;

public final class ItemDrops {

    static final String DATA_PATH = "data/config/exiledSector/item_salvage.csv";
    public static final String TECH_MINING_FIRST_FIND = "techmining_first_find";
    public static final String TECH_MINING_MONTHLY = "techmining_monthly";
    static final int MIN_PARTS_PER_FIND = 1;
    static final int MAX_PARTS_PER_FIND = 3;
    static final float KERNEL_CHANCE_SHARE = 0.35f;
    public static final String BATTLE_PARTS_FIELD_ID = "exiledSector_socketPartsPer100DeploymentPoints";
    public static final int DEFAULT_BATTLE_PARTS_PER_100_DP = 5;
    public static final int MAX_BATTLE_PARTS_PER_100_DP = 200;
    private static final float DEPLOYMENT_POINTS_PER_SETTING_STEP = 100f;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Logger LOG = Logger.getLogger(ItemDrops.class);
    private static final AtomicReference<Map<String, Rule>> RULES = new AtomicReference<>(Map.of());

    record Rule(List<Float> chances, float uniqueChance, boolean otherMod) {
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
                loaded.put(siteId, new Rule(List.copyOf(chances), uniqueChanceText.isEmpty() ? 0f : Float.parseFloat(uniqueChanceText), otherMod));
            } catch (NumberFormatException e) {
                LOG.error("Skipping the salvage drop row for site " + siteId + ": " + e.getMessage());
            }
        });
        return loaded;
    }

    public static boolean hasRule(String siteId) {
        return activeRule(siteId) != null;
    }

    private static Rule activeRule(String siteId) {
        Rule rule = siteId == null ? null : RULES.get().get(siteId);
        return rule == null || rule.otherMod() && !SalvageSiteCompat.dropsEnabled() ? null : rule;
    }

    public static boolean hasUniqueRoll(String siteId) {
        Rule rule = siteId == null ? null : RULES.get().get(siteId);
        return rule != null && rule.uniqueChance() > 0f;
    }

    public static List<ItemData> roll(String siteId, Random random) {
        return roll(siteId, random, 1f);
    }

    public static List<ItemData> roll(String siteId, Random random, float chanceMult) {
        return roll(siteId, random, chanceMult, definition -> ItemUnlock.canDrop(definition, Global.getSector()));
    }

    public static List<ItemData> roll(String siteId, Random random, float chanceMult,
                                                Predicate<ItemDefinition> uniqueAllowed) {
        return roll(siteId, random, ItemChances.of(chanceMult), uniqueAllowed);
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
            ItemDefinition uniqueDefinition = pickUniqueFrom(UniqueSource.Kind.SITE, siteId, random, uniqueAllowed);
            if (uniqueDefinition != null) {
                droppedItems.add(ItemData.rolled(uniqueDefinition, random.nextLong()));
            }
        }
        rollTargeted(rule, random, chances.targeted(ItemKind.SUBROUTINE), chances.rarity(), 1, ItemKind.SUBROUTINE, droppedItems);
        if (!rule.chances().isEmpty()) {
            droppedItems.addAll(ItemTypes.rollSalvage(random, rule.chances().get(0) * chances.general()));
        }
        return droppedItems;
    }

    public static Map<String, Integer> rollMaterials(String siteId, Random random) {
        return rollMaterials(siteId, random, 1f);
    }

    public static Map<String, Integer> rollMaterials(String siteId, Random random, float chanceMult) {
        return rollMaterials(siteId, random, chanceMult, chanceMult);
    }

    public static Map<String, Integer> rollMaterials(String siteId, Random random, float feedstockMult, float currencyMult) {
        Map<String, Integer> droppedMaterials = new LinkedHashMap<>();
        Rule rule = activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return droppedMaterials;
        }
        int partsFound = 0;
        for (float chance : rule.chances()) {
            if (random.nextFloat() < chance * feedstockMult) {
                partsFound += MIN_PARTS_PER_FIND + random.nextInt(MAX_PARTS_PER_FIND - MIN_PARTS_PER_FIND + 1);
            }
        }
        if (partsFound > 0) {
            droppedMaterials.put(ItemDisassembly.PARTS_ITEM_ID, partsFound);
        }
        if (random.nextFloat() < rule.chances().get(0) * KERNEL_CHANCE_SHARE * currencyMult) {
            droppedMaterials.put(pickKernel(random), 1);
        }
        return droppedMaterials;
    }

    public static float battlePartsPerDeploymentPoint() {
        return Math.max(0, ModSettings.intOr(BATTLE_PARTS_FIELD_ID, DEFAULT_BATTLE_PARTS_PER_100_DP)) / DEPLOYMENT_POINTS_PER_SETTING_STEP;
    }

    public static int wholeParts(float exactParts, Random random) {
        if (!(exactParts > 0f)) {
            return 0;
        }
        int wholePartCount = (int) exactParts;
        return wholePartCount + (random.nextFloat() < exactParts - wholePartCount ? 1 : 0);
    }

    static String pickKernel(Random random) {
        List<KernelDrop> kernelDrops = new ArrayList<>();
        for (ItemCurrency currency : ItemCurrencies.all()) {
            if (currency.dropsFromShipLoot()) {
                kernelDrops.add(new KernelDrop(currency.itemId(), currency.dropWeight()));
            }
        }
        for (CraftingWard ward : CraftingWard.values()) {
            kernelDrops.add(new KernelDrop(ward.itemId(), ward.dropWeight()));
        }
        return WeightedPick.pick(kernelDrops, KernelDrop::weight, random).itemId();
    }

    private record KernelDrop(String itemId, float weight) {
    }

    public static ItemDefinition pickBasic(Random random) {
        return pick(random, ItemDrops::canBeSynthesised);
    }

    public static boolean canBeSynthesised(ItemDefinition definition) {
        return definition.type() == ItemType.SUBROUTINE && !definition.unique() && isDroppable(definition);
    }

    public record SectionItemLoot(List<ItemData> items, List<RefitSchematic> upgrades) {

        public static final SectionItemLoot NONE = new SectionItemLoot(List.of(), List.of());

        public boolean isEmpty() {
            return items.isEmpty() && upgrades.isEmpty();
        }
    }

    public record SiteLoot(List<ItemData> items, List<RefitSchematic> upgrades, Map<String, Integer> materials) {

        public static final SiteLoot NONE = new SiteLoot(List.of(), List.of(), Map.of());

        public SiteLoot {
            items = List.copyOf(items);
            upgrades = List.copyOf(upgrades);
            materials = Collections.unmodifiableMap(new LinkedHashMap<>(materials));
        }

        public boolean isEmpty() {
            return items.isEmpty() && upgrades.isEmpty() && materials.isEmpty();
        }

        public SiteLoot plus(List<ItemData> moreItems, List<RefitSchematic> moreUpgrades, Map<String, Integer> moreMaterials) {
            if (moreItems.isEmpty() && moreUpgrades.isEmpty() && moreMaterials.isEmpty()) {
                return this;
            }
            List<ItemData> allItems = new ArrayList<>(items);
            allItems.addAll(moreItems);
            List<RefitSchematic> allUpgrades = new ArrayList<>(upgrades);
            allUpgrades.addAll(moreUpgrades);
            Map<String, Integer> allMaterials = new LinkedHashMap<>(materials);
            moreMaterials.forEach((itemId, quantity) -> allMaterials.merge(itemId, quantity, Integer::sum));
            return new SiteLoot(allItems, allUpgrades, allMaterials);
        }

        public SiteLoot plus(SiteLoot other) {
            return plus(other.items(), other.upgrades(), other.materials());
        }
    }

    public static SiteLoot rollSite(String siteId, Random random, ItemChances chances, float feedstockMult, float currencyMult,
                                    boolean sectionsOpen) {
        List<ItemData> droppedItems = new ArrayList<>(roll(siteId, random, chances));
        Map<String, Integer> droppedMaterials = rollMaterials(siteId, random, feedstockMult, currencyMult);
        List<RefitSchematic> droppedUpgrades = new ArrayList<>();
        if (sectionsOpen) {
            SectionItemLoot sectionItemLoot = rollSectionItemLoot(siteId, random, chances);
            droppedItems.addAll(sectionItemLoot.items());
            droppedUpgrades.addAll(sectionItemLoot.upgrades());
        }
        return new SiteLoot(droppedItems, droppedUpgrades, droppedMaterials);
    }

    public static SiteLoot rollFleetSalvage(String siteId, Random random, ItemChances chances, float feedstockMult, float currencyMult) {
        List<ItemData> droppedItems = rollItems(siteId, random, chances, definition -> false, false);
        return new SiteLoot(droppedItems, List.of(), rollMaterials(siteId, random, feedstockMult, currencyMult));
    }

    public static SectionItemLoot rollSectionItemLoot(String siteId, Random random, float chanceMult) {
        return rollSectionItemLoot(siteId, random, ItemChances.of(chanceMult));
    }

    public static SectionItemLoot rollSectionItemLoot(String siteId, Random random, ItemChances chances) {
        Rule rule = activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return SectionItemLoot.NONE;
        }
        List<ItemData> droppedItems = rollCascade(rule, random, chances.general(), chances.rarity(), ItemDrops::pickSectionItemBasic);
        List<RefitSchematic> droppedUpgrades = new ArrayList<>();
        if (random.nextFloat() < rule.chances().get(0) * chances.general() && RefitSchematic.dropsAllowed()) {
            droppedUpgrades.add(RefitSchematic.roll(random));
        }
        List<ItemKind> kindsWithBasics = sectionKindsWithBasics();
        for (ItemKind kind : kindsWithBasics) {
            rollTargeted(rule, random, chances.targeted(kind), chances.rarity(), kindsWithBasics.size(), kind, droppedItems);
        }
        return new SectionItemLoot(List.copyOf(droppedItems), List.copyOf(droppedUpgrades));
    }

    private static void rollTargeted(Rule rule, Random random, float extraShare, float rarity, int sharedKinds, ItemKind kind,
                                     List<ItemData> droppedItems) {
        if (!(extraShare > 0f) || !hasDroppableBasic(kind)) {
            return;
        }
        int extraCount = wholeParts(expectedCascadeCount(rule.chances()) * extraShare / sharedKinds, random);
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

    private static boolean hasDroppableBasic(ItemKind kind, Predicate<ItemDefinition> filter) {
        return ItemDefinitions.all().stream().anyMatch(definition -> isDroppableBasic(definition, kind) && filter.test(definition));
    }

    static List<ItemKind> sectionKindsWithBasics() {
        return sectionKindsWithBasics(definition -> true);
    }

    private static List<ItemKind> sectionKindsWithBasics(Predicate<ItemDefinition> filter) {
        List<ItemKind> kindsWithBasics = new ArrayList<>();
        for (ItemKind kind : ItemKind.sectionItemTypes()) {
            if (hasDroppableBasic(kind, filter)) {
                kindsWithBasics.add(kind);
            }
        }
        return kindsWithBasics;
    }

    private static List<ItemData> rollCascade(Rule rule, Random random, float chanceMult, float rarity,
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

    public static ItemDefinition pickSectionItemBasic(Random random) {
        return pickSectionItemBasic(random, definition -> true);
    }

    public static ItemDefinition pickSectionItemBasic(Random random, Predicate<ItemDefinition> filter) {
        List<ItemKind> typesWithBasics = sectionKindsWithBasics(filter);
        return typesWithBasics.isEmpty() ? null
                : pickSectionItemBasic(typesWithBasics.get(random.nextInt(typesWithBasics.size())), random, filter);
    }

    public static ItemDefinition pickSectionItemBasic(ItemKind socketType, Random random) {
        return pickSectionItemBasic(socketType, random, definition -> true);
    }

    public static ItemDefinition pickSectionItemBasic(ItemKind socketType, Random random, Predicate<ItemDefinition> filter) {
        return pick(random, definition -> definition.kind() == socketType && !definition.unique() && filter.test(definition));
    }

    private static boolean isDroppableBasic(ItemDefinition definition, ItemKind socketType) {
        return definition.kind() == socketType && !definition.unique() && isDroppable(definition);
    }

    private static boolean isDroppable(ItemDefinition definition) {
        return ItemTypes.inGenericPools(definition) && definition.rarity() > 0f
                && SAFE_ID.matcher(definition.id()).matches();
    }

    public static ItemDefinition pickUnique(Random random, Predicate<ItemDefinition> allowedUniques) {
        return pick(random, definition -> definition.unique() && definition.uniqueSources().isEmpty() && allowedUniques.test(definition));
    }

    public static ItemDefinition pickUniqueFrom(UniqueSource.Kind sourceKind, String sourceId, Random random,
                                                Predicate<ItemDefinition> allowedUniques) {
        return pick(random, definition -> definition.unique() && UniqueSource.allows(definition.uniqueSources(), sourceKind, sourceId)
                && allowedUniques.test(definition));
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
        List<ItemDefinition> candidates = new ArrayList<>();
        for (ItemDefinition definition : ItemDefinitions.all()) {
            if (filter.test(definition) && isDroppable(definition)) {
                candidates.add(definition);
            }
        }
        return WeightedPick.pick(candidates, ItemDefinition::rarity, random);
    }

    public static void clear() {
        RULES.set(Map.of());
    }
}
