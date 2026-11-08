package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.List;
import java.util.Map;

final class DModAuraField extends AuraField<DModAuraListener> {

    static final float RANGE = 1000f;
    static final String ARMOR_MOD_ID = "exiledSector_dModArmorAura";
    static final String TOP_SPEED_MOD_ID = "exiledSector_dModTopSpeedAura";
    private static final String CUSTOM_DATA_KEY = "exiledSector_dModArmorAuraField";

    private final AppliedAuraValues appliedArmorReduction = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);
    private final AppliedAuraValues appliedTopSpeedBonus = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);

    DModAuraField() {
        super(true);
    }

    public static void register(DModAuraListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, DModAuraField.class, DModAuraField::new);
    }

    @Override
    void update(float elapsedSeconds) {
        appliedArmorReduction.update(strongestArmorReductionPerAlly(sources, ENGINE_SHIPS_NEAR), DModAuraField::removeArmor,
                DModAuraField::applyArmor);
        appliedTopSpeedBonus.update(strongestTopSpeedBonusPerAlly(sources, ENGINE_SHIPS_NEAR), DModAuraField::removeTopSpeed,
                DModAuraField::applyTopSpeed);
    }

    static Map<ShipAPI, Float> strongestArmorReductionPerAlly(List<DModAuraListener> sources, ShipsNear shipsNear) {
        return strongestPerShip(sources, shipsNear, RANGE, DModAuraListener::armorDamageTakenReductionPercent, DModAuraField::isCoveredBy);
    }

    static Map<ShipAPI, Float> strongestTopSpeedBonusPerAlly(List<DModAuraListener> sources, ShipsNear shipsNear) {
        return strongestPerShip(sources, shipsNear, RANGE, DModAuraListener::topSpeedPercent, DModAuraField::isCoveredBy);
    }

    static boolean isCoveredBy(ShipAPI sourceShip, ShipAPI other) {
        return CombatQueries.isAlliedShip(sourceShip, other) && !other.isStationModule();
    }

    static void applyArmor(ShipAPI ally, float reductionPercent) {
        ally.getMutableStats().getArmorDamageTakenMult().modifyPercent(ARMOR_MOD_ID, -reductionPercent);
    }

    static void removeArmor(ShipAPI ally) {
        ally.getMutableStats().getArmorDamageTakenMult().unmodifyPercent(ARMOR_MOD_ID);
    }

    static void applyTopSpeed(ShipAPI ally, float bonusPercent) {
        ally.getMutableStats().getMaxSpeed().modifyPercent(TOP_SPEED_MOD_ID, bonusPercent);
    }

    static void removeTopSpeed(ShipAPI ally) {
        ally.getMutableStats().getMaxSpeed().unmodifyPercent(TOP_SPEED_MOD_ID);
    }
}
