package exiledsector.skills.npc;

import exiledlib.ModCsv;
import exiledsector.skills.tags.SkillTags;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class NpcFocus {

    static final String FACTION_PATH = "data/config/exiledSector/npc_faction_focus.csv";
    static final String SHIP_SYSTEM_PATH = "data/config/exiledSector/npc_ship_system_focus.csv";
    private static final String FACTION_COLUMN = "faction";
    private static final String SHIP_SYSTEM_COLUMN = "system";
    private static final String FOCUS_COLUMN = "focus";
    private static final Logger LOG = Logger.getLogger(NpcFocus.class);
    private static final AtomicReference<Map<String, Map<String, Float>>> BY_FACTION = new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<String, Map<String, Float>>> BY_SHIP_SYSTEM = new AtomicReference<>(Map.of());

    private NpcFocus() {
    }

    public static void load() {
        ModCsv.load(FACTION_COLUMN, FACTION_PATH, LOG, NpcFocus::registerFactions);
        ModCsv.load(SHIP_SYSTEM_COLUMN, SHIP_SYSTEM_PATH, LOG, NpcFocus::registerShipSystems);
    }

    public static void registerFactions(JSONArray rows) throws JSONException {
        BY_FACTION.set(parse(rows, FACTION_COLUMN, FACTION_PATH));
    }

    public static void registerShipSystems(JSONArray rows) throws JSONException {
        BY_SHIP_SYSTEM.set(parse(rows, SHIP_SYSTEM_COLUMN, SHIP_SYSTEM_PATH));
    }

    public static void clear() {
        BY_FACTION.set(Map.of());
        BY_SHIP_SYSTEM.set(Map.of());
    }

    public static Map<String, Float> of(String factionId, String... shipSystemIds) {
        Map<String, Float> focus = new HashMap<>(lookup(BY_FACTION.get(), factionId));
        for (String shipSystemId : shipSystemIds) {
            lookup(BY_SHIP_SYSTEM.get(), shipSystemId).forEach((tag, weight) -> focus.merge(tag, weight, (first, second) -> first * second));
        }
        return Map.copyOf(focus);
    }

    private static Map<String, Float> lookup(Map<String, Map<String, Float>> focusById, String id) {
        return id == null ? Map.of() : focusById.getOrDefault(id, Map.of());
    }

    private static Map<String, Map<String, Float>> parse(JSONArray rows, String idColumn, String path) throws JSONException {
        Map<String, Map<String, Float>> focusById = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String id = ModCsv.text(row, idColumn);
            if (!id.isEmpty()) {
                focusById.put(id, Map.copyOf(parseFocus(id, ModCsv.text(row, FOCUS_COLUMN), path)));
            }
        });
        return Map.copyOf(focusById);
    }

    static Map<String, Float> parseFocus(String id, String focusText, String path) {
        Map<String, Float> weightsByTag = new LinkedHashMap<>();
        for (String entry : focusText.split(";")) {
            if (entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split(":");
            String tag = parts[0].trim();
            Float weight = parts.length == 2 ? weight(parts[1].trim()) : null;
            if (!SkillTags.THEME.contains(tag) || weight == null || weight <= 0f) {
                LOG.warn("Ignoring focus '" + entry.trim() + "' for " + id + " in " + path
                        + ": expected theme:weight with a theme tag from " + SkillTags.THEME + " and a positive weight");
                continue;
            }
            weightsByTag.put(tag, weight);
        }
        return weightsByTag;
    }

    private static Float weight(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
