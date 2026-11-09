package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.PhaseField;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ExiledSector;
import exiledsector.PlayerFleet;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;

import exiledsector.skills.skilleffect.SkillEffect;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;

public final class FleetWideEffects {

    static final String POST_BATTLE_SALVAGE_CONTRIBUTION_KEY = "exiledSector_postBattleSalvageContribution";
    static final String PHASE_FIELD_CONTRIBUTION_KEY = "exiledSector_phaseFieldContributionPercent";
    static final String SENSOR_STRENGTH_ALWAYS_COUNTS_KEY = "exiledSector_sensorStrengthAlwaysCountsFlag";
    static final String MOVE_SLOW_BURN_LEVEL_KEY = "exiledSector_moveSlowBurnLevelContribution";
    static final float MAX_MOVE_SLOW_BURN_LEVEL_BONUS = 6f;
    private static final String MOVE_SLOW_BURN_LEVEL_FLEET_MOD = "moveSlowBurnLevel";
    static final String TRANSPONDER_OFF_SENSOR_PROFILE_KEY = "exiledSector_transponderOffSensorProfileContribution";
    static final float MAX_TRANSPONDER_OFF_SENSOR_PROFILE_REDUCTION = 30f;
    private static final String TRANSPONDER_OFF_SENSOR_PROFILE_FLEET_MOD = "transponderOffSensorProfile";
    static final String TRANSPONDER_OFF_BURN_LEVEL_KEY = "exiledSector_transponderOffBurnLevelContribution";
    private static final String TRANSPONDER_OFF_BURN_LEVEL_FLEET_MOD = "transponderOffBurnLevel";
    static final String CORONA_RESISTANCE_CONTRIBUTION_KEY = "exiledSector_fleetCoronaResistanceContribution";
    static final float MAX_CORONA_RESISTANCE_PERCENT = 100f;
    private static final String CORONA_RESISTANCE_FLEET_MOD = "fleetCoronaResistance";
    static final String MOVE_SLOW_SUPPLY_USE_KEY = "exiledSector_moveSlowSupplyUseContribution";
    private static final String MOVE_SLOW_SUPPLY_USE_MEMBER_MOD = "moveSlowSupplyUse";
    static final String SUPPLY_USE_PER_HULL_TYPE_KEY = "exiledSector_supplyUsePerHullTypeContribution";
    static final float MAX_SUPPLY_USE_PER_HULL_TYPE_REDUCTION = 15f;
    private static final String SUPPLY_USE_PER_HULL_TYPE_MEMBER_MOD = "supplyUsePerHullType";
    static final String FUEL_USE_PER_HULL_TYPE_KEY = "exiledSector_fuelUsePerHullTypeContribution";
    static final float MAX_FUEL_USE_PER_HULL_TYPE_REDUCTION = 5f;
    private static final String FUEL_USE_PER_HULL_TYPE_MEMBER_MOD = "fuelUsePerHullType";
    static final String HALF_FUELED_FUEL_USE_KEY = "exiledSector_halfFueledFuelUseContribution";
    static final float HALF_FUELED_SHARE = 0.5f;
    private static final String HALF_FUELED_FUEL_USE_MEMBER_MOD = "halfFueledFuelUse";
    static final String SWARM_SENSOR_STRENGTH_KEY = "exiledSector_swarmSensorStrengthPercent";
    private static final String SWARM_SENSOR_STRENGTH_FLEET_MOD = "swarmSensorStrength";
    static final String TERRAIN_MOVEMENT_PENALTY_KEY = "exiledSector_terrainMovementPenaltyContribution";
    static final float MAX_TERRAIN_MOVEMENT_PENALTY_REDUCTION_PERCENT = 50f;
    private static final String TERRAIN_MOVEMENT_PENALTY_FLEET_MOD = "terrainMovementPenalty";
    private static final String SENSOR_STRENGTH_ALWAYS_COUNTS_FLEET_MOD = "sensorStrengthAlwaysCounts";
    private static final String MAX_SENSOR_SHIPS_SETTING = "maxSensorShips";
    private static final int DEFAULT_MAX_SENSOR_SHIPS = 5;
    private static final String POST_BATTLE_SALVAGE_FLEET_MOD = "postBattleSalvage";
    private static final String EXTENDED_PHASE_FIELD_FLEET_MOD = "extendedPhaseField";

    private static boolean phaseFieldStale = true;
    private static Boolean lastTransponderOn;

    private FleetWideEffects() {
    }

    public static void recomputeSalvageBonus(Set<String> departingMemberIds) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (playerFleet == null) {
            return;
        }
        List<FleetMemberAPI> stayingMembers = playerFleet.getFleetData().getMembersListCopy().stream()
                .filter(member -> !departingMemberIds.contains(member.getId())).toList();
        float totalPercent = FleetContributions.sum(stayingMembers, POST_BATTLE_SALVAGE_CONTRIBUTION_KEY);
        playerFleet.getStats().getDynamic().getStat(Stats.BATTLE_SALVAGE_MULT_FLEET)
                .modifyFlat(ExiledSector.key(POST_BATTLE_SALVAGE_FLEET_MOD), totalPercent / 100f);
    }

    public static void applyAlwaysCountingSensorStrength(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) {
            return;
        }
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        float extra = anyAlwaysCounts(members) ? alwaysCountingStrength(members) : 0f;
        if (extra > 0f) {
            fleet.getStats().getSensorStrengthMod().modifyFlat(ExiledSector.key(SENSOR_STRENGTH_ALWAYS_COUNTS_FLEET_MOD), extra,
                    Translation.gameText("fleet.sensorStrengthAlwaysCounts"));
        } else {
            fleet.getStats().getSensorStrengthMod().unmodifyFlat(ExiledSector.key(SENSOR_STRENGTH_ALWAYS_COUNTS_FLEET_MOD));
        }
    }

    public static void applyMoveSlowBurnLevel(CampaignFleetAPI fleet) {
        DynamicStatsAPI fleetDynamic = fleet == null || fleet.getStats() == null ? null : fleet.getStats().getDynamic();
        if (fleetDynamic == null || fleet.getFleetData() == null) {
            return;
        }
        float bonus = moveSlowBurnLevelBonus(fleet.getFleetData().getMembersListCopy());
        StatBonus moveSlowMod = fleetDynamic.getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD);
        if (bonus > 0f) {
            moveSlowMod.modifyFlat(ExiledSector.key(MOVE_SLOW_BURN_LEVEL_FLEET_MOD), bonus, Translation.gameText("fleet.moveSlowBurnLevel"));
        } else {
            moveSlowMod.unmodifyFlat(ExiledSector.key(MOVE_SLOW_BURN_LEVEL_FLEET_MOD));
        }
    }

    static float moveSlowBurnLevelBonus(List<FleetMemberAPI> members) {
        return Math.min(MAX_MOVE_SLOW_BURN_LEVEL_BONUS, FleetContributions.sum(members, MOVE_SLOW_BURN_LEVEL_KEY));
    }

    public static void applyTransponderOffSensorProfile(CampaignFleetAPI fleet) {
        StatBonus detectedRangeMod = fleet == null || fleet.getStats() == null ? null : fleet.getStats().getDetectedRangeMod();
        if (detectedRangeMod == null || fleet.getFleetData() == null) {
            return;
        }
        float percent = fleet.isTransponderOn() ? 0f : transponderOffSensorProfilePercent(fleet.getFleetData().getMembersListCopy());
        if (percent < 0f) {
            detectedRangeMod.modifyPercent(ExiledSector.key(TRANSPONDER_OFF_SENSOR_PROFILE_FLEET_MOD), percent,
                    Translation.gameText("fleet.transponderOffSensorProfile"));
        } else {
            detectedRangeMod.unmodifyPercent(ExiledSector.key(TRANSPONDER_OFF_SENSOR_PROFILE_FLEET_MOD));
        }
    }

    static float transponderOffSensorProfilePercent(List<FleetMemberAPI> members) {
        float percent = FleetContributions.sum(members, TRANSPONDER_OFF_SENSOR_PROFILE_KEY);
        return Math.max(-MAX_TRANSPONDER_OFF_SENSOR_PROFILE_REDUCTION, Math.min(0f, percent));
    }

    public static void applyTransponderOffBurnLevel(CampaignFleetAPI fleet) {
        StatBonus fleetBurnMod = fleet == null || fleet.getStats() == null ? null : fleet.getStats().getFleetwideMaxBurnMod();
        if (fleetBurnMod == null || fleet.getFleetData() == null) {
            return;
        }
        float bonus = fleet.isTransponderOn() ? 0f : transponderOffBurnLevelBonus(fleet.getFleetData().getMembersListCopy());
        if (bonus > 0f) {
            fleetBurnMod.modifyFlat(ExiledSector.key(TRANSPONDER_OFF_BURN_LEVEL_FLEET_MOD), bonus, Translation.gameText("fleet.transponderOffBurnLevel"));
        } else {
            fleetBurnMod.unmodifyFlat(ExiledSector.key(TRANSPONDER_OFF_BURN_LEVEL_FLEET_MOD));
        }
    }

    static float transponderOffBurnLevelBonus(List<FleetMemberAPI> members) {
        return FleetContributions.largestPositive(members, TRANSPONDER_OFF_BURN_LEVEL_KEY);
    }

    public static void applyCoronaResistance(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) {
            return;
        }
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        float resistancePercent = coronaResistancePercent(members);
        String modId = ExiledSector.key(CORONA_RESISTANCE_FLEET_MOD);
        if (resistancePercent <= 0f) {
            for (FleetMemberAPI member : members) {
                member.getStats().getDynamic().getStat(Stats.CORONA_EFFECT_MULT).unmodifyMult(modId);
            }
            return;
        }
        String source = Translation.gameText("fleet.coronaResistance");
        for (FleetMemberAPI member : members) {
            member.getStats().getDynamic().getStat(Stats.CORONA_EFFECT_MULT).modifyMult(modId, 1f - resistancePercent / 100f, source);
        }
    }

    static float coronaResistancePercent(List<FleetMemberAPI> members) {
        return Math.min(MAX_CORONA_RESISTANCE_PERCENT, FleetContributions.sum(members, CORONA_RESISTANCE_CONTRIBUTION_KEY));
    }

    public static void applyMoveSlowSupplyUse(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) {
            return;
        }
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        float percent = Misc.isSlowMoving(fleet) ? strongestMoveSlowSupplyUse(members) : 0f;
        String modId = ExiledSector.key(MOVE_SLOW_SUPPLY_USE_MEMBER_MOD);
        String source = percent != 0f ? Translation.gameText("fleet.moveSlowSupplyUse") : null;
        for (FleetMemberAPI member : members) {
            MutableStat suppliesPerMonth = member.getStats().getSuppliesPerMonth();
            if (percent != 0f) {
                suppliesPerMonth.modifyMult(modId, 1f + percent / 100f, source);
            } else {
                suppliesPerMonth.unmodifyMult(modId);
            }
        }
    }

    static float strongestMoveSlowSupplyUse(List<FleetMemberAPI> members) {
        return SkillEffect.addedMultiplier(FleetContributions.largestMagnitude(members, MOVE_SLOW_SUPPLY_USE_KEY));
    }

    public static void applySupplyUsePerHullType(CampaignFleetAPI fleet) {
        applyPerHullTypeReduction(fleet, FleetWideEffects::supplyUsePerHullTypeReduction, SUPPLY_USE_PER_HULL_TYPE_MEMBER_MOD,
                "fleet.supplyUsePerHullType", (member, modId, percent, source) ->
                        setPercent(member.getStats().getSuppliesPerMonth(), modId, percent, source));
    }

    static float supplyUsePerHullTypeReduction(List<FleetMemberAPI> members) {
        return perHullTypeReduction(members, SUPPLY_USE_PER_HULL_TYPE_KEY, MAX_SUPPLY_USE_PER_HULL_TYPE_REDUCTION);
    }

    public static void applyFuelUsePerHullType(CampaignFleetAPI fleet) {
        applyPerHullTypeReduction(fleet, FleetWideEffects::fuelUsePerHullTypeReduction, FUEL_USE_PER_HULL_TYPE_MEMBER_MOD,
                "fleet.fuelUsePerHullType", (member, modId, percent, source) ->
                        setPercent(member.getStats().getFuelUseMod(), modId, percent, source));
    }

    static float fuelUsePerHullTypeReduction(List<FleetMemberAPI> members) {
        return perHullTypeReduction(members, FUEL_USE_PER_HULL_TYPE_KEY, MAX_FUEL_USE_PER_HULL_TYPE_REDUCTION);
    }

    private interface MemberPercentStat {
        void set(FleetMemberAPI member, String modId, float percent, String source);
    }

    private static void applyPerHullTypeReduction(CampaignFleetAPI fleet, ToDoubleFunction<List<FleetMemberAPI>> reduction, String memberMod,
                                                  String sourceKey, MemberPercentStat stat) {
        if (fleet == null || !fleet.isPlayerFleet() || fleet.getFleetData() == null) {
            return;
        }
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        float percent = (float) reduction.applyAsDouble(members);
        String modId = ExiledSector.key(memberMod);
        String source = percent > 0f ? Translation.gameText(sourceKey) : null;
        for (FleetMemberAPI member : members) {
            stat.set(member, modId, -percent, source);
        }
    }

    private static void setPercent(MutableStat stat, String modId, float percent, String source) {
        if (percent != 0f) {
            stat.modifyPercent(modId, percent, source);
        } else {
            stat.unmodifyPercent(modId);
        }
    }

    private static void setPercent(StatBonus stat, String modId, float percent, String source) {
        if (percent != 0f) {
            stat.modifyPercent(modId, percent, source);
        } else {
            stat.unmodifyPercent(modId);
        }
    }

    private static float perHullTypeReduction(List<FleetMemberAPI> members, String contributionKey, float maxReduction) {
        float percentPerHullType = FleetContributions.sum(members, contributionKey);
        if (percentPerHullType <= 0f) {
            return 0f;
        }
        return Math.min(maxReduction, percentPerHullType * activeHullTypes(members));
    }

    private static int activeHullTypes(List<FleetMemberAPI> members) {
        Set<String> baseHullIds = new HashSet<>();
        for (FleetMemberAPI member : members) {
            if (!member.isMothballed() && member.getHullSpec() != null) {
                baseHullIds.add(member.getHullSpec().getBaseHullId());
            }
        }
        return baseHullIds.size();
    }

    public static void applyHalfFueledFuelUse(CampaignFleetAPI fleet) {
        if (fleet == null || !fleet.isPlayerFleet() || fleet.getFleetData() == null || fleet.getCargo() == null) {
            return;
        }
        boolean halfFueled = isHalfFueled(fleet.getCargo().getFuel(), fleet.getCargo().getMaxFuel());
        String modId = ExiledSector.key(HALF_FUELED_FUEL_USE_MEMBER_MOD);
        String source = halfFueled ? Translation.gameText("fleet.halfFueledFuelUse") : null;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            float percent = halfFueled ? member.getStats().getDynamic().getValue(HALF_FUELED_FUEL_USE_KEY, 0f) : 0f;
            setPercent(member.getStats().getFuelUseMod(), modId, -Math.max(0f, percent), source);
        }
    }

    static boolean isHalfFueled(float fuel, float maxFuel) {
        return maxFuel > 0f && fuel >= maxFuel * HALF_FUELED_SHARE;
    }

    public static void applySwarmSensorStrength(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getStats() == null || fleet.getFleetData() == null) {
            return;
        }
        float percent = swarmSensorStrengthPercent(fleet.getFleetData().getMembersListCopy());
        StatBonus sensorStrengthMod = fleet.getStats().getSensorStrengthMod();
        if (percent > 0f) {
            sensorStrengthMod.modifyPercent(ExiledSector.key(SWARM_SENSOR_STRENGTH_FLEET_MOD), percent, Translation.gameText("fleet.swarmSensorStrength"));
        } else {
            sensorStrengthMod.unmodifyPercent(ExiledSector.key(SWARM_SENSOR_STRENGTH_FLEET_MOD));
        }
    }

    static float swarmSensorStrengthPercent(List<FleetMemberAPI> members) {
        int swarms = 0;
        for (FleetMemberAPI member : members) {
            if (!member.isMothballed() && FragmentSwarms.hasSwarmHullMod(member.getVariant())) {
                swarms++;
            }
        }
        return swarms == 0 ? 0f : FleetContributions.largestPositive(members, SWARM_SENSOR_STRENGTH_KEY) * swarms;
    }

    public static void applyTerrainMovementPenalty(CampaignFleetAPI fleet) {
        MutableCharacterStatsAPI commanderStats = fleet == null ? null : fleet.getCommanderStats();
        if (commanderStats == null || commanderStats.getDynamic() == null || fleet.getFleetData() == null) {
            return;
        }
        float percent = terrainMovementPenaltyPercent(fleet.getFleetData().getMembersListCopy());
        MutableStat penaltyMult = commanderStats.getDynamic().getStat(Stats.NAVIGATION_PENALTY_MULT);
        if (percent != 0f) {
            penaltyMult.modifyFlat(ExiledSector.key(TERRAIN_MOVEMENT_PENALTY_FLEET_MOD), percent / 100f,
                    Translation.gameText("fleet.terrainMovementPenalty"));
        } else {
            penaltyMult.unmodifyFlat(ExiledSector.key(TERRAIN_MOVEMENT_PENALTY_FLEET_MOD));
        }
    }

    static float terrainMovementPenaltyPercent(List<FleetMemberAPI> members) {
        return Math.max(-MAX_TERRAIN_MOVEMENT_PENALTY_REDUCTION_PERCENT, FleetContributions.sum(members, TERRAIN_MOVEMENT_PENALTY_KEY));
    }

    private static boolean alwaysCounts(FleetMemberAPI member) {
        return !member.isMothballed() && member.getStats().getDynamic().getValue(SENSOR_STRENGTH_ALWAYS_COUNTS_KEY, 0f) > 0f;
    }

    private static boolean anyAlwaysCounts(List<FleetMemberAPI> members) {
        for (FleetMemberAPI member : members) {
            if (alwaysCounts(member)) {
                return true;
            }
        }
        return false;
    }

    private static float alwaysCountingStrength(List<FleetMemberAPI> members) {
        float[] strengths = new float[members.size()];
        boolean[] alwaysCounts = new boolean[members.size()];
        for (int i = 0; i < members.size(); i++) {
            FleetMemberAPI member = members.get(i);
            if (!member.isMothballed()) {
                strengths[i] = Math.round(member.getStats().getSensorStrength().getModifiedValue());
                alwaysCounts[i] = alwaysCounts(member);
            }
        }
        return strengthLeftOutOfTheTop(strengths, alwaysCounts, maxSensorShips());
    }

    static float strengthLeftOutOfTheTop(float[] strengths, boolean[] alwaysCounts, int counted) {
        List<Integer> order = new ArrayList<>(strengths.length);
        for (int i = 0; i < strengths.length; i++) {
            order.add(i);
        }
        order.sort((a, b) -> strengths[a] != strengths[b]
                ? Float.compare(strengths[b], strengths[a])
                : Boolean.compare(alwaysCounts[a], alwaysCounts[b]));
        float extra = 0f;
        for (int rank = counted; rank < order.size(); rank++) {
            int index = order.get(rank);
            if (alwaysCounts[index]) {
                extra += strengths[index];
            }
        }
        return extra;
    }

    private static int maxSensorShips() {
        try {
            return Global.getSettings().getInt(MAX_SENSOR_SHIPS_SETTING);
        } catch (RuntimeException e) {
            return DEFAULT_MAX_SENSOR_SHIPS;
        }
    }

    public static void markPhaseFieldStale() {
        phaseFieldStale = true;
    }

    public static void recomputeExtendedPhaseFieldIfStale() {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (playerFleet == null) {
            return;
        }
        boolean transponderOn = playerFleet.isTransponderOn();
        boolean vanillaFieldReapplied = playerFleet.getStats().getDetectedRangeMod().getMultBonus(PhaseField.MOD_KEY) != null;
        if (!phaseFieldStale && !vanillaFieldReapplied && Boolean.valueOf(transponderOn).equals(lastTransponderOn)) {
            return;
        }
        phaseFieldStale = false;
        lastTransponderOn = transponderOn;
        recomputeExtendedPhaseField(playerFleet);
        applyTransponderOffSensorProfile(playerFleet);
        applyTransponderOffBurnLevel(playerFleet);
    }

    private static void recomputeExtendedPhaseField(CampaignFleetAPI playerFleet) {
        playerFleet.getStats().getDetectedRangeMod().unmodifyMult(PhaseField.MOD_KEY);

        if (playerFleet.isTransponderOn()) {
            playerFleet.getStats().getDetectedRangeMod().unmodifyMult(ExiledSector.key(EXTENDED_PHASE_FIELD_FLEET_MOD));
            return;
        }

        List<FleetMemberAPI> members = playerFleet.getFleetData().getMembersListCopy();
        float[] profiles = new float[members.size()];
        List<Float> phaseSensorValues = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            FleetMemberAPI member = members.get(i);
            profiles[i] = member.getStats().getSensorProfile().getModifiedValue();
            Float contribution = phaseSensorContribution(member);
            if (contribution != null) {
                phaseSensorValues.add(contribution);
            }
        }

        if (phaseSensorValues.isEmpty()) {
            playerFleet.getStats().getDetectedRangeMod().unmodifyMult(ExiledSector.key(EXTENDED_PHASE_FIELD_FLEET_MOD));
            return;
        }

        float[] phaseSensors = new float[phaseSensorValues.size()];
        for (int i = 0; i < phaseSensorValues.size(); i++) {
            phaseSensors[i] = phaseSensorValues.get(i);
        }

        int topShips = maxSensorShips();
        float totalProfile = PhaseField.getTopKValuesSum(profiles, topShips);
        float totalPhaseSensors = PhaseField.getTopKValuesSum(phaseSensors, topShips);
        float total = Math.max(totalProfile + totalPhaseSensors, 1f);
        float mult = Math.max(PhaseField.MIN_FIELD_MULT, Math.min(1f, totalProfile / total));

        playerFleet.getStats().getDetectedRangeMod()
                .modifyMult(ExiledSector.key(EXTENDED_PHASE_FIELD_FLEET_MOD), mult, Translation.gameText("modifier.phaseSensorNetworks"));
    }

    private static Float phaseSensorContribution(FleetMemberAPI member) {
        if (member.isMothballed() || member.getRepairTracker().getCR() < PhaseField.MIN_CR) {
            return null;
        }
        if (member.getVariant().hasHullMod("phasefield")) {
            return member.getStats().getSensorStrength().getModifiedValue();
        }
        float contributionPercent = member.getStats().getDynamic().getValue(PHASE_FIELD_CONTRIBUTION_KEY, 0f);
        if (contributionPercent <= 0f) {
            return null;
        }
        return member.getStats().getSensorStrength().getModifiedValue() * contributionPercent / 100f;
    }
}
