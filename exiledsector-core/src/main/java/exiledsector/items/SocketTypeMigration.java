package exiledsector.items;

import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledlib.items.Item;
import exiledlib.items.ItemStore;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledlib.ui.CampaignMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SocketTypeMigration {

    private SocketTypeMigration() {
    }

    public static void run() {
        List<Item> returnedItems = returnMisfits(ShipSkillDataManager.all(), ItemStore.get());
        SocketCustody.custodies().forEach(custody -> custody.enforceLimits(ShipSkillDataManager.all(), ItemStore.get()));
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
            for (ShipCustody custody : SocketCustody.custodies()) {
                returnedItems.addAll(custody.returnMisfits(shipData, store));
            }
        }
        return returnedItems;
    }
}
