package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableDefinitions;
import exiledsector.socketables.SocketableItemData;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.Random;

public class GrantUniqueSocketablesCommand implements BaseCommand {

    private final Random random;

    public GrantUniqueSocketablesCommand() {
        this(new Random());
    }

    GrantUniqueSocketablesCommand(Random random) {
        this.random = random;
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        if (!args.isBlank()) {
            return CommandResult.BAD_SYNTAX;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            Console.showMessage("No player fleet found.");
            return CommandResult.ERROR;
        }

        int grantedUniqueCount = 0;
        for (SocketableDefinition definition : SocketableDefinitions.all()) {
            if (definition.unique()) {
                playerFleet.getCargo().addSpecial(SocketableItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
                grantedUniqueCount++;
            }
        }
        if (grantedUniqueCount == 0) {
            Console.showMessage("No unique socketables are defined.");
            return CommandResult.ERROR;
        }

        Console.showMessage("Added one copy of each of " + grantedUniqueCount + (grantedUniqueCount == 1 ? " unique socketable" : " unique socketables")
                + " to your cargo.");
        return CommandResult.SUCCESS;
    }
}
