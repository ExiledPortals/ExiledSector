package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.ArrayDeque;
import java.util.Deque;

final class AccretionStacks {

    static final float STACK_SECONDS = 20f;
    static final int MAX_STACKS = 20;
    private static final String CUSTOM_DATA_KEY = "exiledSector_accretionStacks";

    private final Deque<Float> expiryTimes = new ArrayDeque<>();

    static AccretionStacks of(ShipAPI carrier) {
        if (carrier.getCustomData().get(CUSTOM_DATA_KEY) instanceof AccretionStacks stacks) {
            return stacks;
        }
        AccretionStacks stacks = new AccretionStacks();
        carrier.setCustomData(CUSTOM_DATA_KEY, stacks);
        return stacks;
    }

    void add(float now) {
        dropExpired(now);
        if (expiryTimes.size() >= MAX_STACKS) {
            expiryTimes.pollFirst();
        }
        expiryTimes.addLast(now + STACK_SECONDS);
    }

    int activeAt(float now) {
        dropExpired(now);
        return expiryTimes.size();
    }

    private void dropExpired(float now) {
        while (!expiryTimes.isEmpty() && expiryTimes.peekFirst() <= now) {
            expiryTimes.pollFirst();
        }
    }
}
