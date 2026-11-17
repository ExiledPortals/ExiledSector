package exiledsector.items;

import com.fs.starfarer.api.Global;
import exiledsector.ModCsv;
import exiledsector.compat.SalvageSiteCompat;
import exiledsector.data.CsvRows;
import exiledsector.data.RandomRounding;
import exiledsector.loot.DropType;
import exiledsector.loot.Loot;
import exiledsector.loot.LootMultipliers;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

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

    record Rule(List<Float> chances, float uniqueChance, int feedstockMin, int feedstockMax, boolean otherMod) {
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
        ItemDefinition uniqueDefinition = UniqueDropRoll.rollScaledByRarity(random, rule.uniqueChance(), chances,
                picker -> DroppableDefinitions.pickUniqueAtSite(siteId, picker, uniqueAllowed));
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
