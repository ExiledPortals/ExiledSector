package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

public enum StatMode {

    FLAT("desc.flat") {
        @Override
        void apply(MutableStat stat, String modId, float magnitude) {
            stat.modifyFlat(modId, magnitude);
        }

        @Override
        void apply(StatBonus stat, String modId, float magnitude) {
            stat.modifyFlat(modId, magnitude);
        }

        @Override
        float inverse(float magnitude) {
            return -magnitude;
        }
    },
    PERCENT("desc.pct") {
        @Override
        void apply(MutableStat stat, String modId, float magnitude) {
            stat.modifyPercent(modId, magnitude);
        }

        @Override
        void apply(StatBonus stat, String modId, float magnitude) {
            stat.modifyPercent(modId, magnitude);
        }

        @Override
        float inverse(float magnitude) {
            return -magnitude;
        }
    },
    MULT("desc.mult") {
        @Override
        void apply(MutableStat stat, String modId, float magnitude) {
            SkillEffectSupport.applyMult(stat, modId, magnitude);
        }

        @Override
        void apply(StatBonus stat, String modId, float magnitude) {
            SkillEffectSupport.applyMult(stat, modId, magnitude);
        }

        @Override
        float inverse(float magnitude) {
            return (SkillEffectSupport.inverseMultFrom(magnitude) - 1f) * 100f;
        }
    };

    private final String descriptionKey;

    StatMode(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public StyledText description(float magnitude, String statName) {
        return Translation.msg(descriptionKey + (magnitude >= 0 ? ".up" : ".down")).arg("stat", statName)
                .arg("value", Math.abs(magnitude)).styled();
    }

    public StyledText describeStat(float magnitude, String statKey) {
        return description(magnitude, Translation.text(statKey));
    }

    public StyledText description(float magnitude, String subject, String statName) {
        return Translation.msg(descriptionKey + (magnitude >= 0 ? ".up" : ".down") + ".subject").arg("subject", subject)
                .arg("stat", statName).arg("value", Math.abs(magnitude)).styled();
    }

    public StyledText describeStat(float magnitude, String subjectKey, String statKey) {
        return description(magnitude, Translation.text(subjectKey), Translation.text(statKey));
    }

    abstract void apply(MutableStat stat, String modId, float magnitude);

    abstract void apply(StatBonus stat, String modId, float magnitude);

    abstract float inverse(float magnitude);
}
