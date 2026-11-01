package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.MaterialItems;
import org.lazywizard.console.Console;

import java.util.List;

public class GrantCraftingMaterialsCommand extends PlayerFleetCommand {

    static final int DEFAULT_COPIES = 10;
    static final int MAX_COPIES = 1000;

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        Integer copies = ConsoleArgs.copiesOrComplain(args, DEFAULT_COPIES, MAX_COPIES);
        if (copies == null) {
            return CommandResult.BAD_SYNTAX;
        }
        List<String> materialIds = MaterialItems.ids().stream().filter(materialId -> MaterialItems.spec(materialId) != null).toList();
        for (String materialId : materialIds) {
            MaterialItems.add(playerFleet.getCargo(), materialId, copies);
        }
        Console.showMessage("Added " + copies + " of each crafting material to your cargo: "
                + String.join(", ", materialIds.stream().map(MaterialItems::displayName).toList()) + ".");
        return CommandResult.SUCCESS;
    }
}
