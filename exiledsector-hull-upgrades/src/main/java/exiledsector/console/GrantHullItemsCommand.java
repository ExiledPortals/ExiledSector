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

public class GrantHullItemsCommand extends RandomPlayerFleetCommand {

    static final int DEFAULT_AMOUNT = 1;
    static final int MAX_AMOUNT = 200;

    public GrantHullItemsCommand() {
    }

    GrantHullItemsCommand(Random random) {
        super(random);
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        List<String> tokens = ConsoleArgs.tokens(args);
        if (tokens.size() > 2) {
            return CommandResult.BAD_SYNTAX;
        }
        ItemKind sectionKind = null;
        if (!tokens.isEmpty()) {
            sectionKind = ItemKind.byIdOrNull(tokens.get(0).toLowerCase(Locale.ROOT));
            if (sectionKind == null || !sectionKind.fitsSection()) {
                Console.showMessage("Unknown section \"" + tokens.get(0) + "\"; give one of " + sectionKindIds() + ".");
                return CommandResult.BAD_SYNTAX;
            }
        }
        Integer amount = tokens.size() == 2 ? ConsoleArgs.wholeNumber(tokens.get(1), 1, MAX_AMOUNT) : Integer.valueOf(DEFAULT_AMOUNT);
        if (amount == null) {
            Console.showMessage("Give an amount from 1 to " + MAX_AMOUNT + ".");
            return CommandResult.BAD_SYNTAX;
        }
        ItemKind wantedSection = sectionKind;
        List<ItemDefinition> hullItems = ItemDefinitions.ofType(ItemType.HULL_ITEM).stream()
                .filter(definition -> !definition.unique())
                .filter(definition -> wantedSection == null || definition.kind() == wantedSection)
                .toList();
        if (hullItems.isEmpty()) {
            Console.showMessage(sectionKind == null ? "No non-unique hull items are defined."
                    : "No non-unique hull items are defined for the " + sectionKind.id() + " section.");
            return CommandResult.ERROR;
        }
        ConsoleItems.grantRolled(playerFleet.getCargo(), hullItems, amount, random);
        Console.showMessage("Added " + counted(amount, "copy", "copies") + " of each of " + counted(hullItems.size(), "hull item", "hull items")
                + " to your cargo.");
        return CommandResult.SUCCESS;
    }

    private static String sectionKindIds() {
        return ItemKind.sectionItemTypes().stream().map(ItemKind::id).collect(Collectors.joining(", "));
    }
}
