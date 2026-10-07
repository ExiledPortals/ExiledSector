package exiledsector.skills.progression;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public final class ShipOpBudget {

    public final int totalOp;
    public final int usedOp;

    private ShipOpBudget(int totalOp, int usedOp) {
        this.totalOp = totalOp;
        this.usedOp = usedOp;
    }

    public static ShipOpBudget of(FleetMemberAPI member, ShipVariantAPI variant) {
        MutableCharacterStatsAPI commanderStats = commanderStats(member);
        int totalOp = member.getHullSpec().getOrdnancePoints(commanderStats);
        int usedOp = variant.computeOPCost(commanderStats);
        return new ShipOpBudget(totalOp, usedOp);
    }

    private static MutableCharacterStatsAPI commanderStats(FleetMemberAPI member) {
        PersonAPI commander = member.getFleetCommanderForStats();
        if (commander == null) commander = member.getFleetCommander();
        if (commander == null) commander = member.getCaptain();
        return commander != null ? commander.getStats() : null;
    }
}
