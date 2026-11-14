package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerFleetConditionScriptTest {

    private static final float EPSILON = 1e-4f;
    private static final float SLOW_BURN_LEVEL = 1f;
    private static final float FAST_BURN_LEVEL = 20f;

    private MockSector mockSector;
    private CampaignFleetAPI playerFleet;
    private MutableStat suppliesPerMonth;
    private StatBonus fuelUse;
    private PlayerFleetConditionScript script;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        suppliesPerMonth = new MutableStat(10f);
        fuelUse = new StatBonus();
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.MOVE_SLOW_SUPPLY_USE_KEY, 0f)).thenReturn(-5f);
        when(member.getStats().getDynamic().getValue(FleetWideEffects.HALF_FUELED_FUEL_USE_KEY, 0f)).thenReturn(10f);
        when(member.getStats().getSuppliesPerMonth()).thenReturn(suppliesPerMonth);
        when(member.getStats().getFuelUseMod()).thenReturn(fuelUse);
        playerFleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(playerFleet.isPlayerFleet()).thenReturn(true);
        when(playerFleet.getFleetData().getMembersListCopy()).thenReturn(List.of(member));
        when(playerFleet.getFleetData().getMinBurnLevel()).thenReturn(4f);
        StatBonus moveSlowBonus = new StatBonus();
        moveSlowBonus.modifyFlat("test", 5f);
        when(playerFleet.getStats().getDynamic().getMod("move_slow_speed_bonus_mod")).thenReturn(moveSlowBonus);
        when(playerFleet.getCurrBurnLevel()).thenReturn(FAST_BURN_LEVEL);
        when(playerFleet.getCargo().getFuel()).thenReturn(20f);
        when(playerFleet.getCargo().getMaxFuel()).thenReturn(100f);
        when(mockSector.sector.getPlayerFleet()).thenReturn(playerFleet);
        PlayerFleetConditionScript.markPlayerFleetSynced();
        script = new PlayerFleetConditionScript();
        script.advance(0f);
        clearInvocations(playerFleet.getFleetData());
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private void check() {
        script.advance(PlayerFleetConditionScript.CHECK_SECONDS);
    }

    @Test
    void theMembersAreLeftAloneWhileNothingChanges() {
        check();
        check();

        verify(playerFleet.getFleetData(), never()).getMembersListCopy();
    }

    @Test
    void movingSlowlyAndBackChangesTheSupplyUseStraightAway() {
        when(playerFleet.getCurrBurnLevel()).thenReturn(SLOW_BURN_LEVEL);
        check();

        assertEquals(9.5f, suppliesPerMonth.getModifiedValue(), EPSILON);

        when(playerFleet.getCurrBurnLevel()).thenReturn(FAST_BURN_LEVEL);
        check();

        assertEquals(10f, suppliesPerMonth.getModifiedValue(), EPSILON);
    }

    @Test
    void reachingHalfFuelChangesTheFuelUseStraightAway() {
        when(playerFleet.getCargo().getFuel()).thenReturn(60f);
        check();

        assertEquals(-10f, fuelUse.getPercentMod(), EPSILON);
    }

    @Test
    void aPlayerFleetSyncReappliesTheConditionsToTheRebuiltMembers() {
        PlayerFleetConditionScript.markPlayerFleetSynced();
        check();
        check();

        verify(playerFleet.getFleetData(), times(1)).getMembersListCopy();
    }
}
