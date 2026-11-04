package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlackHoleSalvageTest {

    private static FleetMemberAPI member(float percent, boolean mothballed) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.isMothballed()).thenReturn(mothballed);
        when(member.getStats().getDynamic().getValue(BlackHoleSalvage.HULL_ITEM_PERCENT_KEY, 0f)).thenReturn(percent);
        return member;
    }

    @Test
    void everyActiveShipAddsItsBonusAndMothballedShipsAddNothing() {
        assertEquals(10f, BlackHoleSalvage.hullItemPercent(List.of(member(5f, false), member(5f, false), member(5f, true), member(0f, false))));
        assertEquals(0f, BlackHoleSalvage.hullItemPercent((CampaignFleetAPI) null));
    }

    @Test
    void theEffectRecordsItsBonusOnTheShipForTheFleetSumAndSkipsNpcShips() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        StatBonus contribution = stats.getDynamic().getMod(BlackHoleSalvage.HULL_ITEM_PERCENT_KEY);

        LogisticsSkillEffect.BLACK_HOLE_HULL_ITEM_PERCENT.apply(stats, "horizon", 5f);

        verify(contribution).modifyFlat("horizon", 5f);
        assertFalse(LogisticsSkillEffect.BLACK_HOLE_HULL_ITEM_PERCENT.appliesToNpcShips());
    }
}
