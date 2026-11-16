package exiledsector.charts.anomaly.listeningpost;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartFleetMarks;
import exiledsector.charts.content.ChartFleetVisibility;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.loot.ChartFleetLedger;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class Pickets {

    static final String PICKET_MEMORY_KEY = "$exiledSector_chartPicket";
    static final String GUARD_MEMORY_KEY = "$exiledSector_chartPostGuard";
    static final String REPORT_MEMORY_KEY = "$exiledSector_chartPicketReport";
    static final String HUNTING_MEMORY_KEY = "$exiledSector_chartPicketHunting";
    static final String SENSOR_MOD_ID = ChartIds.PREFIX + "picket_sensors";
    static final String REVEAL_MOD_ID = ChartIds.PREFIX + "picket_reveal";

    private Pickets() {
    }

    static ChartAnomalyFleets.Spec spec(String factionId, ChartTier tier) {
        return spec(factionId, tier, false);
    }

    static ChartAnomalyFleets.Spec spec(String factionId, ChartTier tier, boolean garrison) {
        return ChartAnomalyFleets.combat(factionId, garrison ? FleetTypes.PATROL_LARGE : FleetTypes.PATROL_SMALL, tier,
                ListeningPostRules.picketSize(tier, garrison), ListeningPostRules.picketSizeCap(garrison));
    }

    static CampaignFleetAPI spawn(AnomalyContentSite site, PicketStation station, Vector2f postAt, FleetEventListener listener, Random random) {
        ChartGenContext context = site.context();
        boolean garrison = DepotTuning.of(context).garrisonPickets();
        ChartTier tier = ChartAnomalyFleets.tier(site.tier(), context);
        long fleetSeed = random.nextLong();
        CampaignFleetAPI fleet = ChartAnomalyFleets.create(spec(site.factionId(), tier, garrison), new Random(fleetSeed));
        if (fleet == null) {
            return null;
        }
        ChartAnomalyFleets.prepare(fleet, site.runId(), site.tier(), fleetSeed, true, context);
        if (garrison) {
            ChartFleetMarks.setStrengthBonus(fleet.getMemoryWithoutUpdate(),
                    ChartContentPlanner.overflowStrength(ListeningPostRules.picketSize(tier, true), ChartContentPlanner.BOSS_SIZE_CAP));
        }
        dress(fleet, station.index());
        Vector2f at = station.around(postAt);
        site.addFleet(fleet, at);
        SectorEntityToken point = site.system().createToken(at.x, at.y);
        station.setPoint(point);
        station.setFleetId(fleet.getId());
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, point, ChartContentGenerator.ASSIGNMENT_DAYS);
        fleet.addEventListener(listener);
        return fleet;
    }

    static void dress(CampaignFleetAPI fleet, int index) {
        fleet.setName(Translation.gameText("chart.anomaly.listening_post.fleet.picket.name"));
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(PICKET_MEMORY_KEY, index);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
        memory.unset(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT);
        memory.set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        memory.set(MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED, true);
        if (fleet.getStats() != null) {
            fleet.getStats().getSensorRangeMod().modifyMult(SENSOR_MOD_ID, ListeningPostRules.PICKET_SENSOR_MULT);
        }
        ChartFleetVisibility.reveal(fleet, REVEAL_MOD_ID);
    }

    static boolean isPicket(SectorEntityToken entity) {
        return entity instanceof CampaignFleetAPI fleet && fleet.getMemoryWithoutUpdate() != null
                && fleet.getMemoryWithoutUpdate().contains(PICKET_MEMORY_KEY);
    }

    static boolean isGuard(SectorEntityToken entity) {
        return entity instanceof CampaignFleetAPI fleet && fleet.getMemoryWithoutUpdate() != null
                && fleet.getMemoryWithoutUpdate().getBoolean(GUARD_MEMORY_KEY);
    }

    static float reportDays(CampaignFleetAPI picket) {
        MemoryAPI memory = picket.getMemoryWithoutUpdate();
        return memory.contains(REPORT_MEMORY_KEY) ? memory.getFloat(REPORT_MEMORY_KEY) : -1f;
    }

    static void setReportDays(CampaignFleetAPI picket, float days) {
        picket.getMemoryWithoutUpdate().set(REPORT_MEMORY_KEY, days);
    }

    static void gather(CampaignFleetAPI picket, SectorEntityToken post) {
        MemoryAPI memory = picket.getMemoryWithoutUpdate();
        memory.unset(REPORT_MEMORY_KEY);
        picket.clearAssignments();
        picket.addAssignment(FleetAssignment.GO_TO_LOCATION, post, ChartContentGenerator.ASSIGNMENT_DAYS);
    }

    static boolean isHunting(CampaignFleetAPI picket) {
        return picket.getMemoryWithoutUpdate() != null && picket.getMemoryWithoutUpdate().getBoolean(HUNTING_MEMORY_KEY);
    }

    static void hunt(CampaignFleetAPI picket, SectorEntityToken target) {
        MemoryAPI memory = picket.getMemoryWithoutUpdate();
        memory.unset(REPORT_MEMORY_KEY);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
        memory.unset(MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED);
        memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        memory.set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
        memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        memory.set(HUNTING_MEMORY_KEY, true);
        chase(picket, target);
    }

    static void chase(CampaignFleetAPI picket, SectorEntityToken target) {
        picket.clearAssignments();
        picket.addAssignment(FleetAssignment.INTERCEPT, target, ChartContentGenerator.ASSIGNMENT_DAYS);
    }

    static void search(CampaignFleetAPI picket, SectorEntityToken post) {
        picket.clearAssignments();
        picket.addAssignment(FleetAssignment.PATROL_SYSTEM, post, ChartContentGenerator.ASSIGNMENT_DAYS);
    }

    static boolean chasing(CampaignFleetAPI picket) {
        return picket.getCurrentAssignment() != null && picket.getCurrentAssignment().getAssignment() == FleetAssignment.INTERCEPT;
    }

    static void standGuard(CampaignFleetAPI guard, SectorEntityToken post) {
        MemoryAPI memory = guard.getMemoryWithoutUpdate();
        memory.unset(PICKET_MEMORY_KEY);
        memory.unset(REPORT_MEMORY_KEY);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE);
        memory.unset(MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED);
        memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        memory.set(GUARD_MEMORY_KEY, true);
        guard.setName(Translation.gameText("chart.anomaly.listening_post.fleet.guard.name"));
        guard.clearAssignments();
        guard.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, post, ChartContentGenerator.ASSIGNMENT_DAYS);
    }

    static void merge(CampaignFleetAPI picket, CampaignFleetAPI guard) {
        MemoryAPI guardMemory = guard.getMemoryWithoutUpdate();
        MemoryAPI picketMemory = picket.getMemoryWithoutUpdate();
        if (guardMemory.contains(ChartIds.START_DP_MEMORY_KEY) && picketMemory.contains(ChartIds.START_DP_MEMORY_KEY)) {
            addTo(guardMemory, picketMemory, ChartIds.START_DP_MEMORY_KEY);
            addTo(guardMemory, picketMemory, ChartIds.START_FP_MEMORY_KEY);
            addTo(guardMemory, picketMemory, ChartIds.LOOT_DP_PAID_MEMORY_KEY);
            guardMemory.set(ChartFleetLedger.LOOT_FLEETS_MEMORY_KEY, ChartFleetLedger.lootFleets(guard) + ChartFleetLedger.lootFleets(picket));
        }
        picket.getDropRandom().forEach(guard::addDropRandom);
        picket.getDropValue().forEach(guard::addDropValue);
        for (FleetMemberAPI member : picket.getFleetData().getMembersListCopy()) {
            picket.getFleetData().removeFleetMember(member);
            member.setFlagship(false);
            guard.getFleetData().addFleetMember(member);
            if (member.getCaptain() != null && !member.getCaptain().isDefault()) {
                guard.getFleetData().addOfficer(member.getCaptain());
            }
        }
        guard.getFleetData().sort();
        guard.forceSync();
        picket.despawn(FleetDespawnReason.OTHER, null);
    }

    private static void addTo(MemoryAPI guardMemory, MemoryAPI picketMemory, String key) {
        float sum = Math.max(0f, guardMemory.getFloat(key)) + Math.max(0f, picketMemory.getFloat(key));
        if (sum > 0f) {
            guardMemory.set(key, sum);
        }
    }
}
