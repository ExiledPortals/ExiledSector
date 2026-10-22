package exiledsector.items;

import exiledlib.HostMod;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemKind;
import exiledlib.items.ItemType;
import exiledlib.items.ModifierScope;
import exiledlib.items.PoolEntry;
import exiledlib.items.UniqueSource;
import exiledlib.modifiers.ModifierSlot;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Pattern;

public final class SubroutineDefinitions {

    static final String DATA_FILE = "subroutines.csv";
    static final String SUBROUTINE_KIND_COLUMN = "kind";
    static final String UNIQUE_LINES_COLUMN = "lines";
    static final String UNIQUE_SOURCE_COLUMN = "source";
    static final String SOCKET_LIMIT_COLUMN = "socket_limit";
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,9}");

    private SubroutineDefinitions() {
    }

    public static String dataPath() {
        return HostMod.configPath(DATA_FILE);
    }

    static ItemDefinition parse(JSONObject row) {
        String definitionId = ItemDefinition.definitionId(row);
        String kindText = row.optString(SUBROUTINE_KIND_COLUMN, "").trim();
        SubroutineKind subroutineKind = SubroutineKind.byIdOrNull(kindText);
        if (subroutineKind == null) {
            throw new IllegalArgumentException(definitionId + " has kind \"" + kindText + "\"; expected one of " + SubroutineKind.ids());
        }
        String linesText = row.optString(UNIQUE_LINES_COLUMN, "").trim();
        List<PoolEntry> uniqueLines = List.of();
        if (subroutineKind == SubroutineKind.UNIQUE) {
            if (!"true".equalsIgnoreCase(row.optString("unique", "").trim())) {
                throw new IllegalArgumentException(definitionId + " is a " + SubroutineKind.UNIQUE.id() + " subroutine, so unique must be true");
            }
            uniqueLines = ItemDefinition.parsePool(definitionId, linesText, ModifierSlot.PREFIX, ModifierScope.UNIQUE_SUBROUTINE);
            ItemDefinition.rejectRepeats(uniqueLines, List.of());
        } else if (!linesText.isEmpty()) {
            throw new IllegalArgumentException(definitionId + " lists lines, but only " + SubroutineKind.UNIQUE.id() + " subroutines have them");
        }
        List<UniqueSource> sources = UniqueSource.parseList(row.optString(UNIQUE_SOURCE_COLUMN, ""));
        String socketLimitText = row.optString(SOCKET_LIMIT_COLUMN, "");
        Integer socketLimit = parsedSocketLimit(socketLimitText);
        if (socketLimit == null) {
            throw new IllegalArgumentException(definitionId + " has socket limit \"" + socketLimitText.trim()
                    + "\"; expected a whole number of 0 or more, or blank for no limit");
        }
        return ItemDefinition.parseShared(row, ItemKind.SUBROUTINE, ItemType.SUBROUTINE, new ItemDefinition.Pools(uniqueLines, List.of()),
                subroutineKind, sources).withSocketLimit(socketLimit);
    }

    static Integer parsedSocketLimit(String socketLimitText) {
        String trimmed = socketLimitText == null ? "" : socketLimitText.trim();
        if (trimmed.isEmpty()) {
            return ItemDefinition.NO_SOCKET_LIMIT;
        }
        return WHOLE_NUMBER.matcher(trimmed).matches() ? Integer.valueOf(trimmed) : null;
    }
}
