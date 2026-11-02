package exiledsector.skills.tags;

import exiledsector.ModSettings;
import exiledsector.compat.LostSectorCompat;
import exiledsector.compat.NightcrossCompat;
import exiledsector.compat.RandomAssortmentCompat;
import exiledsector.compat.TaniaCompat;
import exiledsector.items.ItemVolumes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VolumeToggles {

    public static final String LOST_SECTOR_FIELD_ID = "exiledSector_lostSectorArea";
    public static final Set<String> LOST_SECTOR_VOLUMES = Set.of("enigma", "kesteven");
    public static final String RANDOM_ASSORTMENT_FIELD_ID = "exiledSector_randomAssortmentArea";
    public static final Set<String> RANDOM_ASSORTMENT_VOLUMES = Set.of("exotech", "abyss");
    public static final String NIGHTCROSS_FIELD_ID = "exiledSector_nightcrossArea";
    public static final Set<String> NIGHTCROSS_VOLUMES = Set.of("nightcross", "stargazer");
    public static final String TANIA_FIELD_ID = "exiledSector_taniaArea";
    public static final Set<String> TANIA_VOLUMES = Set.of("tania");
    public static final List<Set<String>> TOGGLED_VOLUMES = List.of(LOST_SECTOR_VOLUMES, RANDOM_ASSORTMENT_VOLUMES, NIGHTCROSS_VOLUMES, TANIA_VOLUMES);
    public static final String AUTO = "Auto";
    public static final String ON = "On";
    public static final String OFF = "Off";
    public static final List<String> OPTIONS = List.of(AUTO, ON, OFF);
    public static final List<String> REQUIRED_MOD_OPTIONS = List.of(AUTO, OFF);

    public static final ItemVolumes.Toggles ITEM_VOLUMES = new ItemVolumes.Toggles() {
        @Override
        public Set<String> volumes() {
            Set<String> volumes = new HashSet<>();
            TOGGLED_VOLUMES.forEach(volumes::addAll);
            return volumes;
        }

        @Override
        public boolean modInstalled(String volume) {
            return modInstalledFor(volume);
        }

        @Override
        public boolean switchedOn(String volume) {
            return !disabledVolumes().contains(volume);
        }
    };

    private VolumeToggles() {
    }

    public static Set<String> disabledVolumes() {
        Set<String> disabled = new HashSet<>(disabledVolumes(ModSettings.stringOr(LOST_SECTOR_FIELD_ID, AUTO), LostSectorCompat.isModEnabled()));
        disabled.addAll(disabledRandomAssortmentVolumes(ModSettings.stringOr(RANDOM_ASSORTMENT_FIELD_ID, AUTO),
                RandomAssortmentCompat.isModEnabled()));
        disabled.addAll(disabledNightcrossVolumes(ModSettings.stringOr(NIGHTCROSS_FIELD_ID, AUTO), NightcrossCompat.isModEnabled()));
        disabled.addAll(disabledTaniaVolumes(ModSettings.stringOr(TANIA_FIELD_ID, AUTO), TaniaCompat.isModEnabled()));
        return Set.copyOf(disabled);
    }

    public static boolean modInstalledFor(String volume) {
        if (LOST_SECTOR_VOLUMES.contains(volume)) {
            return LostSectorCompat.isModEnabled();
        }
        if (RANDOM_ASSORTMENT_VOLUMES.contains(volume)) {
            return RandomAssortmentCompat.isModEnabled();
        }
        if (NIGHTCROSS_VOLUMES.contains(volume)) {
            return NightcrossCompat.isModEnabled();
        }
        return !TANIA_VOLUMES.contains(volume) || TaniaCompat.isModEnabled();
    }

    static Set<String> disabledVolumes(String lostSectorSetting, boolean lostSectorInstalled) {
        return isShown(lostSectorSetting, lostSectorInstalled) ? Set.of() : LOST_SECTOR_VOLUMES;
    }

    static Set<String> disabledRandomAssortmentVolumes(String randomAssortmentSetting, boolean randomAssortmentInstalled) {
        return randomAssortmentInstalled && !OFF.equals(randomAssortmentSetting) ? Set.of() : RANDOM_ASSORTMENT_VOLUMES;
    }

    static Set<String> disabledNightcrossVolumes(String nightcrossSetting, boolean nightcrossInstalled) {
        return isShown(nightcrossSetting, nightcrossInstalled) ? Set.of() : NIGHTCROSS_VOLUMES;
    }

    static Set<String> disabledTaniaVolumes(String taniaSetting, boolean taniaInstalled) {
        return taniaInstalled && !OFF.equals(taniaSetting) ? Set.of() : TANIA_VOLUMES;
    }

    private static boolean isShown(String volumeSetting, boolean modInstalled) {
        return switch (volumeSetting == null ? AUTO : volumeSetting) {
            case ON -> true;
            case OFF -> false;
            default -> modInstalled;
        };
    }
}
