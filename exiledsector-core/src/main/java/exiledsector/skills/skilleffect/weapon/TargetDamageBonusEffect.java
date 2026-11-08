package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.ListenerEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;

import java.util.function.Function;

record TargetDamageBonusEffect(ListenerEffect<? extends TargetDamageBonusListener> listener, String statKey) implements EffectBacking {

    static <T extends TargetDamageBonusListener> TargetDamageBonusEffect targetDamage(String damagePercentKey, Class<T> listenerType,
            Function<ShipAPI, T> listenerFactory, String statKey) {
        return new TargetDamageBonusEffect(new ListenerEffect<>(damagePercentKey, listenerType, listenerFactory), statKey);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        listener.apply(stats, modId, magnitude);
    }

    @Override
    public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
        listener.applyAfterShipCreation(ship, modId, magnitude);
    }

    @Override
    public Class<?> listenerType() {
        return listener.listenerType();
    }

    @Override
    public boolean supportsTemporaryGating() {
        return listener.supportsTemporaryGating();
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return StatMode.PERCENT.describeStat(magnitude, statKey);
    }
}
