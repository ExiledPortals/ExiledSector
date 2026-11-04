package exiledsector.skills.skilleffect;

public final class DeploymentRepairTally {

    private float repaired;

    public float repaired() {
        return repaired;
    }

    public float remainingBelow(float deploymentCap) {
        return deploymentCap - repaired;
    }

    public void record(float amount) {
        repaired += amount;
    }
}
