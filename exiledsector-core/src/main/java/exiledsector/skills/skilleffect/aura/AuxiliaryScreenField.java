package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;

import java.util.List;
import java.util.Map;

final class AuxiliaryScreenField extends AuraField<AuxiliaryScreenListener> {

    static final float RANGE = 800f;
    static final float MAX_REDUCTION_PERCENT = 90f;
    static final String MOD_ID = "exiledSector_auxiliaryScreen";
    private static final String CUSTOM_DATA_KEY = "exiledSector_auxiliaryScreenField";
    private static final StatTarget DAMAGE_TAKEN_REDUCTION = StatTarget.scaled(StatTarget.DAMAGE_TAKEN, -1f);

    private final AppliedAuraValues appliedReduction = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);

    AuxiliaryScreenField() {
        super(true);
    }

    public static void register(AuxiliaryScreenListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, AuxiliaryScreenField.class, AuxiliaryScreenField::new);
    }

    @Override
    void update(float elapsedSeconds) {
        appliedReduction.update(reductionPerAlly(sources, ENGINE_SHIPS_NEAR), DAMAGE_TAKEN_REDUCTION, StatMode.MULT, MOD_ID);
    }

    static Map<ShipAPI, Float> reductionPerAlly(List<AuxiliaryScreenListener> sources, ShipsNear shipsNear) {
        return strongestPerShip(sources, shipsNear, AuxiliaryScreenListener::range, AuxiliaryScreenListener::damageTakenReductionPercent, AuxiliaryScreenField::isScreenedBy);
    }

    static boolean isScreenedBy(ShipAPI sourceShip, ShipAPI other) {
        return CombatQueries.isAlliedShip(sourceShip, other) && !other.isStationModule()
                && other.getHullSize().ordinal() < sourceShip.getHullSize().ordinal();
    }
}
