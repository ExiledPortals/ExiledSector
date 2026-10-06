package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;

import java.util.function.Predicate;

record ConditionalStatEffect(StatMode mode, StatTarget target, String statKey, Predicate<ShipAPI> condition)
        implements EffectBacking {

    @Override
    public void advanceInCombat(ShipAPI ship, String modId, float magnitude) {
        target.apply(ship.getMutableStats(), modId, mode, condition.test(ship) ? magnitude : 0f);
    }

    @Override
    public boolean advancesInCombat() {
        return true;
    }

    @Override
    public boolean supportsTemporaryGating() {
        return false;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return mode.describeStat(magnitude, statKey);
    }
}
