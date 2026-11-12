package exiledsector.effects;

import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class HullModSecondPass {

    static final String METHOD_NAME = "applyEffectsAfterShipCreation2ndPass";
    private static final MethodHandle SECOND_PASS = resolve(HullModEffect.class);

    private HullModSecondPass() {
    }

    static MethodHandle resolve(Class<?> effectType) {
        try {
            return MethodHandles.publicLookup().findVirtual(effectType, METHOD_NAME, MethodType.methodType(void.class, ShipAPI.class, String.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    static void apply(HullModEffect effect, ShipAPI ship, String id) {
        invoke(SECOND_PASS, effect, ship, id);
    }

    // java:S1181: MethodHandle.invoke declares Throwable; runtime failures are rethrown as the direct call would throw them
    @SuppressWarnings("java:S1181")
    static void invoke(MethodHandle secondPass, Object effect, ShipAPI ship, String id) {
        if (secondPass == null || effect == null) {
            return;
        }
        try {
            secondPass.invoke(effect, ship, id);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
