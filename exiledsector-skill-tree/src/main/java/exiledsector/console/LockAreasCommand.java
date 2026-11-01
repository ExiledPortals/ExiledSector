package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.items.NodeGrantStatus;
import exiledsector.items.SocketCustody;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ItemChargeLedger;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.unlock.NodeGates;
import exiledsector.skills.unlock.RelockedGates;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.skills.unlock.UnlockConditionType;
import org.lazywizard.console.Console;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LockAreasCommand extends CampaignCommand {

    @Override
    protected CommandResult runInCampaign(String args) {
        Map<String, FleetMemberAPI> ownedShipsById = SocketCustody.ownedShips();
        Map<String, ShipSkillData> shipDataById = ShipSkillDataManager.all();
        Map<String, Set<String>> lockedBeforeByShipId = new HashMap<>();
        ownedShipsById.keySet().forEach(shipId -> {
            ShipSkillData shipData = shipDataById.get(shipId);
            if (shipData != null) {
                lockedBeforeByShipId.put(shipId, lockedAllocatedNodeIds(shipData));
            }
        });

        List<UnlockConditionType> relockedGates = RelockedGates.relockOpenAreas(Global.getSector());
        if (relockedGates.isEmpty()) {
            Console.showMessage("No unlocked area to lock.");
            return CommandResult.SUCCESS;
        }

        int releasedNodeCount = 0;
        int changedShipCount = 0;
        for (Map.Entry<String, Set<String>> lockedBefore : lockedBeforeByShipId.entrySet()) {
            ShipSkillData shipData = shipDataById.get(lockedBefore.getKey());
            Set<String> newlyLocked = lockedAllocatedNodeIds(shipData);
            newlyLocked.removeAll(lockedBefore.getValue());
            if (newlyLocked.isEmpty()) {
                continue;
            }
            releasedNodeCount += release(shipData, newlyLocked);
            changedShipCount++;
            ConsoleShips.treeChanged(ownedShipsById.get(lockedBefore.getKey()));
        }
        Console.showMessage("Locked " + relockedGates + " again. Released " + releasedNodeCount + " nodes from "
                + changedShipCount + " ships and refunded their costs. Areas reopen when their trigger happens again, or with ExiledSectorUnlockAreas.");
        return CommandResult.SUCCESS;
    }

    private static Set<String> lockedAllocatedNodeIds(ShipSkillData shipData) {
        Set<String> lockedNodeIds = new HashSet<>();
        Map<String, SkillNode> nodesById = SkillTree.getAllNodes();
        for (String nodeId : shipData.getAllocatedNodeIds()) {
            SkillNode node = nodesById.get(nodeId);
            if (node != null && (NodeGates.isLocked(node, shipData) || SkillTypeUnlockStatus.isLocked(node.getType(), shipData))) {
                lockedNodeIds.add(nodeId);
            }
        }
        return lockedNodeIds;
    }

    private static int release(ShipSkillData shipData, Set<String> lockedNodeIds) {
        int allocatedBefore = shipData.getAllocatedNodeIds().size();
        Set<String> socketedItemIdsBefore = new HashSet<>(shipData.getSocketedItems().values());
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        String startingRootId = ShipTrees.resolveStartingRootId(shipData);
        if (startingRootId == null || lockedNodeIds.contains(startingRootId)) {
            ItemChargeLedger.takeItemCharges(shipData, ShipTrees.resetAllocations(shipData)).forEach(charge -> charge.refundTo(cargo));
        } else {
            SkillTreeTopology topology = SkillTree.topology();
            List<SkillNode> lockedNodes = lockedNodeIds.stream().map(topology::node).filter(Objects::nonNull).toList();
            ShipTrees.release(shipData, topology, startingRootId, lockedNodes).refundTo(cargo);
        }
        socketedItemIdsBefore.removeAll(shipData.getSocketedItems().values());
        socketedItemIdsBefore.forEach(NodeGrantStatus::forget);
        return allocatedBefore - shipData.getAllocatedNodeIds().size();
    }
}
