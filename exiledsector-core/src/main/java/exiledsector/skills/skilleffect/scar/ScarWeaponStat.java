package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

record ScarWeaponStat(WeaponStatFamily family) implements EffectBacking {

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        family.target(WeaponScope.ALL).apply(stats, modId, StatMode.PERCENT, magnitude);
    }

    @Override
    public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
        family.target(WeaponScope.ALL).applyAfterShipCreation(ship);
    }

    @Override
    public StatMode mode() {
        return StatMode.PERCENT;
    }

    @Override
    public boolean supportsTemporaryGating() {
        return family.target(WeaponScope.ALL).supportsTemporaryGating();
    }

    @Override
    public boolean lowerIsBetter() {
        return family.lowerIsBetter();
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return family.description(WeaponScope.ALL, StatMode.PERCENT, magnitude);
    }
}
