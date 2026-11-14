package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public abstract class SingleShipCommand extends PlayerFleetCommand {

    @Override
    protected final CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        if (args.isEmpty()) {
            return CommandResult.BAD_SYNTAX;
        }
        FleetMemberAPI member = ConsoleShips.findOne(playerFleet.getFleetData().getMembersListCopy(), args);
        if (member == null) {
            return CommandResult.ERROR;
        }
        return runForShip(member);
    }

    protected abstract CommandResult runForShip(FleetMemberAPI member);
}
