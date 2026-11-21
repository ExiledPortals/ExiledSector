package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerBoardGenerator;
import exiledsector.charts.officer.OfficerRank;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerPortraitsTest {

    private static final List<OfficerRank> RANKS = List.of(
            new OfficerRank("lieutenant", 10, 0, 1, 2),
            new OfficerRank("admiral", 60, 50, 3, 1));
    private static final FlagshipCatalogue CATALOGUE = (factionId, rankOrder, rankCount, random) ->
            new FlagshipCatalogue.Flagship("eagle", "eagle_Assault");
    private static final TerritoryContext CONTEXT = new TerritoryContext(null, null, CATALOGUE);
    private static final String SHARED = "hegemony_0";

    private static List<String> portraits(String prefix, int count) {
        return IntStream.range(0, count).mapToObj(index -> prefix + "_" + index).toList();
    }

    private static OfficerPortraits.PortraitSource source(int hegemonyPortraits) {
        return new OfficerPortraits.PortraitSource() {
            @Override
            public String seeded(AtlasOfficer officer) {
                return SHARED;
            }

            @Override
            public List<String> options(String factionId, AtlasOfficer officer) {
                return factionId.equals("hegemony") ? portraits("hegemony", hegemonyPortraits) : portraits(factionId, 20);
            }
        };
    }

    private static AtlasState board() {
        AtlasState state = new AtlasState(TestBorders.SEED);
        OfficerBoardGenerator.generate(TestBorders.SEED, RANKS, Map.of("hegemony", 1f), CATALOGUE).forEach(state::putOfficer);
        assertTrue(state.officers().size() >= 3);
        return state;
    }

    private static Set<String> distinctPortraits(AtlasState state) {
        Set<String> portraits = new HashSet<>();
        state.officers().forEach(officer -> portraits.add(officer.portraitPath()));
        return portraits;
    }

    @Test
    void noTwoOfficersShareAPortrait() {
        AtlasState state = board();

        assertTrue(OfficerPortraits.assign(state, CONTEXT, source(20)));

        assertTrue(state.officers().stream().allMatch(AtlasOfficer::hasPortrait));
        assertEquals(state.officers().size(), distinctPortraits(state).size());
        assertTrue(state.officers().stream().allMatch(officer -> officer.portraitPath().startsWith("hegemony")));
        assertFalse(OfficerPortraits.assign(state, CONTEXT, source(20)));
    }

    @Test
    void anOfficerTakesAnIndependentPortraitOnceTheirFactionsAreAllUsed() {
        AtlasState state = board();

        OfficerPortraits.assign(state, CONTEXT, source(1));

        assertEquals(state.officers().size(), distinctPortraits(state).size());
        assertEquals(1, state.officers().stream().filter(officer -> officer.portraitPath().startsWith("hegemony")).count());
        assertTrue(state.officers().stream().filter(officer -> !officer.portraitPath().equals(SHARED))
                .allMatch(officer -> officer.portraitPath().startsWith("independent")));
    }

    @Test
    void storedPortraitsStayAndCountAsUsed() {
        AtlasState state = board();
        AtlasOfficer last = state.officers().get(state.officers().size() - 1);
        state.putOfficer(last.withPortrait(SHARED));

        OfficerPortraits.assign(state, CONTEXT, source(20));

        assertEquals(SHARED, state.officer(last.recordId()).portraitPath());
        assertEquals(state.officers().size(), distinctPortraits(state).size());
    }

    @Test
    void anOfficerInAnOpenRunKeepsThePortraitTheirFleetShows() {
        AtlasState state = board();
        AtlasCell runCell = new AtlasCell(3, 2);
        AtlasOfficer fighting = state.officers().get(state.officers().size() - 1);
        state.putOfficer(fighting.takingCommand(runCell, ""));
        TerritoryContext duringRun = new TerritoryContext(null, runCell, CATALOGUE);

        OfficerPortraits.assign(state, duringRun, source(20));

        assertEquals(SHARED, state.officer(fighting.recordId()).portraitPath());
        state.officers().stream().filter(officer -> officer.recordId() != fighting.recordId())
                .forEach(officer -> assertNotEquals(SHARED, officer.portraitPath()));
        assertEquals(state.officers().size(), distinctPortraits(state).size());
    }
}
