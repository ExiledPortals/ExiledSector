package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.skills.skilleffect.DeploymentRepairTally;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;

final class SwarmOverflowRepairListener extends ShipCombatListener implements AdvanceableListener {

    static final String REPAIR_PERCENT_KEY = "exiledLib_swarmOverflowArmorRepairPercent";
    static final float DEPLOYMENT_CAP_PERCENT = 10f;
    private static final float CHECK_SECONDS = 1f;

    private final IntervalUtil overflowCheck = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final DeploymentRepairTally repairTally = new DeploymentRepairTally();
    private float overflowFragments;

    SwarmOverflowRepairListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        overflowCheck.advance(amount);
        if (!overflowCheck.intervalElapsed()) {
            return;
        }
        RoilingSwarmEffect swarm = FragmentSwarms.swarmOf(ownerShip);
        if (!FragmentSwarms.isFull(swarm) || !ownerIsAliveNotHulk()) {
            overflowFragments = 0f;
            return;
        }
        overflowFragments += CHECK_SECONDS * swarm.getParams().memberRespawnRate;
        int fragments = (int) overflowFragments;
        if (fragments > 0) {
            overflowFragments -= fragments;
            repair(fragments);
        }
    }

    private void repair(int fragments) {
        ArmorGridAPI grid = ownerShip.getArmorGrid();
        float percent = magnitude(REPAIR_PERCENT_KEY);
        if (grid == null || percent <= 0f) {
            return;
        }
        float totalArmor = ArmorCells.total(grid);
        float restored = restoreMostDamaged(grid, fragments, totalArmor * percent / 100f,
                repairTally.remainingBelow(totalArmor * DEPLOYMENT_CAP_PERCENT / 100f));
        if (restored > 0f) {
            repairTally.record(restored);
            ownerShip.syncWithArmorGridState();
        }
    }

    static float restoreMostDamaged(ArmorGridAPI grid, int fragments, float perFragment, float budget) {
        float maxArmorInCell = grid.getMaxArmorInCell();
        float restored = 0f;
        for (int fragment = 0; fragment < fragments && restored < budget; fragment++) {
            int[] cell = ArmorCells.mostDamaged(grid);
            float current = grid.getArmorValue(cell[0], cell[1]);
            float repair = Math.min(Math.min(perFragment, maxArmorInCell - current), budget - restored);
            if (!(repair > 0f)) {
                break;
            }
            grid.setArmorValue(cell[0], cell[1], current + repair);
            restored += repair;
        }
        return restored;
    }
}
