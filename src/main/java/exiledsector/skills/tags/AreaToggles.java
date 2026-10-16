package exiledsector.skills.tags;

import exiledsector.ModSettings;
import exiledsector.compat.LostSectorCompat;

import java.util.List;
import java.util.Set;

public final class AreaToggles {

    public static final String LOST_SECTOR_FIELD_ID = "exiledSector_lostSectorArea";
    public static final Set<String> LOST_SECTOR_REGIONS = Set.of("enigma", "kesteven");
    public static final String AUTO = "Auto";
    public static final String ON = "On";
    public static final String OFF = "Off";
    public static final List<String> OPTIONS = List.of(AUTO, ON, OFF);

    private AreaToggles() {
    }

    public static Set<String> disabledRegions() {
        return disabledRegions(ModSettings.stringOr(LOST_SECTOR_FIELD_ID, AUTO), LostSectorCompat.isModEnabled());
    }

    static Set<String> disabledRegions(String lostSectorSetting, boolean lostSectorInstalled) {
        boolean regionsShown = switch (lostSectorSetting == null ? AUTO : lostSectorSetting) {
            case ON -> true;
            case OFF -> false;
            default -> lostSectorInstalled;
        };
        return regionsShown ? Set.of() : LOST_SECTOR_REGIONS;
    }
}
