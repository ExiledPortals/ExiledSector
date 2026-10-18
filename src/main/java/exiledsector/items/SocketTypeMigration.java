package exiledsector.items;

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
        List<Item> returnedItems = returnMisfits(ShipSkillDataManager.all(), ItemStore.get());
        if (returnedItems.isEmpty() || !CampaignMessages.available()) {
            return;
        }
        String message = I18n.forGameText(() -> Translation.msg("socketable.migration.returned")
                .arg("names", returnedItems.stream().map(Item::name).collect(Collectors.joining(", "))).text());
        CampaignMessages.plain(message, Misc.getTextColor());
    }

    static List<Item> returnMisfits(Map<String, ShipSkillData> shipDataById, ItemStore store) {
        List<Item> returnedItems = new ArrayList<>();
        for (ShipSkillData shipData : shipDataById.values()) {
            for (Map.Entry<String, String> socketedEntry : List.copyOf(shipData.getSocketedItems().entrySet())) {
                Item item = store.find(socketedEntry.getValue());
                if (item != null && item.kind() != null && !SocketCustody.fitsTreeSocket(item)) {
                    shipData.unsocketItem(socketedEntry.getKey());
                    returnedItems.add(item);
                }
            }
            for (Map.Entry<String, String> slotEntry : List.copyOf(shipData.getSectionItems().entrySet())) {
                Item item = store.find(slotEntry.getValue());
                if (item != null && item.kind() != null && !SocketCustody.fitsSection(item, slotEntry.getKey())) {
                    shipData.removeFromSection(slotEntry.getKey());
                    returnedItems.add(item);
                }
            }
        }
        return returnedItems;
    }
}
