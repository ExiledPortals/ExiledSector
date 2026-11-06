package exiledsector.effects;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogisticsDockingTest {

    private static Industry industryTagged(String tag) {
        Industry industry = mock(Industry.class);
        IndustrySpecAPI spec = mock(IndustrySpecAPI.class);
        when(industry.getSpec()).thenReturn(spec);
        when(spec.hasTag(tag)).thenReturn(true);
        return industry;
    }

    private static MarketAPI marketWith(Industry... industries) {
        MarketAPI market = mock(MarketAPI.class);
        when(market.getIndustries()).thenReturn(List.of(industries));
        return market;
    }

    @Test
    void aSpaceportOrStationAllowsLogisticsChanges() {
        assertTrue(LogisticsDocking.hasSpaceportOrStation(marketWith(industryTagged("farming"), industryTagged(Industries.TAG_SPACEPORT))));
        assertTrue(LogisticsDocking.hasSpaceportOrStation(marketWith(industryTagged(Industries.TAG_STATION))));
    }

    @Test
    void aMarketWithoutEitherOrNoMarketDoesNot() {
        assertFalse(LogisticsDocking.hasSpaceportOrStation(marketWith(industryTagged("farming"))));
        assertFalse(LogisticsDocking.hasSpaceportOrStation(marketWith()));
        assertFalse(LogisticsDocking.hasSpaceportOrStation(null));
    }
}
