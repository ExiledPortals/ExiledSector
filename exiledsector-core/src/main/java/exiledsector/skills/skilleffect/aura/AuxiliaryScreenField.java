package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class AuxiliaryScreenField extends AuraField<AuxiliaryScreenListener> {

    static final float RANGE = 800f;
    static final float MAX_REDUCTION_PERCENT = 90f;
    static final String MOD_ID = "exiledSector_auxiliaryScreen";
    private static final String CUSTOM_DATA_KEY = "exiledSector_auxiliaryScreenField";

    private final AppliedAuraValues appliedReduction = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);

    AuxiliaryScreenField() {
        super(true);
    }

    public static void register(AuxiliaryScreenListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, AuxiliaryScreenField.class, AuxiliaryScreenField::new);
    }

    @Override
    void update(float elapsedSeconds) {
        appliedReduction.update(reductionPerAlly(sources, ENGINE_SHIPS_NEAR), AuxiliaryScreenField::remove, AuxiliaryScreenField::apply);
    }

    static Map<ShipAPI, Float> reductionPerAlly(List<AuxiliaryScreenListener> sources, ShipsNear shipsNear) {
        Map<ShipAPI, Float> reductions = new IdentityHashMap<>();
        for (AuxiliaryScreenListener source : sources) {
            float reduction = source.damageTakenReductionPercent();
            if (reduction <= 0f) {
                continue;
            }
            ShipAPI sourceShip = source.source();
            for (ShipAPI ally : shipsNear.find(sourceShip, RANGE, other -> isScreenedBy(sourceShip, other))) {
                reductions.merge(ally, reduction, Float::max);
            }
        }
        return reductions;
    }

    static boolean isScreenedBy(ShipAPI sourceShip, ShipAPI other) {
        return other != sourceShip && other.getOwner() == sourceShip.getOwner() && !other.isFighter() && !other.isDrone()
                && !other.isStationModule() && CombatQueries.isAliveNotHulk(other)
                && other.getHullSize().ordinal() < sourceShip.getHullSize().ordinal()
                && CombatQueries.withinRadius(other.getLocation(), sourceShip.getLocation(), RANGE);
    }

    private static void apply(ShipAPI ally, float reductionPercent) {
        float mult = 1f - reductionPercent / 100f;
        MutableShipStatsAPI stats = ally.getMutableStats();
        stats.getHullDamageTakenMult().modifyMult(MOD_ID, mult);
        stats.getArmorDamageTakenMult().modifyMult(MOD_ID, mult);
        stats.getShieldDamageTakenMult().modifyMult(MOD_ID, mult);
    }

    private static void remove(ShipAPI ally) {
        MutableShipStatsAPI stats = ally.getMutableStats();
        stats.getHullDamageTakenMult().unmodify(MOD_ID);
        stats.getArmorDamageTakenMult().unmodify(MOD_ID);
        stats.getShieldDamageTakenMult().unmodify(MOD_ID);
    }
}
