package exiledlib.skills.skilleffect.stationmodule;

import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.ListenerEffect;

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
