package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.officer.scar.ScarTable;
import exiledsector.charts.officer.scar.ScarText;
import exiledsector.charts.officer.scar.Scars;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GrantScarsCommand extends PlayerFleetCommand {

    static final String PLAYER_TARGET = "player";
    static final String NO_SCARS = "none";
    static final String STACK_SEPARATOR = ":";
    static final String USAGE = "Usage: ExiledSectorGrantScars <scar_id>[:stacks] ... [player], or none to clear.";

    record GrantArguments(Map<String, Integer> scars, boolean onPlayer, String problem) {

        static GrantArguments parse(String args) {
            List<String> tokens = new ArrayList<>(ConsoleArgs.tokens(args));
            boolean onPlayer = tokens.remove(PLAYER_TARGET);
            if (tokens.isEmpty()) {
                return problem("Give at least one scar id, or none.");
            }
            Map<String, Integer> scars = new LinkedHashMap<>();
            if (tokens.size() == 1 && NO_SCARS.equalsIgnoreCase(tokens.get(0))) {
                return new GrantArguments(scars, onPlayer, null);
            }
            for (String token : tokens) {
                int separator = token.indexOf(STACK_SEPARATOR);
                String scarId = (separator < 0 ? token : token.substring(0, separator)).toLowerCase(Locale.ROOT);
                Integer stacks = separator < 0 ? Integer.valueOf(1) : ConsoleArgs.wholeNumber(token.substring(separator + 1), 1, ScarTable.STACK_CAP * 10);
                if (!ScarTable.exists(scarId)) {
                    return problem("No scar is called " + scarId + ".");
                }
                if (stacks == null) {
                    return problem("Stacks for " + scarId + " must be a whole number from 1 to " + ScarTable.STACK_CAP * 10 + ".");
                }
                scars.merge(scarId, stacks, Integer::sum);
            }
            return new GrantArguments(scars, onPlayer, null);
        }

        private static GrantArguments problem(String problem) {
            return new GrantArguments(Map.of(), false, problem);
        }
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        GrantArguments arguments = GrantArguments.parse(args);
        if (arguments.problem() != null) {
            Console.showMessage(arguments.problem() + "\n" + USAGE);
            return CommandResult.BAD_SYNTAX;
        }
        CampaignFleetAPI fleet = arguments.onPlayer() ? playerFleet : nearestOtherFleet(playerFleet);
        if (fleet == null || fleet.getFleetData() == null) {
            Console.showMessage("No other fleet is in this location.");
            return CommandResult.ERROR;
        }
        FleetMemberAPI flagship = fleet.getFlagship();
        if (flagship == null) {
            Console.showMessage(fleet.getName() + " has no flagship.");
            return CommandResult.ERROR;
        }
        Scars.grant(fleet, flagship.getId(), arguments.scars());
        Console.showMessage("Set the scars of " + flagship.getShipName() + " in " + fleet.getName() + " to "
                + describe(arguments.scars()) + ". They apply the next time its stats are rebuilt.");
        return CommandResult.SUCCESS;
    }

    static String describe(Map<String, Integer> scars) {
        if (scars.isEmpty()) {
            return NO_SCARS;
        }
        List<String> parts = new ArrayList<>();
        scars.forEach((scarId, stacks) -> parts.add(ScarText.name(scarId) + " x" + stacks));
        return String.join(", ", parts);
    }

    static CampaignFleetAPI nearestOtherFleet(CampaignFleetAPI playerFleet) {
        if (playerFleet.getContainingLocation() == null) {
            return null;
        }
        CampaignFleetAPI nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (CampaignFleetAPI fleet : playerFleet.getContainingLocation().getFleets()) {
            if (fleet == playerFleet || fleet.getFlagship() == null) {
                continue;
            }
            float distance = Misc.getDistance(playerFleet.getLocation(), fleet.getLocation());
            if (distance < nearestDistance) {
                nearest = fleet;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
