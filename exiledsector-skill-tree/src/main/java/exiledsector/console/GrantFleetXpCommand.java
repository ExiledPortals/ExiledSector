package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.effects.ShipTreeSync;
import exiledsector.skills.progression.ShipLevelSystem;
import org.lazywizard.console.Console;

public class GrantFleetXpCommand extends PlayerFleetCommand {

    private static final float DEFAULT_XP = 1000f;

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        float xp;
        if (args.isEmpty()) {
            xp = DEFAULT_XP;
        } else {
            try {
                xp = Float.parseFloat(args);
            } catch (NumberFormatException e) {
                return CommandResult.BAD_SYNTAX;
            }
        }

        int shipCount = playerFleet.getFleetData().getMembersListCopy().size();
        ShipTreeSync.fleetChanged(playerFleet);
        ShipLevelSystem.awardXpToFleet(playerFleet, xp);
        ShipTreeSync.levelsChanged(playerFleet, playerFleet.getFleetData().getMembersListCopy());

        Console.showMessage("Granted " + (int) xp + " XP to " + counted(shipCount, "ship", "ships") + " in the fleet.");
        return CommandResult.SUCCESS;
    }
}
