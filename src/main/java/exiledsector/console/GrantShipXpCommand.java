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
        List<FleetMemberAPI> fleetMembers = playerFleet.getFleetData().getMembersListCopy();

        float xp = DEFAULT_XP;
        List<FleetMemberAPI> matchingMembers = matching(fleetMembers, query);
        int lastSpace = query.lastIndexOf(' ');
        Float trailingXpAmount = lastSpace > 0 ? parseAmount(query.substring(lastSpace + 1)) : null;
        if (matchingMembers.isEmpty() && trailingXpAmount != null) {
            xp = trailingXpAmount;
            query = query.substring(0, lastSpace).trim();
            matchingMembers = matching(fleetMembers, query);
        }

        if (matchingMembers.isEmpty()) {
            Console.showMessage("No ship in your fleet matches \"" + query + "\".");
            return CommandResult.ERROR;
        }
        if (matchingMembers.size() > 1) {
            List<String> shipDescriptions = new ArrayList<>();
            matchingMembers.forEach(member -> shipDescriptions.add(describe(member)));
            Console.showMessage("Several ships match \"" + query + "\": " + String.join(", ", shipDescriptions)
                    + ". Use more of the ship's name.");
            return CommandResult.ERROR;
        }

        FleetMemberAPI matchedMember = matchingMembers.get(0);
        SkillTreeInstaller.adoptNpcTrees(playerFleet);
        ShipLevelSystem.awardXpToMember(matchedMember, xp);
        Console.showMessage("Granted " + (int) xp + " XP to " + describe(matchedMember) + ".");
        return CommandResult.SUCCESS;
    }

    private static List<FleetMemberAPI> matching(List<FleetMemberAPI> members, String query) {
        String wantedName = query.toLowerCase(Locale.ROOT);
        List<FleetMemberAPI> exactMatches = new ArrayList<>();
        List<FleetMemberAPI> partialMatches = new ArrayList<>();
        for (FleetMemberAPI member : members) {
            String shipName = member.getShipName() == null ? "" : member.getShipName().toLowerCase(Locale.ROOT);
            if (shipName.equals(wantedName) || member.getId().equalsIgnoreCase(query)) {
                exactMatches.add(member);
            } else if (shipName.contains(wantedName)) {
                partialMatches.add(member);
            }
        }
        return exactMatches.isEmpty() ? partialMatches : exactMatches;
    }

    private static Float parseAmount(String amountText) {
        try {
            return Float.parseFloat(amountText);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String describe(FleetMemberAPI member) {
        return member.getShipName() + " (" + member.getHullSpec().getHullName() + ")";
    }
}
