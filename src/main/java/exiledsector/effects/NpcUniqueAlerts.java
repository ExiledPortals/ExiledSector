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

    static void markIfCarrying(CampaignFleetAPI fleet, String treeRecord) {
        if (!uniquesIn(treeRecord).isEmpty()) {
            fleet.getMemoryWithoutUpdate().set(CARRIER_KEY, true);
        }
    }

    static void alertIfSensed(CampaignFleetAPI sensedFleet) {
        MemoryAPI fleetMemory = sensedFleet.getMemoryWithoutUpdate();
        if (fleetMemory == null || !fleetMemory.getBoolean(CARRIER_KEY) || fleetMemory.getBoolean(ALERTED_KEY)) {
            return;
        }
        VisibilityLevel visibility = sensedFleet.getVisibilityLevelToPlayerFleet();
        if (visibility == null || visibility == VisibilityLevel.NONE) {
            return;
        }
        List<Socketable> carriedUniques = new ArrayList<>();
        for (FleetMemberAPI member : sensedFleet.getFleetData().getMembersListCopy()) {
            carriedUniques.addAll(uniquesIn(NpcTreeRecords.of(fleetMemory).get(member.getId())));
        }
        fleetMemory.set(ALERTED_KEY, true);
        CampaignUIAPI campaignUi = Global.getSector().getCampaignUI();
        if (carriedUniques.isEmpty() || campaignUi == null) {
            return;
        }
        boolean identified = visibility == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS;
        I18n.forGameText(() -> {
            String uniqueNames = String.join(", ", carriedUniques.stream().map(Socketable::name).toList());
            String fleetName = identified ? sensedFleet.getNameWithFactionKeepCase() : "";
            String fleetText = identified
                    ? Translation.msg("socketable.npcUnique.identifiedFleet").arg("fleet", fleetName).text()
                    : Translation.text("socketable.npcUnique.unidentifiedFleet");
            String alertMessage = Translation.msg("socketable.npcUnique.sensed").arg("name", uniqueNames).arg("fleet", fleetText).text();
            campaignUi.addMessage(alertMessage.replace("%", "%%"), Misc.getTextColor(), uniqueNames, identified ? fleetName : fleetText, SocketableRarity.UNIQUE.color(), Misc.getHighlightColor());
        });
    }

    private static List<Socketable> uniquesIn(String treeRecord) {
        if (treeRecord == null || !NpcTreeRecords.isLevelled(treeRecord) || !treeRecord.contains(NpcSocketables.ID_PREFIX)) {
            return List.of();
        }
        return NpcSocketables.uniquesCarriedBy(NpcTreeTag.decode(treeRecord));
    }
}
