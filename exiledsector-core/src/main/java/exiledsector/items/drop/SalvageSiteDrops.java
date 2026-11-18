package exiledsector.items.drop;

import com.fs.starfarer.api.Global;
import exiledsector.ModCsv;
import exiledsector.compat.SalvageSiteCompat;
import exiledsector.data.CsvRows;
import exiledsector.data.RandomRounding;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemTypes;
import exiledsector.items.ItemUnlock;
import exiledsector.items.crafting.ItemDisassembly;
import exiledsector.items.modifier.ItemRoll;
import exiledsector.loot.DropType;
import exiledsector.loot.Loot;
import exiledsector.loot.LootMultipliers;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

public final class SalvageSiteDrops {

    static final String DATA_PATH = "data/config/exiledSector/item_salvage.csv";
    public static final String TECH_MINING_FIRST_FIND = "techmining_first_find";
    public static final String TECH_MINING_MONTHLY = "techmining_monthly";
    static final int MIN_FEEDSTOCK_PER_FIND = 1;
    static final int MAX_FEEDSTOCK_PER_FIND = 3;
    static final float CRAFTING_ITEM_SALVAGE_SHARE = 0.7f;
    private static final Logger LOG = Logger.getLogger(SalvageSiteDrops.class);
    private static final AtomicReference<Map<String, Rule>> RULES = new AtomicReference<>(Map.of());

    static final String HULL_ITEM_UNIQUE_CHANCE_COLUMN = "hullItemUniqueChance";
    static final String SUBROUTINE_UNIQUE_CHANCE_COLUMN = "subroutineUniqueChance";

    record Rule(List<Float> chances, float hullItemUniqueChance, float subroutineUniqueChance, int feedstockMin, int feedstockMax,
                boolean otherMod) {

        float hullItemUniqueShareOfCascade() {
            float cascadeCount = expectedCascadeCount(chances);
            return cascadeCount > 0f ? hullItemUniqueChance / cascadeCount : 0f;
        }

        float uniqueChance(ItemDefinition unique) {
            return unique.isHullItem() ? hullItemUniqueChance : subroutineUniqueChance;
        }
    }

    private SalvageSiteDrops() {
    }

    public static void load() {
        ModCsv.load("site", DATA_PATH, LOG, SalvageSiteDrops::register);
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
        CsvRows.forEachIdRow(rows, "site", (siteId, row) -> {
            try {
                List<Float> chances = new ArrayList<>();
                for (String chanceText : row.optString("chances", "").split(";")) {
                    if (!chanceText.isBlank()) {
                        chances.add(Float.parseFloat(chanceText.trim()));
                    }
                }
                String feedstockMinText = ModCsv.text(row, "feedstockMin");
                String feedstockMaxText = ModCsv.text(row, "feedstockMax");
                int feedstockMin = feedstockMinText.isEmpty() ? 0 : Math.max(0, Integer.parseInt(feedstockMinText));
                int feedstockMax = feedstockMaxText.isEmpty() ? feedstockMin : Math.max(feedstockMin, Integer.parseInt(feedstockMaxText));
                loaded.put(siteId, new Rule(List.copyOf(chances), chanceIn(row, HULL_ITEM_UNIQUE_CHANCE_COLUMN),
                        chanceIn(row, SUBROUTINE_UNIQUE_CHANCE_COLUMN), feedstockMin, feedstockMax, otherMod));
            } catch (NumberFormatException e) {
                LOG.error("Skipping the salvage drop row for site " + siteId + ": " + e.getMessage());
            }
        });
        return loaded;
    }

    private static float chanceIn(JSONObject row, String column) {
        String chanceText = ModCsv.text(row, column);
        return chanceText.isEmpty() ? 0f : Float.parseFloat(chanceText);
    }

    public static boolean hasRule(String siteId) {
        return activeRule(siteId) != null;
    }

    static Rule activeRule(String siteId) {
        Rule rule = siteId == null ? null : RULES.get().get(siteId);
        return rule == null || rule.otherMod() && !SalvageSiteCompat.dropsEnabled() ? null : rule;
    }

    public static boolean hasUniqueRoll(ItemDefinition unique, String siteId) {
        Rule rule = activeRule(siteId);
        return rule != null && rule.uniqueChance(unique) > 0f;
    }

    public static String uniqueChanceColumn(ItemDefinition unique) {
        return unique.isHullItem() ? HULL_ITEM_UNIQUE_CHANCE_COLUMN : SUBROUTINE_UNIQUE_CHANCE_COLUMN;
    }

    public static Loot rollSite(String siteId, Random random, LootMultipliers multipliers) {
        List<ItemData> droppedItems = roll(siteId, random, multipliers.chances());
        return new Loot(droppedItems, List.of(), rollMaterials(siteId, random, multipliers));
    }

    public static List<ItemData> roll(String siteId, Random random, ItemChances chances) {
        return roll(siteId, random, chances, definition -> ItemUnlock.canDrop(definition, Global.getSector()));
    }

    public static List<ItemData> roll(String siteId, Random random, ItemChances chances, Predicate<ItemDefinition> uniqueAllowed) {
        Rule rule = activeRule(siteId);
        if (rule == null) {
            return new ArrayList<>();
        }
        List<ItemData> droppedItems = new ArrayList<>();
        ItemDefinition uniqueDefinition = UniqueDropRoll.rollScaledByRarity(random, rule.subroutineUniqueChance(), chances,
                picker -> DroppableDefinitions.pickUniqueAtSite(siteId, picker, candidate -> !candidate.isHullItem() && uniqueAllowed.test(candidate)));
        if (uniqueDefinition != null && ItemTypes.keptFromSalvage(uniqueDefinition, random)) {
            droppedItems.add(ItemData.rolled(uniqueDefinition, random.nextLong()));
        }
        if (!rule.chances().isEmpty()) {
            droppedItems.addAll(ItemTypes.rollSalvage(random, rule.chances().get(0) * chances.quantity(DropType.SUBROUTINE)));
        }
        return droppedItems;
    }

    public static Map<String, Integer> rollMaterials(String siteId, Random random, LootMultipliers multipliers) {
        Map<String, Integer> droppedMaterials = new LinkedHashMap<>();
        Rule rule = activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return droppedMaterials;
        }
        float feedstockMult = multipliers.quantity(DropType.FEEDSTOCK);
        int feedstockFound = rule.feedstockMax() > 0 ? rollFeedstockInRange(rule, random, feedstockMult)
                : rollFeedstockPerChance(rule, random, feedstockMult);
        if (feedstockFound > 0) {
            droppedMaterials.put(ItemDisassembly.FEEDSTOCK_ITEM_ID, feedstockFound);
        }
        float baseCraftingItems = rule.chances().get(0) * CRAFTING_ITEM_SALVAGE_SHARE;
        CraftingDrops.rollInto(baseCraftingItems, CraftingDrops.OUTSIDE_CHARTS, multipliers, random, droppedMaterials);
        return droppedMaterials;
    }

    private static int rollFeedstockInRange(Rule rule, Random random, float feedstockMult) {
        int rolled = rule.feedstockMin() + random.nextInt(rule.feedstockMax() - rule.feedstockMin() + 1);
        return RandomRounding.round(rolled * feedstockMult, random);
    }

    private static int rollFeedstockPerChance(Rule rule, Random random, float feedstockMult) {
        int feedstockFound = 0;
        for (float chance : rule.chances()) {
            if (random.nextFloat() < chance * feedstockMult) {
                feedstockFound += MIN_FEEDSTOCK_PER_FIND + random.nextInt(MAX_FEEDSTOCK_PER_FIND - MIN_FEEDSTOCK_PER_FIND + 1);
            }
        }
        return feedstockFound;
    }

    static void rollTargeted(Rule rule, Random random, float extraShare, ItemChances chances, int sharedKinds, ItemKind kind,
                             List<ItemData> droppedItems) {
        if (!(extraShare > 0f) || !DroppableDefinitions.hasDroppableBasic(kind)) {
            return;
        }
        int extraCount = RandomRounding.round(expectedCascadeCount(rule.chances()) * extraShare / sharedKinds, random);
        float rarity = chances.rarity(DropType.forItemKind(kind));
        for (int i = 0; i < extraCount; i++) {
            ItemDefinition definition = DroppableDefinitions.pickBasicOfKind(kind, random);
            if (definition != null) {
                droppedItems.add(ItemData.rolled(definition, random.nextLong(), ItemRoll.dropAtRarity(rarity)));
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

    static List<ItemData> rollCascade(Rule rule, Random random, float chanceMult, ItemChances chances, Function<Random, ItemDefinition> picker) {
        List<ItemData> droppedItems = new ArrayList<>();
        for (float chance : rule.chances()) {
            if (random.nextFloat() >= chance * chanceMult) {
                break;
            }
            ItemDefinition basicDefinition = picker.apply(random);
            if (basicDefinition != null) {
                droppedItems.add(ItemData.rolled(basicDefinition, random.nextLong(), ItemRoll.dropAtRarity(chances.rarity(DropType.forDefinition(basicDefinition)))));
            }
        }
        return droppedItems;
    }

    public static void clear() {
        RULES.set(Map.of());
    }
}
