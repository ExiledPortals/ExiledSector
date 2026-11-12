package exiledsector.charts.officer.scar;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import exiledsector.skills.skilleffect.SkillEffect;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScarTable {

    public static final String FALLBACK_SCAR_ID = "battle_hardened";
    public static final int STACK_CAP = 3;
    static final String PATH = ExiledSector.configPath("atlas_officer_scars.csv");
    static final String RANDOM_DMOD = "random";
    private static final String ID_COLUMN = "id";
    private static final String EFFECT_SEPARATOR = ":";
    private static final Logger LOG = Logger.getLogger(ScarTable.class);
    private static volatile Map<String, ScarRow> rowsById = Map.of();
    private static volatile List<ScarRow> rowsInTableOrder = List.of();

    private ScarTable() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, ScarTable::register);
    }

    public static List<String> register(JSONArray rows) throws JSONException {
        Map<String, ScarRow> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String id = ModCsv.text(row, ID_COLUMN);
                if (!id.isEmpty()) {
                    ScarRow scar = parse(id, row, csv);
                    if (scar != null) {
                        loaded.put(id, scar);
                    }
                }
            });
        }
        if (rows != null && !loaded.containsKey(FALLBACK_SCAR_ID)) {
            csv.skipRow(FALLBACK_SCAR_ID, "missing; it is the scar taken when no other fits");
        }
        csv.report(LOG);
        rowsInTableOrder = List.copyOf(loaded.values());
        rowsById = Collections.unmodifiableMap(loaded);
        return csv.problems();
    }

    private static ScarRow parse(String id, JSONObject row, CsvRows csv) throws JSONException {
        String detectorId = ModCsv.text(row, "detector");
        ScarDetector detector = detectorId.isEmpty() ? null : ScarDetector.byId(detectorId);
        if (!detectorId.isEmpty() && detector == null) {
            csv.skipRow(id, "unknown detector " + detectorId);
            return null;
        }
        List<ScarRow.Effect> effects = new ArrayList<>();
        for (String entry : CsvRows.entries(ModCsv.text(row, "effects"))) {
            ScarRow.Effect effect = effect(entry);
            if (effect == null) {
                csv.problem(id, "effects", entry);
            } else {
                effects.add(effect);
            }
        }
        List<String> dModIds = new ArrayList<>();
        boolean randomDMod = false;
        for (String dModId : CsvRows.entries(ModCsv.text(row, "dmods"))) {
            if (RANDOM_DMOD.equals(dModId)) {
                randomDMod = true;
            } else {
                dModIds.add(dModId);
            }
        }
        return new ScarRow(id, detector, csv.number(row, "threshold", 0f, id), ModCsv.text(row, "theme_tag"), List.copyOf(effects),
                List.copyOf(dModIds), randomDMod, Math.max(0, csv.integer(row, "extra_levels", 0, id)));
    }

    static ScarRow.Effect effect(String entry) {
        String[] fields = entry.split(EFFECT_SEPARATOR);
        if (fields.length != 3) {
            return null;
        }
        HullFeature feature = HullFeature.byId(fields[0].trim());
        SkillEffect effect = knownEffect(fields[1].trim());
        float magnitude = CsvRows.numberOrNaN(fields[2].trim());
        if (feature == null || effect == null || Float.isNaN(magnitude) || Float.isInfinite(magnitude)) {
            return null;
        }
        return new ScarRow.Effect(feature, effect, magnitude);
    }

    private static SkillEffect knownEffect(String effectName) {
        try {
            return SkillEffect.byName(effectName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void clear() {
        rowsInTableOrder = List.of();
        rowsById = Map.of();
    }

    public static List<ScarRow> rows() {
        return rowsInTableOrder;
    }

    public static ScarRow row(String scarId) {
        return scarId == null ? null : rowsById.get(scarId);
    }

    public static boolean exists(String scarId) {
        return row(scarId) != null;
    }
}
