package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.AllocationCheck;
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
import exiledsector.skills.template.TemplateStepRules;

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
        int pendingCredits = shipData.takePendingTemplateFreeAllocations();
        int allocatedCount = shipData.isAutoAllocatingTemplate() ? allocateTemplateSteps(member, variant, shipData) : 0;
        int unspentCredits = Math.max(0, pendingCredits - allocatedCount);
        for (int i = 0; i < unspentCredits; i++) {
            ShipTrees.refundBankedFreeAllocationAsOp(shipData, SkillTree.getAllNodes().values());
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
        String rootId = ShipTrees.resolveStartingRootId(shipData);
        SkillTreeTemplate template = SkillTreeTemplateStore.assignedTo(member.getId(), rootId);
        int opCostPerNode = SkillNodeOpCost.perNode(member.getHullSpec());
        if (template == null || opCostPerNode <= 0) {
            return 0;
        }
        ShipFacts shipFacts = PlayerShipFacts.of(member, variant);
        AllocationCheck.Budget budget = new AllocationCheck.Budget(0, opCostPerNode, ShipLevelConfig.maxAllocatedNodes());
        boolean logisticsChangesAllowed = LogisticsDocking.canChangeNow();
        return AutoAllocateRun.run(template.steps(), step -> {
            AllocationCheck gate = AllocationCheck.forPlayerShip(shipData, shipFacts, rootId, budget, itemId -> 0d, logisticsChangesAllowed);
            StepVerdict verdict = TemplateStepRules.verdict(step, shipData, rootId, gate::allocation);
            if (verdict == StepVerdict.ALLOCATE) {
                SkillNode node = SkillTree.get(step.nodeId());
                ShipTrees.allocate(shipData, node, TemplateStepRules.optionFor(step, node, shipData), opCostPerNode);
            }
            return verdict;
        }, () -> shipData.getBankedFreeAllocations() > 0).allocated();
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
