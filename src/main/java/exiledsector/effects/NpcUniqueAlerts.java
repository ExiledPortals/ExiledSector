package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.npc.NpcTreeRecords;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.socketables.NpcSocketables;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableRarity;

import java.util.ArrayList;
import java.util.List;

public final class NpcUniqueAlerts {

    static final String CARRIER_KEY = "$exiledSector_carriesUniqueSocketable";
    static final String ALERTED_KEY = "$exiledSector_uniqueSocketableAlerted";

    private NpcUniqueAlerts() {
    }

    static void markIfCarrying(CampaignFleetAPI fleet, String record) {
        if (!uniquesIn(record).isEmpty()) {
            fleet.getMemoryWithoutUpdate().set(CARRIER_KEY, true);
        }
    }

    static void alertIfSensed(CampaignFleetAPI fleet) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        if (memory == null || !memory.getBoolean(CARRIER_KEY) || memory.getBoolean(ALERTED_KEY)) {
            return;
        }
        VisibilityLevel visibility = fleet.getVisibilityLevelToPlayerFleet();
        if (visibility == null || visibility == VisibilityLevel.NONE) {
            return;
        }
        List<Socketable> carried = new ArrayList<>();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            carried.addAll(uniquesIn(NpcTreeRecords.of(memory).get(member.getId())));
        }
        memory.set(ALERTED_KEY, true);
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (carried.isEmpty() || ui == null) {
            return;
        }
        boolean identified = visibility == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS;
        I18n.forGameText(() -> {
            String uniques = String.join(", ", carried.stream().map(Socketable::name).toList());
            String fleetName = identified ? fleet.getNameWithFactionKeepCase() : "";
            String fleetText = identified
                    ? Translation.msg("socketable.npcUnique.identifiedFleet").arg("fleet", fleetName).text()
                    : Translation.text("socketable.npcUnique.unidentifiedFleet");
            String message = Translation.msg("socketable.npcUnique.sensed").arg("name", uniques).arg("fleet", fleetText).text();
            ui.addMessage(message.replace("%", "%%"), Misc.getTextColor(), uniques, identified ? fleetName : fleetText, SocketableRarity.UNIQUE.color(), Misc.getHighlightColor());
        });
    }

    private static List<Socketable> uniquesIn(String record) {
        if (record == null || !NpcTreeRecords.isLevelled(record) || !record.contains(NpcSocketables.ID_PREFIX)) {
            return List.of();
        }
        return NpcSocketables.uniquesCarriedBy(NpcTreeTag.decode(record));
    }
}
