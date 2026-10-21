package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateStep;
import exiledsector.skills.template.TemplateStepRules;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;

final class TemplateLevelUp {

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
        int pendingCredits = shipData.takePendingTemplateCredits();
        int allocatedCount = shipData.isAutoAllocatingTemplate() ? allocateTemplateSteps(member, variant, shipData) : 0;
        int unspentCredits = Math.max(0, pendingCredits - allocatedCount);
        for (int i = 0; i < unspentCredits; i++) {
            shipData.refundBankedCreditAsOp(SkillTree.getAllNodes().values());
        }
        if (allocatedCount > 0 || unspentCredits > 0) {
            member.setStatUpdateNeeded(true);
        }
        if (allocatedCount > 0) {
            returnUnhousedWings(member, variant);
        }
        return allocatedCount;
    }

    private static int allocateTemplateSteps(FleetMemberAPI member, ShipVariantAPI variant, ShipSkillData shipData) {
        String rootId = shipData.resolveStartingRootId();
        SkillTreeTemplate template = SkillTreeTemplateStore.assignedTo(member.getId(), rootId);
        int opCostPerNode = SkillNodeOpCost.perNode(member.getHullSpec());
        if (template == null || opCostPerNode <= 0) {
            return 0;
        }
        ShipFacts shipFacts = PlayerShipFacts.of(member, variant);
        AllocationGate.Budget budget = new AllocationGate.Budget(0, opCostPerNode, ShipLevelConfig.maxAllocatedNodes());
        int allocatedCount = 0;
        while (shipData.getBankedFreeAllocations() > 0) {
            NodeEligibility.Context eligibility = NodeEligibility.Context.of(shipData, shipFacts,
                    SkillTypeUnlockStatus.lockedFor(shipData), itemId -> 0d);
            AllocationGate gate = new AllocationGate(shipData, SkillTree.topology(), rootId, budget, eligibility);
            TemplateStep nextStep = TemplateStepRules.firstAllocatable(template.steps(), shipData, rootId, gate);
            if (nextStep == null) {
                break;
            }
            SkillNode node = SkillTree.get(nextStep.nodeId());
            SkillType option = TemplateStepRules.optionFor(nextStep, node, shipData);
            shipData.allocate(node, option, opCostPerNode);
            allocatedCount++;
        }
        return allocatedCount;
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
