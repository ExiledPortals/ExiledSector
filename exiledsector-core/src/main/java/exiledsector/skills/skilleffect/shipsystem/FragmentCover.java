package exiledsector.skills.skilleffect.shipsystem;

import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;

public final class FragmentCover {

    private final float coveredPerFragment;
    private float coveredSinceLastFragment;

    public FragmentCover(float coveredPerFragment) {
        this.coveredPerFragment = coveredPerFragment;
    }

    public static float coveredAmount(float paidAmount, float coveredPercent) {
        return coveredPercent <= 0f || coveredPercent >= 100f ? 0f : paidAmount * coveredPercent / (100f - coveredPercent);
    }

    public boolean charge(RoilingSwarmEffect swarm, float paidAmount, float coveredPercent) {
        coveredSinceLastFragment += coveredAmount(paidAmount, coveredPercent);
        while (coveredSinceLastFragment >= coveredPerFragment) {
            if (!FragmentSwarms.spend(swarm, 1)) {
                coveredSinceLastFragment = 0f;
                return false;
            }
            coveredSinceLastFragment -= coveredPerFragment;
        }
        return true;
    }
}
