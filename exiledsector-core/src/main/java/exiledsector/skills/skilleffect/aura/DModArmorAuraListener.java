package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;

final class DModArmorAuraListener extends ShipCombatListener implements AuraField.Source {

    static final String PERCENT_PER_DMOD_KEY = "exiledSector_dModArmorAuraPercent";

    private final int countedDMods;

    DModArmorAuraListener(ShipAPI ownerShip) {
        super(ownerShip);
        countedDMods = SkillEffectSupport.countedDMods(ownerShip.getVariant());
        if (countedDMods > 0) {
            DModArmorAuraField.register(this);
        }
    }

    @Override
    public ShipAPI source() {
        return ownerShip;
    }

    float armorDamageTakenReductionPercent() {
        return ownerShip.isRetreating() ? 0f : magnitude(PERCENT_PER_DMOD_KEY) * countedDMods;
    }
}
