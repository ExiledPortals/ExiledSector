package exiledsector.items;

import java.util.Set;

public final class ItemVolumes {

    public static final String VOLUME_COLUMN = "volume";

    public interface Toggles {

        Set<String> volumes();

        boolean modInstalled(String volume);

        boolean switchedOn(String volume);
    }

    private static volatile Toggles toggles;

    private ItemVolumes() {
    }

    public static void bind(Toggles areaToggles) {
        toggles = areaToggles;
    }

    public static Set<String> known() {
        Toggles current = toggles;
        return current == null ? Set.of() : current.volumes();
    }

    public static boolean isKnown(String volume) {
        Toggles current = toggles;
        return current == null || current.volumes().contains(volume);
    }

    public static boolean loads(ItemDefinition definition) {
        Toggles current = toggles;
        return definition.volume() == null || current == null || current.modInstalled(definition.volume());
    }

    public static boolean dropsNow(ItemDefinition definition) {
        Toggles current = toggles;
        return definition.volume() == null || current == null
                || current.modInstalled(definition.volume()) && current.switchedOn(definition.volume());
    }
}
