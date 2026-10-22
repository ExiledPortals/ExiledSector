package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.EnumMap;
import java.util.Map;

public final class ChartFleetConditions {

    public static final String MOD_ID = ChartIds.PREFIX + "fleet_conditions";

    private static Map<ChartFleetStat, Float> activeMults = Map.of();
    private static Map<ChartFleetStat, Float> lastSource = Map.of();
    private static int battleSizeBonus;

    private ChartFleetConditions() {
    }

    public static Map<ChartFleetStat, Float> active() {
        return activeMults;
    }

    public static boolean isActive() {
        return !activeMults.isEmpty() || battleSizeBonus != 0;
    }

    public static int battleSizeBonus() {
        return battleSizeBonus;
    }

    static void setBattleSizeBonus(int points) {
        battleSizeBonus = points;
    }

    public static float mult(ChartFleetStat stat) {
        return activeMults.getOrDefault(stat, 1f);
    }

    static boolean set(Map<ChartFleetStat, Float> mults, CampaignFleetAPI playerFleet) {
        Map<ChartFleetStat, Float> next = mults == null || mults.isEmpty() ? Map.of() : mults;
        if (next == lastSource) {
            return false;
        }
        lastSource = next;
        if (next.equals(activeMults)) {
            return false;
        }
        activeMults = next.isEmpty() ? Map.of() : new EnumMap<>(next);
        markForStatUpdate(playerFleet);
        return true;
    }

    public static boolean clear(CampaignFleetAPI playerFleet) {
        battleSizeBonus = 0;
        return set(Map.of(), playerFleet);
    }

    public static void applyToShip(MutableShipStatsAPI stats) {
        if (activeMults.isEmpty() || stats == null || !isPlayerFleetMember(stats.getFleetMember())) {
            return;
        }
        apply(stats);
    }

    public static void applyToFighter(ShipAPI fighter, ShipAPI ship) {
        if (activeMults.isEmpty() || fighter == null || ship == null || !isPlayerFleetMember(ship.getMutableStats().getFleetMember())) {
            return;
        }
        apply(fighter.getMutableStats());
    }

    private static void apply(MutableShipStatsAPI stats) {
        for (Map.Entry<ChartFleetStat, Float> entry : activeMults.entrySet()) {
            entry.getKey().applyToShip(stats, MOD_ID, entry.getValue());
        }
    }

    static boolean isPlayerFleetMember(FleetMemberAPI member) {
        FleetDataAPI fleetData = member == null ? null : member.getFleetData();
        CampaignFleetAPI fleet = fleetData == null ? null : fleetData.getFleet();
        return fleet != null && fleet.isPlayerFleet();
    }

    private static void markForStatUpdate(CampaignFleetAPI playerFleet) {
        FleetDataAPI fleetData = playerFleet == null ? null : playerFleet.getFleetData();
        if (fleetData == null) {
            return;
        }
        for (FleetMemberAPI member : fleetData.getMembersListCopy()) {
            member.setStatUpdateNeeded(true);
        }
    }
}
