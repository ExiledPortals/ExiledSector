package exiledsector.loot;

import java.util.List;

public record FleetLoss(float deploymentPoints, float averageDifficulty, float averageShipLevel, FleetLootRules rules) {

    @FunctionalInterface
    public interface Factor {

        float of(FleetLoss loss);
    }

    public FleetLoss {
        deploymentPoints = Math.max(0f, deploymentPoints);
        rules = rules == null ? FleetLootRules.LEVEL_SCALED : rules;
    }

    public float lootDeploymentPoints() {
        return deploymentPoints * rules.deploymentPointsMult();
    }

    public static float totalDeploymentPoints(List<FleetLoss> losses) {
        float total = 0f;
        for (FleetLoss loss : losses) {
            total += loss.deploymentPoints();
        }
        return total;
    }
}
