package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import exiledsector.skills.AffinitySteps;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.defense.ArmorCells;
import exiledsector.skills.skilleffect.logistics.LogisticsSkillEffect;

import java.util.ArrayList;
import java.util.List;

public final class AffinityStrikecraft {

    private AffinityStrikecraft() {
    }

    public static boolean granted(List<EffectContribution> contributions) {
        for (EffectContribution contribution : contributions) {
            if (contribution.permanent()) {
                for (SkillTypeEffect effect : contribution.effects()) {
                    if (effect.effect() == FighterSkillEffect.STRIKECRAFT_GAIN_AFFINITY_BONUSES) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void apply(ShipAPI fighter, List<EffectContribution> contributions) {
        if (!granted(contributions)) {
            return;
        }
        List<CarriedBonus> carried = carriedBonuses(contributions);
        if (carried.isEmpty()) {
            return;
        }
        MutableShipStatsAPI stats = fighter.getMutableStats();
        ShipHullSpecAPI hullSpec = fighter.getHullSpec();
        float hullBase = hullSpec.getHitpoints();
        float armorBase = hullSpec.getArmorRating();
        float hullBefore = effective(stats.getHullBonus(), hullBase);
        float armorBefore = effective(stats.getArmorBonus(), armorBase);

        carried.forEach(bonus -> bonus.effect().apply(stats, bonus.modId(), bonus.magnitude()));

        rescaleHull(fighter, ratio(hullBefore, effective(stats.getHullBonus(), hullBase)));
        rescaleArmor(fighter.getArmorGrid(), ratio(armorBefore, effective(stats.getArmorBonus(), armorBase)));
        carried.forEach(bonus -> bonus.effect().applyAfterShipCreation(fighter, bonus.modId(), bonus.magnitude()));
    }

    static boolean carriesOver(SkillEffect effect) {
        return !(effect instanceof LogisticsSkillEffect) && !(effect instanceof FighterSkillEffect);
    }

    private static List<CarriedBonus> carriedBonuses(List<EffectContribution> contributions) {
        List<CarriedBonus> carried = new ArrayList<>();
        for (EffectContribution contribution : contributions) {
            if (contribution.origin() instanceof AffinitySteps.Origin || contribution.origin() instanceof AffinitySteps.ScaledOrigin) {
                for (SkillTypeEffect bonus : contribution.effects()) {
                    if (carriesOver(bonus.effect())) {
                        carried.add(new CarriedBonus(bonus.effect(), contribution.modIdPrefix() + bonus.effect().name(), bonus.magnitude()));
                    }
                }
            }
        }
        return carried;
    }

    private static float effective(StatBonus bonus, float base) {
        return base > 0f ? bonus.computeEffective(base) : 0f;
    }

    private static float ratio(float before, float after) {
        return before > 0f ? after / before : 1f;
    }

    private static void rescaleHull(ShipAPI fighter, float hullRatio) {
        if (hullRatio != 1f) {
            fighter.setMaxHitpoints(fighter.getMaxHitpoints() * hullRatio);
            fighter.setHitpoints(fighter.getHitpoints() * hullRatio);
        }
    }

    private static void rescaleArmor(ArmorGridAPI grid, float armorRatio) {
        if (armorRatio != 1f && grid != null) {
            ArmorCells.update(grid, current -> current * armorRatio);
        }
    }

    private record CarriedBonus(SkillEffect effect, String modId, float magnitude) {
    }
}
