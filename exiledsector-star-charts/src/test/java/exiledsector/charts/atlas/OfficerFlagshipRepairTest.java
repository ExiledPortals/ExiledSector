package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerBoardGenerator;
import exiledsector.charts.officer.OfficerRank;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerFlagshipRepairTest {

    private static final List<OfficerRank> RANKS = List.of(
            new OfficerRank("lieutenant", 10, 0, 1, 2),
            new OfficerRank("admiral", 60, 50, 3, 1));
    private static final FlagshipCatalogue OLD_CATALOGUE = (factionId, rankOrder, rankCount, random) ->
            new FlagshipCatalogue.Flagship("buffalo", "buffalo_Standard");
    private static final FlagshipCatalogue NEW_CATALOGUE = (factionId, rankOrder, rankCount, random) -> rankOrder == rankCount - 1
            ? new FlagshipCatalogue.Flagship("onslaught", "onslaught_Elite")
            : new FlagshipCatalogue.Flagship("eagle", "eagle_Assault");
    private static final TerritoryContext CONTEXT = new TerritoryContext(null, null, NEW_CATALOGUE);

    private static AtlasState boardFromBeforeTheReroll() {
        AtlasState state = new AtlasState(TestBorders.SEED);
        OfficerBoardGenerator.generate(TestBorders.SEED, RANKS, Map.of("hegemony", 1f), OLD_CATALOGUE)
                .forEach(officer -> state.putOfficer(officer.withDModIds(List.of("comp_armor", "degraded_shields"))));
        state.setFlagshipRoll(0);
        return state;
    }

    private static List<String> withoutShieldDMods(FlagshipCatalogue.Flagship flagship, List<String> dModIds) {
        return dModIds.stream().filter(dModId -> !dModId.equals("degraded_shields")).toList();
    }

    @Test
    void everyOfficerFromBeforeTheRerollGetsANewFlagshipOnce() {
        AtlasState state = boardFromBeforeTheReroll();

        assertTrue(OfficerFlagshipRepair.repair(state, RANKS, CONTEXT, OfficerFlagshipRepairTest::withoutShieldDMods));

        for (AtlasOfficer officer : state.officers()) {
            String expectedHull = officer.rankId().equals("admiral") ? "onslaught" : "eagle";
            assertEquals(expectedHull, officer.flagshipHullId());
            assertEquals(List.of("comp_armor"), officer.dmodIds());
        }
        assertEquals(AtlasState.FLAGSHIP_ROLL, state.flagshipRoll());
        assertFalse(OfficerFlagshipRepair.repair(state, RANKS, CONTEXT, OfficerFlagshipRepairTest::withoutShieldDMods));
    }

    @Test
    void aNewAtlasNeverRerolls() {
        AtlasState state = new AtlasState(TestBorders.SEED);
        OfficerBoardGenerator.generate(TestBorders.SEED, RANKS, Map.of("hegemony", 1f), OLD_CATALOGUE).forEach(state::putOfficer);

        assertFalse(OfficerFlagshipRepair.repair(state, RANKS, CONTEXT, OfficerFlagshipRepairTest::withoutShieldDMods));
        assertTrue(state.officers().stream().allMatch(officer -> officer.flagshipHullId().equals("buffalo")));
    }

    @Test
    void theRerollWaitsWhileAnOfficerFightsInAnOpenRun() {
        AtlasState state = boardFromBeforeTheReroll();
        AtlasCell runCell = new AtlasCell(3, 2);
        AtlasOfficer fighting = state.officers().get(0);
        state.putOfficer(fighting.takingCommand(runCell, ""));
        TerritoryContext duringRun = new TerritoryContext(null, runCell, NEW_CATALOGUE);

        assertFalse(OfficerFlagshipRepair.repair(state, RANKS, duringRun, OfficerFlagshipRepairTest::withoutShieldDMods));
        assertEquals("buffalo", state.officer(fighting.recordId()).flagshipHullId());
        assertTrue(OfficerFlagshipRepair.repair(state, RANKS, CONTEXT, OfficerFlagshipRepairTest::withoutShieldDMods));
    }

    @Test
    void anOfficerWhoseFactionHasNoWarshipsKeepsTheirFlagship() {
        AtlasState state = boardFromBeforeTheReroll();

        assertFalse(OfficerFlagshipRepair.repair(state, RANKS, TerritoryContext.NONE, OfficerFlagshipRepairTest::withoutShieldDMods));

        assertTrue(state.officers().stream().allMatch(officer -> officer.flagshipHullId().equals("buffalo")));
        assertEquals(AtlasState.FLAGSHIP_ROLL, state.flagshipRoll());
    }
}
