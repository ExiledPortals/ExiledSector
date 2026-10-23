package exiledsector.ui.decoration;

import exiledlib.ModCsv;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.tags.SkillTags;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public final class TreeFleetFactions {

    static final String DATA_PATH = "data/config/exiledSector/tree_fleet_factions.csv";
    private static final String FACTION_COLUMN = "faction";
    private static final String REGION_COLUMN = "region";
    private static final Logger LOG = Logger.getLogger(TreeFleetFactions.class);
    private static final AtomicReference<Map<String, String>> REGION_BY_FACTION = new AtomicReference<>(Map.of());

    private TreeFleetFactions() {
    }

    public static void load() {
        ModCsv.load(FACTION_COLUMN, DATA_PATH, LOG, TreeFleetFactions::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String faction = ModCsv.text(row, FACTION_COLUMN);
            String region = ModCsv.text(row, REGION_COLUMN);
            if (faction.isEmpty()) {
                return;
            }
            if (!SkillTags.isRegion(region) || SkillTags.CORE_REGION.equals(region)) {
                LOG.warn("Ignoring faction " + faction + " in " + DATA_PATH + ": '" + region + "' is not a faction volume");
                return;
            }
            loaded.put(faction, region);
        });
        REGION_BY_FACTION.set(Map.copyOf(loaded));
    }

    static List<String> regions() {
        return SkillTags.REGION.stream()
                .filter(region -> SkillTags.FACTION_VOLUMES.contains(region) || REGION_BY_FACTION.get().containsValue(region)).toList();
    }

    static List<String> factionsIn(String region) {
        Stream<String> fleetOnly = REGION_BY_FACTION.get().entrySet().stream().filter(entry -> entry.getValue().equals(region))
                .map(Map.Entry::getKey);
        return Stream.concat(NpcFactionVolumes.factionsIn(region).stream(), fleetOnly).distinct().sorted().toList();
    }

    public static void clear() {
        REGION_BY_FACTION.set(Map.of());
    }
}
