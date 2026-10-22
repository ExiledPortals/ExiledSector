package exiledsector.items;

import com.fs.starfarer.api.Global;
import exiledlib.PlayerFleet;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ReleasedNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.tags.SkillTags;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class WormholeAnchorMigration {

    static final String DONE_KEY = "exiledSector_wormholeAnchorMigrationDone";
    private static final Logger LOG = Logger.getLogger(WormholeAnchorMigration.class);

    record Summary(int shipCount, int nodeCount) {
    }

    private WormholeAnchorMigration() {
    }

    public static void run() {
        if (!SkillTree.isLoadedCompletely()) {
            return;
        }
        WormholeAnchorLinks.refresh();
        Summary summary = runOnce(Global.getSector().getPersistentData(), ShipSkillDataManager.all(), SkillTree.topology(),
                SocketCustody.ownedShipIds()::contains, charged -> charged.refundTo(PlayerFleet.cargoOrNull()));
        if (summary != null) {
            LOG.info("[ExiledSector] Wormhole anchor migration: released " + summary.nodeCount() + " faction volume nodes on "
                    + summary.shipCount() + " ships that the removed wormholes used to reach, refunding their OP");
        }
    }

    static Summary runOnce(Map<String, Object> persistentData, Map<String, ShipSkillData> shipDataById, SkillTreeTopology topology,
                           Predicate<String> isOwnedShip, Consumer<SkillItemCost> refund) {
        if (Boolean.TRUE.equals(persistentData.get(DONE_KEY))) {
            return null;
        }
        Summary summary = releaseStrandedVolumes(shipDataById, topology, isOwnedShip, refund);
        persistentData.put(DONE_KEY, Boolean.TRUE);
        return summary;
    }

    static Summary releaseStrandedVolumes(Map<String, ShipSkillData> shipDataById, SkillTreeTopology topology, Predicate<String> isOwnedShip,
                                          Consumer<SkillItemCost> refund) {
        int shipCount = 0;
        int nodeCount = 0;
        for (Map.Entry<String, ShipSkillData> shipEntry : shipDataById.entrySet()) {
            ShipSkillData shipData = shipEntry.getValue();
            ReleasedNodes released = ShipTrees.releaseUnreachable(shipData, topology, ShipTrees.resolveStartingRootId(shipData, topology.sortedById()),
                    node -> !SkillTags.CORE_REGION.equals(node.getRegion()));
            if (released.isEmpty()) {
                continue;
            }
            shipCount++;
            nodeCount += released.nodeIds().size();
            if (isOwnedShip.test(shipEntry.getKey())) {
                released.charges().forEach(refund);
            }
        }
        return new Summary(shipCount, nodeCount);
    }
}
