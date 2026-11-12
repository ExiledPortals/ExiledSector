package exiledsector.charts.officer.scar;

import java.util.Map;
import java.util.function.Function;

public final class ScarLevels {

    static final int LEVELS_PER_SCAR = 1;

    private ScarLevels() {
    }

    public static int extraLevels(Map<String, Integer> scars) {
        return extraLevels(scars, ScarTable::row);
    }

    static int extraLevels(Map<String, Integer> scars, Function<String, ScarRow> rowOf) {
        if (scars == null) {
            return 0;
        }
        int levels = 0;
        for (Map.Entry<String, Integer> scar : scars.entrySet()) {
            int stacks = scar.getValue() == null ? 0 : Math.max(0, scar.getValue());
            ScarRow row = rowOf.apply(scar.getKey());
            levels += stacks * (LEVELS_PER_SCAR + (row == null ? 0 : row.extraLevels()));
        }
        return levels;
    }
}
