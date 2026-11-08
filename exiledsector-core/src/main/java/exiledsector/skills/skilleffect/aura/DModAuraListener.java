package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;

final class DModAuraListener extends ShipCombatListener implements AuraField.Source {

    static final String ARMOR_PERCENT_PER_DMOD_KEY = "exiledSector_dModArmorAuraPercent";
    static final String TOP_SPEED_PERCENT_PER_DMOD_KEY = "exiledSector_dModTopSpeedAuraPercent";

    private final int countedDMods;

    DModAuraListener(ShipAPI ownerShip) {
        super(ownerShip);
        countedDMods = SkillEffectSupport.countedDMods(ownerShip.getVariant());
        if (countedDMods > 0) {
            DModAuraField.register(this);
        }
    }

    @Override
    public ShipAPI source() {
        return ownerShip;
    }

    float armorDamageTakenReductionPercent() {
        return percentForEachDMod(ARMOR_PERCENT_PER_DMOD_KEY);
    }

    float topSpeedPercent() {
        return percentForEachDMod(TOP_SPEED_PERCENT_PER_DMOD_KEY);
    }

    private float percentForEachDMod(String percentPerDModKey) {
        return ownerShip.isRetreating() ? 0f : magnitude(percentPerDModKey) * countedDMods;
    }
}
