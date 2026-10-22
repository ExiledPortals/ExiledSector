package exiledsector.loot;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public final class BattleLoot {

    private final Random random;
    private final Random independentRandom;
    private final Map<Object, Object> states = new IdentityHashMap<>();
    private boolean playerWon;

    public BattleLoot(Random random, Random independentRandom) {
        this.random = random;
        this.independentRandom = independentRandom;
    }

    public boolean playerWon() {
        return playerWon;
    }

    public void setPlayerWon(boolean playerWon) {
        this.playerWon = playerWon;
    }

    public Random random() {
        return random;
    }

    public Random independentRandom() {
        return independentRandom;
    }

    // each key is only ever stored with the one type its supplier creates
    @SuppressWarnings("unchecked")
    public <T> T state(Object key, Supplier<T> created) {
        Object state = states.get(key);
        if (state == null) {
            state = created.get();
            states.put(key, state);
        }
        return (T) state;
    }
}
