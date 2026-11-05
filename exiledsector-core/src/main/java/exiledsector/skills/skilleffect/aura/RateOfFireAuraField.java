package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class RateOfFireAuraField extends AuraField<RateOfFireAuraListener> {

    static final float RANGE = 1000f;
    static final String MOD_ID = "exiledSector_rateOfFireAura";
    private static final String CUSTOM_DATA_KEY = "exiledSector_rateOfFireAuraField";

    private final AppliedAuraValues appliedPercent = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);

    RateOfFireAuraField() {
        super(true);
    }

    public static void register(RateOfFireAuraListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, RateOfFireAuraField.class, RateOfFireAuraField::new);
    }

    @Override
    void update(float elapsedSeconds) {
        appliedPercent.update(strongestPerTarget(sources, ENGINE_SHIPS_NEAR), RateOfFireAuraField::remove, RateOfFireAuraField::apply);
    }

    static void apply(ShipAPI target, float percent) {
        MutableShipStatsAPI stats = target.getMutableStats();
        float mult = Math.max(0f, 1f - percent / 100f);
        stats.getBallisticRoFMult().modifyMult(MOD_ID, mult);
        stats.getEnergyRoFMult().modifyMult(MOD_ID, mult);
        stats.getMissileRoFMult().modifyMult(MOD_ID, mult);
    }

    static void remove(ShipAPI target) {
        MutableShipStatsAPI stats = target.getMutableStats();
        stats.getBallisticRoFMult().unmodifyMult(MOD_ID);
        stats.getEnergyRoFMult().unmodifyMult(MOD_ID);
        stats.getMissileRoFMult().unmodifyMult(MOD_ID);
    }

    static Map<ShipAPI, Float> strongestPerTarget(List<RateOfFireAuraListener> sources, ShipsNear shipsNear) {
        Map<ShipAPI, Float> strongest = new IdentityHashMap<>();
        for (RateOfFireAuraListener source : sources) {
            float percent = source.rateOfFirePenaltyPercent();
            if (percent <= 0f) {
                continue;
            }
            ShipAPI sourceShip = source.source();
            for (ShipAPI target : shipsNear.find(sourceShip, RANGE, other -> !other.isFighter() && CombatQueries.isAliveNotHulk(other)
                    && CombatQueries.isHostile(sourceShip, other)
                    && CombatQueries.withinRadius(other.getLocation(), sourceShip.getLocation(), RANGE))) {
                strongest.merge(target, percent, Math::max);
            }
        }
        return strongest;
    }
}
