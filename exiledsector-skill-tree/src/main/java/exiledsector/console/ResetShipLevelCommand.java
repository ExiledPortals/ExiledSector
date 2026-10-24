package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.PlayerFleet;
import exiledsector.items.NodeGrantStatus;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import org.lazywizard.console.Console;

import java.util.List;

public class ResetShipLevelCommand extends PlayerFleetCommand {

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        if (args.isEmpty()) {
            return CommandResult.BAD_SYNTAX;
        }
        FleetMemberAPI member = ConsoleShips.findOne(playerFleet.getFleetData().getMembersListCopy(), args);
        if (member == null) {
            return CommandResult.ERROR;
        }

        ShipSkillData shipData = ShipSkillDataManager.get(member.getId());
        List<String> socketedItemIds = List.copyOf(shipData.getSocketedItems().values());
        List<String> releasedNodeIds = ShipTrees.resetProgress(shipData);
        socketedItemIds.forEach(NodeGrantStatus::forget);
        ShipTrees.takeItemCharges(shipData, releasedNodeIds).forEach(charged -> charged.refundTo(PlayerFleet.cargoOrNull()));
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
