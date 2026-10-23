package exiledlib.skills.skilleffect.hullmodule;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.ListenerEffect;

import java.util.function.Function;

public enum StationModuleSkillEffect implements BackedSkillEffect {

    MODULE_HULL_PERCENT(ModuleBoost.HULL_PERCENT_KEY),
    MODULE_ARMOR_PERCENT(ModuleBoost.ARMOR_PERCENT_KEY),
    MODULE_WEAPON_DAMAGE_PERCENT(ModuleBoost.WEAPON_DAMAGE_PERCENT_KEY),
    MODULE_WEAPON_RANGE_PERCENT(ModuleBoost.WEAPON_RANGE_PERCENT_KEY),
    MODULE_WEAPON_RATE_OF_FIRE_PERCENT(ModuleBoost.WEAPON_FIRE_RATE_PERCENT_KEY),
    TOP_SPEED_PERCENT_PER_DESTROYED_MODULE(JettisonListener.TOP_SPEED_PERCENT_PER_MODULE_KEY, JettisonListener.class, JettisonListener::new),
    MANEUVERABILITY_PERCENT_PER_DESTROYED_MODULE(JettisonListener.MANEUVERABILITY_PERCENT_PER_MODULE_KEY, JettisonListener.class,
            JettisonListener::new);

    private final EffectBacking backing;

    StationModuleSkillEffect(String magnitudeKey) {
        this.backing = new ListenerEffect<>(magnitudeKey, null, null);
    }

    <T> StationModuleSkillEffect(String magnitudeKey, Class<T> listenerType, Function<ShipAPI, ? extends T> listenerFactory) {
        this.backing = new ListenerEffect<>(magnitudeKey, listenerType, listenerFactory);
    }

    @Override
    public EffectBacking backing() {
        return backing;
    }
}
