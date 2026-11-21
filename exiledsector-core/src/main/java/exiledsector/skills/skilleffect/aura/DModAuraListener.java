package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.SkillEffectSupport;

final class DModAuraListener extends AuraSourceListener {

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

    float armorDamageTakenReductionPercent() {
        return percentForEachDMod(ARMOR_PERCENT_PER_DMOD_KEY);
    }

    float topSpeedPercent() {
        return percentForEachDMod(TOP_SPEED_PERCENT_PER_DMOD_KEY);
    }

    private float percentForEachDMod(String percentPerDModKey) {
        return unlessRetreating(magnitude(percentPerDModKey) * countedDMods);
    }
}
