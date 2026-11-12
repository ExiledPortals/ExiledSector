package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScarMemory {

    public static final String FLAGSHIP_ID_KEY = "$exiledSector_scarFlagshipId";
    public static final String SCARS_KEY = "$exiledSector_scars";
    private static final String ENTRY_SEPARATOR = ";";
    private static final String STACK_SEPARATOR = ":";

    private static String lastEncoded;
    private static Map<String, Integer> lastDecoded = Map.of();

    private ScarMemory() {
    }

    static void write(MemoryAPI memory, String flagshipMemberId, Map<String, Integer> scars) {
        memory.set(FLAGSHIP_ID_KEY, flagshipMemberId);
        String encoded = encode(scars);
        if (encoded.isEmpty()) {
            memory.unset(SCARS_KEY);
        } else {
            memory.set(SCARS_KEY, encoded);
        }
    }

    public static String flagshipId(MemoryAPI memory) {
        return memory != null && memory.get(FLAGSHIP_ID_KEY) instanceof String flagshipId ? flagshipId : null;
    }

    static String encodedScars(MemoryAPI memory) {
        return memory != null && memory.get(SCARS_KEY) instanceof String encoded ? encoded : null;
    }

    public static Map<String, Integer> scars(MemoryAPI memory) {
        return decode(encodedScars(memory));
    }

    static String encode(Map<String, Integer> scars) {
        if (scars == null) {
            return "";
        }
        List<String> entries = new ArrayList<>();
        scars.forEach((scarId, stacks) -> {
            if (scarId != null && !scarId.isEmpty() && stacks != null && stacks > 0) {
                entries.add(scarId + STACK_SEPARATOR + stacks);
            }
        });
        return String.join(ENTRY_SEPARATOR, entries);
    }

    static synchronized Map<String, Integer> decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return Map.of();
        }
        if (!encoded.equals(lastEncoded)) {
            lastDecoded = parse(encoded);
            lastEncoded = encoded;
        }
        return lastDecoded;
    }

    private static Map<String, Integer> parse(String encoded) {
        Map<String, Integer> scars = new LinkedHashMap<>();
        for (String entry : encoded.split(ENTRY_SEPARATOR)) {
            int separator = entry.lastIndexOf(STACK_SEPARATOR);
            if (separator <= 0) {
                continue;
            }
            int stacks = stacks(entry.substring(separator + 1).trim());
            if (stacks > 0) {
                scars.merge(entry.substring(0, separator).trim(), stacks, Integer::sum);
            }
        }
        return Collections.unmodifiableMap(scars);
    }

    private static int stacks(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
