package exiledsector.skills.skilleffect.stationmodule;

import exiledsector.skills.skilleffect.BackedSkillEffect;
import exiledsector.skills.skilleffect.EffectBacking;

public enum StationModuleSkillEffect implements BackedSkillEffect {

    MODULE_HULL_PERCENT,
    MODULE_ARMOR_PERCENT;

    private static final EffectBacking READ_BY_MODULES = new ModuleBonusBacking();

    @Override
    public EffectBacking backing() {
        return READ_BY_MODULES;
    }
}
