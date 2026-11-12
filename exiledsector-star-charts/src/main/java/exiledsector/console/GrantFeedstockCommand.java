package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.InfusedFeedstock;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.MaterialItems;
import org.lazywizard.console.Console;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GrantFeedstockCommand extends PlayerFleetCommand {

    static final int MAX_AMOUNT = 100000;
    static final Map<String, String> FEEDSTOCK_IDS_BY_TYPE = feedstockIdsByType();

    record GrantArguments(String feedstockId, int amount, String problem) {

        static GrantArguments parse(String args) {
            List<String> tokens = ConsoleArgs.tokens(args);
            if (tokens.size() != 2) {
                return problem("Give one feedstock type and an amount.");
            }
            String feedstockId = FEEDSTOCK_IDS_BY_TYPE.get(tokens.get(0).toLowerCase(Locale.ROOT));
            if (feedstockId == null) {
                return problem("No feedstock type is called " + tokens.get(0) + ".");
            }
            Integer amount = ConsoleArgs.wholeNumber(tokens.get(1), 1, MAX_AMOUNT);
            if (amount == null) {
                return problem("The amount must be a whole number from 1 to " + MAX_AMOUNT + ".");
            }
            return new GrantArguments(feedstockId, amount, null);
        }

        private static GrantArguments problem(String problem) {
            return new GrantArguments(null, 0, problem);
        }
    }

    private static Map<String, String> feedstockIdsByType() {
        Map<String, String> idsByType = new LinkedHashMap<>();
        idsByType.put("nanite", ItemDisassembly.FEEDSTOCK_ITEM_ID);
        for (InfusedFeedstock feedstock : InfusedFeedstock.values()) {
            idsByType.put(feedstock.name().toLowerCase(Locale.ROOT), feedstock.itemId());
        }
        return Collections.unmodifiableMap(idsByType);
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        GrantArguments arguments = GrantArguments.parse(args);
        if (arguments.problem() != null) {
            Console.showMessage(arguments.problem() + "\nUsage: ExiledSectorGrantFeedstock <type> <amount>, type one of "
                    + String.join(", ", FEEDSTOCK_IDS_BY_TYPE.keySet()) + ", amount 1 to " + MAX_AMOUNT + ".");
            return CommandResult.BAD_SYNTAX;
        }
        MaterialItems.add(playerFleet.getCargo(), arguments.feedstockId(), arguments.amount());
        Console.showMessage("Added " + arguments.amount() + " " + MaterialItems.displayName(arguments.feedstockId()) + " to your cargo.");
        return CommandResult.SUCCESS;
    }
}
