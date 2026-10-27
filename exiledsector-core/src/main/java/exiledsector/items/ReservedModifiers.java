package exiledsector.items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ReservedModifiers {

    private static final AtomicReference<Map<String, String>> OWNERS_BY_MODIFIER = new AtomicReference<>(Map.of());

    private ReservedModifiers() {
    }

    public static void reserve(String modifierId, String ownerId) {
        if (modifierId == null || ownerId == null) {
            throw new IllegalArgumentException("a reserved modifier needs a modifier id and an owner");
        }
        Map<String, String> previous;
        Map<String, String> updated;
        do {
            previous = OWNERS_BY_MODIFIER.get();
            String owner = previous.get(modifierId);
            if (owner != null && !owner.equals(ownerId)) {
                throw new IllegalArgumentException(modifierId + " is already reserved by " + owner);
            }
            Map<String, String> reserved = new LinkedHashMap<>(previous);
            reserved.put(modifierId, ownerId);
            updated = Map.copyOf(reserved);
        } while (!OWNERS_BY_MODIFIER.compareAndSet(previous, updated));
    }

    public static void release(String ownerId) {
        Map<String, String> previous;
        Map<String, String> updated;
        do {
            previous = OWNERS_BY_MODIFIER.get();
            Map<String, String> remaining = new LinkedHashMap<>(previous);
            remaining.values().removeIf(owner -> owner.equals(ownerId));
            updated = Map.copyOf(remaining);
        } while (!OWNERS_BY_MODIFIER.compareAndSet(previous, updated));
    }

    public static boolean isReserved(String modifierId) {
        return modifierId != null && OWNERS_BY_MODIFIER.get().containsKey(modifierId);
    }

    public static String owner(String modifierId) {
        return modifierId == null ? null : OWNERS_BY_MODIFIER.get().get(modifierId);
    }

    public static void clear() {
        OWNERS_BY_MODIFIER.set(Map.of());
    }
}
