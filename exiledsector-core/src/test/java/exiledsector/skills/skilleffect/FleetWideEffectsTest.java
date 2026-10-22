package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import com.fs.starfarer.api.impl.hullmods.PhaseField;
import exiledsector.effects.SkillTreeHullMod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FleetWideEffectsTest {

    private MockedStatic<Global> globalMock;
    private CampaignFleetAPI playerFleet;
    private StatBonus detectedRange;
    private FleetDataAPI fleetData;

    @BeforeEach
    void setUp() {
        SectorAPI sector = mock(SectorAPI.class);
        playerFleet = mock(CampaignFleetAPI.class);
        MutableFleetStatsAPI fleetStats = mock(MutableFleetStatsAPI.class);
        detectedRange = mock(StatBonus.class);
        fleetData = mock(FleetDataAPI.class);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(playerFleet.getStats()).thenReturn(fleetStats);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        when(playerFleet.isPlayerFleet()).thenReturn(true);
        when(playerFleet.isTransponderOn()).thenReturn(true);
        when(fleetStats.getDetectedRangeMod()).thenReturn(detectedRange);
        when(fleetStats.getSensorStrengthMod()).thenReturn(new com.fs.starfarer.api.combat.StatBonus());
        when(fleetData.getMembersListCopy()).thenReturn(List.of());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getInt("maxSensorShips")).thenReturn(6);
        globalMock.when(Global::getSettings).thenReturn(settings);

        FleetWideEffects.markPhaseFieldStale();
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();
        clearInvocations(detectedRange);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Test
    void doesNotRecomputeWhileNothingHasChanged() {
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();

        verify(detectedRange, never()).unmodifyMult(PhaseField.MOD_KEY);
    }

    @Test
    void recomputesOnceAfterBeingMarkedStale() {
        FleetWideEffects.markPhaseFieldStale();

        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();

        verify(detectedRange, times(1)).unmodifyMult(PhaseField.MOD_KEY);
    }

    @Test
    void recomputesWhenTheTransponderIsToggled() {
        when(playerFleet.isTransponderOn()).thenReturn(false);

        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();

        verify(detectedRange, times(1)).unmodifyMult(PhaseField.MOD_KEY);
    }

    @Test
    void recomputesWhenVanillaPutsItsOwnPhaseFieldModifierBack() {
        MutableStat.StatMod vanillaModifier = mock(MutableStat.StatMod.class);
        when(detectedRange.getMultBonus(PhaseField.MOD_KEY)).thenReturn(vanillaModifier);

        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();

        verify(detectedRange, times(1)).unmodifyMult(PhaseField.MOD_KEY);
    }

    @Test
    void onlyAPlayerFleetSyncMarksThePhaseFieldStale() {
        CampaignFleetAPI npcFleet = mock(CampaignFleetAPI.class);
        SkillTreeHullMod hullMod = new SkillTreeHullMod();

        hullMod.onFleetSync(npcFleet);
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();
        verify(detectedRange, never()).unmodifyMult(PhaseField.MOD_KEY);

        hullMod.onFleetSync(playerFleet);
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();
        verify(detectedRange, times(1)).unmodifyMult(PhaseField.MOD_KEY);
    }

    @Test
    void aShipWithAPhaseFieldContributionNodeMarksTheFieldStaleWhenCreated() {
        LogisticsSkillEffect.PHASE_FIELD_CONTRIBUTION_PERCENT.applyAfterShipCreation(mock(ShipAPI.class), "mod_id", 50f);
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();

        verify(detectedRange, times(1)).unmodifyMult(PhaseField.MOD_KEY);
    }

    private static FleetMemberAPI member(float sensorProfile, float sensorStrength, float contributionPercent, float cr) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat profile = mock(MutableStat.class);
        MutableStat strength = mock(MutableStat.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        RepairTrackerAPI repair = mock(RepairTrackerAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(member.getStats()).thenReturn(stats);
        when(member.getRepairTracker()).thenReturn(repair);
        when(member.getVariant()).thenReturn(variant);
        when(stats.getSensorProfile()).thenReturn(profile);
        when(stats.getSensorStrength()).thenReturn(strength);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(profile.getModifiedValue()).thenReturn(sensorProfile);
        when(strength.getModifiedValue()).thenReturn(sensorStrength);
        when(dynamic.getValue("exiledSector_phaseFieldContributionPercent", 0f)).thenReturn(contributionPercent);
        when(repair.getCR()).thenReturn(cr);
        return member;
    }

    private void recomputeWithTransponderOff(FleetMemberAPI... members) {
        when(playerFleet.isTransponderOn()).thenReturn(false);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(members));
        FleetWideEffects.markPhaseFieldStale();
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();
    }

    @Test
    void theFieldShrinksDetectionByTheFleetsProfileShareOfProfilePlusPhaseSensors() {
        recomputeWithTransponderOff(member(100f, 200f, 50f, 0.7f), member(300f, 400f, 0f, 0.7f));

        verify(detectedRange).modifyMult(eq("exiledSector_extendedPhaseField"), eq(0.8f), anyString());
    }

    @Test
    void mothballedShipsAndShipsBelowTheMinimumCrContributeNoPhaseSensors() {
        FleetMemberAPI mothballed = member(100f, 200f, 50f, 0.7f);
        when(mothballed.isMothballed()).thenReturn(true);

        recomputeWithTransponderOff(mothballed, member(100f, 200f, 50f, 0.05f), member(300f, 400f, 0f, 0.7f));

        verify(detectedRange).unmodifyMult("exiledSector_extendedPhaseField");
        verify(detectedRange, never()).modifyMult(eq("exiledSector_extendedPhaseField"), anyFloat(), anyString());
    }

    @Test
    void aShipWithTheRealPhaseFieldHullModContributesItsWholeSensorStrength() {
        FleetMemberAPI phaseShip = member(300f, 100f, 0f, 0.7f);
        when(phaseShip.getVariant().hasHullMod("phasefield")).thenReturn(true);

        recomputeWithTransponderOff(phaseShip);

        verify(detectedRange).modifyMult(eq("exiledSector_extendedPhaseField"), eq(0.75f), anyString());
    }

    @Test
    void theFieldNeverShrinksDetectionBelowVanillasFloor() {
        recomputeWithTransponderOff(member(100f, 10000f, 100f, 0.7f));

        verify(detectedRange).modifyMult(eq("exiledSector_extendedPhaseField"), eq(PhaseField.MIN_FIELD_MULT), anyString());
    }

    @Test
    void aShipsTransponderTurnsTheFieldOff() {
        FleetMemberAPI contributor = member(100f, 200f, 50f, 0.7f);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(contributor));
        FleetWideEffects.markPhaseFieldStale();
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();

        verify(detectedRange).unmodifyMult("exiledSector_extendedPhaseField");
        verify(detectedRange, never()).modifyMult(eq("exiledSector_extendedPhaseField"), anyFloat(), anyString());
    }
}
