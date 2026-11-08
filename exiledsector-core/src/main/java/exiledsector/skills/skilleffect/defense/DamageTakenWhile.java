package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.ConditionalStatEffect;
import exiledsector.skills.skilleffect.DescribeContext;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import static exiledsector.skills.skilleffect.StatTarget.all;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;

record DamageTakenWhile(ConditionalStatEffect conditional, ToDoubleFunction<DescribeContext> describedRange) implements EffectBacking {

    private static final StatTarget DAMAGE_TAKEN = all(liveStat(MutableShipStatsAPI::getHullDamageTakenMult),
            liveStat(MutableShipStatsAPI::getArmorDamageTakenMult), liveStat(MutableShipStatsAPI::getShieldDamageTakenMult));

    static DamageTakenWhile of(Predicate<ShipAPI> condition) {
        return within(condition, null);
    }

    static DamageTakenWhile within(Predicate<ShipAPI> condition, ToDoubleFunction<DescribeContext> describedRange) {
        return new DamageTakenWhile(new ConditionalStatEffect(StatMode.PERCENT, DAMAGE_TAKEN, "stat.damageTaken", condition), describedRange);
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
    public boolean lowerIsBetter() {
        return true;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return describe(effect, magnitude, DescribeContext.NONE);
    }

    StyledText describe(SkillEffect effect, float magnitude, DescribeContext context) {
        Message line = EffectText.msg(effect).arg("value", Math.abs(magnitude));
        return describedRange == null ? line.styled() : line.arg("range", (float) describedRange.applyAsDouble(context)).styled();
    }
}
