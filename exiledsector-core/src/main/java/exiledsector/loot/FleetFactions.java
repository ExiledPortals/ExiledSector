package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FleetFactions {

    public static final String REAL_FACTION_MEMORY_KEY = "$exiledSector_realFaction";
    public static final String MEMBER_FACTIONS_MEMORY_KEY = "$exiledSector_memberFactions";

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

    public static String factionIdOf(FleetMemberAPI member, CampaignFleetAPI fleet) {
        String memberFactionId = memberFactionIdOf(member, fleet);
        return memberFactionId != null ? memberFactionId : realFactionIdOf(fleet);
    }

    public static String memberFactionIdOf(FleetMemberAPI member, CampaignFleetAPI fleet) {
        MemoryAPI memory = fleet == null || member == null ? null : fleet.getMemoryWithoutUpdate();
        Object recorded = memory == null ? null : memory.get(MEMBER_FACTIONS_MEMORY_KEY);
        Object memberFactionId = recorded instanceof Map<?, ?> factionIdsByMember ? factionIdsByMember.get(member.getId()) : null;
        return memberFactionId instanceof String factionId && !factionId.isEmpty() ? factionId : null;
    }

    public static void recordMemberFactions(CampaignFleetAPI fleet, Map<FleetMemberAPI, String> factionIdsByMember) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        if (factionIdsByMember.isEmpty()) {
            memory.unset(MEMBER_FACTIONS_MEMORY_KEY);
            return;
        }
        Map<String, String> factionIdsByMemberId = new LinkedHashMap<>();
        factionIdsByMember.forEach((member, factionId) -> factionIdsByMemberId.put(member.getId(), factionId));
        memory.set(MEMBER_FACTIONS_MEMORY_KEY, factionIdsByMemberId);
    }
}
