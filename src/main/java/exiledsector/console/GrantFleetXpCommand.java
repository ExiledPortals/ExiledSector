package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.effects.ShipTreeSync;
import exiledsector.skills.progression.ShipLevelSystem;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class GrantFleetXpCommand implements BaseCommand {

    private static final float DEFAULT_XP = 1000f;

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        float xp;
        if (args.isEmpty()) {
            xp = DEFAULT_XP;
        } else {
            try {
                xp = Float.parseFloat(args.trim());
            } catch (NumberFormatException e) {
                return CommandResult.BAD_SYNTAX;
            }
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            Console.showMessage("No player fleet found.");
            return CommandResult.ERROR;
        }

        int shipCount = playerFleet.getFleetData().getMembersListCopy().size();
        ShipTreeSync.fleetChanged(playerFleet);
        ShipLevelSystem.awardXpToFleet(playerFleet, xp);
        ShipTreeSync.levelsChanged(playerFleet, playerFleet.getFleetData().getMembersListCopy());

        Console.showMessage("Granted " + (int) xp + " XP to " + shipCount
                + (shipCount == 1 ? " ship" : " ships") + " in the fleet.");
        return CommandResult.SUCCESS;
    }
}
