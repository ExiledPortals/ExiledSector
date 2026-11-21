package exiledsector.skills.template;

public final class TemplateBudget {

    private TemplateBudget() {
    }

    public static boolean hasPointsLeft(int allocatedCount, int maxNodes, int bankedFreeAllocations, int spentOp,
                                        int opCostPerNode, int totalOpBudget, boolean freeAllocationsOnly) {
        if (allocatedCount >= maxNodes) {
            return false;
        }
        if (bankedFreeAllocations > 0 || opCostPerNode <= 0) {
            return true;
        }
        return !freeAllocationsOnly && spentOp + opCostPerNode <= totalOpBudget;
    }
}
