package exiledsector.items;

import java.util.Set;

public final class ItemAreas {

    public static final String AREA_COLUMN = "area";

    public interface Toggles {

        Set<String> areas();

        boolean modInstalled(String area);

        boolean switchedOn(String area);
    }

    private static volatile Toggles toggles;

    private ItemAreas() {
    }

    public static void bind(Toggles areaToggles) {
        toggles = areaToggles;
    }

    public static Set<String> known() {
        Toggles current = toggles;
        return current == null ? Set.of() : current.areas();
    }

    public static boolean isKnown(String area) {
        Toggles current = toggles;
        return current == null || current.areas().contains(area);
    }

    public static boolean loads(ItemDefinition definition) {
        Toggles current = toggles;
        return definition.area() == null || current == null || current.modInstalled(definition.area());
    }

    public static boolean dropsNow(ItemDefinition definition) {
        Toggles current = toggles;
        return definition.area() == null || current == null
                || current.modInstalled(definition.area()) && current.switchedOn(definition.area());
    }
}
