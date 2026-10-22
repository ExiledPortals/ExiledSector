package exiledsector.skills.npc;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.Random;

public final class NpcShipSelector {

    private NpcShipSelector() {
    }

    public static boolean isCandidate(FleetMemberAPI member) {
        return member != null && !member.isFighterWing() && !member.isStation() && !member.isCivilian()
                && !member.isMothballed() && member.getVariant() != null;
    }

    public static boolean isChosen(FleetMemberAPI member, Random random) {
        boolean chanceRoll = random.nextFloat() < NpcTreeConfig.otherShipChance();
        if (NpcTreeConfig.levelsOfficeredShips() && hasOfficer(member)) {
            return true;
        }
        if (NpcTreeConfig.levelsFlagship() && member.isFlagship()) {
            return true;
        }
        return chanceRoll;
    }

    private static boolean hasOfficer(FleetMemberAPI member) {
        PersonAPI captain = member.getCaptain();
        return captain != null && !captain.isDefault();
    }
}
