package exiledsector.effects;

import com.fs.starfarer.api.EveryFrameScript;

// TODO: delete once saves that still contain this script no longer need to load - XP is awarded by CombatXpAward
public class ShipLevelScript implements EveryFrameScript {

    // kept so saves that serialized this field still deserialize
    @SuppressWarnings("java:S1068")
    private boolean xpAwarded;

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    // a retired script has nothing to do; isDone() lets the sector drop it
    @Override
    @SuppressWarnings("java:S1186")
    public void advance(float amount) {
    }
}
