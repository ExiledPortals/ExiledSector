package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.HullUpgradePoints;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import org.lazywizard.console.Console;

import java.util.List;

public class RemoveHullSocketsCommand extends PlayerFleetCommand {

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
        int socketCount = shipData.getUnlockedSocketTypeIds().size();
        int pointsBefore = HullUpgradePoints.of(shipData);
        List<String> returnedItemIds = shipData.clearHullUpgrades();
        ConsoleShips.treeChanged(member);
        Console.showMessage("Removed " + socketCount + " hull sockets from " + ConsoleShips.describe(member) + " and set its Hull Upgrade points from "
                + pointsBefore + " to " + HullUpgradePoints.of(shipData) + "; " + returnedItemIds.size() + " installed items went back to storage.");
        return CommandResult.SUCCESS;
    }
}
