package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConvertedHangarPenaltyTest {

    private MockSector mockSector;
    private MutableShipStatsAPI stats;
    private StatBonus deploymentPointsMod;
    private StatBonus deploymentWaiver;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getModManager()).thenReturn(mock(ModManagerAPI.class));
        mockSector.global.when(Global::getSettings).thenReturn(settings);

        stats = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        FighterWingSpecAPI wing = mock(FighterWingSpecAPI.class);
        when(settings.getFighterWingSpec("wing")).thenReturn(wing);
        when(wing.getOpCost(stats)).thenReturn(12f);
        when(stats.getVariant().getFittedWings()).thenReturn(List.of("wing"));
        when(stats.getNumFighterBays().getBaseValue()).thenReturn(0f);

        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(stats.getFleetMember()).thenReturn(member);
        when(member.getHullSpec().getSuppliesToRecover()).thenReturn(6f);

        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        deploymentPointsMod = mock(StatBonus.class);
        deploymentWaiver = mock(StatBonus.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD)).thenReturn(deploymentPointsMod);
        when(dynamic.getMod(Stats.CONVERTED_HANGAR_NO_DP_INCREASE)).thenReturn(deploymentWaiver);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    @Test
    void convertedHangarPenaltiesColourLikeTheStatsTheyPenalise() {
        assertTrue(FighterSkillEffect.CONVERTED_HANGAR_REFIT_TIME_MULT.lowerIsBetter());
        assertTrue(FighterSkillEffect.CONVERTED_HANGAR_RELAUNCH_TIME_FLAT.lowerIsBetter());
        assertTrue(FighterSkillEffect.CONVERTED_HANGAR_MIN_CREW_FLAT.lowerIsBetter());
        assertTrue(FighterSkillEffect.CONVERTED_HANGAR_DEPLOYMENT_COST_FLAT.lowerIsBetter());
        assertFalse(FighterSkillEffect.CONVERTED_HANGAR_REPLACEMENT_RATE_MULT.lowerIsBetter());
    }

    @Test
    void deploymentCostRisesByOnePointForEveryFiveStrikecraftOrdnancePointsLikeVanilla() {
        FighterSkillEffect.CONVERTED_HANGAR_DEPLOYMENT_COST_FLAT.apply(stats, "node", 1f);

        verify(deploymentPointsMod).modifyFlat("node", 3f);
        verify(stats.getSuppliesToRecover()).modifyFlat("node", 3f);
        verify(stats.getCRPerDeploymentPercent()).modifyMult("node", 1.5f);
    }

    @Test
    void deploymentCostIsWaivedByTheVanillaWaiverStatThatTaniaPlatesSet() {
        when(deploymentWaiver.computeEffective(0f)).thenReturn(1f);

        FighterSkillEffect.CONVERTED_HANGAR_DEPLOYMENT_COST_FLAT.apply(stats, "node", 1f);

        verify(deploymentPointsMod).modifyFlat("node", 0f);
        verify(stats.getSuppliesToRecover()).modifyFlat("node", 0f);
        verify(stats.getCRPerDeploymentPercent()).modifyMult("node", 1f);
    }
}
