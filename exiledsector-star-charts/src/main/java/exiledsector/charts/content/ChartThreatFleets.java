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
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager;
import com.fs.starfarer.api.impl.combat.threat.ThreatFIDConfig;
import exiledsector.i18n.Translation;
import exiledsector.items.WeightedPick;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToDoubleFunction;

final class ChartThreatFleets {

    static final String FACTION_ID = "threat";
    static final String FABRICATOR_ROLE = "threatFabricator";
    static final String HIVE_ROLE = "threatHive";
    static final String OVERSEER_ROLE = "threatOverseer";
    static final String FLEET_TYPE_KEY = "$fleetType";
    static final List<Map.Entry<String, Float>> ESCORT_ROLES = List.of(Map.entry(ShipRoles.COMBAT_SMALL, 4f), Map.entry(ShipRoles.COMBAT_MEDIUM, 3f),
            Map.entry(ShipRoles.COMBAT_LARGE, 2f), Map.entry(OVERSEER_ROLE, 1.5f), Map.entry(HIVE_ROLE, 1f));
    static final float SECOND_FABRICATOR_POINTS = 1500f;
    static final int MOST_SHIPS = 60;
    static final int MOST_FAILED_PICKS = 3;
    static final List<String> WRECK_FACTIONS = List.of(Factions.TRITACHYON, Factions.PIRATES, Factions.INDEPENDENT, Factions.MERCENARY);

    private ChartThreatFleets() {
    }

    static boolean builds(String factionId) {
        return FACTION_ID.equals(factionId);
    }

    static String wreckFaction(String factionId, Random random) {
        return builds(factionId) ? WRECK_FACTIONS.get(random.nextInt(WRECK_FACTIONS.size())) : factionId;
    }

    static String threatFleetType(String chartFleetType) {
        if (FleetTypes.TASK_FORCE.equals(chartFleetType)) {
            return FleetTypes.PATROL_LARGE;
        }
        return FleetTypes.PATROL_LARGE.equals(chartFleetType) ? FleetTypes.PATROL_MEDIUM : FleetTypes.PATROL_SMALL;
    }

    static int fabricators(float combatPoints, boolean boss) {
        if (!boss) {
            return 0;
        }
        return combatPoints >= SECOND_FABRICATOR_POINTS ? 2 : 1;
    }

    static void fill(float combatPoints, boolean boss, Random random, ToDoubleFunction<String> addShips) {
        for (int i = 0; i < fabricators(combatPoints, boss); i++) {
            addShips.applyAsDouble(FABRICATOR_ROLE);
        }
        List<Map.Entry<String, Float>> roles = new ArrayList<>(ESCORT_ROLES);
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

    static CampaignFleetAPI create(FleetParamsV3 params) {
        Random random = params.random != null ? params.random : new Random();
        FactionAPI faction = Global.getSector().getFaction(FACTION_ID);
        String fleetType = threatFleetType(params.fleetType);
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(FACTION_ID, faction.getFleetTypeName(fleetType), true);
        fleet.setInflater(null);
        fleet.getMemoryWithoutUpdate().set(FLEET_TYPE_KEY, fleetType);
        ShipPickParams pickParams = new ShipPickParams(ShipPickMode.ALL);
        pickParams.blockFallback = true;
        pickParams.maxFP = Integer.MAX_VALUE;
        fill(params.combatPts, FleetTypes.TASK_FORCE.equals(params.fleetType), random, role -> addShips(fleet, faction, role, pickParams, random));
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getFleetData().sort();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, new ThreatFIDConfig());
        fleet.getDetectedRangeMod().modifyMult(DisposableThreatFleetManager.THREAT_DETECTED_RANGE_MULT_ID,
                DisposableThreatFleetManager.THREAT_DETECTED_RANGE_MULT, Translation.gameText("chart.fleet.threatStealth"));
        return fleet;
    }

    private static float addShips(CampaignFleetAPI fleet, FactionAPI faction, String role, ShipPickParams pickParams, Random random) {
        float added = 0f;
        for (ShipRolePick pick : faction.pickShip(role, pickParams, null, random)) {
            FleetMemberAPI member = fleet.getFleetData().addFleetMember(pick.variantId);
            if (member != null) {
                added += member.getFleetPointCost();
            }
        }
        return added;
    }
}
