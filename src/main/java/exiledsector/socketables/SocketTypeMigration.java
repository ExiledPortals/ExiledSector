package exiledsector.socketables;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SocketTypeMigration {

    private SocketTypeMigration() {
    }

    public static void run() {
        SocketableStore.get().migrateLegacyFrameworks(ShipSkillDataManager.all());
        List<Socketable> returnedItems = returnMisfits(ShipSkillDataManager.all(), SocketableStore.get());
        CampaignUIAPI campaignUi = Global.getSector().getCampaignUI();
        if (returnedItems.isEmpty() || campaignUi == null) {
            return;
        }
        String itemNames = returnedItems.stream().map(Socketable::name).collect(Collectors.joining(", "));
        campaignUi.addMessage(Translation.msg("socketable.migration.returned").arg("names", itemNames).text().replace("%", "%%"),
                Misc.getTextColor());
    }

    static List<Socketable> returnMisfits(Map<String, ShipSkillData> shipDataById, SocketableStore store) {
        List<Socketable> returnedItems = new ArrayList<>();
        for (ShipSkillData shipData : shipDataById.values()) {
            for (Map.Entry<String, String> socketedEntry : List.copyOf(shipData.getSocketedItems().entrySet())) {
                Socketable socketable = store.find(socketedEntry.getValue());
                SocketType socketType = socketable == null ? null : socketable.kind();
                if (socketType != null && socketType != SocketType.SUBROUTINE) {
                    shipData.unsocketItem(socketedEntry.getKey());
                    returnedItems.add(socketable);
                }
            }
            for (Map.Entry<String, String> slotEntry : List.copyOf(shipData.getFrameworkSocketedItems().entrySet())) {
                Socketable socketable = store.find(slotEntry.getValue());
                if (socketable != null && socketable.kind() != null
                        && !socketable.canSocketInto(SocketType.byIdOrNull(slotEntry.getKey()))) {
                    shipData.unsocketFrameworkItem(slotEntry.getKey());
                    returnedItems.add(socketable);
                }
            }
        }
        return returnedItems;
    }
}
