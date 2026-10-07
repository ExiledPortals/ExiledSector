package exiledsector.ui.node;

import exiledsector.skills.AllocationGate;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.template.StepVerdict;
import exiledsector.skills.template.TemplateBudget;
import exiledsector.skills.template.TemplateStep;
import exiledsector.skills.template.TemplateStepRules;
import org.apache.log4j.Logger;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class TemplateStepExecutor {

    private final NodeAllocator allocator;
    private final Supplier<NodeAllocator.Snapshot> snapshotSupplier;
    private final Consumer<SkillNode> onAllocated;
    private NodeAllocator.Snapshot pointsLeftSnapshot;
    private boolean cachedPointsLeft;

    TemplateStepExecutor(NodeAllocator allocator, Supplier<NodeAllocator.Snapshot> snapshotSupplier, Consumer<SkillNode> onAllocated) {
        this.allocator = allocator;
        this.snapshotSupplier = snapshotSupplier;
        this.onAllocated = onAllocated;
    }

    StepVerdict attempt(TemplateStep step) {
        NodeAllocator.Snapshot currentSnapshot = snapshotSupplier.get();
        AllocationGate gate = currentSnapshot.gate();
        StepVerdict verdict = TemplateStepRules.verdict(step, currentSnapshot.skillData(), currentSnapshot.satisfiedRootId(), gate::allocation);
        if (verdict != StepVerdict.ALLOCATE) {
            logSkip(step, verdict);
            return verdict;
        }
        SkillNode node = SkillTree.get(step.nodeId());
        if (!allocator.allocate(node, TemplateStepRules.optionFor(step, node.getType()), currentSnapshot)) {
            logSkip(step, StepVerdict.NOT_ALLOCATABLE);
            return StepVerdict.NOT_ALLOCATABLE;
        }
        onAllocated.accept(node);
        return StepVerdict.ALLOCATE;
    }

    boolean hasPointsLeft() {
        NodeAllocator.Snapshot currentSnapshot = snapshotSupplier.get();
        if (currentSnapshot != pointsLeftSnapshot) {
            pointsLeftSnapshot = currentSnapshot;
            ShipSkillData skillData = currentSnapshot.skillData();
            cachedPointsLeft = TemplateBudget.hasPointsLeft(skillData.getAllocatedNodeIds().size(), currentSnapshot.maxAllocatedNodes(),
                    skillData.getBankedFreeAllocations(), currentSnapshot.gate().spentOp(), currentSnapshot.opCostPerNode(),
                    currentSnapshot.totalOpBudget());
        }
        return cachedPointsLeft;
    }

    private static void logSkip(TemplateStep step, StepVerdict verdict) {
        Logger logger = Logger.getLogger(TemplateStepExecutor.class);
        if (verdict != StepVerdict.ALREADY_ALLOCATED && verdict != StepVerdict.NOT_ALLOCATABLE && logger.isDebugEnabled()) {
            logger.debug("[ExiledSector] Auto-allocate skipped " + step.nodeId() + ": " + verdict);
        }
    }
}
