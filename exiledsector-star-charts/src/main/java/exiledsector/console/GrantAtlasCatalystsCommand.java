package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.items.MaterialItems;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GrantAtlasCatalystsCommand extends PlayerFleetCommand {

    static final int DEFAULT_COPIES = 1;
    static final int MAX_COPIES = 100;
    static final String ID_PREFIX = "exiledSector_";

    record GrantArguments(int copies, AtlasCatalysts.Catalyst only, String problem) {

        static GrantArguments parse(String args) {
            List<String> tokens = ConsoleArgs.tokens(args);
            if (tokens.size() > 2) {
                return problem("Give at most a number of copies and one catalyst id.");
            }
            Integer copies = null;
            AtlasCatalysts.Catalyst only = null;
            for (String token : tokens) {
                if (token.chars().allMatch(Character::isDigit)) {
                    if (copies != null) {
                        return problem("Give the number of copies once.");
                    }
                    copies = ConsoleArgs.wholeNumber(token, 1, MAX_COPIES);
                    if (copies == null) {
                        return problem("The number of copies must be a whole number from 1 to " + MAX_COPIES + ".");
                    }
                } else {
                    if (only != null) {
                        return problem("Give one catalyst id at most.");
                    }
                    only = catalyst(token);
                    if (only == null) {
                        return problem("No loaded Atlas Catalyst is called " + token + ". Loaded: " + String.join(", ", shortIds()) + ".");
                    }
                }
            }
            return new GrantArguments(copies == null ? DEFAULT_COPIES : copies, only, null);
        }

        private static GrantArguments problem(String problem) {
            return new GrantArguments(0, null, problem);
        }
    }

    static AtlasCatalysts.Catalyst catalyst(String text) {
        String wanted = text.toLowerCase(Locale.ROOT);
        for (AtlasCatalysts.Catalyst catalyst : AtlasCatalysts.all()) {
            String itemId = catalyst.itemId().toLowerCase(Locale.ROOT);
            if (itemId.equals(wanted) || itemId.equals(ID_PREFIX.toLowerCase(Locale.ROOT) + wanted)) {
                return catalyst;
            }
        }
        return null;
    }

    static List<String> shortIds() {
        List<String> ids = new ArrayList<>();
        for (AtlasCatalysts.Catalyst catalyst : AtlasCatalysts.all()) {
            String itemId = catalyst.itemId();
            ids.add(itemId.startsWith(ID_PREFIX) ? itemId.substring(ID_PREFIX.length()) : itemId);
        }
        return ids;
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        if (AtlasCatalysts.all().isEmpty()) {
            Console.showMessage("No Atlas Catalysts are loaded.");
            return CommandResult.ERROR;
        }
        GrantArguments arguments = GrantArguments.parse(args);
        if (arguments.problem() != null) {
            Console.showMessage(arguments.problem() + "\nUsage: ExiledSectorGrantAtlasCatalysts [copies] [catalyst id], copies 1 to " + MAX_COPIES
                    + " (default " + DEFAULT_COPIES + "), every loaded catalyst unless one id is given.");
            return CommandResult.BAD_SYNTAX;
        }
        List<AtlasCatalysts.Catalyst> granted = arguments.only() == null ? List.copyOf(AtlasCatalysts.all()) : List.of(arguments.only());
        List<String> names = new ArrayList<>();
        for (AtlasCatalysts.Catalyst catalyst : granted) {
            MaterialItems.add(playerFleet.getCargo(), catalyst.itemId(), arguments.copies());
            names.add(MaterialItems.displayName(catalyst.itemId()));
        }
        Console.showMessage("Added " + arguments.copies() + " of each Atlas Catalyst to your cargo: " + String.join(", ", names)
                + ". Open the atlas and its Catalysts window to use them.");
        return CommandResult.SUCCESS;
    }
}
