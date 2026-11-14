package exiledsector.console;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.HullUpgradePoints;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import org.lazywizard.console.Console;

import java.util.List;

public class LockSectionsCommand extends SingleShipCommand {

    @Override
    protected CommandResult runForShip(FleetMemberAPI member) {
        ShipSkillData shipData = ShipSkillDataManager.get(member.getId());
        int sectionCount = shipData.getUnlockedSectionIds().size();
        int pointsBefore = HullUpgradePoints.of(shipData);
        List<String> returnedItemIds = shipData.clearHullUpgrades();
        ConsoleShips.treeChanged(member);
        Console.showMessage("Locked " + sectionCount + " sections on " + ConsoleShips.describe(member) + " and set its Hull Upgrade points from "
                + pointsBefore + " to " + HullUpgradePoints.of(shipData) + "; " + returnedItemIds.size() + " installed items went back to storage.");
        return CommandResult.SUCCESS;
    }
}
