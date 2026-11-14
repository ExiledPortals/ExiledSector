package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public final class FleetFactions {

    public static final String REAL_FACTION_MEMORY_KEY = "$exiledSector_realFaction";

    private FleetFactions() {
    }

    public static String realFactionIdOf(CampaignFleetAPI fleet) {
        if (fleet == null) {
            return null;
        }
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        String realFactionId = memory == null ? null : memory.getString(REAL_FACTION_MEMORY_KEY);
        if (realFactionId != null && !realFactionId.isEmpty()) {
            return realFactionId;
        }
        return fleet.getFaction() == null ? null : fleet.getFaction().getId();
    }
}
