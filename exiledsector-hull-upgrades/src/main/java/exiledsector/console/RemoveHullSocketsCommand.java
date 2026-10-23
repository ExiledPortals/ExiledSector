package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.HullUpgradePoints;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;

public class RemoveHullSocketsCommand implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        String query = args.trim();
        if (query.isEmpty()) {
            return CommandResult.BAD_SYNTAX;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            Console.showMessage("No player fleet found.");
            return CommandResult.ERROR;
        }
        FleetMemberAPI member = ConsoleShips.findOne(playerFleet.getFleetData().getMembersListCopy(), query);
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
