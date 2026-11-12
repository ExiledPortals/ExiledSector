package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerText;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTerritoryTransferTest {

    private static final long SEED = 0x7a5f3L;

    private static AtlasGenerator generator(AtlasState state) {
        AtlasRules rules = new AtlasRules(List.of(new AtlasRegion(AtlasRegion.NORMAL, 5f, 0, "", "", Map.of(), Map.of(), Map.of())),
                Map.of("compact", 1f), Map.of("pirates", 1f, "hegemony", 1f), AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER, List.of(),
                List.of());
        return new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
    }

    private record Border(AtlasCell hegemonyCentre, AtlasCell borderCell, AtlasCell pirateCentre, AtlasCell beyondCell) {
    }

    private static Border border(AtlasGenerator generator, AtlasState state) {
        for (AtlasCell borderCell : AtlasCell.ORIGIN.around(6)) {
            List<AtlasCell> lanes = generator.lanes(borderCell).stream().filter(lane -> !lane.equals(AtlasCell.ORIGIN)).toList();
            if (borderCell.equals(AtlasCell.ORIGIN) || generator.isVoid(borderCell) || lanes.size() < 3
                    || generator.lanes(lanes.get(0)).contains(lanes.get(1))) {
                continue;
            }
            AtlasCell hegemonyCentre = lanes.get(0);
            AtlasCell beyondCell = lanes.stream().skip(2).filter(lane -> !generator.lanes(hegemonyCentre).contains(lane)).findFirst().orElse(null);
            if (beyondCell == null) {
                continue;
            }
            TestTerritories.establish(state, hegemonyCentre, "hegemony");
            TestTerritories.establish(state, lanes.get(1), "pirates");
            return new Border(hegemonyCentre, borderCell, lanes.get(1), beyondCell);
        }
        throw new AssertionError("no system lies between two others");
    }

    @Test
    void aSystemJoinsANeighbouringTerritoryOfItsNewFaction() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        Border border = border(generator, state);

        assertEquals(AtlasTerritoryTransfer.Result.JOINED, AtlasTerritoryTransfer.transfer(generator, state, border.borderCell(), "hegemony"));
        assertEquals(border.hegemonyCentre(), state.commandCentreOf(border.borderCell()));
        assertEquals("hegemony", state.claimedFaction(border.borderCell()));

        assertEquals(AtlasTerritoryTransfer.Result.JOINED, AtlasTerritoryTransfer.transfer(generator, state, border.borderCell(), "pirates"));
        assertEquals(border.pirateCentre(), state.commandCentreOf(border.borderCell()));
    }

    @Test
    void aSystemWithNoNeighbouringTerritoryOfItsNewFactionIsReleased() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        Border border = border(generator, state);
        state.claim(border.borderCell(), border.hegemonyCentre());

        assertEquals(AtlasTerritoryTransfer.Result.RELEASED, AtlasTerritoryTransfer.transfer(generator, state, border.borderCell(), "tritachyon"));
        assertFalse(state.isClaimed(border.borderCell()));
    }

    @Test
    void aCommandCentreCannotChangeHands() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        Border border = border(generator, state);

        assertEquals(AtlasTerritoryTransfer.Result.COMMAND_CENTRE,
                AtlasTerritoryTransfer.transfer(generator, state, border.hegemonyCentre(), "pirates"));
        assertEquals("hegemony", state.claimedFaction(border.hegemonyCentre()));
    }

    @Test
    void aSystemListsTheOtherTerritoriesItBorders() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        Border border = border(generator, state);

        assertEquals(Map.of("hegemony", border.hegemonyCentre(), "pirates", border.pirateCentre()),
                AtlasTerritoryTransfer.neighbouringCentres(generator, state, border.borderCell()));
        state.claim(border.borderCell(), border.hegemonyCentre());
        assertEquals(Map.of("pirates", border.pirateCentre()), AtlasTerritoryTransfer.neighbouringCentres(generator, state, border.borderCell()));
    }

    @Test
    void vacatingACommandCentreSendsItsOfficerAwayAndBreaksUpItsTerritory() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        Border border = border(generator, state);
        state.claim(border.borderCell(), border.hegemonyCentre());
        int recordId = state.officerAt(border.hegemonyCentre()).recordId();

        AtlasTerritoryTransfer.vacate(generator, state, border.hegemonyCentre());

        AtlasOfficer fled = state.officer(recordId);
        assertTrue(fled.hasFled());
        assertNull(state.officerAt(border.hegemonyCentre()));
        assertEquals(Set.of(), state.claimsOf(border.hegemonyCentre()));
        assertFalse(state.isClaimed(border.borderCell()));
        assertEquals(new OfficerText.HistoryEntry(AtlasTerritoryPlacer.FLED_HISTORY, border.hegemonyCentre(), border.hegemonyCentre()),
                OfficerText.latestEntry(fled));
        assertEquals("pirates", state.claimedFaction(border.pirateCentre()));
    }

    @Test
    void vacatingAnOrdinarySystemOnlyReleasesIt() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        Border border = border(generator, state);
        state.claim(border.borderCell(), border.hegemonyCentre());

        AtlasTerritoryTransfer.vacate(generator, state, border.borderCell());

        assertFalse(state.isClaimed(border.borderCell()));
        assertTrue(state.officerAt(border.hegemonyCentre()).isActive());
    }

    @Test
    void systemsCutOffFromTheirCommandCentreAreReleased() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        Border border = border(generator, state);
        state.claim(border.borderCell(), border.hegemonyCentre());
        state.claim(border.beyondCell(), border.hegemonyCentre());

        assertEquals(AtlasTerritoryTransfer.Result.JOINED, AtlasTerritoryTransfer.transfer(generator, state, border.borderCell(), "pirates"));

        assertFalse(state.isClaimed(border.beyondCell()));
        assertEquals(Set.of(border.hegemonyCentre()), state.claimsOf(border.hegemonyCentre()));
    }

    @Test
    void vacatingASystemReleasesWhatItCutsOff() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        Border border = border(generator, state);
        state.claim(border.borderCell(), border.hegemonyCentre());
        state.claim(border.beyondCell(), border.hegemonyCentre());

        AtlasTerritoryTransfer.vacate(generator, state, border.borderCell());

        assertFalse(state.isClaimed(border.beyondCell()));
        assertTrue(state.officerAt(border.hegemonyCentre()).isActive());
    }
}
