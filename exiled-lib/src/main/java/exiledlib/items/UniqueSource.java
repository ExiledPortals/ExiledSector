package exiledlib.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public record UniqueSource(Kind kind, String sourceId) {

    public enum Kind {
        FACTION("faction"),
        SITE("site");

        private final String prefix;

        Kind(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }

        static Kind byPrefixOrNull(String prefix) {
            for (Kind kind : values()) {
                if (kind.prefix.equals(prefix)) {
                    return kind;
                }
            }
            return null;
        }
    }

    static final String SOURCE_SEPARATOR = ";";
    static final String KIND_SEPARATOR = ":";
    private static final Pattern SOURCE_ID = Pattern.compile("[A-Za-z0-9_.-]+");

    public static UniqueSource parse(String sourceText) {
        String trimmed = sourceText == null ? "" : sourceText.trim();
        int separatorIndex = trimmed.indexOf(KIND_SEPARATOR);
        Kind kind = separatorIndex < 0 ? null : Kind.byPrefixOrNull(trimmed.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT));
        String sourceId = separatorIndex < 0 ? "" : trimmed.substring(separatorIndex + KIND_SEPARATOR.length()).trim();
        if (kind == null || !SOURCE_ID.matcher(sourceId).matches()) {
            throw new IllegalArgumentException("source \"" + trimmed + "\" is not faction:FACTION_ID or site:SALVAGE_SITE_ID");
        }
        return new UniqueSource(kind, sourceId);
    }

    public static List<UniqueSource> parseList(String sourcesText) {
        List<UniqueSource> sources = new ArrayList<>();
        for (String sourceText : (sourcesText == null ? "" : sourcesText).split(SOURCE_SEPARATOR)) {
            if (!sourceText.isBlank()) {
                UniqueSource source = parse(sourceText);
                if (sources.contains(source)) {
                    throw new IllegalArgumentException("source \"" + sourceText.trim() + "\" is listed more than once");
                }
                sources.add(source);
            }
        }
        return List.copyOf(sources);
    }

    public static boolean allows(List<UniqueSource> sources, Kind kind, String sourceId) {
        if (sources == null || sources.isEmpty()) {
            return true;
        }
        return sourceId != null && sources.contains(new UniqueSource(kind, sourceId));
    }

    public String text() {
        return kind.prefix() + KIND_SEPARATOR + sourceId;
    }
}
