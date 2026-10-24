package exiledsector.console;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

final class ConsoleItems {

    private ConsoleItems() {
    }

    static List<ItemData> grantRolled(CargoAPI cargo, Collection<ItemDefinition> definitions, int copies, Random random) {
        List<ItemData> granted = new ArrayList<>();
        for (ItemDefinition definition : definitions) {
            for (int copy = 0; copy < copies; copy++) {
                ItemData item = ItemData.rolled(definition, random.nextLong());
                cargo.addSpecial(item.toSpecialItem(), 1f);
                granted.add(item);
            }
        }
        return granted;
    }
}
