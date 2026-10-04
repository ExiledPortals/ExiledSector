package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.PhaseField;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.List;

public final class FleetWideEffects {

    static final String POST_BATTLE_SALVAGE_CONTRIBUTION_KEY = "exiledSector_postBattleSalvageContribution";
    static final String PHASE_FIELD_CONTRIBUTION_KEY = "exiledSector_phaseFieldContributionPercent";
    static final String SENSOR_STRENGTH_ALWAYS_COUNTS_KEY = "exiledSector_sensorStrengthAlwaysCounts";
    private static final String SENSOR_STRENGTH_ALWAYS_COUNTS_MOD_ID = "exiledSector_sensorStrengthAlwaysCounts";
    private static final String MAX_SENSOR_SHIPS_SETTING = "maxSensorShips";
    private static final int DEFAULT_MAX_SENSOR_SHIPS = 5;
    private static final String POST_BATTLE_SALVAGE_FLEET_MOD_ID = "exiledSector_postBattleSalvage";
    private static final String EXTENDED_PHASE_FIELD_MOD_ID = "exiledSector_extendedPhaseField";

    private static boolean phaseFieldStale = true;
    private static Boolean lastTransponderOn;

    private FleetWideEffects() {
    }

    private static CampaignFleetAPI playerFleet() {
        SectorAPI sector = Global.getSector();
        return sector == null ? null : sector.getPlayerFleet();
    }

    public static void recomputeSalvageBonus() {
        CampaignFleetAPI fleet = playerFleet();
        if (fleet == null) {
            return;
        }
        float totalPercent = 0f;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            totalPercent += member.getStats().getDynamic().getValue(POST_BATTLE_SALVAGE_CONTRIBUTION_KEY, 0f);
        }
        fleet.getStats().getDynamic().getStat(Stats.BATTLE_SALVAGE_MULT_FLEET)
                .modifyFlat(POST_BATTLE_SALVAGE_FLEET_MOD_ID, totalPercent / 100f);
    }

    public static void applyAlwaysCountingSensorStrength(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) {
            return;
        }
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        float[] strengths = new float[members.size()];
        boolean[] alwaysCounts = new boolean[members.size()];
        boolean any = false;
        for (int i = 0; i < members.size(); i++) {
            FleetMemberAPI member = members.get(i);
            if (member.isMothballed()) {
                continue;
            }
            strengths[i] = Math.round(member.getStats().getSensorStrength().getModifiedValue());
            alwaysCounts[i] = member.getStats().getDynamic().getValue(SENSOR_STRENGTH_ALWAYS_COUNTS_KEY, 0f) > 0f;
            any |= alwaysCounts[i];
        }
        float extra = any ? strengthLeftOutOfTheTop(strengths, alwaysCounts, maxSensorShips()) : 0f;
        if (extra > 0f) {
            fleet.getStats().getSensorStrengthMod().modifyFlat(SENSOR_STRENGTH_ALWAYS_COUNTS_MOD_ID, extra,
                    Translation.gameText("fleet.sensorStrengthAlwaysCounts"));
        } else {
            fleet.getStats().getSensorStrengthMod().unmodifyFlat(SENSOR_STRENGTH_ALWAYS_COUNTS_MOD_ID);
        }
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
        CampaignFleetAPI fleet = playerFleet();
        if (fleet == null) {
            return;
        }
        boolean transponderOn = fleet.isTransponderOn();
        boolean vanillaFieldReapplied = fleet.getStats().getDetectedRangeMod().getMultBonus(PhaseField.MOD_KEY) != null;
        if (!phaseFieldStale && !vanillaFieldReapplied && Boolean.valueOf(transponderOn).equals(lastTransponderOn)) {
            return;
        }
        phaseFieldStale = false;
        lastTransponderOn = transponderOn;
        recomputeExtendedPhaseField(fleet);
    }

    private static void recomputeExtendedPhaseField(CampaignFleetAPI fleet) {

        // This replaces vanilla's own Phase Field fleet-wide calculation (which only ever counts
        // real phase ships) with one that also folds in ships with this node, so both share a
        // single pool and a single floor instead of stacking two independently-clamped
        // multipliers (which could otherwise compound below vanilla's intended floor).
        fleet.getStats().getDetectedRangeMod().unmodifyMult(PhaseField.MOD_KEY);

        if (fleet.isTransponderOn()) {
            fleet.getStats().getDetectedRangeMod().unmodifyMult(EXTENDED_PHASE_FIELD_MOD_ID);
            return;
        }

        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
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
            fleet.getStats().getDetectedRangeMod().unmodifyMult(EXTENDED_PHASE_FIELD_MOD_ID);
            return;
        }

        float[] phaseSensors = new float[phaseSensorValues.size()];
        for (int i = 0; i < phaseSensorValues.size(); i++) {
            phaseSensors[i] = phaseSensorValues.get(i);
        }

        int topShips = Global.getSettings().getInt("maxSensorShips");
        float totalProfile = PhaseField.getTopKValuesSum(profiles, topShips);
        float totalPhaseSensors = PhaseField.getTopKValuesSum(phaseSensors, topShips);
        float total = Math.max(totalProfile + totalPhaseSensors, 1f);
        float mult = Math.max(PhaseField.MIN_FIELD_MULT, Math.min(1f, totalProfile / total));

        fleet.getStats().getDetectedRangeMod()
                .modifyMult(EXTENDED_PHASE_FIELD_MOD_ID, mult, Translation.gameText("modifier.phaseSensorNetworks"));
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
