package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.ConditionalStatEffect;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;

import java.util.function.Predicate;

import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;

record ConditionalScarStat(ConditionalStatEffect conditional, boolean lowerIsBetter, String argName, float argValue) implements EffectBacking {

    static final StatTarget DAMAGE_TAKEN = all(liveStat(MutableShipStatsAPI::getHullDamageTakenMult),
            liveStat(MutableShipStatsAPI::getArmorDamageTakenMult), liveStat(MutableShipStatsAPI::getShieldDamageTakenMult));

    static ConditionalScarStat damageTakenWhile(Predicate<ShipAPI> condition, String argName, float argValue) {
        return new ConditionalScarStat(new ConditionalStatEffect(StatMode.PERCENT, DAMAGE_TAKEN, "stat.damageTaken", condition), true,
                argName, argValue);
    }

    static ConditionalScarStat raisedWhile(StatTarget target, Predicate<ShipAPI> condition, String argName, float argValue) {
        return new ConditionalScarStat(new ConditionalStatEffect(StatMode.PERCENT, target, null, condition), false, argName, argValue);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, String modId, float magnitude) {
        conditional.advanceInCombat(ship, modId, magnitude);
    }

    @Override
    public StatMode mode() {
        return conditional.mode();
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
        return EffectText.msg(effect).arg("value", Math.abs(magnitude)).arg(argName, argValue).styled();
    }
}
