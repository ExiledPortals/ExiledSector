package exiledsector.hullitems;

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

public final class HullItemDrops {

    static final String DATA_PATH = "data/config/exiledSector/socketable_salvage.csv";
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
    private static final Logger LOG = Logger.getLogger(HullItemDrops.class);
    private static final AtomicReference<Map<String, Rule>> RULES = new AtomicReference<>(Map.of());

    record Rule(List<Float> chances, float uniqueChance, boolean otherMod) {
    }

    private HullItemDrops() {
    }

    public static void load() {
        ModCsv.load("site", DATA_PATH, LOG, HullItemDrops::register);
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

    public static List<HullItemData> roll(String siteId, Random random) {
        return roll(siteId, random, 1f);
    }

    public static List<HullItemData> roll(String siteId, Random random, float chanceMult) {
        return roll(siteId, random, chanceMult, definition -> HullItemUnlock.canDrop(definition, Global.getSector()));
    }

    public static List<HullItemData> roll(String siteId, Random random, float chanceMult,
                                                Predicate<HullItemDefinition> uniqueAllowed) {
        Rule rule = activeRule(siteId);
        if (rule == null) {
            return new ArrayList<>();
        }
        List<HullItemData> droppedItems = rollCascade(rule, random, chanceMult, HullItemDrops::pickBasic);
        if (random.nextFloat() < rule.uniqueChance() * chanceMult) {
            HullItemDefinition uniqueDefinition = pickUnique(random, uniqueAllowed);
            if (uniqueDefinition != null) {
                droppedItems.add(HullItemData.rolled(uniqueDefinition, random.nextLong()));
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
            droppedMaterials.put(HullItemDisassembly.PARTS_COMMODITY_ID, partsFound);
        }
        if (random.nextFloat() < rule.chances().get(0) * KERNEL_CHANCE_SHARE * chanceMult) {
            droppedMaterials.put(pickKernel(random).commodityId(), 1);
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

    static SocketCurrency pickKernel(Random random) {
        return WeightedPick.pick(List.of(SocketCurrency.values()), SocketCurrency::dropWeight, random);
    }

    public static HullItemDefinition pickBasic(Random random) {
        return pick(random, HullItemDrops::canBeSynthesised);
    }

    public static boolean canBeSynthesised(HullItemDefinition definition) {
        return definition.kind() == HullItemType.SUBROUTINE && !definition.unique() && isDroppable(definition);
    }

    public record FrameworkLoot(List<HullItemData> items, List<HullUpgradeData> upgrades) {

        public static final FrameworkLoot NONE = new FrameworkLoot(List.of(), List.of());

        public boolean isEmpty() {
            return items.isEmpty() && upgrades.isEmpty();
        }
    }

    public static FrameworkLoot rollFrameworkLoot(String siteId, Random random, float chanceMult) {
        Rule rule = activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return FrameworkLoot.NONE;
        }
        List<HullItemData> droppedItems = rollCascade(rule, random, chanceMult, HullItemDrops::pickFrameworkBasic);
        List<HullUpgradeData> droppedUpgrades = new ArrayList<>();
        if (random.nextFloat() < rule.chances().get(0) * chanceMult) {
            droppedUpgrades.add(new HullUpgradeData(HullUpgradeRoller.rollHullSize(random)));
        }
        return new FrameworkLoot(List.copyOf(droppedItems), List.copyOf(droppedUpgrades));
    }

    private static List<HullItemData> rollCascade(Rule rule, Random random, float chanceMult,
                                                        Function<Random, HullItemDefinition> picker) {
        List<HullItemData> droppedItems = new ArrayList<>();
        for (float chance : rule.chances()) {
            if (random.nextFloat() >= chance * chanceMult) {
                break;
            }
            HullItemDefinition basicDefinition = picker.apply(random);
            if (basicDefinition != null) {
                droppedItems.add(HullItemData.rolled(basicDefinition, random.nextLong()));
            }
        }
        return droppedItems;
    }

    public static HullItemDefinition pickFrameworkBasic(Random random) {
        List<HullItemType> typesWithBasics = new ArrayList<>();
        for (HullItemType socketType : HullItemType.frameworkTypes()) {
            if (HullItemDefinitions.all().stream().anyMatch(definition -> isDroppableBasic(definition, socketType))) {
                typesWithBasics.add(socketType);
            }
        }
        return typesWithBasics.isEmpty() ? null : pickFrameworkBasic(typesWithBasics.get(random.nextInt(typesWithBasics.size())), random);
    }

    public static HullItemDefinition pickFrameworkBasic(HullItemType socketType, Random random) {
        return pick(random, definition -> definition.kind() == socketType && !definition.unique());
    }

    private static boolean isDroppableBasic(HullItemDefinition definition, HullItemType socketType) {
        return definition.kind() == socketType && !definition.unique() && isDroppable(definition);
    }

    private static boolean isDroppable(HullItemDefinition definition) {
        return definition.rarity() > 0f && SAFE_ID.matcher(definition.id()).matches();
    }

    public static HullItemDefinition pickUnique(Random random, Predicate<HullItemDefinition> allowedUniques) {
        return pick(random, definition -> definition.unique() && allowedUniques.test(definition));
    }

    private static HullItemDefinition pick(Random random, Predicate<HullItemDefinition> filter) {
        List<HullItemDefinition> candidates = new ArrayList<>();
        for (HullItemDefinition definition : HullItemDefinitions.all()) {
            if (filter.test(definition) && isDroppable(definition)) {
                candidates.add(definition);
            }
        }
        return WeightedPick.pick(candidates, HullItemDefinition::rarity, random);
    }

    public static void clear() {
        RULES.set(Map.of());
    }
}
