package exiledsector.console;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.CargoRefund;
import exiledsector.items.NodeGrantStatus;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ItemCostLedger;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import org.lazywizard.console.Console;

import java.util.List;

public class ResetShipLevelCommand extends SingleShipCommand {

    @Override
    protected CommandResult runForShip(FleetMemberAPI member) {
        ShipSkillData shipData = ShipSkillDataManager.get(member.getId());
        List<String> socketedItemIds = List.copyOf(shipData.getSocketedItems().values());
        List<String> releasedNodeIds = ShipTrees.resetProgress(shipData);
        socketedItemIds.forEach(NodeGrantStatus::forget);
        ItemCostLedger.takePaidItemCosts(shipData, releasedNodeIds).forEach(CargoRefund::refundToPlayer);
        ConsoleShips.treeChanged(member);

        String message = "Reset " + ConsoleShips.describe(member) + ": removed " + releasedNodeIds.size()
                + " nodes and cleared its level and XP.";
        if (shipData.getLevel() > 0) {
            message += " The minimum ship level setting raised it straight back to level " + shipData.getLevel() + ".";
        }
        Console.showMessage(message);
        return CommandResult.SUCCESS;
    }
}
