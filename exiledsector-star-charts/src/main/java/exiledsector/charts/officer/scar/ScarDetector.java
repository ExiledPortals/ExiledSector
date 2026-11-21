package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.officer.scar.ScarEvidence.FiredAs;

import java.util.Locale;
import java.util.function.ToDoubleFunction;

public enum ScarDetector {
    MISSILE_SHARE(evidence -> evidence.largestShare(FiredAs.MISSILE, ScarEvidence.DELIVERIES)),
    STRIKE_SHARE(evidence -> evidence.share(FiredAs.STRIKE)),
    MINE_SHARE(evidence -> evidence.share(FiredAs.MINE)),
    KINETIC_SHARE(evidence -> evidence.largestShare(FiredAs.KINETIC, ScarEvidence.DAMAGE_TYPES)),
    HIGH_EXPLOSIVE_SHARE(evidence -> evidence.largestShare(FiredAs.HIGH_EXPLOSIVE, ScarEvidence.DAMAGE_TYPES)),
    ENERGY_SHARE(evidence -> evidence.largestShare(FiredAs.ENERGY, ScarEvidence.DAMAGE_TYPES)),
    FRAGMENTATION_SHARE(evidence -> evidence.share(FiredAs.FRAGMENTATION)),
    EMP_SHARE(ScarEvidence::empShare),
    BEAM_SHARE(evidence -> evidence.largestShare(FiredAs.BEAM, ScarEvidence.DELIVERIES)),
    FIGHTER_SHARE(evidence -> evidence.largestShare(FiredAs.FIGHTER, ScarEvidence.DELIVERIES)),
    BOMBER_SHARE(evidence -> evidence.share(FiredAs.BOMBER)),
    PHASE_ATTACKER_SHARE(evidence -> evidence.share(FiredAs.PHASE_ATTACKER)),
    REAR_HIT_SHARE(evidence -> evidence.share(FiredAs.REAR)),
    BEYOND_RANGE_SHARE(evidence -> evidence.share(FiredAs.BEYOND_RANGE)),
    KITED(ScarEvidence::kitedShare),
    COLLISION_SHARE(evidence -> evidence.share(FiredAs.COLLISION)),
    OVERLOADS(ScarEvidence::overloads),
    HIGH_FLUX_SECONDS(ScarEvidence::highFluxSeconds),
    SWARM_ATTACKERS(ScarEvidence::mostAttackersInWindow),
    PLAYER_FLAGSHIP_KILL(evidence -> evidence.killedByPlayerFlagship() ? 1f : 0f),
    QUICK_DEATH(ScarEvidence::quickDeath),
    PEAK_CR_RAN_OUT(evidence -> evidence.peakCrRanOutBeforeDeath() ? 1f : 0f),
    MALFUNCTIONS(ScarEvidence::malfunctions),
    ESCORT_LOSSES(ScarEvidence::escortLossShareAtFirstHit),
    SYSTEM_ACTIVE_SHARE(evidence -> evidence.share(FiredAs.SYSTEM_ACTIVE)),
    OUT_OF_RANGE_TIME(ScarEvidence::outOfRangeShare),
    FIGHTER_BAY_SHIP_SHARE(evidence -> evidence.fleet().shipShare(ScarFleetSnapshot.DeployedShip::fighterBays)),
    PHASE_SHIP_SHARE(evidence -> evidence.fleet().shipShare(ScarFleetSnapshot.DeployedShip::phase)),
    FRIGATE_SHARE(evidence -> evidence.fleet().shipShare(ship -> ship.hullSize() == HullSize.FRIGATE)),
    DESTROYER_LEAD(evidence -> evidence.fleet().leadingHullSizeShare(HullSize.DESTROYER)),
    CRUISER_LEAD(evidence -> evidence.fleet().leadingHullSizeShare(HullSize.CRUISER)),
    CAPITAL_LEAD(evidence -> evidence.fleet().leadingHullSizeShare(HullSize.CAPITAL_SHIP)),
    LOW_TECH_SHARE(evidence -> evidence.fleet().designShare(ScarFleetSnapshot.LOW_TECH)),
    HIGH_TECH_SHARE(evidence -> evidence.fleet().designShare(ScarFleetSnapshot.HIGH_TECH)),
    AUTOMATED_SHARE(evidence -> evidence.fleet().shipShare(ScarFleetSnapshot.DeployedShip::automated)),
    SAFETY_OVERRIDES_SHARE(evidence -> evidence.fleet().shipShare(ScarFleetSnapshot.DeployedShip::safetyOverrides)),
    PRISTINE_SHARE(evidence -> evidence.fleet().shipShare(ship -> ship.dMods() == 0)),
    AVERAGE_D_MODS(evidence -> evidence.fleet().averageDMods()),
    ELECTRONIC_WARFARE_LEAD(evidence -> evidence.fleet().electronicWarfareLead()),
    OBJECTIVES_HELD(evidence -> evidence.fleet().objectivesHeld()),
    RANGE_RATIO(evidence -> evidence.fleet().rangeRatio()),
    BEAM_OP_SHARE(evidence -> evidence.fleet().weaponOpShare(ScarFleetSnapshot.WeaponOp::beam)),
    KINETIC_OP_SHARE(evidence -> evidence.fleet().weaponOpShare(ScarFleetSnapshot.WeaponOp::kinetic)),
    MISSILE_OP_SHARE(evidence -> evidence.fleet().weaponOpShare(ScarFleetSnapshot.WeaponOp::missile)),
    EMP_OP_SHARE(evidence -> evidence.fleet().weaponOpShare(ScarFleetSnapshot.WeaponOp::emp)),
    SPEED_RATIO(evidence -> evidence.fleet().speedRatio()),
    OFFICER_LEVEL_LEAD(evidence -> evidence.fleet().officerLevelLead()),
    DEPLOYED_DP_RATIO(evidence -> evidence.fleet().deploymentPointsRatio());

    private final ToDoubleFunction<ScarEvidence> measure;

    ScarDetector(ToDoubleFunction<ScarEvidence> measure) {
        this.measure = measure;
    }

    public float measure(ScarEvidence evidence) {
        return (float) measure.applyAsDouble(evidence);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    static ScarDetector byId(String id) {
        for (ScarDetector detector : values()) {
            if (detector.id().equals(id)) {
                return detector;
            }
        }
        return null;
    }
}
