package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.function.Function;

public final class Backings {

    private Backings() {
    }

    public static SimpleStatEffect statEffect(StatMode mode, StatTarget target, String statKey, boolean lowerIsBetter) {
        return new SimpleStatEffect(mode, target, statKey, lowerIsBetter);
    }

    public static <T> ListenerEffect<T> listener(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        return new ListenerEffect<>(magnitudeKey, listenerType, listenerFactory);
    }

    public static <T> ListenerEffect<T> listener(Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        return listener(null, listenerType, listenerFactory);
    }

    public static FlagEffect flag(String flagKey) {
        return new FlagEffect(flagKey);
    }
}
