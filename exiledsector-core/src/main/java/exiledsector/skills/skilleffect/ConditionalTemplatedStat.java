package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public record ConditionalTemplatedStat(ConditionalStatEffect conditional, boolean lowerIsBetter, String argName,
                                       ToDoubleFunction<DescribeContext> argValue) implements EffectBacking {

    public static ConditionalTemplatedStat damageTakenWhile(Predicate<ShipAPI> condition) {
        return damageTakenWhile(condition, null, null);
    }

    public static ConditionalTemplatedStat damageTakenWhile(Predicate<ShipAPI> condition, String argName, float argValue) {
        return damageTakenWhile(condition, argName, context -> argValue);
    }

    public static ConditionalTemplatedStat damageTakenWhile(Predicate<ShipAPI> condition, String argName, ToDoubleFunction<DescribeContext> argValue) {
        return new ConditionalTemplatedStat(new ConditionalStatEffect(StatMode.PERCENT, StatTarget.DAMAGE_TAKEN, "stat.damageTaken", condition), true,
                argName, argValue);
    }

    public static ConditionalTemplatedStat raisedWhile(StatTarget target, Predicate<ShipAPI> condition, String argName, float argValue) {
        return new ConditionalTemplatedStat(new ConditionalStatEffect(StatMode.PERCENT, target, null, condition), false, argName, context -> argValue);
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
    public boolean invertsDescriptionColours() {
        return false;
    }

    @Override
    public boolean describesWithContext() {
        return true;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return description(effect, magnitude, DescribeContext.NONE);
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude, DescribeContext context) {
        Message line = EffectText.msg(effect).arg("value", Math.abs(magnitude));
        return argName == null ? line.styled() : line.arg(argName, (float) argValue.applyAsDouble(context)).styled();
    }
}
