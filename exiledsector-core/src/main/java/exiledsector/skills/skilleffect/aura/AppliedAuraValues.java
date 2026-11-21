package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

final class AppliedAuraValues {

    interface Change {
        Change EXACT = (applied, wanted) -> Float.compare(applied, wanted) != 0;
        Change ALWAYS = (applied, wanted) -> true;

        static Change beyond(float epsilon) {
            return (applied, wanted) -> Math.abs(applied - wanted) > epsilon;
        }

        boolean needsReapply(float applied, float wanted);
    }

    interface Apply {
        void apply(ShipAPI target, float value);
    }

    private final Map<ShipAPI, Float> appliedByTarget = new IdentityHashMap<>();
    private final Change change;

    AppliedAuraValues(Change change) {
        this.change = change;
    }

    void update(Map<ShipAPI, Float> wantedByTarget, StatTarget stat, StatMode mode, String modId) {
        update(wantedByTarget, target -> stat.remove(target.getMutableStats(), modId),
                (target, value) -> stat.apply(target.getMutableStats(), modId, mode, value));
    }

    void update(Map<ShipAPI, Float> wantedByTarget, Consumer<ShipAPI> remove, Apply apply) {
        Iterator<Map.Entry<ShipAPI, Float>> applied = appliedByTarget.entrySet().iterator();
        while (applied.hasNext()) {
            ShipAPI previous = applied.next().getKey();
            if (!wantedByTarget.containsKey(previous)) {
                remove.accept(previous);
                applied.remove();
            }
        }
        wantedByTarget.forEach((target, wanted) -> {
            Float appliedValue = appliedByTarget.get(target);
            if (appliedValue == null || change.needsReapply(appliedValue, wanted)) {
                apply.apply(target, wanted);
                appliedByTarget.put(target, wanted);
            }
        });
    }
}
