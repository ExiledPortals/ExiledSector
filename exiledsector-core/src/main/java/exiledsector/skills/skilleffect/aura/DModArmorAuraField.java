package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.IdentityHashMap;
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
        Map<ShipAPI, Float> strongest = new IdentityHashMap<>();
        for (DModArmorAuraListener source : sources) {
            float reduction = source.armorDamageTakenReductionPercent();
            if (reduction <= 0f) {
                continue;
            }
            ShipAPI sourceShip = source.source();
            for (ShipAPI ally : shipsNear.find(sourceShip, RANGE, other -> isCoveredBy(sourceShip, other))) {
                strongest.merge(ally, reduction, Float::max);
            }
        }
        return strongest;
    }

    static boolean isCoveredBy(ShipAPI sourceShip, ShipAPI other) {
        return other != sourceShip && other.getOwner() == sourceShip.getOwner() && !other.isFighter() && !other.isDrone()
                && !other.isStationModule() && CombatQueries.isAliveNotHulk(other)
                && CombatQueries.withinRadius(other.getLocation(), sourceShip.getLocation(), RANGE);
    }

    static void apply(ShipAPI ally, float reductionPercent) {
        ally.getMutableStats().getArmorDamageTakenMult().modifyPercent(MOD_ID, -reductionPercent);
    }

    static void remove(ShipAPI ally) {
        ally.getMutableStats().getArmorDamageTakenMult().unmodifyPercent(MOD_ID);
    }
}
