package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ExpiringStacks;

final class AccretionStacks {

    static final float STACK_SECONDS = 20f;
    static final int MAX_STACKS = 20;
    private static final String CUSTOM_DATA_KEY = "exiledSector_accretionStacks";

    private final ExpiringStacks<Void> lostStrikecraft = new ExpiringStacks<>(MAX_STACKS, STACK_SECONDS);

    static AccretionStacks of(ShipAPI carrier) {
        if (carrier.getCustomData().get(CUSTOM_DATA_KEY) instanceof AccretionStacks stacks) {
            return stacks;
        }
        AccretionStacks stacks = new AccretionStacks();
        carrier.setCustomData(CUSTOM_DATA_KEY, stacks);
        return stacks;
    }

    void add(float now) {
        lostStrikecraft.add(now);
    }

    int activeAt(float now) {
        lostStrikecraft.dropExpired(now);
        return lostStrikecraft.size();
    }
}
