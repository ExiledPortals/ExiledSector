package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class TimedTargetDebuff implements AdvanceableListener {

    @FunctionalInterface
    interface MultFor {
        float of(float percent);
    }

    record Spec(String modId, float seconds, Function<MutableShipStatsAPI, MutableStat> stat, MultFor multFor) {
    }

    private static final class Active {
        private final Spec spec;
        private float strongestPercent;
        private float remainingSeconds;

        private Active(Spec spec) {
            this.spec = spec;
        }
    }

    private final ShipAPI debuffedShip;
    private final List<Active> active = new ArrayList<>(2);

    TimedTargetDebuff(ShipAPI debuffedShip) {
        this.debuffedShip = debuffedShip;
    }

    static void refresh(ShipAPI target, Spec spec, float percent) {
        SkillEffectSupport.ensureListener(target, TimedTargetDebuff.class, TimedTargetDebuff::new).refresh(spec, percent);
    }

    private void refresh(Spec spec, float percent) {
        Active debuff = activeFor(spec);
        debuff.remainingSeconds = spec.seconds();
        if (percent > debuff.strongestPercent) {
            debuff.strongestPercent = percent;
            spec.stat().apply(debuffedShip.getMutableStats()).modifyMult(spec.modId(), spec.multFor().of(percent));
        }
    }

    private Active activeFor(Spec spec) {
        for (Active debuff : active) {
            if (debuff.spec == spec) {
                return debuff;
            }
        }
        Active debuff = new Active(spec);
        active.add(debuff);
        return debuff;
    }

    @Override
    public void advance(float amount) {
        for (int index = active.size() - 1; index >= 0; index--) {
            Active debuff = active.get(index);
            debuff.remainingSeconds -= amount;
            if (debuff.remainingSeconds <= 0f) {
                debuff.spec.stat().apply(debuffedShip.getMutableStats()).unmodify(debuff.spec.modId());
                active.remove(index);
            }
        }
        if (active.isEmpty()) {
            debuffedShip.removeListener(this);
        }
    }
}
