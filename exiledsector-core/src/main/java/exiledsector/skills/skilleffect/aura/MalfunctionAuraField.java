package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;

import java.util.List;
import java.util.Map;

final class MalfunctionAuraField extends AuraField<MalfunctionAuraListener> {

    static final float RANGE = 1000f;
    static final String MOD_ID = "exiledSector_malfunctionAura";
    private static final String CUSTOM_DATA_KEY = "exiledSector_malfunctionAuraField";
    private static final StatTarget MALFUNCTION_CHANCE = new StatTarget.PercentagePoints(MutableShipStatsAPI::getWeaponMalfunctionChance);

    private final AppliedAuraValues appliedPercent = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);

    MalfunctionAuraField() {
        super(true);
    }

    public static void register(MalfunctionAuraListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, MalfunctionAuraField.class, MalfunctionAuraField::new);
    }

    @Override
    void update(float elapsedSeconds) {
        appliedPercent.update(strongestPerTarget(sources, ENGINE_SHIPS_NEAR), MALFUNCTION_CHANCE, StatMode.PERCENT, MOD_ID);
    }

    static Map<ShipAPI, Float> strongestPerTarget(List<MalfunctionAuraListener> sources, ShipsNear shipsNear) {
        return strongestPerShip(sources, shipsNear, RANGE, MalfunctionAuraListener::malfunctionPercent, AuraField::isHostileShip);
    }
}
