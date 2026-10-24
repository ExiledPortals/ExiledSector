package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemType;
import org.lazywizard.console.Console;

import java.util.Collection;
import java.util.Random;

public class GrantHullItemsCommand extends PlayerFleetCommand {

    static final int DEFAULT_COPIES = 5;
    static final int MAX_COPIES = 200;

    private final Random random;

    public GrantHullItemsCommand() {
        this(new Random());
    }

    GrantHullItemsCommand(Random random) {
        this.random = random;
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        Integer copies = ConsoleArgs.count(args, DEFAULT_COPIES, 1, MAX_COPIES);
        if (copies == null) {
            Console.showMessage("Give a number of copies from 1 to " + MAX_COPIES + ".");
            return CommandResult.BAD_SYNTAX;
        }
        Collection<ItemDefinition> hullItems = ItemDefinitions.ofType(ItemType.HULL_ITEM);
        if (hullItems.isEmpty()) {
            Console.showMessage("No hull items are defined.");
            return CommandResult.ERROR;
        }
        ConsoleItems.grantRolled(playerFleet.getCargo(), hullItems, copies, random);
        Console.showMessage("Added " + counted(copies, "copy", "copies") + " of each of " + counted(hullItems.size(), "hull item", "hull items")
                + " to your cargo.");
        return CommandResult.SUCCESS;
    }
}
