package exiledsector.ui.cargo;

import com.fs.starfarer.api.campaign.CargoStackAPI;

public final class CargoIconAnimation {

    private final int phaseSeed = System.identityHashCode(this);
    private float firstSeenSeconds = -1f;
    private boolean celebrating;

    public float burstAge(CargoIconLook look, CargoStackAPI stack) {
        float now = CargoIconFx.seconds();
        if (firstSeenSeconds < 0f) {
            firstSeenSeconds = now;
            celebrating = CargoIconLooks.celebratesDrop(look) && FreshLoot.isFreshLoot(stack);
        }
        if (!celebrating) {
            return -1f;
        }
        float age = now - firstSeenSeconds;
        if (age >= CargoIconFx.BURST_SECONDS) {
            celebrating = false;
            return -1f;
        }
        return age;
    }

    public int phaseSeed() {
        return phaseSeed;
    }
}
