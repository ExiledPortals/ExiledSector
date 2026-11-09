package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.AllocationCheck;
import exiledsector.skills.AllocationPath;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.template.AutoAllocateRun;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.StepVerdict;
import exiledsector.skills.template.TemplateAnchors;
import exiledsector.skills.template.TemplateBridge;
import exiledsector.skills.template.TemplateStepRules;
import exiledsector.skills.unlock.NodeGates;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

final class TemplateLevelUp {

    record TemplateRun(int allocated, int waitingForDock, int anchorsSocketed) {
        static final TemplateRun NONE = new TemplateRun(0, 0, 0);

        boolean changedTree() {
            return allocated > 0 || anchorsSocketed > 0;
        }
    }

    private TemplateLevelUp() {
    }

    static int spendFreeAllocations(FleetMemberAPI member) {
        return spendFreeAllocations(member, member.getVariant());
    }

    static int spendFreeAllocations(FleetMemberAPI member, ShipVariantAPI variant) {
        ShipSkillData shipData = ShipSkillDataManager.find(member.getId());
        if (shipData == null) {
            return 0;
        }
        int pendingCredits = shipData.takePendingTemplateFreeAllocations();
        TemplateRun run = shipData.isAutoAllocatingTemplate()
                ? allocateTemplateSteps(member, variant, shipData, LogisticsDocking.canChangeNow()) : TemplateRun.NONE;
        int allocatedCount = run.allocated();
        int unspentCredits = Math.max(0, pendingCredits - allocatedCount - run.waitingForDock());
        for (int i = 0; i < unspentCredits; i++) {
            ShipTrees.refundBankedFreeAllocationAsOp(shipData, SkillTree.getAllNodes().values());
        }
        if (run.changedTree() || unspentCredits > 0) {
            member.setStatUpdateNeeded(true);
        }
        if (run.changedTree()) {
            returnUnhousedWings(member, variant);
        }
        return allocatedCount;
    }

    static int spendBankedWhileDocked(FleetMemberAPI member, boolean logisticsChangesAllowed) {
        ShipSkillData shipData = ShipSkillDataManager.find(member.getId());
        if (shipData == null || !shipData.isAutoAllocatingTemplate() || shipData.getBankedFreeAllocations() <= 0) {
            return 0;
        }
        ShipVariantAPI variant = member.getVariant();
        TemplateRun run = allocateTemplateSteps(member, variant, shipData, logisticsChangesAllowed);
        if (run.changedTree()) {
            member.setStatUpdateNeeded(true);
            returnUnhousedWings(member, variant);
        }
        return run.allocated();
    }

    private static TemplateRun allocateTemplateSteps(FleetMemberAPI member, ShipVariantAPI variant, ShipSkillData shipData,
                                                     boolean logisticsChangesAllowed) {
        String rootId = ShipTrees.resolveStartingRootId(shipData);
        SkillTreeTemplate template = rootId == null ? null : SkillTreeTemplateStore.assignedTo(member.getId());
        int opCostPerNode = SkillNodeOpCost.perNode(member.getHullSpec());
        if (template == null || opCostPerNode <= 0) {
            return TemplateRun.NONE;
        }
        ShipFacts shipFacts = PlayerShipFacts.of(member, variant);
        int maxAllocatedNodes = ShipLevelConfig.maxAllocatedNodes();
        AllocationCheck.Budget budget = new AllocationCheck.Budget(0, opCostPerNode, maxAllocatedNodes);
        Set<String> waitingForDock = new HashSet<>();
        TemplateAnchors anchors = TemplateAnchors.fromStorage(template, (socket, anchor) -> {
            if (!shipData.socketItem(socket.getId(), anchor.id())) {
                return false;
            }
            ShipTrees.openAnchorWormholes(shipData, maxAllocatedNodes);
            return true;
        });
        anchors.fill(shipData);
        Supplier<AllocationCheck> gateSupplier = () -> AllocationCheck.forPlayerShip(shipData, shipFacts, rootId, budget, itemId -> 0d,
                logisticsChangesAllowed);
        AutoAllocateRun.Bridge bridge = TemplateBridge.crossesRoots(template, rootId)
                ? rootBridge(shipData, gateSupplier, new TemplateBridge(template.steps()), opCostPerNode) : AutoAllocateRun.Bridge.NONE;
        int allocatedCount = AutoAllocateRun.run(template.steps(), anchors.fillingAfter(step -> {
            AllocationCheck gate = gateSupplier.get();
            StepVerdict verdict = TemplateStepRules.verdict(step, shipData, rootId, (node, option) -> {
                AllocationCheck.Verdict allocation = gate.allocation(node, option);
                if (allocation.block() != null && allocation.block().kind() == NodeEligibility.Kind.LOGISTICS_UNDOCKED) {
                    waitingForDock.add(node.getId());
                }
                return allocation;
            });
            if (verdict == StepVerdict.ALLOCATE) {
                SkillNode node = SkillTree.get(step.nodeId());
                ShipTrees.allocate(shipData, node, TemplateStepRules.optionFor(step, node, shipData), opCostPerNode);
            }
            return verdict;
        }, shipData), bridge, () -> shipData.getBankedFreeAllocations() > 0).allocated();
        return new TemplateRun(allocatedCount, waitingForDock.size(), anchors.socketedCount());
    }

    private static AutoAllocateRun.Bridge rootBridge(ShipSkillData shipData, Supplier<AllocationCheck> gateSupplier, TemplateBridge templateBridge,
                                                     int opCostPerNode) {
        return unreachedSteps -> {
            AllocationCheck gate = gateSupplier.get();
            AllocationPath.Step step = templateBridge.nextStep(unreachedSteps, gate, SkillTree.topology(), node -> NodeGates.isUnseen(node, shipData),
                    ThreadLocalRandom.current());
            if (step == null || !gate.allocation(step.node(), step.option()).allowed()) {
                return false;
            }
            ShipTrees.allocate(shipData, step.node(), step.option(), opCostPerNode);
            return true;
        };
    }

    private static void returnUnhousedWings(FleetMemberAPI member, ShipVariantAPI variant) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (playerFleet == null || member.getStats() == null) {
            return;
        }
        member.updateStats();
        FighterBayOverflow.returnUnhousedWingsAndAnnounce(member, variant, playerFleet.getCargo());
    }
}
