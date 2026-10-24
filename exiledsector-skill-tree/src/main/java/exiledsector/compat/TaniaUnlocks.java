package exiledsector.compat;

import com.fs.starfarer.api.campaign.SectorAPI;

public final class TaniaUnlocks {

    static final String BLOOD_TRAIL_DEFEATED_FLAG = "$ramirez_dead";
    static final String FROST_FIRE_DEFEATED_FLAG = "$tania_spies_dead";
    static final String LAST_STRIVE_COMPLETED_FLAG = "$tania_final_boss_dead";

    private TaniaUnlocks() {
    }

    public static boolean isBloodTrailDefeated(SectorAPI sector) {
        return sectorFlag(sector, BLOOD_TRAIL_DEFEATED_FLAG);
    }

    public static boolean isFrostFireDefeated(SectorAPI sector) {
        return sectorFlag(sector, FROST_FIRE_DEFEATED_FLAG);
    }

    public static boolean isLastStriveCompleted(SectorAPI sector) {
        return sectorFlag(sector, LAST_STRIVE_COMPLETED_FLAG);
    }

    private static boolean sectorFlag(SectorAPI sector, String flag) {
        return sector != null && sector.getMemoryWithoutUpdate() != null && sector.getMemoryWithoutUpdate().getBoolean(flag);
    }
}
