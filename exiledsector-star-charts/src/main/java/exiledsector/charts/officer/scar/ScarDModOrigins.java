package exiledsector.charts.officer.scar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ScarDModOrigins {

    private ScarDModOrigins() {
    }

    public static Map<String, List<String>> byScar(Map<String, Integer> scars, List<String> dModIds, Map<String, String> origins) {
        return byScar(scars, dModIds, origins, ScarTable::row);
    }

    static Map<String, List<String>> byScar(Map<String, Integer> scars, List<String> dModIds, Map<String, String> origins,
                                            Function<String, ScarRow> rowOf) {
        Map<String, List<String>> byScar = new LinkedHashMap<>();
        scars.keySet().forEach(scarId -> byScar.put(scarId, new ArrayList<>()));
        List<String> unattributed = new ArrayList<>();
        for (String dModId : dModIds) {
            List<String> owned = byScar.get(origins.get(dModId));
            if (owned != null) {
                owned.add(dModId);
            } else {
                unattributed.add(dModId);
            }
        }
        unattributed.removeIf(dModId -> attachToNamingScar(dModId, byScar, scars, rowOf));
        List<String> openScars = byScar.keySet().stream().filter(scarId -> freeStacks(scarId, byScar, scars) > 0).toList();
        if (openScars.size() == 1) {
            List<String> owned = byScar.get(openScars.get(0));
            unattributed.stream().limit(freeStacks(openScars.get(0), byScar, scars)).toList().forEach(owned::add);
        }
        return byScar;
    }

    private static boolean attachToNamingScar(String dModId, Map<String, List<String>> byScar, Map<String, Integer> scars,
                                              Function<String, ScarRow> rowOf) {
        for (Map.Entry<String, List<String>> scar : byScar.entrySet()) {
            ScarRow row = rowOf.apply(scar.getKey());
            if (row != null && row.dModIds().contains(dModId) && freeStacks(scar.getKey(), byScar, scars) > 0) {
                scar.getValue().add(dModId);
                return true;
            }
        }
        return false;
    }

    private static int freeStacks(String scarId, Map<String, List<String>> byScar, Map<String, Integer> scars) {
        Integer stacks = scars.get(scarId);
        return (stacks == null ? 0 : stacks) - byScar.get(scarId).size();
    }
}
