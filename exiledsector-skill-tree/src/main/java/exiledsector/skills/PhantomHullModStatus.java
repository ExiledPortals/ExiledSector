package exiledsector.skills;

import java.util.HashSet;
import java.util.Set;

public final class PhantomHullModStatus {

    private static final Set<String> ACTIVE = new HashSet<>();

    private PhantomHullModStatus() {
    }

    public static boolean isActive(String hullModId) {
        return ACTIVE.contains(hullModId);
    }

    public static void markActive(String hullModId) {
        ACTIVE.add(hullModId);
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
