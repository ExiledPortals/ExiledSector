package exiledsector.ui.decoration;

import exiledsector.ModCsv;
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
    private static final String VOLUME_COLUMN = "volume";
    private static final Logger LOG = Logger.getLogger(TreeFleetFactions.class);
    private static final AtomicReference<Map<String, String>> VOLUME_BY_FACTION = new AtomicReference<>(Map.of());

    private TreeFleetFactions() {
    }

    public static void load() {
        ModCsv.load(FACTION_COLUMN, DATA_PATH, LOG, TreeFleetFactions::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String faction = ModCsv.text(row, FACTION_COLUMN);
            String volume = ModCsv.text(row, VOLUME_COLUMN);
            if (faction.isEmpty()) {
                return;
            }
            if (!SkillTags.isVolume(volume) || SkillTags.CORE_VOLUME.equals(volume)) {
                LOG.warn("Ignoring faction " + faction + " in " + DATA_PATH + ": '" + volume + "' is not a faction volume");
                return;
            }
            loaded.put(faction, volume);
        });
        VOLUME_BY_FACTION.set(Map.copyOf(loaded));
    }

    static List<String> volumes() {
        return SkillTags.VOLUME.stream()
                .filter(volume -> SkillTags.FACTION_VOLUMES.contains(volume) || VOLUME_BY_FACTION.get().containsValue(volume)).toList();
    }

    static List<String> factionsIn(String volume) {
        Stream<String> fleetOnly = VOLUME_BY_FACTION.get().entrySet().stream().filter(entry -> entry.getValue().equals(volume))
                .map(Map.Entry::getKey);
        return Stream.concat(NpcFactionVolumes.factionsIn(volume).stream(), fleetOnly).distinct().sorted().toList();
    }

    public static void clear() {
        VOLUME_BY_FACTION.set(Map.of());
    }
}
