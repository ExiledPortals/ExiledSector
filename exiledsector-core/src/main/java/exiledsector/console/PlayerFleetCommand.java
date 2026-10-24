package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledlib.PlayerFleet;
import org.lazywizard.console.Console;

public abstract class PlayerFleetCommand extends CampaignCommand {

    static final String NO_PLAYER_FLEET = "No player fleet found.";

    @Override
    protected final CommandResult runInCampaign(String args) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (playerFleet == null) {
            Console.showMessage(NO_PLAYER_FLEET);
            return CommandResult.ERROR;
        }
        return runForFleet(args, playerFleet);
    }

    protected abstract CommandResult runForFleet(String args, CampaignFleetAPI playerFleet);
}
