package exiledsector.skills.skilleffect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeploymentRepairTallyTest {

    @Test
    void repairsAddUpWithinOneTally() {
        DeploymentRepairTally tally = new DeploymentRepairTally();

        tally.record(30f);
        tally.record(15f);

        assertEquals(45f, tally.repaired(), 0.001f);
        assertEquals(55f, tally.remainingBelow(100f), 0.001f);
    }

    @Test
    void eachTallyStartsEmpty() {
        DeploymentRepairTally firstDeployment = new DeploymentRepairTally();
        firstDeployment.record(30f);

        DeploymentRepairTally nextDeployment = new DeploymentRepairTally();

        assertEquals(0f, nextDeployment.repaired());
        assertEquals(100f, nextDeployment.remainingBelow(100f), 0.001f);
    }
}
