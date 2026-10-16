package exiledsector.socketables;

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
        List<Socketable> returnedItems = returnMisfits(ShipSkillDataManager.all(), SocketableStore.get());
        if (returnedItems.isEmpty() || !CampaignMessages.available()) {
            return;
        }
        String message = I18n.forGameText(() -> Translation.msg("socketable.migration.returned")
                .arg("names", returnedItems.stream().map(Socketable::name).collect(Collectors.joining(", "))).text());
        CampaignMessages.plain(message, Misc.getTextColor());
    }

    static List<Socketable> returnMisfits(Map<String, ShipSkillData> shipDataById, SocketableStore store) {
        List<Socketable> returnedItems = new ArrayList<>();
        for (ShipSkillData shipData : shipDataById.values()) {
            for (Map.Entry<String, String> socketedEntry : List.copyOf(shipData.getSocketedItems().entrySet())) {
                Socketable socketable = store.find(socketedEntry.getValue());
                if (socketable != null && socketable.kind() != null && !SocketCustody.fitsTreeSocket(socketable)) {
                    shipData.unsocketItem(socketedEntry.getKey());
                    returnedItems.add(socketable);
                }
            }
            for (Map.Entry<String, String> slotEntry : List.copyOf(shipData.getFrameworkSocketedItems().entrySet())) {
                Socketable socketable = store.find(slotEntry.getValue());
                if (socketable != null && socketable.kind() != null && !SocketCustody.fitsFrameworkSocket(socketable, slotEntry.getKey())) {
                    shipData.unsocketFrameworkItem(slotEntry.getKey());
                    returnedItems.add(socketable);
                }
            }
        }
        return returnedItems;
    }
}
