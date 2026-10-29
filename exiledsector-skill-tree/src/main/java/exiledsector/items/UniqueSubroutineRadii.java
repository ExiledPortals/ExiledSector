package exiledsector.items;

import exiledsector.ModCsv;
import exiledsector.skills.radius.RadiusSize;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class UniqueSubroutineRadii {

    static final String ID_COLUMN = "id";
    static final String RADIUS_COLUMN = "radius";
    private static final Logger LOG = Logger.getLogger(UniqueSubroutineRadii.class);
    private static final AtomicReference<Map<String, RadiusSize>> SIZES_BY_ID = new AtomicReference<>(Map.of());

    private UniqueSubroutineRadii() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, SubroutineDefinitions.dataPath(), LOG, UniqueSubroutineRadii::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, RadiusSize> sizes = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            ItemDefinition definition = ItemDefinitions.get(ModCsv.text(row, ID_COLUMN));
            String radiusText = ModCsv.text(row, RADIUS_COLUMN);
            if (definition == null || SubroutineKind.of(definition) != SubroutineKind.UNIQUE || radiusText.isEmpty()) {
                return;
            }
            RadiusSize size = RadiusSize.byDataKeyOrNull(radiusText);
            if (size == null) {
                LOG.error(radiusProblem(definition.id(), radiusText) + ", so it has no radius");
                return;
            }
            sizes.put(definition.id(), size);
        });
        SIZES_BY_ID.set(Collections.unmodifiableMap(sizes));
    }

    static String radiusProblem(String definitionId, String radiusText) {
        return definitionId + " has radius \"" + radiusText + "\"; expected one of " + RadiusSize.dataKeys() + ", or blank for none";
    }

    public static RadiusSize sizeOf(String definitionId) {
        return definitionId == null ? null : SIZES_BY_ID.get().get(ItemIdAliases.currentId(definitionId));
    }

    public static void clear() {
        SIZES_BY_ID.set(Map.of());
    }
}
