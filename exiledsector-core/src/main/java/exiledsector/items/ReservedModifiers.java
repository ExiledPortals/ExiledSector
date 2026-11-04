package exiledsector.items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ReservedModifiers {

    private record Reservation(String ownerId, String headerKey) {
    }

    private static final AtomicReference<Map<String, Reservation>> RESERVATIONS_BY_MODIFIER = new AtomicReference<>(Map.of());

    private ReservedModifiers() {
    }

    public static void reserve(String modifierId, String ownerId) {
        reserve(modifierId, ownerId, null);
    }

    public static void reserve(String modifierId, String ownerId, String headerKey) {
        if (modifierId == null || ownerId == null) {
            throw new IllegalArgumentException("a reserved modifier needs a modifier id and an owner");
        }
        Map<String, Reservation> previous;
        Map<String, Reservation> updated;
        do {
            previous = RESERVATIONS_BY_MODIFIER.get();
            Reservation existing = previous.get(modifierId);
            if (existing != null && !existing.ownerId().equals(ownerId)) {
                throw new IllegalArgumentException(modifierId + " is already reserved by " + existing.ownerId());
            }
            Map<String, Reservation> reserved = new LinkedHashMap<>(previous);
            reserved.put(modifierId, new Reservation(ownerId, headerKey));
            updated = Map.copyOf(reserved);
        } while (!RESERVATIONS_BY_MODIFIER.compareAndSet(previous, updated));
    }

    public static void release(String ownerId) {
        Map<String, Reservation> previous;
        Map<String, Reservation> updated;
        do {
            previous = RESERVATIONS_BY_MODIFIER.get();
            Map<String, Reservation> remaining = new LinkedHashMap<>(previous);
            remaining.values().removeIf(reservation -> reservation.ownerId().equals(ownerId));
            updated = Map.copyOf(remaining);
        } while (!RESERVATIONS_BY_MODIFIER.compareAndSet(previous, updated));
    }

    public static boolean isReserved(String modifierId) {
        return modifierId != null && RESERVATIONS_BY_MODIFIER.get().containsKey(modifierId);
    }

    public static String owner(String modifierId) {
        Reservation reservation = modifierId == null ? null : RESERVATIONS_BY_MODIFIER.get().get(modifierId);
        return reservation == null ? null : reservation.ownerId();
    }

    public static String headerKey(String modifierId) {
        Reservation reservation = modifierId == null ? null : RESERVATIONS_BY_MODIFIER.get().get(modifierId);
        return reservation == null ? null : reservation.headerKey();
    }

    public static void clear() {
        RESERVATIONS_BY_MODIFIER.set(Map.of());
    }
}
