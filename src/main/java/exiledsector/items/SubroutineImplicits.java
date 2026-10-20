package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

public final class SubroutineImplicits {

    static final String DATA_PATH = "data/config/exiledSector/subroutine_corruption_implicits.csv";
    static final String IMPLICIT_COLUMN = CorruptionImplicitPool.IMPLICIT_COLUMN;
    private static final CorruptionImplicitPool CORRUPTION_POOL = new CorruptionImplicitPool(DATA_PATH, ItemType.SUBROUTINE,
            Logger.getLogger(SubroutineImplicits.class));

    private SubroutineImplicits() {
    }

    public static void load() {
        CORRUPTION_POOL.load();
    }

    public static void register(JSONArray rows) throws JSONException {
        CORRUPTION_POOL.register(rows);
    }

    public static List<Modifier> corruptionPool() {
        return CORRUPTION_POOL.implicits();
    }

    static Modifier byId(String modifierId) {
        return CORRUPTION_POOL.byId(modifierId);
    }

    static ModifierLine line(String effectName) {
        return CORRUPTION_POOL.line(effectName);
    }

    public static void clear() {
        CORRUPTION_POOL.clear();
    }
}
