package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.List;
import java.util.Map;

final class DModArmorAuraField extends AuraField<DModArmorAuraListener> {

    static final float RANGE = 1000f;
    static final String MOD_ID = "exiledSector_dModArmorAura";
    private static final String CUSTOM_DATA_KEY = "exiledSector_dModArmorAuraField";

    private final AppliedAuraValues appliedReduction = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);

    DModArmorAuraField() {
        super(true);
    }

    public static void register(DModArmorAuraListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, DModArmorAuraField.class, DModArmorAuraField::new);
    }

    @Override
    void update(float elapsedSeconds) {
        appliedReduction.update(strongestPerAlly(sources, ENGINE_SHIPS_NEAR), DModArmorAuraField::remove, DModArmorAuraField::apply);
    }

    static Map<ShipAPI, Float> strongestPerAlly(List<DModArmorAuraListener> sources, ShipsNear shipsNear) {
        return strongestPerShip(sources, shipsNear, RANGE, DModArmorAuraListener::armorDamageTakenReductionPercent, DModArmorAuraField::isCoveredBy);
    }

    static boolean isCoveredBy(ShipAPI sourceShip, ShipAPI other) {
        return CombatQueries.isAlliedShip(sourceShip, other) && !other.isStationModule();
    }

    static void apply(ShipAPI ally, float reductionPercent) {
        ally.getMutableStats().getArmorDamageTakenMult().modifyPercent(MOD_ID, -reductionPercent);
    }

    static void remove(ShipAPI ally) {
        ally.getMutableStats().getArmorDamageTakenMult().unmodifyPercent(MOD_ID);
    }
}
