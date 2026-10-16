package exiledsector.effects;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

final class DormantNodeWake {

    private static final Logger LOG = Logger.getLogger(DormantNodeWake.class);

    private DormantNodeWake() {
    }

    static boolean wake(FleetMemberAPI member) {
        ShipSkillData shipData = ShipSkillDataManager.find(member.getId());
        Map<String, SkillNode> tree = SkillTree.getAllNodes();
        if (shipData == null || !shipData.hasReturningDormantNodes(tree)) {
            return false;
        }
        ShipVariantAPI variant = member.getVariant();
        ShipOpBudget opBudget = ShipOpBudget.of(member, variant);
        int opCostPerNode = SkillNodeOpCost.perNode(member.getHullSpec());
        int totalOp = OpReserveParity.availableOp(opBudget, OpReserveParity.reservedOp(variant));
        ShipFacts shipFacts = PlayerShipFacts.of(member, variant);
        List<String> releasedNodeIds = shipData.wakeDormantNodes(tree, ShipLevelConfig.maxAllocatedNodes(),
                dormantNode -> fitsTheOpBudget(shipData, dormantNode, opCostPerNode, totalOp) && isEligible(shipData, dormantNode, shipFacts));
        refund(shipData, releasedNodeIds);
        if (!releasedNodeIds.isEmpty()) {
            LOG.info("[ExiledSector] Released hidden-area nodes of ship " + member.getId()
                    + " that no longer connect to its tree, fit its OP or node cap, or are allowed on it: " + releasedNodeIds);
        }
        member.setStatUpdateNeeded(true);
        return true;
    }

    private static boolean fitsTheOpBudget(ShipSkillData shipData, SkillNode dormantNode, int opCostPerNode, int totalOp) {
        return shipData.isFreeNode(dormantNode.getId()) || shipData.getBankedFreeAllocations() > 0
                || shipData.getSpentOp(opCostPerNode) + opCostPerNode <= totalOp;
    }

    private static boolean isEligible(ShipSkillData shipData, SkillNode dormantNode, ShipFacts shipFacts) {
        SkillType option = dormantNode.getType().isOptional() ? SkillTree.getType(shipData.getOptionalSelection(dormantNode.getId())) : null;
        NodeEligibility.Context context = NodeEligibility.Context.of(shipData, shipFacts, SkillTypeUnlockStatus.lockedFor(shipData),
                itemId -> Double.MAX_VALUE);
        return NodeEligibility.check(dormantNode, option, context) == null;
    }

    private static void refund(ShipSkillData shipData, List<String> releasedNodeIds) {
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        shipData.takeItemCharges(releasedNodeIds).forEach(charged -> charged.refundTo(cargo));
    }
}
