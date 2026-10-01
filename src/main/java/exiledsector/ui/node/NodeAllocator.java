package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.compat.SecondInCommandCompat;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.effects.SkillTreeInstaller;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.HullModNames;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.skilleffect.FleetWideEffects;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

final class NodeAllocator {

    static final String LOCKED_REASON = "Unidentified - explore the sector to discover this node.";
    private static final String WRONG_HULL_SIZE_REASON = "This node can't be allocated on this hull size.";

    record Snapshot(ShipSkillData data, String satisfiedRootId, ShipOpBudget budget, int totalOpBudget, int opCostPerNode,
                    int maxAllocatedNodes, int revision, Set<String> hiddenNodeIds, Set<String> allocatableNodeIds) {

        int opCostFor(SkillNode node) {
            return NodeAllocator.opCostFor(node, satisfiedRootId, opCostPerNode);
        }

        boolean canAllocate(SkillNode node) {
            return allocatableNodeIds.contains(node.getId());
        }

        boolean isHidden(SkillNode node) {
            return hiddenNodeIds.contains(node.getId());
        }
    }

    private final FleetMemberAPI member;
    private final ShipVariantAPI variant;
    private final Supplier<SkillNode> startingRoot;
    private int revision;

    NodeAllocator(FleetMemberAPI member, ShipVariantAPI variant, Supplier<SkillNode> startingRoot) {
        this.member = member;
        this.variant = variant;
        this.startingRoot = startingRoot;
    }

    ShipSkillData data() {
        return ShipSkillDataManager.get(member.getId());
    }

    String satisfiedRootId() {
        SkillNode root = startingRoot.get();
        return root == null ? null : root.getId();
    }

    private boolean isStartingRoot(SkillNode node) {
        SkillNode root = startingRoot.get();
        return root != null && node.getId().equals(root.getId());
    }

    Snapshot snapshot() {
        ShipSkillData data = data();
        ShipOpBudget budget = ShipOpBudget.of(member, variant);
        int opCostPerNode = SkillNodeOpCost.perNode(member.getHullSpec());
        String rootId = satisfiedRootId();
        int reservedOp = OpReserveParity.reservedOp(variant);
        OpReserveParity.warnIfOutOfSync(member, variant, data.getSpentOp(opCostPerNode), reservedOp, "while allocating nodes");
        int totalOpBudget = budget.total - budget.used + reservedOp;
        int maxAllocatedNodes = ShipLevelConfig.maxAllocatedNodes();
        return new Snapshot(data, rootId, budget, totalOpBudget, opCostPerNode, maxAllocatedNodes, revision,
                hiddenNodeIds(data), allocatableNodeIds(data, rootId, totalOpBudget, opCostPerNode, maxAllocatedNodes));
    }

    private static int opCostFor(SkillNode node, String rootId, int opCostPerNode) {
        return node.getId().equals(rootId) ? 0 : opCostPerNode;
    }

    private static Set<String> allocatableNodeIds(ShipSkillData data, String rootId, int totalOpBudget, int opCostPerNode,
                                                  int maxAllocatedNodes) {
        Set<String> allocatable = new HashSet<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (data.canAllocate(node, rootId, totalOpBudget, opCostFor(node, rootId, opCostPerNode), maxAllocatedNodes)) {
                allocatable.add(node.getId());
            }
        }
        return allocatable;
    }

    private static Set<String> hiddenNodeIds(ShipSkillData data) {
        Set<String> hidden = new HashSet<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (SkillTypeUnlockStatus.isHidden(node.getType(), data)) {
                hidden.add(node.getId());
            }
        }
        return hidden;
    }

    boolean canAllocate(SkillNode node) {
        return snapshot().canAllocate(node);
    }

    boolean canDeallocate(SkillNode node) {
        return !isStartingRoot(node) && blockDeallocationReason(node) == null
                && data().canDeallocate(node, SkillTree.topology(), satisfiedRootId());
    }

    boolean toggle(SkillNode node) {
        Snapshot snapshot = snapshot();
        ShipSkillData data = snapshot.data();
        boolean wasAllocated = data.isAllocated(node.getId());
        data.toggle(node, SkillTree.topology(), snapshot.satisfiedRootId(), snapshot.totalOpBudget(),
                snapshot.opCostFor(node), snapshot.maxAllocatedNodes());
        boolean isAllocatedNow = data.isAllocated(node.getId());
        if (isAllocatedNow == wasAllocated) {
            return false;
        }
        applyItemCost(node.getType(), isAllocatedNow);
        refreshShipStats();
        return true;
    }

    void allocateOption(SkillNode node, SkillType chosenOption) {
        data().selectOption(node, chosenOption, snapshot().opCostFor(node));
        refreshShipStats();
    }

    boolean chooseStartingRoot(SkillNode root) {
        if (!data().chooseStartingRoot(root)) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    boolean canUnchooseStartingRoot(SkillNode node) {
        return isStartingRoot(node) && data().canUnchooseStartingRoot();
    }

    boolean unchooseStartingRoot(SkillNode node) {
        if (!canUnchooseStartingRoot(node) || !data().unchooseStartingRoot()) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    String blockAllocationReason(SkillType type) {
        ShipSkillData data = data();
        if (SkillTypeUnlockStatus.isLocked(type, data)) {
            return LOCKED_REASON;
        }

        if (!type.allowsHullSize(member.getHullSpec().getHullSize())) {
            return WRONG_HULL_SIZE_REASON;
        }

        String hullModReason = hullModConflictReason(type);
        if (hullModReason != null) {
            return hullModReason;
        }

        String skillTypeReason = skillTypeConflictReason(type, data);
        if (skillTypeReason != null) {
            return skillTypeReason;
        }

        String itemCostReason = itemCostReason(type);
        if (itemCostReason != null) {
            return itemCostReason;
        }

        return effectBlockReason(type);
    }

    String blockDeallocationReason(SkillNode node) {
        SkillType type = node.resolveEffectiveType(data());
        for (SkillTypeEffect effect : type.effectsFor(member.getHullSpec().getHullSize())) {
            String blockReason = effect.effect().blockDeallocationReason(member, effect.magnitude());
            if (blockReason != null) {
                return blockReason;
            }
        }
        return null;
    }

    private void refreshShipStats() {
        revision++;
        SkillTreeInstaller.ensureInstalled(member, variant);
        FleetWideEffects.markPhaseFieldStale();
        new SkillTreeHullMod().applyEffectsBeforeShipCreation(member.getHullSpec().getHullSize(), member.getStats(), SkillTreeHullMod.ID);
        SkillTreeHullMod.syncOpSpentHullMod(member, variant);
        SkillTreeHullMod.syncInstalledHullMods(member, variant);
        member.setStatUpdateNeeded(true);
        member.updateStats();
    }

    private static void applyItemCost(SkillType type, boolean allocated) {
        SkillItemCost itemCost = type.getItemCost();
        if (itemCost == null) {
            return;
        }
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        if (allocated) {
            cargo.removeCommodity(itemCost.itemId(), itemCost.quantity());
        } else {
            cargo.addCommodity(itemCost.itemId(), itemCost.quantity());
        }
    }

    private static String itemCostReason(SkillType type) {
        SkillItemCost itemCost = type.getItemCost();
        if (itemCost == null) {
            return null;
        }
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        float have = cargo.getCommodityQuantity(itemCost.itemId());
        if (have >= itemCost.quantity()) {
            return null;
        }
        return "Requires " + itemCost.formattedQuantity() + " " + itemCost.commodityName()
                + " (have " + SkillItemCost.formatQuantity(have) + ").";
    }

    private String hullModConflictReason(SkillType type) {
        List<String> exclusiveHullModIds = type.getExclusiveHullModIds();
        if (exclusiveHullModIds.isEmpty()) {
            return null;
        }

        member.setStatUpdateNeeded(true);
        member.updateStats();
        SkillTreeHullMod.syncOpSpentHullMod(member, variant);
        for (String hullModId : exclusiveHullModIds) {
            if (variant.hasHullMod(hullModId)) {
                return "Ship already has " + HullModNames.displayName(hullModId) + " installed.";
            }
            if (SecondInCommandCompat.hasDeactivatedSMod(variant, hullModId)) {
                return "Ship has a deactivated " + HullModNames.displayName(hullModId) + " S-mod that Best of the Best will restore.";
            }
        }
        return null;
    }

    private static String skillTypeConflictReason(SkillType type, ShipSkillData data) {
        for (AllocatedNode allocated : AllocatedNode.of(data)) {
            if (allocated.isExclusiveWith(type)) {
                return "Already have " + allocated.effectiveType().getDisplayName() + " allocated.";
            }
        }
        return null;
    }

    private String effectBlockReason(SkillType type) {
        List<SkillEffect> currentlyAllocatedEffects = AllocatedSkillEffects.forMember(member);
        for (SkillTypeEffect effect : type.effectsFor(member.getHullSpec().getHullSize())) {
            String blockReason = effect.effect().blockAllocationReason(member, effect.magnitude(), currentlyAllocatedEffects);
            if (blockReason != null) {
                return blockReason;
            }
        }
        return null;
    }
}
