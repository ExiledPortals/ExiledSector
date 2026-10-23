package exiledsector.skills.tags;

import exiledlib.ModSettings;
import exiledlib.compat.LostSectorCompat;
import exiledsector.compat.NightcrossCompat;
import exiledsector.compat.RandomAssortmentCompat;
import exiledsector.compat.TaniaCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AreaToggles {

    public static final String LOST_SECTOR_FIELD_ID = "exiledSector_lostSectorArea";
    public static final Set<String> LOST_SECTOR_REGIONS = Set.of("enigma", "kesteven");
    public static final String RANDOM_ASSORTMENT_FIELD_ID = "exiledSector_randomAssortmentArea";
    public static final Set<String> RANDOM_ASSORTMENT_REGIONS = Set.of("exotech", "abyss");
    public static final String NIGHTCROSS_FIELD_ID = "exiledSector_nightcrossArea";
    public static final Set<String> NIGHTCROSS_REGIONS = Set.of("nightcross", "stargazer");
    public static final String TANIA_FIELD_ID = "exiledSector_taniaArea";
    public static final Set<String> TANIA_REGIONS = Set.of("tania");
    public static final List<Set<String>> TOGGLED_AREAS = List.of(LOST_SECTOR_REGIONS, RANDOM_ASSORTMENT_REGIONS, NIGHTCROSS_REGIONS, TANIA_REGIONS);
    public static final String AUTO = "Auto";
    public static final String ON = "On";
    public static final String OFF = "Off";
    public static final List<String> OPTIONS = List.of(AUTO, ON, OFF);
    public static final List<String> REQUIRED_MOD_OPTIONS = List.of(AUTO, OFF);

    private AreaToggles() {
    }

    public static Set<String> disabledRegions() {
        Set<String> disabled = new HashSet<>(disabledRegions(ModSettings.stringOr(LOST_SECTOR_FIELD_ID, AUTO), LostSectorCompat.isModEnabled()));
        disabled.addAll(disabledRandomAssortmentRegions(ModSettings.stringOr(RANDOM_ASSORTMENT_FIELD_ID, AUTO),
                RandomAssortmentCompat.isModEnabled()));
        disabled.addAll(disabledNightcrossRegions(ModSettings.stringOr(NIGHTCROSS_FIELD_ID, AUTO), NightcrossCompat.isModEnabled()));
        disabled.addAll(disabledTaniaRegions(ModSettings.stringOr(TANIA_FIELD_ID, AUTO), TaniaCompat.isModEnabled()));
        return Set.copyOf(disabled);
    }

    public static boolean modInstalledFor(String region) {
        if (LOST_SECTOR_REGIONS.contains(region)) {
            return LostSectorCompat.isModEnabled();
        }
        if (RANDOM_ASSORTMENT_REGIONS.contains(region)) {
            return RandomAssortmentCompat.isModEnabled();
        }
        if (NIGHTCROSS_REGIONS.contains(region)) {
            return NightcrossCompat.isModEnabled();
        }
        return !TANIA_REGIONS.contains(region) || TaniaCompat.isModEnabled();
    }

    static Set<String> disabledRegions(String lostSectorSetting, boolean lostSectorInstalled) {
        return isShown(lostSectorSetting, lostSectorInstalled) ? Set.of() : LOST_SECTOR_REGIONS;
    }

    static Set<String> disabledRandomAssortmentRegions(String randomAssortmentSetting, boolean randomAssortmentInstalled) {
        return randomAssortmentInstalled && !OFF.equals(randomAssortmentSetting) ? Set.of() : RANDOM_ASSORTMENT_REGIONS;
    }

    static Set<String> disabledNightcrossRegions(String nightcrossSetting, boolean nightcrossInstalled) {
        return isShown(nightcrossSetting, nightcrossInstalled) ? Set.of() : NIGHTCROSS_REGIONS;
    }

    static Set<String> disabledTaniaRegions(String taniaSetting, boolean taniaInstalled) {
        return taniaInstalled && !OFF.equals(taniaSetting) ? Set.of() : TANIA_REGIONS;
    }

    private static boolean isShown(String areaSetting, boolean modInstalled) {
        return switch (areaSetting == null ? AUTO : areaSetting) {
            case ON -> true;
            case OFF -> false;
            default -> modInstalled;
        };
    }
}
