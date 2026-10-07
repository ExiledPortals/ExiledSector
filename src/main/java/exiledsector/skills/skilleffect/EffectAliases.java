package exiledsector.skills.skilleffect;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class EffectAliases {

    public static final String DATA_PATH = "data/config/exiledSector/effect_aliases.csv";
    private static final String ALIAS_COLUMN = "alias";
    private static final String EFFECT_COLUMN = "effect";
    private static final Logger LOG = Logger.getLogger(EffectAliases.class);
    private static final AtomicReference<Map<String, String>> CURRENT_NAMES_BY_ALIAS = new AtomicReference<>(Map.of());

    private EffectAliases() {
    }

    public static void load() {
        ModCsv.load(ALIAS_COLUMN, DATA_PATH, LOG, EffectAliases::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new HashMap<>();
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String alias = ModCsv.text(row, ALIAS_COLUMN);
                String currentName = ModCsv.text(row, EFFECT_COLUMN);
                if (alias.isEmpty() || currentName.isEmpty()) {
                    return;
                }
                if (!SkillEffectRegistry.contains(currentName)) {
                    LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + currentName + " is not a skill effect");
                } else if (SkillEffectRegistry.contains(alias)) {
                    LOG.warn("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + alias + " is still a skill effect of its own");
                } else {
                    loaded.put(alias, currentName);
                }
            });
        }
        CURRENT_NAMES_BY_ALIAS.set(Map.copyOf(loaded));
    }

    public static void clear() {
        CURRENT_NAMES_BY_ALIAS.set(Map.of());
    }

    static String currentName(String alias) {
        return CURRENT_NAMES_BY_ALIAS.get().get(alias);
    }
}
