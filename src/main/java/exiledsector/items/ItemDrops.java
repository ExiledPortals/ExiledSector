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

    public static List<ItemData> roll(String siteId, Random random) {
        return roll(siteId, random, 1f);
    }

    public static List<ItemData> roll(String siteId, Random random, float chanceMult) {
        return roll(siteId, random, chanceMult, definition -> ItemUnlock.canDrop(definition, Global.getSector()));
    }

    public static List<ItemData> roll(String siteId, Random random, float chanceMult,
                                                Predicate<ItemDefinition> uniqueAllowed) {
        Rule rule = activeRule(siteId);
        if (rule == null) {
            return new ArrayList<>();
        }
        List<ItemData> droppedItems = rollCascade(rule, random, chanceMult, ItemDrops::pickBasic);
        if (random.nextFloat() < rule.uniqueChance() * chanceMult) {
            ItemDefinition uniqueDefinition = pickUnique(random, uniqueAllowed);
            if (uniqueDefinition != null) {
                droppedItems.add(ItemData.rolled(uniqueDefinition, random.nextLong()));
            }
        }
        return droppedItems;
    }

    public static Map<String, Integer> rollMaterials(String siteId, Random random) {
        return rollMaterials(siteId, random, 1f);
    }

    public static Map<String, Integer> rollMaterials(String siteId, Random random, float chanceMult) {
        Map<String, Integer> droppedMaterials = new LinkedHashMap<>();
        Rule rule = activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return droppedMaterials;
        }
        int partsFound = 0;
        for (float chance : rule.chances()) {
            if (random.nextFloat() < chance * chanceMult) {
                partsFound += MIN_PARTS_PER_FIND + random.nextInt(MAX_PARTS_PER_FIND - MIN_PARTS_PER_FIND + 1);
            }
        }
        if (partsFound > 0) {
            droppedMaterials.put(ItemDisassembly.PARTS_ITEM_ID, partsFound);
        }
        if (random.nextFloat() < rule.chances().get(0) * KERNEL_CHANCE_SHARE * chanceMult) {
            droppedMaterials.put(pickKernel(random).itemId(), 1);
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

    static CraftingCurrency pickKernel(Random random) {
        return WeightedPick.pick(List.of(CraftingCurrency.values()), CraftingCurrency::dropWeight, random);
    }

    public static ItemDefinition pickBasic(Random random) {
        return pick(random, ItemDrops::canBeSynthesised);
    }

    public static boolean canBeSynthesised(ItemDefinition definition) {
        return definition.type() == ItemType.SUBROUTINE && !definition.unique() && isDroppable(definition);
    }

    public record SectionItemLoot(List<ItemData> items, List<HullUpgradeData> upgrades) {

        public static final SectionItemLoot NONE = new SectionItemLoot(List.of(), List.of());

        public boolean isEmpty() {
            return items.isEmpty() && upgrades.isEmpty();
        }
    }

    public static SectionItemLoot rollSectionItemLoot(String siteId, Random random, float chanceMult) {
        Rule rule = activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return SectionItemLoot.NONE;
        }
        List<ItemData> droppedItems = rollCascade(rule, random, chanceMult, ItemDrops::pickSectionItemBasic);
        List<HullUpgradeData> droppedUpgrades = new ArrayList<>();
        if (random.nextFloat() < rule.chances().get(0) * chanceMult) {
            droppedUpgrades.add(new HullUpgradeData(HullUpgradeRoller.rollHullSize(random)));
        }
        return new SectionItemLoot(List.copyOf(droppedItems), List.copyOf(droppedUpgrades));
    }

    private static List<ItemData> rollCascade(Rule rule, Random random, float chanceMult,
                                                        Function<Random, ItemDefinition> picker) {
        List<ItemData> droppedItems = new ArrayList<>();
        for (float chance : rule.chances()) {
            if (random.nextFloat() >= chance * chanceMult) {
                break;
            }
            ItemDefinition basicDefinition = picker.apply(random);
            if (basicDefinition != null) {
                droppedItems.add(ItemData.rolled(basicDefinition, random.nextLong()));
            }
        }
        return droppedItems;
    }

    public static ItemDefinition pickSectionItemBasic(Random random) {
        List<ItemKind> typesWithBasics = new ArrayList<>();
        for (ItemKind socketType : ItemKind.sectionItemTypes()) {
            if (ItemDefinitions.all().stream().anyMatch(definition -> isDroppableBasic(definition, socketType))) {
                typesWithBasics.add(socketType);
            }
        }
        return typesWithBasics.isEmpty() ? null : pickSectionItemBasic(typesWithBasics.get(random.nextInt(typesWithBasics.size())), random);
    }

    public static ItemDefinition pickSectionItemBasic(ItemKind socketType, Random random) {
        return pick(random, definition -> definition.kind() == socketType && !definition.unique());
    }

    private static boolean isDroppableBasic(ItemDefinition definition, ItemKind socketType) {
        return definition.kind() == socketType && !definition.unique() && isDroppable(definition);
    }

    private static boolean isDroppable(ItemDefinition definition) {
        return definition.rarity() > 0f && SAFE_ID.matcher(definition.id()).matches();
    }

    public static ItemDefinition pickUnique(Random random, Predicate<ItemDefinition> allowedUniques) {
        return pick(random, definition -> definition.unique() && allowedUniques.test(definition));
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
