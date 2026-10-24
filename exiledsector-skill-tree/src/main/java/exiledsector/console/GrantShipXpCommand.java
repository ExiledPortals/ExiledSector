package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.ShipTreeSync;
import exiledsector.skills.progression.ShipLevelSystem;
import org.lazywizard.console.Console;

import java.util.List;

public class GrantShipXpCommand extends PlayerFleetCommand {

    private static final float DEFAULT_XP = 1000f;

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        String query = args;
        if (query.isEmpty()) {
            return CommandResult.BAD_SYNTAX;
        }
        List<FleetMemberAPI> fleetMembers = playerFleet.getFleetData().getMembersListCopy();

        float xp = DEFAULT_XP;
        List<FleetMemberAPI> matchingMembers = ConsoleShips.matching(fleetMembers, query);
        int lastSpace = query.lastIndexOf(' ');
        Float trailingXpAmount = lastSpace > 0 ? parseAmount(query.substring(lastSpace + 1)) : null;
        if (matchingMembers.isEmpty() && trailingXpAmount != null) {
            xp = trailingXpAmount;
            query = query.substring(0, lastSpace).trim();
        }

        FleetMemberAPI matchedMember = ConsoleShips.findOne(fleetMembers, query);
        if (matchedMember == null) {
            return CommandResult.ERROR;
        }
        ShipTreeSync.fleetChanged(playerFleet);
        ShipLevelSystem.awardXpToMember(matchedMember, xp);
        ShipTreeSync.levelsChanged(playerFleet, List.of(matchedMember));
        Console.showMessage("Granted " + (int) xp + " XP to " + ConsoleShips.describe(matchedMember) + ".");
        return CommandResult.SUCCESS;
    }

    private static Float parseAmount(String amountText) {
        try {
            return Float.parseFloat(amountText);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
