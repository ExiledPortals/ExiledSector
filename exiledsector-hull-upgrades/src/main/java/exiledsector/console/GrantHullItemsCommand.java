package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemType;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

public class GrantHullItemsCommand extends PlayerFleetCommand {

    static final int DEFAULT_AMOUNT = 1;
    static final int MAX_AMOUNT = 200;

    private final Random random;

    public GrantHullItemsCommand() {
        this(new Random());
    }

    GrantHullItemsCommand(Random random) {
        this.random = random;
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        String[] tokens = args.isEmpty() ? new String[0] : args.split("\\s+");
        if (tokens.length > 2) {
            return CommandResult.BAD_SYNTAX;
        }
        ItemKind socketType = null;
        if (tokens.length > 0) {
            socketType = ItemKind.byIdOrNull(tokens[0].toLowerCase(Locale.ROOT));
            if (socketType == null || !socketType.fitsSection()) {
                Console.showMessage("Unknown socket type \"" + tokens[0] + "\"; give one of " + socketTypeIds() + ".");
                return CommandResult.BAD_SYNTAX;
            }
        }
        Integer amount = tokens.length == 2 ? ConsoleArgs.wholeNumber(tokens[1], 1, MAX_AMOUNT) : Integer.valueOf(DEFAULT_AMOUNT);
        if (amount == null) {
            Console.showMessage("Give an amount from 1 to " + MAX_AMOUNT + ".");
            return CommandResult.BAD_SYNTAX;
        }
        ItemKind wantedSocketType = socketType;
        List<ItemDefinition> hullItems = ItemDefinitions.ofType(ItemType.HULL_ITEM).stream()
                .filter(definition -> !definition.unique())
                .filter(definition -> wantedSocketType == null || definition.kind() == wantedSocketType)
                .toList();
        if (hullItems.isEmpty()) {
            Console.showMessage(socketType == null ? "No non-unique hull items are defined."
                    : "No non-unique hull items are defined for the " + socketType.id() + " socket type.");
            return CommandResult.ERROR;
        }
        ConsoleItems.grantRolled(playerFleet.getCargo(), hullItems, amount, random);
        Console.showMessage("Added " + counted(amount, "copy", "copies") + " of each of " + counted(hullItems.size(), "hull item", "hull items")
                + " to your cargo.");
        return CommandResult.SUCCESS;
    }

    private static String socketTypeIds() {
        return ItemKind.sectionItemTypes().stream().map(ItemKind::id).collect(Collectors.joining(", "));
    }
}
