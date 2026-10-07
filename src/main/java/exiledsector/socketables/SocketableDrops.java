package exiledsector.socketables;

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
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class SocketableDrops {

    static final String DATA_PATH = "data/config/exiledSector/socketable_salvage.csv";
    public static final String TECH_MINING_FIRST_FIND = "techmining_first_find";
    public static final String TECH_MINING_MONTHLY = "techmining_monthly";
    static final int MIN_PARTS_PER_FIND = 1;
    static final int MAX_PARTS_PER_FIND = 3;
    static final float KERNEL_CHANCE_SHARE = 0.35f;
    public static final String BATTLE_PARTS_FIELD_ID = "exiledSector_socketPartsPerDeploymentPoint";
    public static final float DEFAULT_BATTLE_PARTS_PER_DP = 0.05f;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Logger LOG = Logger.getLogger(SocketableDrops.class);
    private static final AtomicReference<Map<String, Rule>> RULES = new AtomicReference<>(Map.of());

    record Rule(List<Float> chances, float uniqueChance, boolean otherMod) {
    }

    private SocketableDrops() {
    }

    public static void load() {
        ModCsv.load("site", DATA_PATH, LOG, SocketableDrops::register);
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
        Rule rule = siteId == null ? null : RULES.get().get(siteId);
        return rule != null && (!rule.otherMod() || SalvageSiteCompat.dropsEnabled());
    }

    public static List<SocketableItemData> roll(String siteId, Random random) {
        return roll(siteId, random, 1f);
    }

    public static List<SocketableItemData> roll(String siteId, Random random, float chanceMult) {
        return roll(siteId, random, chanceMult, definition -> SocketableUnlock.canDrop(definition, Global.getSector()));
    }

    public static List<SocketableItemData> roll(String siteId, Random random, float chanceMult,
                                                Predicate<SocketableDefinition> uniqueAllowed) {
        List<SocketableItemData> droppedItems = new ArrayList<>();
        Rule rule = siteId == null ? null : RULES.get().get(siteId);
        if (rule == null || rule.otherMod() && !SalvageSiteCompat.dropsEnabled()) {
            return droppedItems;
        }
        for (float chance : rule.chances()) {
            if (random.nextFloat() >= chance * chanceMult) {
                break;
            }
            SocketableDefinition basicDefinition = pickBasic(random);
            if (basicDefinition != null) {
                droppedItems.add(SocketableItemData.rolled(basicDefinition, random.nextLong()));
            }
        }
        if (random.nextFloat() < rule.uniqueChance() * chanceMult) {
            SocketableDefinition uniqueDefinition = pickUnique(random, uniqueAllowed);
            if (uniqueDefinition != null) {
                droppedItems.add(SocketableItemData.rolled(uniqueDefinition, random.nextLong()));
            }
        }
        return droppedItems;
    }

    public static Map<String, Integer> rollMaterials(String siteId, Random random) {
        return rollMaterials(siteId, random, 1f);
    }

    public static Map<String, Integer> rollMaterials(String siteId, Random random, float chanceMult) {
        Map<String, Integer> droppedMaterials = new LinkedHashMap<>();
        Rule rule = siteId == null ? null : RULES.get().get(siteId);
        if (rule == null || rule.chances().isEmpty() || rule.otherMod() && !SalvageSiteCompat.dropsEnabled()) {
            return droppedMaterials;
        }
        int partsFound = 0;
        for (float chance : rule.chances()) {
            if (random.nextFloat() < chance * chanceMult) {
                partsFound += MIN_PARTS_PER_FIND + random.nextInt(MAX_PARTS_PER_FIND - MIN_PARTS_PER_FIND + 1);
            }
        }
        if (partsFound > 0) {
            droppedMaterials.put(SocketableDisassembly.PARTS_COMMODITY_ID, partsFound);
        }
        if (random.nextFloat() < rule.chances().get(0) * KERNEL_CHANCE_SHARE * chanceMult) {
            droppedMaterials.put(pickKernel(random).commodityId(), 1);
        }
        return droppedMaterials;
    }

    public static float battlePartsPerDeploymentPoint() {
        return Math.max(0f, ModSettings.floatOr(BATTLE_PARTS_FIELD_ID, DEFAULT_BATTLE_PARTS_PER_DP));
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

    public static SocketableDefinition pickBasic(Random random) {
        return pick(random, definition -> definition.kind() == SocketType.SUBROUTINE && !definition.unique());
    }


    public static SocketableDefinition pickUnique(Random random, Predicate<SocketableDefinition> allowedUniques) {
        return pick(random, definition -> definition.unique() && allowedUniques.test(definition));
    }

    private static SocketableDefinition pick(Random random, Predicate<SocketableDefinition> filter) {
        List<SocketableDefinition> candidates = new ArrayList<>();
        for (SocketableDefinition definition : SocketableDefinitions.all()) {
            if (filter.test(definition) && definition.rarity() > 0f && SAFE_ID.matcher(definition.id()).matches()) {
                candidates.add(definition);
            }
        }
        return WeightedPick.pick(candidates, SocketableDefinition::rarity, random);
    }

    public static void clear() {
        RULES.set(Map.of());
    }
}
