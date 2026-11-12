package exiledsector.charts.officer;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfficerSightingsTest {

    private static final AtlasCell SEEN_CENTRE = new AtlasCell(1, 1);
    private static final AtlasCell HIDDEN_CENTRE = new AtlasCell(5, 5);

    private static AtlasOfficer waiting(int recordId) {
        return AtlasOfficer.waiting(recordId, "hegemony", "commander", "steady", new AtlasOfficer.OfficerSeeds(1L, 2L, 3L, 4L),
                new FlagshipCatalogue.Flagship("onslaught", "onslaught_Elite"));
    }

    private static AtlasOfficer commanding(int recordId, AtlasCell centre) {
        return waiting(recordId).takingCommand(centre, "arrived:" + centre.pack());
    }

    private static AtlasState stateWith(AtlasOfficer... officers) {
        AtlasState state = new AtlasState(9L);
        for (AtlasOfficer officer : officers) {
            state.putOfficer(officer);
        }
        return state;
    }

    @Test
    void onlyActiveOfficersWhoseCentreIsInViewAreIdentified() {
        AtlasState state = stateWith(commanding(1, SEEN_CENTRE), commanding(2, HIDDEN_CENTRE), waiting(3));

        assertTrue(OfficerSightings.identifyInView(state, Set.of(SEEN_CENTRE)));

        assertTrue(state.officer(1).identified());
        assertFalse(state.officer(2).identified());
        assertFalse(state.officer(3).identified());
    }

    @Test
    void identifyingIgnoresUnknownAndAlreadyIdentifiedOfficers() {
        AtlasState state = stateWith(commanding(1, SEEN_CENTRE).identifiedNow());

        assertFalse(OfficerSightings.identify(state, List.of(1, 99)));
        assertTrue(OfficerSightings.identify(stateWith(waiting(2)), List.of(2)));
    }

    @Test
    void onlyIdentifiedActiveOfficersShowTheirSystems() {
        AtlasState state = stateWith(commanding(1, SEEN_CENTRE).identifiedNow(), commanding(2, HIDDEN_CENTRE), waiting(3).identifiedNow());

        assertEquals(Set.of(SEEN_CENTRE), OfficerSightings.shownSystems(state));
    }

    @Test
    void foughtFleetsNameTheirOfficers() {
        CampaignFleetAPI officerFleet = mock(CampaignFleetAPI.class);
        MemoryAPI officerMemory = mock(MemoryAPI.class);
        when(officerFleet.getMemoryWithoutUpdate()).thenReturn(officerMemory);
        when(officerMemory.get(OfficerFleets.RECORD_MEMORY_KEY)).thenReturn(4);
        CampaignFleetAPI plainFleet = mock(CampaignFleetAPI.class);
        when(plainFleet.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));

        assertEquals(List.of(4), OfficerSightings.officersIn(List.of(officerFleet, plainFleet)));
        assertTrue(OfficerSightings.officersIn(null).isEmpty());
    }
}
