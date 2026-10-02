package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.skills.skilleffect.CompatSkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import second_in_command.SCUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecondInCommandCompatTest {

    private MockedStatic<Global> globalMock;
    private ModManagerAPI modManager;
    private SettingsAPI settings;
    private FleetMemberAPI member;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(settings.getScriptClassLoader()).thenReturn(getClass().getClassLoader());

        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        member = mock(FleetMemberAPI.class);
        when(member.getFleetData()).thenReturn(fleetData);
        when(fleetData.getFleet()).thenReturn(playerFleet);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);

        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(sector);
        SCUtils.ACTIVE_SKILLS.clear();
        SCUtils.failure = null;
        SecondInCommandCompat.clearCachedLookups();
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        SCUtils.ACTIVE_SKILLS.clear();
        SCUtils.failure = null;
        SecondInCommandCompat.clearCachedLookups();
    }

    private void enableSecondInCommandWith(String... activeSkillIds) {
        when(modManager.isModEnabled(SecondInCommandCompat.MOD_ID)).thenReturn(true);
        SCUtils.ACTIVE_SKILLS.addAll(List.of(activeSkillIds));
    }

    private MutableShipStatsAPI mockStats() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(stats.getFleetMember()).thenReturn(member);
        return stats;
    }

    @Test
    void skillIsNeverActiveWhenSecondInCommandIsNotEnabled() {
        SCUtils.ACTIVE_SKILLS.add(SecondInCommandCompat.RECONFIGURATION_SKILL_ID);

        assertFalse(SecondInCommandCompat.isSkillActive(member, SecondInCommandCompat.RECONFIGURATION_SKILL_ID));
    }

    @Test
    void readsSkillActivityFromSecondInCommandFleetData() {
        enableSecondInCommandWith();
        assertFalse(SecondInCommandCompat.isSkillActive(member, SecondInCommandCompat.RECONFIGURATION_SKILL_ID));

        SCUtils.ACTIVE_SKILLS.add(SecondInCommandCompat.RECONFIGURATION_SKILL_ID);

        assertTrue(SecondInCommandCompat.isSkillActive(member, SecondInCommandCompat.RECONFIGURATION_SKILL_ID));
    }

    @Test
    void memberWithoutAFleetNeverHasActiveSkills() {
        enableSecondInCommandWith(SecondInCommandCompat.RECONFIGURATION_SKILL_ID);

        assertFalse(SecondInCommandCompat.isSkillActive(mock(FleetMemberAPI.class), SecondInCommandCompat.RECONFIGURATION_SKILL_ID));
    }

    @Test
    void detectsAHullModParkedAsADeactivatedSMod() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasTag("sc_inactive_smods_hbi")).thenReturn(true);

        assertTrue(SecondInCommandCompat.hasDeactivatedSMod(variant, "hbi"));
        assertFalse(SecondInCommandCompat.hasDeactivatedSMod(variant, "heavyarmor"));
    }

    @Test
    void convertedHangarPenaltyAppliesItsMagnitudeWhenNotWaived() {
        enableSecondInCommandWith();
        MutableShipStatsAPI stats = mockStats();

        CompatSkillEffect.CONVERTED_HANGAR_MIN_CREW_FLAT.apply(stats, "node", 20f);

        verify(stats.getMinCrewMod()).modifyFlat("node", 20f);
    }

    @Test
    void convertedHangarPenaltyIsWaivedByReconfiguration() {
        enableSecondInCommandWith(SecondInCommandCompat.RECONFIGURATION_SKILL_ID);
        MutableShipStatsAPI stats = mockStats();

        CompatSkillEffect.CONVERTED_HANGAR_MIN_CREW_FLAT.apply(stats, "node", 20f);

        verify(stats.getMinCrewMod()).modifyFlat("node", 0f);
    }

    @Test
    void convertedHangarPenaltyIsWaivedByTheVanillaWaiverStat() {
        MutableShipStatsAPI stats = mockStats();
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        StatBonus waiver = mock(StatBonus.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod(Stats.CONVERTED_HANGAR_NO_CREW_INCREASE)).thenReturn(waiver);
        when(waiver.computeEffective(0f)).thenReturn(1f);

        CompatSkillEffect.CONVERTED_HANGAR_MIN_CREW_FLAT.apply(stats, "node", 20f);

        verify(stats.getMinCrewMod()).modifyFlat("node", 0f);
    }

    @Test
    void convertedHangarPenaltiesSupportTemporaryGating() {
        assertTrue(CompatSkillEffect.CONVERTED_HANGAR_REFIT_TIME_MULT.supportsTemporaryGating());
    }

    @Test
    void missingSecondInCommandClassesDisableTheChecksInsteadOfThrowing() {
        enableSecondInCommandWith(SecondInCommandCompat.RECONFIGURATION_SKILL_ID);
        when(settings.getScriptClassLoader()).thenReturn(new ClassLoader(null) {
        });

        assertFalse(SecondInCommandCompat.isSkillActive(member, SecondInCommandCompat.RECONFIGURATION_SKILL_ID));

        when(settings.getScriptClassLoader()).thenReturn(getClass().getClassLoader());
        assertFalse(SecondInCommandCompat.isSkillActive(member, SecondInCommandCompat.RECONFIGURATION_SKILL_ID));
    }

    @Test
    void anExceptionInsideSecondInCommandDisablesTheChecksInsteadOfCrashing() {
        enableSecondInCommandWith(SecondInCommandCompat.RECONFIGURATION_SKILL_ID);
        SCUtils.failure = new SecurityException("File access and reflection are not allowed to scripts.");

        assertFalse(SecondInCommandCompat.isSkillActive(member, SecondInCommandCompat.RECONFIGURATION_SKILL_ID));

        SCUtils.failure = null;
        assertFalse(SecondInCommandCompat.isSkillActive(member, SecondInCommandCompat.RECONFIGURATION_SKILL_ID));
    }
}
