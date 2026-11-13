package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FoundItemQuantityTest {

    private static FleetMemberAPI member(String key, float percent) {
        FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(member.getStats().getDynamic().getValue(key, 0f)).thenReturn(percent);
        return member;
    }

    @Test
    void theFleetsQuantityPercentsAddTogether() {
        String key = FoundItemQuantity.HULL_ITEM_PERCENT_KEY;

        assertEquals(1.25f, FoundItemQuantity.mult(List.of(member(key, 10f), member(key, 15f)), key), 0.001f);
        assertEquals(1f, FoundItemQuantity.mult(List.of(), key), 0.001f);
    }

    @Test
    void theMultiplierNeverGoesBelowZeroAndIsOneWithoutAFleet() {
        String key = FoundItemQuantity.CRAFTING_ITEM_PERCENT_KEY;

        assertEquals(0f, FoundItemQuantity.mult(List.of(member(key, -150f)), key), 0.001f);
        assertEquals(1f, FoundItemQuantity.mult((CampaignFleetAPI) null, key), 0.001f);
    }
}
