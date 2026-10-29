package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.PhaseField;
import exiledsector.ExiledSector;
import exiledsector.PlayerFleet;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FleetWideEffects {

    static final String POST_BATTLE_SALVAGE_CONTRIBUTION_KEY = "exiledLib_postBattleSalvageContribution";
    static final String PHASE_FIELD_CONTRIBUTION_KEY = "exiledLib_phaseFieldContributionPercent";
    static final String SENSOR_STRENGTH_ALWAYS_COUNTS_KEY = "exiledLib_sensorStrengthAlwaysCounts";
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
        float totalPercent = 0f;
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member.isMothballed() || departingMemberIds.contains(member.getId())) {
                continue;
            }
            totalPercent += member.getStats().getDynamic().getValue(POST_BATTLE_SALVAGE_CONTRIBUTION_KEY, 0f);
        }
        playerFleet.getStats().getDynamic().getStat(Stats.BATTLE_SALVAGE_MULT_FLEET)
                .modifyFlat(ExiledSector.key(POST_BATTLE_SALVAGE_FLEET_MOD), totalPercent / 100f);
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
            fleet.getStats().getSensorStrengthMod().modifyFlat(ExiledSector.key(SENSOR_STRENGTH_ALWAYS_COUNTS_FLEET_MOD), extra,
                    Translation.gameText("fleet.sensorStrengthAlwaysCounts"));
        } else {
            fleet.getStats().getSensorStrengthMod().unmodifyFlat(ExiledSector.key(SENSOR_STRENGTH_ALWAYS_COUNTS_FLEET_MOD));
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
