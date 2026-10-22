package exiledsector.skills.skilleffect;

import java.util.Set;

public final class SkillEffectNames {

    private SkillEffectNames() {
    }

    public static Set<String> all() {
        return SkillEffectRegistry.names();
    }
}
