package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemData;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.Random;

public class GrantUniqueHullItemsCommand implements BaseCommand {

    private final Random random;

    public GrantUniqueHullItemsCommand() {
        this(new Random());
    }

    GrantUniqueHullItemsCommand(Random random) {
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
        for (ItemDefinition definition : ItemDefinitions.all()) {
            if (definition.unique()) {
                playerFleet.getCargo().addSpecial(ItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
                grantedUniqueCount++;
            }
        }
        if (grantedUniqueCount == 0) {
            Console.showMessage("No unique items are defined.");
            return CommandResult.ERROR;
        }

        Console.showMessage("Added one copy of each of " + grantedUniqueCount + (grantedUniqueCount == 1 ? " unique item" : " unique items")
                + " to your cargo.");
        return CommandResult.SUCCESS;
    }
}
