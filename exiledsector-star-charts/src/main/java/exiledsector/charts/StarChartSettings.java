package exiledsector.charts;

import exiledsector.ModSettings;
import exiledsector.i18n.Translation;
import exiledsector.domain.DomainSwitch;
import exiledsector.items.StarChart;
import exiledsector.modifiers.TierGroup;

import java.util.List;

public final class StarChartSettings {

    public static final String ENABLED_FIELD_ID = "exiledSector_starChartsEnabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final List<WormholeEntries> ENTRIES = List.of(
            new WormholeEntries(TierGroup.LOW, "exiledSector_starChartEntriesLow", 6),
            new WormholeEntries(TierGroup.MID, "exiledSector_starChartEntriesMid", 3),
            new WormholeEntries(TierGroup.HIGH, "exiledSector_starChartEntriesHigh", 1));
    public static final int MIN_ENTRIES = 1;
    public static final int MAX_ENTRIES = 10;
    public static final String FLEET_LIMIT_FIELD_ID = "exiledSector_starChartFleetLimit";
    public static final int DEFAULT_FLEET_LIMIT = 7;
    public static final int MIN_FLEET_LIMIT = 1;
    public static final int MAX_FLEET_LIMIT = 12;
    public static final String OPEN_ANYWHERE_FIELD_ID = "exiledSector_starChartOpenAnywhereDev";
    public static final boolean DEFAULT_OPEN_ANYWHERE = false;
    public static final String NO_COLONY_FIELD_ID = "exiledSector_starChartNoColony";
    public static final boolean DEFAULT_NO_COLONY = false;
    public static final String DEBUG_TOOLS_FIELD_ID = "exiledSector_starChartDebugTools";
    public static final boolean DEFAULT_DEBUG_TOOLS = false;

    public static final DomainSwitch SWITCH = new DomainSwitch(ENABLED_FIELD_ID, DEFAULT_ENABLED, () -> {
    });

    private static boolean noColony = DEFAULT_NO_COLONY;

    private StarChartSettings() {
    }

    public static String settingsTab() {
        return Translation.text("settings.tab.starCharts");
    }

    public static void reloadSettings() {
        SWITCH.reload();
        noColony = ModSettings.booleanOr(NO_COLONY_FIELD_ID, DEFAULT_NO_COLONY);
    }

    public static boolean enabled() {
        return SWITCH.on();
    }

    public static void setEnabledForTests(boolean enabled) {
        SWITCH.setForTests(enabled);
    }

    public record WormholeEntries(TierGroup group, String fieldId, int defaultEntries) {

        public String nameKey() {
            return "settings.starCharts.entries." + group.id() + ".name";
        }

        public String tooltipKey() {
            return "settings.starCharts.entries." + group.id() + ".tooltip";
        }
    }

    public static WormholeEntries entriesFor(int tier) {
        TierGroup group = TierGroup.forTier(StarChart.clampTier(tier));
        return ENTRIES.stream().filter(entries -> entries.group() == group).findFirst().orElse(ENTRIES.get(0));
    }

    public static int wormholeEntries(int tier) {
        WormholeEntries entries = entriesFor(tier);
        int configured = ModSettings.intOr(entries.fieldId(), entries.defaultEntries());
        return Math.max(MIN_ENTRIES, Math.min(MAX_ENTRIES, configured));
    }

    public static int fleetLimit() {
        int configured;
        try {
            configured = ModSettings.intOr(FLEET_LIMIT_FIELD_ID, DEFAULT_FLEET_LIMIT);
        } catch (RuntimeException | LinkageError e) {
            configured = DEFAULT_FLEET_LIMIT;
        }
        return Math.max(MIN_FLEET_LIMIT, Math.min(MAX_FLEET_LIMIT, configured));
    }

    public static boolean openAnywhere() {
        return ModSettings.booleanOr(OPEN_ANYWHERE_FIELD_ID, DEFAULT_OPEN_ANYWHERE) || noColony();
    }

    public static boolean noColony() {
        return noColony;
    }

    public static void setNoColonyForTests(boolean value) {
        noColony = value;
    }

    public static boolean debugTools() {
        return ModSettings.booleanOr(DEBUG_TOOLS_FIELD_ID, DEFAULT_DEBUG_TOOLS);
    }
}
