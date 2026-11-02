package exiledsector.charts.effect;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.ChartIds;
import exiledsector.effects.PlayerFleetStats;
import exiledsector.effects.SkillTreeHullMod;

import java.util.EnumMap;
import java.util.Map;

public final class ChartPlayerModifiers {

    public static final String MOD_ID = ChartIds.PREFIX + "fleet_conditions";

    private static Map<ChartFleetStat, Float> activeMults = Map.of();
    private static Map<ChartFleetStat, Float> lastSource = Map.of();
    private static int battleSizeBonus;
    private static boolean shipSystemsDisabled;
    private static boolean autopilotForced;
    private static float arcStrikeEmp;
    private static float weaponMisfireRate;
    private static float damagePercentPerSMod;

    private ChartPlayerModifiers() {
    }

    public static Map<ChartFleetStat, Float> active() {
        return activeMults;
    }

    public static boolean isActive() {
        return !activeMults.isEmpty() || battleSizeBonus != 0 || shipSystemsDisabled || autopilotForced || arcStrikeEmp > 0f || weaponMisfireRate > 0f
                || damagePercentPerSMod != 0f;
    }

    public static float arcStrikeEmp() {
        return arcStrikeEmp;
    }

    public static void setArcStrikeEmp(float emp) {
        arcStrikeEmp = emp;
    }

    public static float weaponMisfireRate() {
        return weaponMisfireRate;
    }

    public static void setWeaponMisfireRate(float misfiresPerSecond) {
        weaponMisfireRate = misfiresPerSecond;
    }

    public static float damagePercentPerSMod() {
        return damagePercentPerSMod;
    }

    public static void setDamagePercentPerSMod(float percent) {
        damagePercentPerSMod = percent;
    }

    public static boolean autopilotForced() {
        return autopilotForced;
    }

    public static void setAutopilotForced(boolean forced) {
        autopilotForced = forced;
    }

    public static boolean shipSystemsDisabled() {
        return shipSystemsDisabled;
    }

    public static void setShipSystemsDisabled(boolean disabled) {
        shipSystemsDisabled = disabled;
    }

    public static int battleSizeBonus() {
        return battleSizeBonus;
    }

    public static void setBattleSizeBonus(int points) {
        battleSizeBonus = points;
    }

    public static float mult(ChartFleetStat stat) {
        return activeMults.getOrDefault(stat, 1f);
    }

    public static boolean set(Map<ChartFleetStat, Float> mults, CampaignFleetAPI playerFleet) {
        Map<ChartFleetStat, Float> next = mults == null || mults.isEmpty() ? Map.of() : mults;
        if (next == lastSource) {
            return false;
        }
        lastSource = next;
        if (next.equals(activeMults)) {
            return false;
        }
        activeMults = next.isEmpty() ? Map.of() : new EnumMap<>(next);
        PlayerFleetStats.markForStatUpdate(playerFleet);
        return true;
    }

    public static boolean clear(CampaignFleetAPI playerFleet) {
        battleSizeBonus = 0;
        shipSystemsDisabled = false;
        autopilotForced = false;
        arcStrikeEmp = 0f;
        weaponMisfireRate = 0f;
        damagePercentPerSMod = 0f;
        return set(Map.of(), playerFleet);
    }

    public static void afterShipCreation(ShipAPI ship) {
        if (!shipSystemsDisabled && damagePercentPerSMod == 0f || ship == null || !isPlayerFleetMember(SkillTreeHullMod.memberOf(ship))) {
            return;
        }
        if (shipSystemsDisabled) {
            ship.setShipSystemDisabled(true);
        }
        addSModDamageListener(ship, ship.getVariant());
    }

    public static void applyToShip(MutableShipStatsAPI stats) {
        if (activeMults.isEmpty() || stats == null || !isPlayerFleetMember(stats.getFleetMember())) {
            return;
        }
        apply(stats);
    }

    public static void applyToFighter(ShipAPI fighter, ShipAPI ship) {
        if (activeMults.isEmpty() && damagePercentPerSMod == 0f || fighter == null || ship == null
                || !isPlayerFleetMember(ship.getMutableStats().getFleetMember())) {
            return;
        }
        if (!activeMults.isEmpty()) {
            apply(fighter.getMutableStats());
        }
        addSModDamageListener(fighter, ship.getVariant());
    }

    private static void addSModDamageListener(ShipAPI ship, ShipVariantAPI sModSource) {
        ChartSModDamageListener listener = ChartSModDamageListener.of(damagePercentPerSMod, sModSource);
        if (listener != null && !ship.hasListenerOfClass(ChartSModDamageListener.class)) {
            ship.addListener(listener);
        }
    }

    private static void apply(MutableShipStatsAPI stats) {
        for (Map.Entry<ChartFleetStat, Float> entry : activeMults.entrySet()) {
            if (entry.getKey().appliesToShips()) {
                entry.getKey().applyToShip(stats, MOD_ID, entry.getValue());
            }
        }
    }

    static boolean isPlayerFleetMember(FleetMemberAPI member) {
        CampaignFleetAPI fleet = sourceFleetOf(member);
        return fleet != null && fleet.isPlayerFleet();
    }

    static CampaignFleetAPI sourceFleetOf(FleetMemberAPI member) {
        if (member == null) {
            return null;
        }
        FleetDataAPI fleetData = member.getFleetDataForStats() != null ? member.getFleetDataForStats() : member.getFleetData();
        return fleetData == null ? null : fleetData.getFleet();
    }
}
