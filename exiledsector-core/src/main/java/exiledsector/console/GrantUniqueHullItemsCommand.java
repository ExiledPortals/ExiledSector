package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Random;

public class GrantUniqueHullItemsCommand extends RandomPlayerFleetCommand {

    public GrantUniqueHullItemsCommand() {
    }

    GrantUniqueHullItemsCommand(Random random) {
        super(random);
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        if (!args.isEmpty()) {
            return CommandResult.BAD_SYNTAX;
        }
        List<ItemDefinition> uniques = ItemDefinitions.all().stream().filter(ItemDefinition::unique).toList();
        if (uniques.isEmpty()) {
            Console.showMessage("No unique items are defined.");
            return CommandResult.ERROR;
        }
        ConsoleItems.grantRolled(playerFleet.getCargo(), uniques, 1, random);
        Console.showMessage("Added one copy of each of " + counted(uniques.size(), "unique item", "unique items") + " to your cargo.");
        return CommandResult.SUCCESS;
    }
}
