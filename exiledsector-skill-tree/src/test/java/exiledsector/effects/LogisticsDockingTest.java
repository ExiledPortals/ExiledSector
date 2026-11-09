package exiledsector.effects;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Set;

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

    private static SectorEntityToken entityAt(LocationAPI location, float x, float radius) {
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.getContainingLocation()).thenReturn(location);
        when(entity.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(entity.getRadius()).thenReturn(radius);
        return entity;
    }

    private static MarketAPI spaceportAt(SectorEntityToken primary, SectorEntityToken... connected) {
        MarketAPI market = marketWith(industryTagged(Industries.TAG_SPACEPORT));
        when(market.getPrimaryEntity()).thenReturn(primary);
        when(market.getConnectedEntities()).thenReturn(Set.of(connected));
        return market;
    }

    @Test
    void aFleetWithinRangeOfASpaceportsSurfaceMayChangeLogistics() {
        LocationAPI system = mock(LocationAPI.class);
        SectorEntityToken fleet = entityAt(system, 0f, 20f);
        MarketAPI market = spaceportAt(entityAt(system, 820f, 300f));
        assertTrue(LogisticsDocking.nearSpaceportOrStation(fleet, List.of(market), 500f));
        assertFalse(LogisticsDocking.nearSpaceportOrStation(fleet, List.of(market), 499f));
    }

    @Test
    void aConnectedStationCountsAsTheMarket() {
        LocationAPI system = mock(LocationAPI.class);
        SectorEntityToken fleet = entityAt(system, 0f, 0f);
        MarketAPI market = spaceportAt(entityAt(system, 5000f, 100f), entityAt(system, 300f, 50f));
        assertTrue(LogisticsDocking.nearSpaceportOrStation(fleet, List.of(market), 500f));
    }

    @Test
    void farMarketsAndMarketsWithoutASpaceportDoNot() {
        LocationAPI system = mock(LocationAPI.class);
        SectorEntityToken fleet = entityAt(system, 0f, 0f);
        MarketAPI farSpaceport = spaceportAt(entityAt(system, 5000f, 100f));
        SectorEntityToken farmPlanet = entityAt(system, 100f, 100f);
        MarketAPI nearFarm = marketWith(industryTagged("farming"));
        when(nearFarm.getPrimaryEntity()).thenReturn(farmPlanet);
        assertFalse(LogisticsDocking.nearSpaceportOrStation(fleet, List.of(farSpaceport, nearFarm), 500f));
    }

    @Test
    void aSpaceportInAnotherLocationDoesNot() {
        SectorEntityToken fleet = entityAt(mock(LocationAPI.class), 0f, 0f);
        MarketAPI elsewhere = spaceportAt(entityAt(mock(LocationAPI.class), 0f, 100f));
        assertFalse(LogisticsDocking.nearSpaceportOrStation(fleet, List.of(elsewhere), 500f));
    }
}
