package exiledlib.skills;

import exiledlib.skills.skilleffect.weapon.BallisticAsEnergy;
import exiledlib.skills.skilleffect.weapon.ScopedWeaponEffect;
import exiledlib.skills.skilleffect.weapon.WeaponSkillEffect;

import java.util.ArrayList;
import java.util.List;

public final class BallisticEnergyMirror {

    static final String MOD_ID_PREFIX = "exiledLib_ballisticAsEnergy_";

    public record Origin(Object mirroredOrigin) {
    }

    private BallisticEnergyMirror() {
    }

    static List<EffectContribution> withMirroredBonuses(List<EffectContribution> contributions) {
        if (!ballisticCountsAsEnergy(contributions)) {
            return contributions;
        }
        List<EffectContribution> withMirrors = new ArrayList<>(contributions);
        for (int index = 0; index < contributions.size(); index++) {
            EffectContribution contribution = contributions.get(index);
            List<SkillTypeEffect> mirroredEffects = new ArrayList<>();
            for (SkillTypeEffect effect : contribution.effects()) {
                ScopedWeaponEffect ballisticEffect = BallisticAsEnergy.ballisticCounterpart(effect.effect());
                if (ballisticEffect != null) {
                    mirroredEffects.add(new SkillTypeEffect(ballisticEffect, effect.magnitude()));
                }
            }
            if (!mirroredEffects.isEmpty()) {
                withMirrors.add(new EffectContribution(new Origin(contribution.origin()), MOD_ID_PREFIX + index + "_" + contribution.modIdPrefix(),
                        List.copyOf(mirroredEffects), contribution.temporarySeconds(), null, false, contribution.bonusScaled(), contribution.gate()));
            }
        }
        return withMirrors;
    }

    private static boolean ballisticCountsAsEnergy(List<EffectContribution> contributions) {
        for (EffectContribution contribution : contributions) {
            if (contribution.permanent()) {
                for (SkillTypeEffect effect : contribution.effects()) {
                    if (effect.effect() == WeaponSkillEffect.BALLISTIC_COUNTS_AS_ENERGY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
