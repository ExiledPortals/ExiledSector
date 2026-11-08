package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;

public record FlagEffect(String flagKey, ListenerEffect<?> listener) implements EffectBacking {

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        stats.getDynamic().getMod(flagKey).modifyFlat(modId, 1f);
    }

    @Override
    public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
        if (listener != null) {
            listener.applyAfterShipCreation(ship, modId, magnitude);
        }
    }

    @Override
    public Class<?> listenerType() {
        return listener == null ? null : listener.listenerType();
    }

    @Override
    public boolean supportsTemporaryGating() {
        return false;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return EffectText.templated(effect, magnitude);
    }
}
