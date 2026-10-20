package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.npc.NpcLevelCaps;
import exiledsector.skills.npc.NpcLevelCategory;
import exiledsector.skills.npc.NpcTreeSize;
import exiledsector.skills.progression.SkillNodeOpCost;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class PlayerTreePower {

    private static final Logger LOG = Logger.getLogger(PlayerTreePower.class);
    private static final float REFRESH_DAYS = 1f;
    private static final AtomicReference<Snapshot> SNAPSHOT = new AtomicReference<>();

    private record Snapshot(List<Integer> shipPowers, long timestamp) {
    }

    private PlayerTreePower() {
    }

    public static List<Integer> current() {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return List.of();
        }
        Snapshot snapshot = SNAPSHOT.get();
        if (snapshot != null && sector.getClock() != null && sector.getClock().getElapsedDaysSince(snapshot.timestamp()) < REFRESH_DAYS) {
            return snapshot.shipPowers();
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        List<FleetMemberAPI> members = playerFleet == null || playerFleet.getFleetData() == null
                ? List.of() : playerFleet.getFleetData().getMembersListCopy();
        List<Integer> shipPowers = of(members, ShipTreeSync.currentLevelFloor());
        if (sector.getClock() != null) {
            SNAPSHOT.set(new Snapshot(shipPowers, sector.getClock().getTimestamp()));
        }
        return shipPowers;
    }

    public static void invalidate() {
        SNAPSHOT.set(null);
    }

    public static void logNpcScaling() {
        List<Integer> shipPowers = current();
        if (shipPowers.isEmpty()) {
            return;
        }
        NpcTreeSize.PercentRange percentRange = NpcTreeSize.percentRange();
        int fewestNodes = NpcTreeSize.nodesFor(shipPowers.get(0), percentRange.min());
        int mostNodes = NpcTreeSize.nodesFor(shipPowers.get(shipPowers.size() - 1), percentRange.max());
        LOG.info("[ExiledSector] Your combat ships' tree power is " + shipPowers + ", so new NPC trees get " + fewestNodes + " to " + mostNodes
                + " nodes (" + percentRange.min() + " to " + percentRange.max() + " percent of a random ship's tree power), capped at "
                + categoryCaps() + ".");
    }

    private static String categoryCaps() {
        List<String> capEntries = new ArrayList<>();
        for (NpcLevelCategory category : NpcLevelCategory.values()) {
            int levelCap = NpcLevelCaps.capOf(category);
            capEntries.add(category.csvId() + " " + levelCap + " (bonuses x" + NpcBonusScale.current(levelCap) + ")");
        }
        return String.join(", ", capEntries);
    }

    static List<Integer> of(List<FleetMemberAPI> members, int levelFloor) {
        List<Integer> shipPowers = new ArrayList<>();
        for (FleetMemberAPI member : members) {
            if (member.isFighterWing() || member.isCivilian() || member.isMothballed()) {
                continue;
            }
            shipPowers.add(shipPower(ShipSkillDataManager.find(member.getId()), levelFloor, SkillNodeOpCost.perNode(member.getHullSpec())));
        }
        if (shipPowers.isEmpty()) {
            shipPowers.add(Math.max(0, levelFloor));
        }
        shipPowers.sort(null);
        return List.copyOf(shipPowers);
    }

    static int shipPower(ShipSkillData shipData, int levelFloor, int opCostPerNode) {
        int level = Math.max(shipData == null ? 0 : shipData.getLevel(), levelFloor);
        if (shipData == null) {
            return level;
        }
        if (opCostPerNode <= 0) {
            return Math.max(level, allocatedBesidesRoot(shipData));
        }
        return level + shipData.paidNodeCount();
    }

    private static int allocatedBesidesRoot(ShipSkillData shipData) {
        String startingRoot = shipData.resolveStartingRootId();
        int allocated = shipData.getAllocatedNodeIds().size();
        return startingRoot != null && shipData.getAllocatedNodeIds().contains(startingRoot) ? allocated - 1 : allocated;
    }

    public static float average(List<Integer> shipPowers) {
        if (shipPowers.isEmpty()) {
            return 0f;
        }
        long total = 0;
        for (int shipPower : shipPowers) {
            total += shipPower;
        }
        return (float) total / shipPowers.size();
    }
}
