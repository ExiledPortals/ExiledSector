package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.effects.DoctrineRevertConflicts;
import exiledsector.effects.DoctrineRevertRelease;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.PhantomConflictWatch;
import exiledsector.effects.PlayerShipFacts;
import exiledsector.effects.ShipTreeSync;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.HullModNames;
import exiledsector.skills.ItemChargeLedger;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ReleasedNodes;
import exiledsector.skills.RespecPlan;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.CostItems;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.unlock.NodeGates;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.items.EmptyShells;
import exiledsector.items.DoctrineOverrides;
import exiledsector.items.NodeGrantStatus;
import exiledsector.ui.refit.ShipEditSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class NodeAllocator {

    private static final String BEST_OF_THE_BEST_SKILL_ID = "best_of_the_best";

    static final class Snapshot {

        private final ShipSkillData skillData;
        private final ShipOpBudget opBudget;
        private final int statsRevision;
        private final int dataRevision;
        private final Set<String> hiddenNodeIds;
        private final AllocationGate gate;
        private final RefusalDescriber describer;
        private final Map<String, String> reasonsByKey = new HashMap<>();
        private final Map<String, List<NodeGrantStatus>> grantStatusesBySocketId = new HashMap<>();

        Snapshot(ShipSkillData skillData, ShipOpBudget opBudget, int statsRevision, Set<String> hiddenNodeIds, AllocationGate gate,
                 RefusalDescriber describer) {
            this.skillData = skillData;
            this.opBudget = opBudget;
            this.statsRevision = statsRevision;
            this.dataRevision = skillData.revision();
            this.hiddenNodeIds = hiddenNodeIds;
            this.gate = gate;
            this.describer = describer;
        }

        ShipSkillData skillData() {
            return skillData;
        }

        String satisfiedRootId() {
            return gate.rootId();
        }

        ShipOpBudget opBudget() {
            return opBudget;
        }

        int totalOpBudget() {
            return gate.budget().totalOp();
        }

        int opCostPerNode() {
            return gate.budget().opCostPerNode();
        }

        int maxAllocatedNodes() {
            return gate.budget().maxAllocatedNodes();
        }

        int statsRevision() {
            return statsRevision;
        }

        AllocationGate gate() {
            return gate;
        }

        int opCostFor(SkillNode node) {
            return gate.opCostFor(node);
        }

        boolean canAllocate(SkillNode node) {
            return gate.allocation(node).allowed();
        }

        boolean isHidden(SkillNode node) {
            return hiddenNodeIds.contains(node.getId());
        }

        boolean isCurrent(ShipSkillData currentData) {
            return currentData == skillData && currentData.revision() == dataRevision;
        }

        String refusalReason(SkillNode node) {
            if (isHidden(node) || skillData.isAllocated(node.getId())) {
                return null;
            }
            return allocationRefusalReason(node, null);
        }

        String allocationRefusalReason(SkillNode node, SkillType option) {
            if (option == null) {
                return reason(node.getId(), gate.allocation(node), null);
            }
            return reason(node.getId() + '/' + option.getId(), gate.allocation(node, option), null);
        }

        String optionRefusalReason(SkillNode node, SkillType option) {
            if (!skillData.isAllocated(node.getId())) {
                return allocationRefusalReason(node, option);
            }
            return reason(node.getId() + '/' + option.getId(), gate.optionSwitch(node, option), ItemChargeLedger.itemCharge(skillData, node.getId()));
        }

        private String reason(String key, AllocationGate.Verdict verdict, SkillItemCost refund) {
            if (!verdict.worthExplaining()) {
                return null;
            }
            return reasonsByKey.computeIfAbsent(key, ignored -> describer.describe(verdict, gate, refund));
        }

        List<NodeGrantStatus> nodeGrantStatuses(String socketId) {
            return grantStatusesBySocketId.computeIfAbsent(socketId, this::computeNodeGrantStatuses);
        }

        NodeGrantStatus nodeGrantStatus(String socketId, String nodeId) {
            return NodeGrantStatus.find(nodeGrantStatuses(socketId), nodeId);
        }

        private List<NodeGrantStatus> computeNodeGrantStatuses(String socketId) {
            List<String> grantedNodeIds = ShipTreeLinks.grantedNodesBySocket(skillData).getOrDefault(socketId, List.of());
            List<NodeGrantStatus> statuses = new ArrayList<>(grantedNodeIds.size());
            for (String grantedNodeId : grantedNodeIds) {
                statuses.add(computeNodeGrantStatus(socketId, grantedNodeId));
            }
            return List.copyOf(statuses);
        }

        private NodeGrantStatus computeNodeGrantStatus(String socketId, String grantedNodeId) {
            if (SkillTree.getDeclared(grantedNodeId) == null) {
                return NodeGrantStatus.of(grantedNodeId, NodeGrantStatus.State.UNKNOWN_NODE);
            }
            SkillNode grantedNode = SkillTree.get(grantedNodeId);
            if (grantedNode == null) {
                return NodeGrantStatus.of(grantedNodeId, NodeGrantStatus.State.AREA_OFF);
            }
            if (skillData.isAllocated(grantedNodeId)) {
                if (!skillData.isGrantFree(grantedNodeId)) {
                    return NodeGrantStatus.of(grantedNodeId, NodeGrantStatus.State.BY_PATH);
                }
                String grantingSocketId = ShipTreeLinks.grantingSocket(skillData, grantedNodeId);
                return grantingSocketId == null || grantingSocketId.equals(socketId) ? NodeGrantStatus.of(grantedNodeId, NodeGrantStatus.State.ACTIVE)
                        : NodeGrantStatus.of(grantedNodeId, NodeGrantStatus.State.DUPLICATE);
            }
            AllocationGate.Verdict verdict = gate.grant(grantedNode);
            if (verdict.allowed()) {
                return NodeGrantStatus.of(grantedNodeId, NodeGrantStatus.State.READY);
            }
            return new NodeGrantStatus(grantedNodeId, NodeGrantStatus.State.INELIGIBLE, reason("grant/" + grantedNodeId, verdict, null));
        }
    }

    private final FleetMemberAPI fleetMember;
    private final ShipVariantAPI shipVariant;
    private final Supplier<SkillNode> startingRootSupplier;
    private final ShipEditSession shipEdit;
    private boolean variantHullModsRefreshed;

    NodeAllocator(FleetMemberAPI fleetMember, ShipVariantAPI shipVariant, Supplier<SkillNode> startingRootSupplier) {
        this.fleetMember = fleetMember;
        this.shipVariant = shipVariant;
        this.startingRootSupplier = startingRootSupplier;
        this.shipEdit = new ShipEditSession(fleetMember, shipVariant);
    }

    ShipSkillData data() {
        return ShipSkillDataManager.get(fleetMember.getId());
    }

    String satisfiedRootId() {
        SkillNode root = startingRootSupplier.get();
        return root == null ? null : root.getId();
    }

    private boolean isStartingRoot(SkillNode node) {
        SkillNode root = startingRootSupplier.get();
        return root != null && node.getId().equals(root.getId());
    }

    List<String> hullModNamesLostOnDoctrineRemoval(SkillNode socket) {
        return socket == null ? List.of() : DoctrineRevertConflicts.hullModNamesLostOnRemoval(data(), shipVariant, socket.getId());
    }

    Snapshot snapshot() {
        refreshVariantHullModsOnce();
        ShipSkillData skillData = data();
        ShipOpBudget opBudget = ShipOpBudget.of(fleetMember, shipVariant);
        int opCostPerNode = SkillNodeOpCost.perNode(fleetMember.getHullSpec());
        int reservedOp = OpReserveParity.reservedOp(shipVariant);
        OpReserveParity.warnIfOutOfSync(fleetMember, shipVariant, ShipTrees.getSpentOp(skillData, opCostPerNode), reservedOp, "while allocating nodes");
        int totalOpBudget = OpReserveParity.availableOp(opBudget, reservedOp);
        AllocationGate.Budget budget = new AllocationGate.Budget(totalOpBudget, opCostPerNode, ShipLevelConfig.maxAllocatedNodes());
        AllocationGate gate = AllocationGate.forShip(skillData, PlayerShipFacts.of(fleetMember, shipVariant), satisfiedRootId(), budget,
                NodeAllocator::heldInCargo);
        return new Snapshot(skillData, opBudget, shipEdit.revision(), hiddenNodeIds(skillData), gate, this::describe);
    }

    Snapshot planningSnapshot(ShipSkillData planData, Snapshot liveSnapshot) {
        ShipSkillData liveData = liveSnapshot.skillData();
        AllocationGate.Budget budget = new AllocationGate.Budget(Integer.MAX_VALUE, 0, ShipLevelConfig.maxAllocatedNodes());
        ShipFacts shipFacts = PlayerShipFacts.of(fleetMember, shipVariant);
        DoctrineOverrides.forget(planData);
        NodeEligibility.Context eligibility = NodeEligibility.Context.forShip(planData, shipFacts,
                SkillTypeUnlockStatus.lockedFor(liveData), itemId -> Double.MAX_VALUE);
        AllocationGate gate = new AllocationGate(planData, SkillTree.topology(), satisfiedRootId(), budget, eligibility);
        return new Snapshot(planData, liveSnapshot.opBudget(), liveSnapshot.statsRevision(), liveSnapshot.hiddenNodeIds, gate, this::describe);
    }

    private static Set<String> hiddenNodeIds(ShipSkillData skillData) {
        Set<String> hidden = new HashSet<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (SkillTypeUnlockStatus.isHidden(node.getType(), skillData) || NodeGates.isHidden(node, skillData)) {
                hidden.add(node.getId());
            }
        }
        return hidden;
    }

    boolean allocate(SkillNode node, SkillType option, Snapshot snapshot) {
        AllocationGate gate = snapshot.gate();
        SkillItemCost itemCost = gate.itemCost(node, option);
        if (!gate.allocation(node, option).allowed() || itemCost != null && heldInCargo(itemCost.itemId()) < itemCost.quantity()) {
            return false;
        }
        ShipSkillData skillData = snapshot.skillData();
        ShipTrees.allocate(skillData, node, option, gate.opCostFor(node));
        chargeItems(skillData, node.getId(), itemCost);
        return changed(true);
    }

    boolean switchOption(SkillNode node, SkillType option, Snapshot snapshot) {
        AllocationGate gate = snapshot.gate();
        if (!gate.optionSwitch(node, option).allowed()) {
            return false;
        }
        ShipSkillData skillData = snapshot.skillData();
        SkillItemCost refund = ItemChargeLedger.itemCharge(skillData, node.getId());
        SkillItemCost itemCost = gate.itemCost(node, option);
        if (itemCost != null && heldInCargo(itemCost.itemId()) + refundOf(refund, itemCost.itemId()) < itemCost.quantity()) {
            return false;
        }
        if (refund != null) {
            ItemChargeLedger.takeItemCharge(skillData, node.getId());
            refund.refundTo(PlayerFleet.cargoOrNull());
        }
        ShipTrees.selectOption(skillData, node, option, gate.opCostFor(node));
        chargeItems(skillData, node.getId(), itemCost);
        return changed(true);
    }

    boolean deallocate(SkillNode node, Snapshot snapshot) {
        if (!snapshot.gate().deallocation(node).allowed()) {
            return false;
        }
        ShipTrees.release(snapshot.skillData(), SkillTree.topology(), satisfiedRootId(), List.of(node)).refundTo(PlayerFleet.cargoOrNull());
        return changed(true);
    }

    boolean socketItem(SkillNode node, String itemId) {
        ShipSkillData skillData = data();
        Set<String> reachableBefore = reachable(skillData);
        if (!skillData.socketItem(node.getId(), itemId)) {
            return false;
        }
        releaseStranded(skillData, reachableBefore);
        ShipTrees.openAnchorWormholes(skillData, ShipLevelConfig.maxAllocatedNodes());
        return changed(true);
    }

    boolean unsocketItem(SkillNode node) {
        ShipSkillData skillData = data();
        Set<String> reachableBefore = reachable(skillData);
        if (skillData.unsocketItem(node.getId()) == null) {
            return false;
        }
        releaseStranded(skillData, reachableBefore);
        return changed(true);
    }

    boolean openAnchorWormholes() {
        return changed(!ShipTrees.openAnchorWormholes(data(), ShipLevelConfig.maxAllocatedNodes()).isEmpty());
    }

    boolean grantSocketedNodes() {
        ShipSkillData skillData = data();
        ReleasedNodes ungranted = ShipTrees.releaseUngrantedFreeNodes(skillData, SkillTree.topology(), satisfiedRootId());
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        ungranted.refundTo(cargo);
        boolean changedAny = !ungranted.nodeIds().isEmpty();
        Map<String, List<String>> grantedNodesBySocket = ShipTreeLinks.grantedNodesBySocket(skillData);
        if (grantedNodesBySocket.isEmpty()) {
            NodeGrantStatus.rememberForShip(fleetMember.getId(), Map.of());
            return changed(changedAny);
        }
        changedAny |= takeOverEmptyShellNodes(skillData, grantedNodesBySocket, cargo);
        Set<String> grantedNodeIds = new LinkedHashSet<>();
        grantedNodesBySocket.values().forEach(grantedNodeIds::addAll);
        Snapshot current = null;
        for (String grantedNodeId : grantedNodeIds) {
            SkillNode grantedNode = SkillTree.get(grantedNodeId);
            if (grantedNode == null || skillData.isAllocated(grantedNodeId)) {
                continue;
            }
            if (current == null) {
                current = snapshot();
            }
            if (current.gate().grant(grantedNode).allowed()) {
                ShipTrees.allocateGranted(skillData, grantedNode);
                changedAny = true;
                current = null;
            }
        }
        boolean changed = changed(changedAny);
        rememberGrantStatuses(skillData, changedAny || current == null ? snapshot() : current);
        return changed;
    }

    private static boolean takeOverEmptyShellNodes(ShipSkillData skillData, Map<String, List<String>> grantedNodesBySocket, CargoAPI cargo) {
        boolean tookOver = false;
        for (Map.Entry<String, List<String>> grantLink : grantedNodesBySocket.entrySet()) {
            if (!EmptyShells.holdsEmptyShell(skillData, grantLink.getKey())) {
                continue;
            }
            for (String nodeId : grantLink.getValue()) {
                if (skillData.isAllocated(nodeId) && !skillData.isFreeNode(nodeId)) {
                    ShipTrees.takeOverAsGranted(skillData, nodeId).forEach(refund -> refund.refundTo(cargo));
                    tookOver = true;
                }
            }
        }
        return tookOver;
    }

    private void rememberGrantStatuses(ShipSkillData skillData, Snapshot snapshot) {
        Map<String, List<NodeGrantStatus>> statusesByItemId = new LinkedHashMap<>();
        for (String socketId : ShipTreeLinks.grantedNodesBySocket(skillData).keySet()) {
            String itemId = skillData.getSocketedItem(socketId);
            if (itemId != null) {
                statusesByItemId.put(itemId, snapshot.nodeGrantStatuses(socketId));
            }
        }
        NodeGrantStatus.rememberForShip(fleetMember.getId(), statusesByItemId);
    }

    Set<String> previewRelease(Consumer<ShipSkillData> change) {
        ShipFacts shipFacts = PlayerShipFacts.of(fleetMember, shipVariant);
        String rootId = satisfiedRootId();
        return ShipTrees.previewRelease(data(), SkillTree.topology(), rootId,
                change.andThen(planned -> DoctrineRevertRelease.release(planned, shipFacts, SkillTree.topology(), rootId)));
    }

    private Set<String> reachable(ShipSkillData skillData) {
        return ShipTrees.reachableAllocatedNodeIds(skillData, SkillTree.topology(), satisfiedRootId());
    }

    private void releaseStranded(ShipSkillData skillData, Set<String> reachableBefore) {
        ShipTrees.releaseNewlyStranded(skillData, SkillTree.topology(), satisfiedRootId(), reachableBefore).refundTo(PlayerFleet.cargoOrNull());
    }

    boolean chooseStartingRoot(SkillNode root) {
        return changed(ShipTrees.chooseStartingRoot(data(), root));
    }

    boolean canUnchooseStartingRoot(SkillNode node) {
        return isStartingRoot(node) && data().canUnchooseStartingRoot();
    }

    boolean unchooseStartingRoot(SkillNode node) {
        return changed(canUnchooseStartingRoot(node) && ShipTrees.unchooseStartingRoot(data()));
    }

    List<SkillNode> respecPlan(SkillNode node) {
        return RespecPlan.of(data(), SkillTree.topology(), satisfiedRootId(), node);
    }

    boolean applyPendingPhantomConflicts() {
        return changed(PhantomConflictWatch.hasPendingReverts() && PhantomConflictWatch.applyPendingReverts(fleetMember.getId()));
    }

    private boolean changed(boolean applied) {
        if (!shipEdit.changed(applied)) {
            return false;
        }
        variantHullModsRefreshed = true;
        return true;
    }

    private static double heldInCargo(String itemId) {
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        return CostItems.held(cargo, itemId);
    }

    private static float refundOf(SkillItemCost refund, String itemId) {
        return refund != null && refund.itemId().equals(itemId) ? refund.quantity() : 0f;
    }

    private static void chargeItems(ShipSkillData skillData, String nodeId, SkillItemCost itemCost) {
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        if (itemCost == null || cargo == null) {
            return;
        }
        CostItems.remove(cargo, itemCost.itemId(), itemCost.quantity());
        ItemChargeLedger.recordItemCharge(skillData, nodeId, itemCost);
    }

    private void refreshVariantHullModsOnce() {
        if (variantHullModsRefreshed) {
            return;
        }
        variantHullModsRefreshed = true;
        fleetMember.setStatUpdateNeeded(true);
        fleetMember.updateStats();
        ShipTreeSync.syncVariant(fleetMember, shipVariant);
    }

    private static String bestOfTheBestName() {
        SkillSpecAPI spec = Global.getSettings().getSkillSpec(BEST_OF_THE_BEST_SKILL_ID);
        return spec == null ? BEST_OF_THE_BEST_SKILL_ID : spec.getName();
    }

    interface RefusalDescriber {
        String describe(AllocationGate.Verdict verdict, AllocationGate gate, SkillItemCost refund);
    }

    private String describe(AllocationGate.Verdict verdict, AllocationGate gate, SkillItemCost refund) {
        return switch (verdict.refusal()) {
            case NODE_CAP -> Translation.msg("node.block.nodeCap").arg("maxNodes", gate.budget().maxAllocatedNodes()).text();
            case OUT_OF_OP -> Translation.msg("node.block.outOfOp").arg("cost", gate.budget().opCostPerNode()).arg("free", gate.freeOp()).text();
            case INELIGIBLE -> describe(verdict.block(), refund);
            default -> null;
        };
    }

    private String describe(NodeEligibility.Block block, SkillItemCost refund) {
        return switch (block.kind()) {
            case LOCKED -> Translation.text("node.block.locked");
            case INVALID_OPTION -> Translation.text("node.block.invalidOption");
            case WRONG_HULL_SIZE -> Translation.text("node.block.wrongHullSize");
            case UNMET_HULL_REQUIREMENT, UNMET_SHIP_REQUIREMENT -> Translation.text("node.requires." + block.detail());
            case HULL_MOD_CONFLICT -> shipVariant.hasHullMod(block.detail())
                    ? Translation.msg("node.block.hullModInstalled").arg("hullmod", HullModNames.displayName(block.detail())).text()
                    : Translation.msg("node.block.deactivatedSMod").arg("hullmod", HullModNames.displayName(block.detail()))
                            .arg("skill", bestOfTheBestName()).text();
            case LEARNED_CONFLICT -> block.detail() == null ? Translation.text("node.block.learnedHullConflict")
                    : Translation.msg("node.block.learnedConflict").arg("hullmod", HullModNames.displayName(block.detail())).text();
            case TYPE_CONFLICT -> Translation.msg("node.block.typeAllocated").arg("node", block.conflictingType().getDisplayName()).text();
            case EFFECT_BLOCK -> block.detail();
            case ITEM_COST -> Translation.msg("node.block.itemCost").arg("quantity", block.itemCost().formattedQuantity())
                    .arg("item", block.itemCost().itemName())
                    .arg("have", SkillItemCost.formatQuantity((float) heldInCargo(block.itemCost().itemId())
                            + refundOf(refund, block.itemCost().itemId())))
                    .text();
        };
    }
}
