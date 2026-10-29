package exiledsector.skills.skilleffect.hullmodule;

import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.ListenerEffect;

public enum StationModuleSkillEffect implements BackedSkillEffect {

    MODULE_HULL_PERCENT(ModuleBoost.HULL_PERCENT_KEY),
    MODULE_ARMOR_PERCENT(ModuleBoost.ARMOR_PERCENT_KEY);

    private final EffectBacking backing;

    StationModuleSkillEffect(String magnitudeKey) {
        this.backing = new ListenerEffect<>(magnitudeKey, null, null);
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
