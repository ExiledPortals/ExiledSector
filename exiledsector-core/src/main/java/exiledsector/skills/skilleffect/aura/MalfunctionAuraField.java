package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.List;
import java.util.Map;

final class MalfunctionAuraField extends AuraField<MalfunctionAuraListener> {

    static final float RANGE = 1000f;
    static final String MOD_ID = "exiledSector_malfunctionAura";
    private static final String CUSTOM_DATA_KEY = "exiledSector_malfunctionAuraField";

    private final AppliedAuraValues appliedPercent = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);

    MalfunctionAuraField() {
        super(true);
    }

    public static void register(MalfunctionAuraListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, MalfunctionAuraField.class, MalfunctionAuraField::new);
    }

    @Override
    void update(float elapsedSeconds) {
        appliedPercent.update(strongestPerTarget(sources, ENGINE_SHIPS_NEAR),
                target -> target.getMutableStats().getWeaponMalfunctionChance().unmodify(MOD_ID),
                (target, percent) -> target.getMutableStats().getWeaponMalfunctionChance().modifyFlat(MOD_ID, percent / 100f));
    }

    static Map<ShipAPI, Float> strongestPerTarget(List<MalfunctionAuraListener> sources, ShipsNear shipsNear) {
        return strongestPerShip(sources, shipsNear, RANGE, MalfunctionAuraListener::malfunctionPercent, AuraField::isHostileShip);
    }
}
