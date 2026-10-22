package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;

import java.util.function.Predicate;
import java.util.function.Supplier;

record HullModStatEffect(StatMode mode, StatTarget target, Supplier<String> statName, boolean lowerIsBetter, String hullModId,
                         boolean builtInOnly, Predicate<ShipAPI> combatCondition) implements EffectBacking {

    static HullModStatEffect installed(StatMode mode, StatTarget target, Supplier<String> statName, boolean lowerIsBetter, String hullModId) {
        return new HullModStatEffect(mode, target, statName, lowerIsBetter, hullModId, false, null);
    }

    static HullModStatEffect builtIn(StatMode mode, StatTarget target, Supplier<String> statName, boolean lowerIsBetter, String hullModId) {
        return new HullModStatEffect(mode, target, statName, lowerIsBetter, hullModId, true, null);
    }

    static HullModStatEffect builtInWhile(StatMode mode, StatTarget target, Supplier<String> statName, String hullModId,
                                          Predicate<ShipAPI> combatCondition) {
        return new HullModStatEffect(mode, target, statName, false, hullModId, true, combatCondition);
    }

    boolean hasHullMod(ShipVariantAPI variant) {
        if (variant == null) {
            return false;
        }
        return builtInOnly ? variant.getSMods().contains(hullModId) : variant.hasHullMod(hullModId);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        if (combatCondition == null) {
            applyWhen(stats, modId, magnitude, hasHullMod(stats.getVariant()));
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, String modId, float magnitude) {
        if (combatCondition != null) {
            applyWhen(ship.getMutableStats(), modId, magnitude, hasHullMod(ship.getVariant()) && combatCondition.test(ship));
        }
    }

    private void applyWhen(MutableShipStatsAPI stats, String modId, float magnitude, boolean active) {
        if (active) {
            target.apply(stats, modId, mode, magnitude);
        } else {
            target.remove(stats, modId);
        }
    }

    @Override
    public StatMode mode() {
        return mode;
    }

    @Override
    public boolean advancesInCombat() {
        return combatCondition != null;
    }

    @Override
    public boolean supportsTemporaryGating() {
        return combatCondition == null && target.supportsTemporaryGating();
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        String stat = Translation.msg(builtInOnly ? "stat.withBuiltInHullMod" : "stat.withHullMod").arg("stat", statName.get())
                .arg("hullmod", hullModName()).text();
        return mode.description(magnitude, stat);
    }

    private String hullModName() {
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
        return spec != null ? spec.getDisplayName() : hullModId;
    }
}
