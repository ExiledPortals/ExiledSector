package exiledsector.skills.template;

public final class TemplateBudget {

    private TemplateBudget() {
    }

    public static boolean hasPointsLeft(int allocatedCount, int maxNodes, int bankedFreeAllocations, int spentOp,
                                        int opCostPerNode, int totalOpBudget) {
        if (allocatedCount >= maxNodes) {
            return false;
        }
        return bankedFreeAllocations > 0 || opCostPerNode <= 0 || spentOp + opCostPerNode <= totalOpBudget;
    }
}
