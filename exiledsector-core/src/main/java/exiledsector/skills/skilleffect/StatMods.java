package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;

public final class StatMods {

    private StatMods() {
    }

    public static void setFlat(MutableStat stat, String modId, float value) {
        if (value != 0f) {
            stat.modifyFlat(modId, value);
        } else {
            stat.unmodifyFlat(modId);
        }
    }

    public static void setFlat(MutableStat stat, String modId, float value, String sourceText) {
        if (value != 0f) {
            stat.modifyFlat(modId, value, sourceText);
        } else {
            stat.unmodifyFlat(modId);
        }
    }

    public static void setFlat(StatBonus bonus, String modId, float value) {
        if (value != 0f) {
            bonus.modifyFlat(modId, value);
        } else {
            bonus.unmodifyFlat(modId);
        }
    }

    public static void setFlat(StatBonus bonus, String modId, float value, String sourceText) {
        if (value != 0f) {
            bonus.modifyFlat(modId, value, sourceText);
        } else {
            bonus.unmodifyFlat(modId);
        }
    }

    public static void setPercent(MutableStat stat, String modId, float percent) {
        if (percent != 0f) {
            stat.modifyPercent(modId, percent);
        } else {
            stat.unmodifyPercent(modId);
        }
    }

    public static void setPercent(MutableStat stat, String modId, float percent, String sourceText) {
        if (percent != 0f) {
            stat.modifyPercent(modId, percent, sourceText);
        } else {
            stat.unmodifyPercent(modId);
        }
    }

    public static void setPercent(StatBonus bonus, String modId, float percent) {
        if (percent != 0f) {
            bonus.modifyPercent(modId, percent);
        } else {
            bonus.unmodifyPercent(modId);
        }
    }

    public static void setPercent(StatBonus bonus, String modId, float percent, String sourceText) {
        if (percent != 0f) {
            bonus.modifyPercent(modId, percent, sourceText);
        } else {
            bonus.unmodifyPercent(modId);
        }
    }

    public static void setMult(MutableStat stat, String modId, float mult) {
        if (mult != 1f) {
            stat.modifyMult(modId, mult);
        } else {
            stat.unmodifyMult(modId);
        }
    }

    public static void setMult(MutableStat stat, String modId, float mult, String sourceText) {
        if (mult != 1f) {
            stat.modifyMult(modId, mult, sourceText);
        } else {
            stat.unmodifyMult(modId);
        }
    }

    public static void setMult(StatBonus bonus, String modId, float mult) {
        if (mult != 1f) {
            bonus.modifyMult(modId, mult);
        } else {
            bonus.unmodifyMult(modId);
        }
    }

    public static void setMult(StatBonus bonus, String modId, float mult, String sourceText) {
        if (mult != 1f) {
            bonus.modifyMult(modId, mult, sourceText);
        } else {
            bonus.unmodifyMult(modId);
        }
    }
}
