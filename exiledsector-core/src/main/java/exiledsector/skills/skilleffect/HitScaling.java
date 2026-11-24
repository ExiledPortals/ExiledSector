package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableStat;

import java.util.Arrays;

final class HitScaling {

    private final float[] increasedPercent = new float[HitConversions.PORTIONS];
    private final float[] moreMult = new float[HitConversions.PORTIONS];

    HitScaling() {
        Arrays.fill(moreMult, 1f);
    }

    void increase(int portion, float percent) {
        increasedPercent[portion] += percent;
    }

    void more(int portion, float mult) {
        moreMult[portion] *= Math.max(0f, mult);
    }

    float factor(int portion, float genericPool) {
        float more = moreMult[portion];
        if (genericPool <= 0f) {
            return more;
        }
        return Math.max(0f, genericPool + increasedPercent[portion] / 100f) / genericPool * more;
    }

    static float genericPool(MutableStat modifier) {
        if (modifier == null) {
            return 1f;
        }
        float percent = 0f;
        for (MutableStat.StatMod mod : modifier.getPercentMods().values()) {
            percent += mod.getValue();
        }
        float flat = 0f;
        for (MutableStat.StatMod mod : modifier.getFlatMods().values()) {
            flat += mod.getValue();
        }
        return modifier.getBaseValue() * (1f + percent / 100f) + flat;
    }
}
