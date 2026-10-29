package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.AffinitySteps;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.defense.ArmorCells;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.skills.skilleffect.flux.FluxSkillEffect;

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
        for (EffectContribution contribution : contributions) {
            if (contribution.origin() instanceof AffinitySteps.Origin) {
                for (SkillTypeEffect bonus : contribution.effects()) {
                    applyBonus(fighter, contribution.modIdPrefix() + bonus.effect().name(), bonus);
                }
            }
        }
    }

    static void applyBonus(ShipAPI fighter, String modId, SkillTypeEffect bonus) {
        SkillEffect effect = bonus.effect();
        float multiplier = 1f + bonus.magnitude() / 100f;
        if (effect == DefenseSkillEffect.HULL_MULT) {
            fighter.setMaxHitpoints(fighter.getMaxHitpoints() * multiplier);
            fighter.setHitpoints(fighter.getHitpoints() * multiplier);
        } else if (effect == DefenseSkillEffect.ARMOR_MULT) {
            fighter.getMutableStats().getArmorBonus().modifyMult(modId, multiplier);
            ArmorGridAPI grid = fighter.getArmorGrid();
            if (grid != null) {
                ArmorCells.update(grid, current -> current * multiplier);
            }
        } else if (effect == FluxSkillEffect.FLUX_CAPACITY_MULT || effect == FluxSkillEffect.FLUX_DISSIPATION_MULT) {
            effect.apply(fighter.getMutableStats(), modId, bonus.magnitude());
        }
    }
}
