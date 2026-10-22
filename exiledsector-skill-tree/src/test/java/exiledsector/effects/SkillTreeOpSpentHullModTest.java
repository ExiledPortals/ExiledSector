package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class SkillTreeOpSpentHullModTest {

    @Test
    void theRefitCanNeverStripTheReserveEvenWhenDockedAtAMarket() {
        SkillTreeOpSpentHullMod reserve = new SkillTreeOpSpentHullMod();
        ShipAPI ship = mock(ShipAPI.class);

        assertFalse(reserve.canBeAddedOrRemovedNow(ship, null, null));
        assertFalse(reserve.canBeAddedOrRemovedNow(ship, mock(MarketAPI.class), CampaignUIAPI.CoreUITradeMode.OPEN));
    }
}
