package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.ShipTreeSync;
import exiledsector.skills.progression.ShipLevelSystem;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;

public class GrantShipXpCommand implements BaseCommand {

    private static final float DEFAULT_XP = 1000f;

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
        List<FleetMemberAPI> fleetMembers = playerFleet.getFleetData().getMembersListCopy();

        float xp = DEFAULT_XP;
        List<FleetMemberAPI> matchingMembers = ConsoleShips.matching(fleetMembers, query);
        int lastSpace = query.lastIndexOf(' ');
        Float trailingXpAmount = lastSpace > 0 ? parseAmount(query.substring(lastSpace + 1)) : null;
        if (matchingMembers.isEmpty() && trailingXpAmount != null) {
            xp = trailingXpAmount;
            query = query.substring(0, lastSpace).trim();
            matchingMembers = ConsoleShips.matching(fleetMembers, query);
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
