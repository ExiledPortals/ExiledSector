package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class ScarFleetSnapshot {

    static final String LOW_TECH = "Low Tech";
    static final String HIGH_TECH = "High Tech";

    public record DeployedShip(HullSize hullSize, float deploymentPoints, String designType, boolean automated,
                               boolean safetyOverrides, int dMods, boolean fighterBays, boolean phase, int officerLevel,
                               float longestRange, float topSpeed, WeaponOp weaponOp) {

        static DeployedShip of(ShipAPI ship) {
            ShipVariantAPI variant = ship.getVariant();
            ShipHullSpecAPI hull = ship.getHullSpec();
            FleetMemberAPI member = ship.getFleetMember();
            int officerLevel = ship.getCaptain() == null || ship.getCaptain().isDefault() ? 0 : ship.getCaptain().getStats().getLevel();
            return new DeployedShip(ship.getHullSize(), member == null ? 0f : member.getDeploymentPointsCost(),
                    hull == null ? null : hull.getManufacturer(), variant != null && variant.hasHullMod(HullMods.AUTOMATED),
                    variant != null && variant.hasHullMod(HullMods.SAFETYOVERRIDES), dModCount(variant),
                    ship.getNumFighterBays() > 0, hull != null && hull.isPhase(), officerLevel, ScarFleetSnapshot.longestRange(ship),
                    ship.getMaxSpeedWithoutBoost(), WeaponOp.of(ship));
        }
    }

    public record WeaponOp(float total, float beam, float kinetic, float missile, float emp) {

        static final WeaponOp NONE = new WeaponOp(0f, 0f, 0f, 0f, 0f);

        static WeaponOp of(ShipAPI ship) {
            float total = 0f;
            float beam = 0f;
            float kinetic = 0f;
            float missile = 0f;
            float emp = 0f;
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.isDecorative() || weapon.getSlot() != null && weapon.getSlot().isBuiltIn()) {
                    continue;
                }
                float op = weapon.getSpec().getOrdnancePointCost(null);
                total += op;
                beam += weapon.isBeam() ? op : 0f;
                kinetic += weapon.getDamageType() == DamageType.KINETIC ? op : 0f;
                missile += weapon.getType() == WeaponType.MISSILE ? op : 0f;
                emp += weapon.getDerivedStats() != null && weapon.getDerivedStats().getEmpPerSecond() > 0f ? op : 0f;
            }
            return new WeaponOp(total, beam, kinetic, missile, emp);
        }
    }

    private final Set<String> seenShipIds = new HashSet<>();
    private final List<DeployedShip> playerShips = new ArrayList<>();
    private float bossSideDeploymentPoints;
    private float flagshipLongestRange;
    private float flagshipTopSpeed;
    private int officerLevel;
    private float electronicWarfareLead;
    private float objectiveShareSum;
    private int objectiveSamples;

    boolean firstSighting(String shipId) {
        return seenShipIds.add(shipId);
    }

    void addPlayerShip(DeployedShip ship) {
        playerShips.add(ship);
    }

    void addBossSideDeploymentPoints(float deploymentPoints) {
        bossSideDeploymentPoints += deploymentPoints;
    }

    void flagship(float longestRange, float topSpeed, int flagshipOfficerLevel) {
        flagshipLongestRange = longestRange;
        flagshipTopSpeed = topSpeed;
        officerLevel = flagshipOfficerLevel;
    }

    float flagshipLongestRange() {
        return flagshipLongestRange;
    }

    void noteElectronicWarfareLead(float lead) {
        electronicWarfareLead = Math.max(electronicWarfareLead, lead);
    }

    void sampleObjectives(int held, int total) {
        if (total > 0) {
            objectiveShareSum += held / (float) total;
            objectiveSamples++;
        }
    }

    float shipShare(Predicate<DeployedShip> test) {
        if (playerShips.isEmpty()) {
            return 0f;
        }
        int matching = 0;
        for (DeployedShip ship : playerShips) {
            if (test.test(ship)) {
                matching++;
            }
        }
        return matching / (float) playerShips.size();
    }

    float leadingHullSizeShare(HullSize hullSize) {
        Map<HullSize, Float> points = new EnumMap<>(HullSize.class);
        float total = 0f;
        for (DeployedShip ship : playerShips) {
            if (ship.hullSize() != null) {
                points.merge(ship.hullSize(), ship.deploymentPoints(), Float::sum);
                total += ship.deploymentPoints();
            }
        }
        float own = points.getOrDefault(hullSize, 0f);
        if (total <= 0f || own <= 0f) {
            return 0f;
        }
        for (Float other : points.values()) {
            if (other > own) {
                return 0f;
            }
        }
        return own / total;
    }

    float designShare(String designType) {
        return shipShare(ship -> designType.equals(ship.designType()));
    }

    float averageDMods() {
        if (playerShips.isEmpty()) {
            return 0f;
        }
        float sum = 0f;
        for (DeployedShip ship : playerShips) {
            sum += ship.dMods();
        }
        return sum / playerShips.size();
    }

    float electronicWarfareLead() {
        return electronicWarfareLead;
    }

    float objectivesHeld() {
        return objectiveSamples == 0 ? 0f : objectiveShareSum / objectiveSamples;
    }

    float rangeRatio() {
        return flagshipLongestRange <= 0f ? 0f : average(DeployedShip::longestRange) / flagshipLongestRange;
    }

    float speedRatio() {
        return flagshipTopSpeed <= 0f ? 0f : average(DeployedShip::topSpeed) / flagshipTopSpeed;
    }

    float officerLevelLead() {
        float sum = 0f;
        int officers = 0;
        for (DeployedShip ship : playerShips) {
            if (ship.officerLevel() > 0) {
                sum += ship.officerLevel();
                officers++;
            }
        }
        return officers == 0 ? 0f : sum / officers - officerLevel;
    }

    float deploymentPointsRatio() {
        float playerPoints = 0f;
        for (DeployedShip ship : playerShips) {
            playerPoints += ship.deploymentPoints();
        }
        return playerPoints <= 0f ? 0f : bossSideDeploymentPoints / playerPoints;
    }

    float weaponOpShare(ToDoubleFunction<WeaponOp> part) {
        float total = 0f;
        float matching = 0f;
        for (DeployedShip ship : playerShips) {
            total += ship.weaponOp().total();
            matching += (float) part.applyAsDouble(ship.weaponOp());
        }
        return total <= 0f ? 0f : matching / total;
    }

    private float average(ToDoubleFunction<DeployedShip> value) {
        if (playerShips.isEmpty()) {
            return 0f;
        }
        double sum = 0d;
        for (DeployedShip ship : playerShips) {
            sum += value.applyAsDouble(ship);
        }
        return (float) (sum / playerShips.size());
    }

    static float longestRange(ShipAPI ship) {
        float longest = 0f;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (!weapon.isDecorative() && weapon.getType() != WeaponType.MISSILE) {
                longest = Math.max(longest, weapon.getRange());
            }
        }
        return longest;
    }

    static int dModCount(ShipVariantAPI variant) {
        if (variant == null) {
            return 0;
        }
        int count = 0;
        for (String hullModId : variant.getHullMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
            if (spec != null && spec.hasTag(Tags.HULLMOD_DMOD)) {
                count++;
            }
        }
        return count;
    }
}
