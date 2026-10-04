package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.SkillTreeInstaller;
import exiledsector.skills.progression.ShipLevelSystem;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GrantShipXpCommand implements BaseCommand {

    private static final float DEFAULT_XP = 1000f;

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        String query = args.trim();
        if (query.isEmpty()) {
            return CommandResult.BAD_SYNTAX;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            Console.showMessage("No player fleet found.");
            return CommandResult.ERROR;
        }
        List<FleetMemberAPI> members = playerFleet.getFleetData().getMembersListCopy();

        float xp = DEFAULT_XP;
        List<FleetMemberAPI> matches = matching(members, query);
        int lastSpace = query.lastIndexOf(' ');
        Float amount = lastSpace > 0 ? parseAmount(query.substring(lastSpace + 1)) : null;
        if (matches.isEmpty() && amount != null) {
            xp = amount;
            query = query.substring(0, lastSpace).trim();
            matches = matching(members, query);
        }

        if (matches.isEmpty()) {
            Console.showMessage("No ship in your fleet matches \"" + query + "\".");
            return CommandResult.ERROR;
        }
        if (matches.size() > 1) {
            List<String> names = new ArrayList<>();
            matches.forEach(member -> names.add(describe(member)));
            Console.showMessage("Several ships match \"" + query + "\": " + String.join(", ", names)
                    + ". Use more of the ship's name.");
            return CommandResult.ERROR;
        }

        FleetMemberAPI member = matches.get(0);
        SkillTreeInstaller.adoptNpcTrees(playerFleet);
        ShipLevelSystem.awardXpToMember(member, xp);
        Console.showMessage("Granted " + (int) xp + " XP to " + describe(member) + ".");
        return CommandResult.SUCCESS;
    }

    private static List<FleetMemberAPI> matching(List<FleetMemberAPI> members, String query) {
        String wanted = query.toLowerCase(Locale.ROOT);
        List<FleetMemberAPI> exact = new ArrayList<>();
        List<FleetMemberAPI> partial = new ArrayList<>();
        for (FleetMemberAPI member : members) {
            String name = member.getShipName() == null ? "" : member.getShipName().toLowerCase(Locale.ROOT);
            if (name.equals(wanted) || member.getId().equalsIgnoreCase(query)) {
                exact.add(member);
            } else if (name.contains(wanted)) {
                partial.add(member);
            }
        }
        return exact.isEmpty() ? partial : exact;
    }

    private static Float parseAmount(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String describe(FleetMemberAPI member) {
        return member.getShipName() + " (" + member.getHullSpec().getHullName() + ")";
    }
}
