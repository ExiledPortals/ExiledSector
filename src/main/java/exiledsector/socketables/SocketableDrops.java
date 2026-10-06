package exiledsector.socketables;

import com.fs.starfarer.api.Global;
import exiledsector.ModCsv;
import exiledsector.compat.SalvageSiteCompat;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
        Map<String, Rule> merged = new HashMap<>(parse(rows, true));
        merged.putAll(RULES.get());
        RULES.set(Map.copyOf(merged));
    }

    private static Map<String, Rule> parse(JSONArray rows, boolean otherMod) throws JSONException {
        Map<String, Rule> loaded = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String site = ModCsv.text(row, "site");
            if (site.isEmpty()) {
                return;
            }
            try {
                List<Float> chances = new ArrayList<>();
                for (String chance : row.optString("chances", "").split(";")) {
                    if (!chance.isBlank()) {
                        chances.add(Float.parseFloat(chance.trim()));
                    }
                }
                String unique = ModCsv.text(row, "uniqueChance");
                loaded.put(site, new Rule(List.copyOf(chances), unique.isEmpty() ? 0f : Float.parseFloat(unique), otherMod));
            } catch (NumberFormatException e) {
                LOG.error("Skipping the salvage drop row for site " + site + ": " + e.getMessage());
            }
        });
        return loaded;
    }

    public static boolean hasRule(String site) {
        Rule rule = site == null ? null : RULES.get().get(site);
        return rule != null && (!rule.otherMod() || SalvageSiteCompat.dropsEnabled());
    }

    public static List<SocketableItemData> roll(String site, Random random) {
        return roll(site, random, 1f);
    }

    public static List<SocketableItemData> roll(String site, Random random, float chanceMult) {
        return roll(site, random, chanceMult, definition -> SocketableUnlock.canDrop(definition, Global.getSector()));
    }

    public static List<SocketableItemData> roll(String site, Random random, float chanceMult,
                                                Predicate<SocketableDefinition> uniqueAllowed) {
        List<SocketableItemData> items = new ArrayList<>();
        Rule rule = site == null ? null : RULES.get().get(site);
        if (rule == null || rule.otherMod() && !SalvageSiteCompat.dropsEnabled()) {
            return items;
        }
        for (float chance : rule.chances()) {
            if (random.nextFloat() >= chance * chanceMult) {
                break;
            }
            SocketableDefinition basic = pickBasic(random);
            if (basic != null) {
                items.add(SocketableItemData.rolled(basic, random.nextLong()));
            }
        }
        if (random.nextFloat() < rule.uniqueChance() * chanceMult) {
            SocketableDefinition unique = pickUnique(random, uniqueAllowed);
            if (unique != null) {
                items.add(SocketableItemData.rolled(unique, random.nextLong()));
            }
        }
        return items;
    }

    public static SocketableDefinition pickBasic(Random random) {
        return pick(random, definition -> definition.kind() == SocketableKind.SUBROUTINE && !definition.unique());
    }

    public static SocketableDefinition pickUnique(Random random, Predicate<SocketableDefinition> allowed) {
        return pick(random, definition -> definition.unique() && allowed.test(definition));
    }

    private static SocketableDefinition pick(Random random, Predicate<SocketableDefinition> filter) {
        List<SocketableDefinition> candidates = new ArrayList<>();
        float total = 0f;
        for (SocketableDefinition definition : SocketableDefinitions.all()) {
            if (filter.test(definition) && definition.rarity() > 0f && SAFE_ID.matcher(definition.id()).matches()) {
                candidates.add(definition);
                total += definition.rarity();
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        float roll = random.nextFloat() * total;
        for (SocketableDefinition definition : candidates) {
            roll -= definition.rarity();
            if (roll < 0f) {
                return definition;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    public static void clear() {
        RULES.set(Map.of());
    }
}
