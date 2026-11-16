package exiledsector.charts.anomaly.relay;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RelayBroadcastTest {

    private static final CarriedItem KERNELS = new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 2);
    private static final CarriedItem ENGRAM = CarriedItem.of(RewardKind.ENGRAM, "engram_a");

    private static AtlasOfficer waiting(int recordId) {
        return AtlasOfficer.waiting(recordId, "hegemony", "commander", "steady", new AtlasOfficer.OfficerSeeds(1L, 2L, 3L, 4L), null);
    }

    private static AtlasOfficer active(int recordId) {
        return waiting(recordId).takingCommand(AtlasCell.ORIGIN, "took command");
    }

    @Test
    void theBroadcastGoesOnlyToAnOfficerInCommand() {
        AtlasOfficer commanding = active(2);

        assertSame(commanding, RelayBroadcast.pickOfficer(List.of(waiting(1), commanding, waiting(3)), new Random(4L)));
        assertNull(RelayBroadcast.pickOfficer(List.of(waiting(1)), new Random(4L)));
        assertNull(RelayBroadcast.pickOfficer(List.of(), new Random(4L)));
    }

    @Test
    void theOfficerCarriesTheRewardsAndTakesEveryScarOfTheChoice() {
        AtlasOfficer officer = active(1).withCarried(List.of(KERNELS)).withScars(Map.of("kited", 1));

        AtlasOfficer shared = RelayBroadcast.shared(officer, List.of(ENGRAM, ENGRAM), List.of("kited", "rammed"));

        assertEquals(CarriedItem.joined(List.of(KERNELS), List.of(ENGRAM, ENGRAM)), shared.carried());
        assertEquals(Map.of("kited", 2, "rammed", 1), shared.scars());
        assertEquals(officer.recordId(), shared.recordId());
    }
}
