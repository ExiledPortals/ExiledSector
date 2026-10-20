package exiledsector.charts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

public enum ChartFleetStat {

    FRAGMENTATION_DAMAGE_TAKEN((stats, id, mult) -> {
        stats.getFragmentationDamageTakenMult().modifyMult(id, mult);
        stats.getFragmentationShieldDamageTakenMult().modifyMult(id, mult);
    }),
    KINETIC_DAMAGE_TAKEN((stats, id, mult) -> {
        stats.getKineticDamageTakenMult().modifyMult(id, mult);
        stats.getKineticShieldDamageTakenMult().modifyMult(id, mult);
    }),
    HIGH_EXPLOSIVE_DAMAGE_TAKEN((stats, id, mult) -> {
        stats.getHighExplosiveDamageTakenMult().modifyMult(id, mult);
        stats.getHighExplosiveShieldDamageTakenMult().modifyMult(id, mult);
    }),
    ENERGY_DAMAGE_TAKEN((stats, id, mult) -> {
        stats.getEnergyDamageTakenMult().modifyMult(id, mult);
        stats.getEnergyShieldDamageTakenMult().modifyMult(id, mult);
    }),
    TOP_SPEED((stats, id, mult) -> stats.getMaxSpeed().modifyMult(id, mult)),
    MANEUVERABILITY((stats, id, mult) -> {
        stats.getAcceleration().modifyMult(id, mult);
        stats.getDeceleration().modifyMult(id, mult);
        stats.getTurnAcceleration().modifyMult(id, mult);
        stats.getMaxTurnRate().modifyMult(id, mult);
    }),
    SHIELD_ARC((stats, id, mult) -> stats.getShieldArcBonus().modifyMult(id, mult)),
    PHASE_ACTIVATION_COST((stats, id, mult) -> stats.getPhaseCloakActivationCostBonus().modifyMult(id, mult)),
    FLUX_DISSIPATION((stats, id, mult) -> stats.getFluxDissipation().modifyMult(id, mult)),
    WEAPON_RANGE((stats, id, mult) -> {
        stats.getBallisticWeaponRangeBonus().modifyMult(id, mult);
        stats.getEnergyWeaponRangeBonus().modifyMult(id, mult);
        stats.getMissileWeaponRangeBonus().modifyMult(id, mult);
    }),
    DEPLOYMENT_POINTS(null),
    ENEMY_DEPLOYMENT_POINTS(null),
    COMMAND_POINT_RATE(null),
    ENEMY_OBJECTIVE_CAPTURE_RATE(null);

    static final float MIN_MULT = 0.1f;
    static final float MAX_MULT = 10f;

    interface ShipStatApplier {
        void apply(MutableShipStatsAPI stats, String id, float mult);
    }

    private final ShipStatApplier shipStatApplier;

    ChartFleetStat(ShipStatApplier shipStatApplier) {
        this.shipStatApplier = shipStatApplier;
    }

    public boolean appliesToShips() {
        return shipStatApplier != null;
    }

    void applyToShip(MutableShipStatsAPI stats, String id, float mult) {
        if (shipStatApplier != null) {
            shipStatApplier.apply(stats, id, mult);
        }
    }

    static float multFor(float percent) {
        return Math.max(MIN_MULT, Math.min(MAX_MULT, 1f + percent / ChartGenContext.PERCENT));
    }
}
