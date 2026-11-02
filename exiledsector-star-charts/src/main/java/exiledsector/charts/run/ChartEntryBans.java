package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.EnumSet;
import java.util.Set;

public final class ChartEntryBans {

    private ChartEntryBans() {
    }

    public static Set<HullSize> breached(ChartRun run, CampaignFleetAPI fleet) {
        return breached(ChartRunEffects.contextOf(run).bannedCombatHullSizes(), fleet);
    }

    static Set<HullSize> breached(Set<HullSize> banned, CampaignFleetAPI fleet) {
        if (banned.isEmpty() || fleet == null || fleet.getFleetData() == null) {
            return Set.of();
        }
        Set<HullSize> breached = EnumSet.noneOf(HullSize.class);
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            HullSize hullSize = combatHullSize(member);
            if (hullSize != null && banned.contains(hullSize)) {
                breached.add(hullSize);
            }
        }
        return breached;
    }

    static HullSize combatHullSize(FleetMemberAPI member) {
        if (member == null || member.isFighterWing() || member.isCivilian() || member.getHullSpec() == null) {
            return null;
        }
        return member.getHullSpec().getHullSize();
    }
}
