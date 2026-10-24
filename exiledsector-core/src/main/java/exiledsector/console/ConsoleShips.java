package exiledsector.console;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.PlayerFleet;
import exiledlib.skills.skilleffect.logistics.FleetWideEffects;
import exiledsector.ExiledSector;
import exiledsector.effects.FighterBayOverflow;
import exiledsector.effects.UnfitWeapons;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ConsoleShips {

    private ConsoleShips() {
    }

    static FleetMemberAPI findOne(List<FleetMemberAPI> members, String query) {
        List<FleetMemberAPI> matchingMembers = matching(members, query);
        if (matchingMembers.isEmpty()) {
            Console.showMessage("No ship in your fleet matches \"" + query + "\".");
            return null;
        }
        if (matchingMembers.size() > 1) {
            List<String> shipDescriptions = new ArrayList<>();
            matchingMembers.forEach(member -> shipDescriptions.add(describe(member)));
            Console.showMessage("Several ships match \"" + query + "\": " + String.join(", ", shipDescriptions)
                    + ". Use more of the ship's name.");
            return null;
        }
        return matchingMembers.get(0);
    }

    static List<FleetMemberAPI> matching(List<FleetMemberAPI> members, String query) {
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

    static String describe(FleetMemberAPI member) {
        return member.getShipName() + " (" + member.getHullSpec().getHullName() + ")";
    }

    static void treeChanged(FleetMemberAPI member) {
        ExiledSector.domains().forEach(domain -> domain.memberChanged(member, null));
        FleetWideEffects.markPhaseFieldStale();
        member.setStatUpdateNeeded(true);
        member.updateStats();
        if (member.getFleetData() != null && member.getFleetData().getFleet() == PlayerFleet.orNull()) {
            FighterBayOverflow.returnUnhousedWingsAndAnnounce(member, member.getVariant(), PlayerFleet.cargoOrNull());
            UnfitWeapons.returnUnfitWeaponsAndAnnounce(member, member.getVariant(), PlayerFleet.cargoOrNull());
        }
    }
}
