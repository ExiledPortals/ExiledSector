package exiledsector.charts.anomaly;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.charts.ChartIds;
import exiledsector.charts.loot.ChartFleetLedger;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class SettlingDustFleets {

    static final String ADDED_FLEET_POINTS_KEY = "$exiledSector_settlingDustAddedFp";
    static final int MOST_SHIPS_ADDED_AT_ONCE = 30;
    private static final float PERCENT = 100f;

    private SettlingDustFleets() {
    }

    static void grow(AnomalyRun anomalyRun, float sizePercent) {
        if (sizePercent <= 0f || anomalyRun.system() == null) {
            return;
        }
        for (CampaignFleetAPI fleet : new ArrayList<>(anomalyRun.system().getFleets())) {
            if (grows(fleet, anomalyRun.run())) {
                growTo(fleet, sizePercent);
            }
        }
    }

    static boolean grows(CampaignFleetAPI fleet, ChartRun run) {
        MemoryAPI memory = fleet == null ? null : fleet.getMemoryWithoutUpdate();
        return memory != null && !fleet.isPlayerFleet() && !fleet.isStationMode() && !fleet.isDespawning() && fleet.getFleetData() != null
                && memory.getBoolean(ChartIds.fleetRunKey(run.runId())) && memory.getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE)
                && memory.contains(ChartIds.START_FP_MEMORY_KEY) && !ChartFleetLedger.lootSpent(fleet) && !ChartFleetLedger.isBoss(fleet, run);
    }

    static void growTo(CampaignFleetAPI fleet, float sizePercent) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        float addedFleetPoints = memory.contains(ADDED_FLEET_POINTS_KEY) ? memory.getFloat(ADDED_FLEET_POINTS_KEY) : 0f;
        float startFleetPoints = memory.getFloat(ChartIds.START_FP_MEMORY_KEY) - addedFleetPoints;
        float missingFleetPoints = startFleetPoints * sizePercent / PERCENT - addedFleetPoints;
        Random namesRandom = new Random(fleet.getId() == null ? 0L : fleet.getId().hashCode());
        for (int added = 0; added < MOST_SHIPS_ADDED_AT_ONCE; added++) {
            FleetMemberAPI source = pickSource(fleet.getFleetData().getMembersListCopy(), missingFleetPoints);
            if (source == null) {
                break;
            }
            FleetMemberAPI copy = addCopy(fleet, source, namesRandom);
            missingFleetPoints -= copy.getFleetPointCost();
            addedFleetPoints += copy.getFleetPointCost();
            raise(memory, ChartIds.START_FP_MEMORY_KEY, copy.getFleetPointCost());
            raise(memory, ChartIds.START_DP_MEMORY_KEY, copy.getDeploymentPointsCost());
        }
        memory.set(ADDED_FLEET_POINTS_KEY, addedFleetPoints);
        fleet.getFleetData().sort();
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
    }

    static FleetMemberAPI pickSource(List<FleetMemberAPI> members, float missingFleetPoints) {
        FleetMemberAPI largestFitting = null;
        FleetMemberAPI smallest = null;
        for (FleetMemberAPI member : members) {
            int fleetPoints = member.getFleetPointCost();
            if (member.isFlagship() || member.isCivilian() || member.isFighterWing() || member.isStation() || fleetPoints <= 0) {
                continue;
            }
            if (fleetPoints <= missingFleetPoints && (largestFitting == null || fleetPoints > largestFitting.getFleetPointCost())) {
                largestFitting = member;
            }
            if (smallest == null || fleetPoints < smallest.getFleetPointCost()) {
                smallest = member;
            }
        }
        if (largestFitting != null) {
            return largestFitting;
        }
        return smallest != null && smallest.getFleetPointCost() <= missingFleetPoints * 2f ? smallest : null;
    }

    private static FleetMemberAPI addCopy(CampaignFleetAPI fleet, FleetMemberAPI source, Random namesRandom) {
        FleetMemberAPI copy = Global.getFactory().createFleetMember(FleetMemberType.SHIP, source.getVariant().clone());
        copy.setShipName(fleet.getFleetData().pickShipName(copy, namesRandom));
        copy.getRepairTracker().setCR(source.getRepairTracker().getCR());
        fleet.getFleetData().addFleetMember(copy);
        return copy;
    }

    private static void raise(MemoryAPI memory, String key, float added) {
        memory.set(key, memory.getFloat(key) + added);
    }
}
