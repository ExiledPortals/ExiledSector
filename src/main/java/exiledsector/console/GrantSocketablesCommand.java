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

public class GrantSocketablesCommand implements BaseCommand {

    static final int DEFAULT_COPIES = 5;
    static final int MAX_COPIES = 200;

    private final Random random;

    public GrantSocketablesCommand() {
        this(new Random());
    }

    GrantSocketablesCommand(Random random) {
        this.random = random;
    }

    private static int parseCopies(String args) {
        if (args.isEmpty()) {
            return DEFAULT_COPIES;
        }
        try {
            return Integer.parseInt(args.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        int copies = parseCopies(args);
        if (copies < 1) {
            return CommandResult.BAD_SYNTAX;
        }
        if (copies > MAX_COPIES) {
            Console.showMessage("At most " + MAX_COPIES + " copies of each socketable can be granted at once.");
            return CommandResult.BAD_SYNTAX;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            Console.showMessage("No player fleet found.");
            return CommandResult.ERROR;
        }
        if (SocketableDefinitions.all().isEmpty()) {
            Console.showMessage("No socketables are defined.");
            return CommandResult.ERROR;
        }

        int grantedDefinitionCount = 0;
        for (SocketableDefinition definition : SocketableDefinitions.all()) {
            for (int i = 0; i < copies; i++) {
                playerFleet.getCargo().addSpecial(SocketableItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
            }
            grantedDefinitionCount++;
        }

        Console.showMessage("Added " + copies + (copies == 1 ? " copy" : " copies") + " of each of " + grantedDefinitionCount
                + (grantedDefinitionCount == 1 ? " socketable" : " socketables") + " to your cargo.");
        return CommandResult.SUCCESS;
    }
}
