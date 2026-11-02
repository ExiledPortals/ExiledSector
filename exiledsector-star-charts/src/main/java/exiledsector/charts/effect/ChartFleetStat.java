package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.skills.skilleffect.SkillEffectSupport;

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
    SHIELD_DAMAGE_TAKEN((stats, id, mult) -> stats.getShieldDamageTakenMult().modifyMult(id, mult)),
    ARMOR_DAMAGE_TAKEN((stats, id, mult) -> stats.getArmorDamageTakenMult().modifyMult(id, mult)),
    ARMOR((stats, id, mult) -> stats.getArmorBonus().modifyMult(id, mult)),
    TOP_SPEED((stats, id, mult) -> stats.getMaxSpeed().modifyMult(id, mult)),
    LARGE_SHIP_TOP_SPEED((stats, id, mult) -> {
        if (isLargeShip(stats)) {
            stats.getMaxSpeed().modifyMult(id + ChartFleetStat.LARGE_SHIP_MOD_SUFFIX, mult);
        }
    }),
    MANEUVERABILITY((stats, id, mult) -> {
        stats.getAcceleration().modifyMult(id, mult);
        stats.getDeceleration().modifyMult(id, mult);
        stats.getTurnAcceleration().modifyMult(id, mult);
        stats.getMaxTurnRate().modifyMult(id, mult);
    }),
    SHIELD_ARC((stats, id, mult) -> stats.getShieldArcBonus().modifyMult(id, mult)),
    SHIELD_UPKEEP((stats, id, mult) -> stats.getShieldUpkeepMult().modifyMult(id, mult)),
    SHIELD_RAISE_RATE((stats, id, mult) -> stats.getShieldUnfoldRateMult().modifyMult(id, mult)),
    PHASE_ACTIVATION_COST((stats, id, mult) -> stats.getPhaseCloakActivationCostBonus().modifyMult(id, mult)),
    FLUX_DISSIPATION((stats, id, mult) -> stats.getFluxDissipation().modifyMult(id, mult)),
    WEAPON_AND_ENGINE_REPAIR_SPEED((stats, id, mult) -> {
        stats.getCombatWeaponRepairTimeMult().modifyMult(id, 1f / mult);
        stats.getCombatEngineRepairTimeMult().modifyMult(id, 1f / mult);
    }),
    BALLISTIC_WEAPON_RANGE((stats, id, mult) -> stats.getBallisticWeaponRangeBonus().modifyMult(id, mult)),
    ENERGY_WEAPON_RANGE((stats, id, mult) -> stats.getEnergyWeaponRangeBonus().modifyMult(id, mult)),
    MISSILE_WEAPON_RANGE((stats, id, mult) -> stats.getMissileWeaponRangeBonus().modifyMult(id, mult)),
    DMOD_EFFECT((stats, id, mult) -> SkillEffectSupport.applyDModEffectMult(stats, id, (mult - 1f) * ChartGenContext.PERCENT)),
    DEPLOYMENT_COST((stats, id, mult) -> stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, mult)),
    ENEMY_DEPLOYMENT_COST(Scope.BATTLE, null),
    COMMAND_POINT_RATE(Scope.BATTLE, null),
    ENEMY_OBJECTIVE_CAPTURE_RATE(Scope.ENEMY_SHIPS,
            (stats, id, mult) -> stats.getDynamic().getStat(Stats.SHIP_OBJECTIVE_CAP_RATE_MULT).modifyMult(id, mult)),
    ENEMY_KINETIC_DAMAGE_DEALT(Scope.ENEMY_SHIPS, null),
    ENEMY_HIGH_EXPLOSIVE_DAMAGE_DEALT(Scope.ENEMY_SHIPS, null),
    ENEMY_ENERGY_DAMAGE_DEALT(Scope.ENEMY_SHIPS, null),
    ENEMY_FRAGMENTATION_DAMAGE_DEALT(Scope.ENEMY_SHIPS, null);

    static final float MIN_MULT = 0.1f;
    static final float MAX_MULT = 10f;
    static final String LARGE_SHIP_MOD_SUFFIX = "_large_ship";

    enum Scope {
        PLAYER_SHIPS,
        ENEMY_SHIPS,
        BATTLE
    }

    interface ShipStatApplier {
        void apply(MutableShipStatsAPI stats, String id, float mult);
    }

    private final Scope scope;
    private final ShipStatApplier shipStatApplier;

    ChartFleetStat(ShipStatApplier shipStatApplier) {
        this(Scope.PLAYER_SHIPS, shipStatApplier);
    }

    ChartFleetStat(Scope scope, ShipStatApplier shipStatApplier) {
        this.scope = scope;
        this.shipStatApplier = shipStatApplier;
    }

    public boolean appliesToShips() {
        return scope == Scope.PLAYER_SHIPS;
    }

    public boolean appliesToEnemyShips() {
        return scope == Scope.ENEMY_SHIPS;
    }

    public boolean battleRule() {
        return scope == Scope.BATTLE;
    }

    void applyToShip(MutableShipStatsAPI stats, String id, float mult) {
        if (shipStatApplier != null) {
            shipStatApplier.apply(stats, id, mult);
        }
    }

    private static boolean isLargeShip(MutableShipStatsAPI stats) {
        HullSize hullSize = stats.getVariant() == null ? null : stats.getVariant().getHullSize();
        return hullSize == HullSize.CRUISER || hullSize == HullSize.CAPITAL_SHIP;
    }

    public static float multFor(float percent) {
        return Math.max(MIN_MULT, Math.min(MAX_MULT, 1f + percent / ChartGenContext.PERCENT));
    }
}
