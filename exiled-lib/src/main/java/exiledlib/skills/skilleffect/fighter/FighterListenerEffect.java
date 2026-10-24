package exiledlib.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledlib.i18n.StyledText;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.SkillEffect;

import java.util.function.BiFunction;

record FighterListenerEffect<T>(String magnitudeKey, Class<T> listenerType, BiFunction<ShipAPI, ShipAPI, ? extends T> listenerFactory)
        implements EffectBacking {

    static float carrierValue(ShipAPI carrier, String magnitudeKey) {
        return carrier == null ? 0f : carrier.getMutableStats().getDynamic().getValue(magnitudeKey, 0f);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        stats.getDynamic().getMod(magnitudeKey).modifyFlat(modId, magnitude);
    }

    @Override
    public void applyToFighterSpawnedByShip(ShipAPI fighter, ShipAPI parentShip, String modId, float magnitude) {
        if (fighter == null || parentShip == null || magnitude <= 0f || fighter.hasListenerOfClass(listenerType)) {
            return;
        }
        T listener = listenerFactory.apply(fighter, parentShip);
        if (listener != null) {
            fighter.addListener(listener);
        }
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
