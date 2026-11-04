package exiledsector.items;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class ModifierSources {

    public record Source(String ownerId, Set<ItemKind> parts, String headerKey) {
    }

    private static final AtomicReference<Map<String, Source>> SOURCES_BY_MODIFIER = new AtomicReference<>(Map.of());

    private ModifierSources() {
    }

    public static void reserve(String modifierId, String ownerId, Collection<ItemKind> parts) {
        reserve(modifierId, ownerId, parts, null);
    }

    public static void reserve(String modifierId, String ownerId, Collection<ItemKind> parts, String headerKey) {
        if (modifierId == null || ownerId == null || parts == null) {
            throw new IllegalArgumentException("a reserved modifier needs a modifier id, an owner and its parts");
        }
        Map<String, Source> previous;
        Map<String, Source> updated;
        do {
            previous = SOURCES_BY_MODIFIER.get();
            Source earlier = previous.get(modifierId);
            if (earlier != null && !earlier.ownerId().equals(ownerId)) {
                throw new IllegalArgumentException(modifierId + " is already reserved by " + earlier.ownerId());
            }
            Set<ItemKind> joinedParts = EnumSet.noneOf(ItemKind.class);
            if (earlier != null) {
                joinedParts.addAll(earlier.parts());
            }
            joinedParts.addAll(parts);
            Map<String, Source> reserved = new LinkedHashMap<>(previous);
            String joinedHeaderKey = headerKey != null || earlier == null ? headerKey : earlier.headerKey();
            reserved.put(modifierId, new Source(ownerId, Collections.unmodifiableSet(joinedParts), joinedHeaderKey));
            updated = Collections.unmodifiableMap(reserved);
        } while (!SOURCES_BY_MODIFIER.compareAndSet(previous, updated));
    }

    public static void release(String ownerId) {
        Map<String, Source> previous;
        Map<String, Source> updated;
        do {
            previous = SOURCES_BY_MODIFIER.get();
            Map<String, Source> remaining = new LinkedHashMap<>(previous);
            remaining.values().removeIf(source -> source.ownerId().equals(ownerId));
            if (remaining.size() == previous.size()) {
                return;
            }
            updated = Collections.unmodifiableMap(remaining);
        } while (!SOURCES_BY_MODIFIER.compareAndSet(previous, updated));
    }

    public static boolean isReserved(String modifierId) {
        return modifierId != null && SOURCES_BY_MODIFIER.get().containsKey(modifierId);
    }

    public static String owner(String modifierId) {
        Source source = modifierId == null ? null : SOURCES_BY_MODIFIER.get().get(modifierId);
        return source == null ? null : source.ownerId();
    }

    public static Set<ItemKind> parts(String modifierId) {
        Source source = modifierId == null ? null : SOURCES_BY_MODIFIER.get().get(modifierId);
        return source == null ? Set.of() : source.parts();
    }

    public static String headerKey(String modifierId) {
        Source source = modifierId == null ? null : SOURCES_BY_MODIFIER.get().get(modifierId);
        return source == null ? null : source.headerKey();
    }

    public static Map<String, Source> all() {
        return SOURCES_BY_MODIFIER.get();
    }

    public static void clear() {
        SOURCES_BY_MODIFIER.set(Map.of());
    }
}
