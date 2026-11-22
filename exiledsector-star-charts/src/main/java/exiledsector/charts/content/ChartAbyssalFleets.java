package exiledsector.charts.content;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickParams;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import exiledsector.data.WeightedPick;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

public final class ChartAbyssalFleets {

    public static final Set<String> FACTION_IDS = Set.of(ChartThreatFleets.FACTION_ID, ChartDwellerFleets.FACTION_ID);
    static final String FLEET_TYPE_KEY = "$fleetType";
    static final float SECOND_LEADER_POINTS = 375f;
    static final int MOST_SHIPS = 60;
    static final int MOST_FAILED_PICKS = 3;
    static final List<String> WRECK_FACTIONS = List.of(Factions.TRITACHYON, Factions.PIRATES, Factions.INDEPENDENT, Factions.MERCENARY);

    record Makeup(String factionId, String leaderRole, List<Map.Entry<String, Float>> escortRoles) {
    }

    private ChartAbyssalFleets() {
    }

    static boolean builds(String factionId) {
        return FACTION_IDS.contains(factionId);
    }

    static CampaignFleetAPI create(FleetParamsV3 params) {
        return ChartThreatFleets.builds(params.factionId) ? ChartThreatFleets.create(params) : ChartDwellerFleets.create(params);
    }

    static String wreckFaction(String factionId, Random random) {
        return builds(factionId) ? WRECK_FACTIONS.get(random.nextInt(WRECK_FACTIONS.size())) : factionId;
    }

    static String fleetType(String chartFleetType) {
        if (FleetTypes.TASK_FORCE.equals(chartFleetType)) {
            return FleetTypes.PATROL_LARGE;
        }
        return FleetTypes.PATROL_LARGE.equals(chartFleetType) ? FleetTypes.PATROL_MEDIUM : FleetTypes.PATROL_SMALL;
    }

    static int leaders(float combatPoints, boolean boss) {
        if (!boss) {
            return 0;
        }
        return combatPoints >= SECOND_LEADER_POINTS ? 2 : 1;
    }

    static void fill(Makeup makeup, float combatPoints, boolean boss, Random random, ToDoubleFunction<String> addShips) {
        for (int i = 0; i < leaders(combatPoints, boss); i++) {
            addShips.applyAsDouble(makeup.leaderRole());
        }
        List<Map.Entry<String, Float>> roles = new ArrayList<>(makeup.escortRoles());
        float spent = 0f;
        int failedPicks = 0;
        for (int picks = 0; picks < MOST_SHIPS && spent < combatPoints && !roles.isEmpty(); picks++) {
            Map.Entry<String, Float> role = WeightedPick.pick(roles, Map.Entry::getValue, random);
            float added = (float) addShips.applyAsDouble(role.getKey());
            if (added > 0f) {
                spent += added;
            } else if (++failedPicks >= MOST_FAILED_PICKS) {
                roles.remove(role);
                failedPicks = 0;
            }
        }
    }

    static CampaignFleetAPI build(Makeup makeup, FleetParamsV3 params, BiConsumer<CampaignFleetAPI, FleetMemberAPI> onAdded) {
        Random random = params.random != null ? params.random : new Random();
        FactionAPI faction = Global.getSector().getFaction(makeup.factionId());
        String fleetType = fleetType(params.fleetType);
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(makeup.factionId(), faction.getFleetTypeName(fleetType), true);
        fleet.setInflater(null);
        fleet.getMemoryWithoutUpdate().set(FLEET_TYPE_KEY, fleetType);
        ShipPickParams pickParams = new ShipPickParams(ShipPickMode.ALL);
        pickParams.blockFallback = true;
        pickParams.maxFP = Integer.MAX_VALUE;
        fill(makeup, params.combatPts, FleetTypes.TASK_FORCE.equals(params.fleetType), random,
                role -> addShips(fleet, faction, role, pickParams, random, onAdded));
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getFleetData().sort();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }
        return fleet;
    }

    private static float addShips(CampaignFleetAPI fleet, FactionAPI faction, String role, ShipPickParams pickParams, Random random,
                                  BiConsumer<CampaignFleetAPI, FleetMemberAPI> onAdded) {
        float added = 0f;
        for (ShipRolePick pick : faction.pickShip(role, pickParams, null, random)) {
            FleetMemberAPI member = fleet.getFleetData().addFleetMember(pick.variantId);
            if (member != null) {
                added += member.getFleetPointCost();
                onAdded.accept(fleet, member);
            }
        }
        return added;
    }
}
