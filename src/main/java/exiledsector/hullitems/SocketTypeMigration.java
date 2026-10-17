package exiledsector.hullitems;

import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.ui.CampaignMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SocketTypeMigration {

    private SocketTypeMigration() {
    }

    public static void run() {
        List<HullItem> returnedItems = returnMisfits(ShipSkillDataManager.all(), HullItemStore.get());
        if (returnedItems.isEmpty() || !CampaignMessages.available()) {
            return;
        }
        String message = I18n.forGameText(() -> Translation.msg("socketable.migration.returned")
                .arg("names", returnedItems.stream().map(HullItem::name).collect(Collectors.joining(", "))).text());
        CampaignMessages.plain(message, Misc.getTextColor());
    }

    static List<HullItem> returnMisfits(Map<String, ShipSkillData> shipDataById, HullItemStore store) {
        List<HullItem> returnedItems = new ArrayList<>();
        for (ShipSkillData shipData : shipDataById.values()) {
            for (Map.Entry<String, String> socketedEntry : List.copyOf(shipData.getSocketedItems().entrySet())) {
                HullItem hullItem = store.find(socketedEntry.getValue());
                if (hullItem != null && hullItem.kind() != null && !SocketCustody.fitsTreeSocket(hullItem)) {
                    shipData.unsocketItem(socketedEntry.getKey());
                    returnedItems.add(hullItem);
                }
            }
            for (Map.Entry<String, String> slotEntry : List.copyOf(shipData.getSectionItems().entrySet())) {
                HullItem hullItem = store.find(slotEntry.getValue());
                if (hullItem != null && hullItem.kind() != null && !SocketCustody.fitsSection(hullItem, slotEntry.getKey())) {
                    shipData.removeFromSection(slotEntry.getKey());
                    returnedItems.add(hullItem);
                }
            }
        }
        return returnedItems;
    }
}
