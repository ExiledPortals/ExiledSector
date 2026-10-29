package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ui.refit.RefitFleetList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RefitShipSwitchTest {

    private final FleetMemberAPI target = mock(FleetMemberAPI.class);
    private final RefitFleetList.Shown shown = new RefitFleetList.Shown(target, mock(ShipVariantAPI.class));
    private final List<FleetMemberAPI> selected = new ArrayList<>();
    private final List<RefitFleetList.Shown> switched = new ArrayList<>();

    private RefitShipSwitch switchTo(boolean[] syncPending, RefitFleetList.Shown result) {
        return new RefitShipSwitch(target, () -> syncPending[0], member -> {
            selected.add(member);
            return result;
        }, switched::add);
    }

    @Test
    void switchesTheRefitScreenOnItsFirstFrame() {
        RefitShipSwitch shipSwitch = switchTo(new boolean[]{false}, shown);

        shipSwitch.advance(0f);

        assertEquals(List.of(target), selected);
        assertEquals(List.of(shown), switched);
        assertTrue(shipSwitch.isDone());
    }

    @Test
    void waitsForAPendingVariantSyncSoTheOldShipKeepsItsEdits() {
        boolean[] syncPending = {true};
        RefitShipSwitch shipSwitch = switchTo(syncPending, shown);

        shipSwitch.advance(0f);
        assertTrue(selected.isEmpty());
        assertFalse(shipSwitch.isDone());

        syncPending[0] = false;
        shipSwitch.advance(0f);
        assertEquals(List.of(shown), switched);
    }

    @Test
    void givesUpWaitingOnAStuckSync() {
        RefitShipSwitch shipSwitch = switchTo(new boolean[]{true}, shown);

        for (int frame = 0; frame <= RefitShipSwitch.MAX_WAIT_FRAMES; frame++) {
            shipSwitch.advance(0f);
        }

        assertEquals(List.of(target), selected);
        assertTrue(shipSwitch.isDone());
    }

    @Test
    void reportsARefusedSwitchOnlyOnce() {
        RefitShipSwitch shipSwitch = switchTo(new boolean[]{false}, null);

        shipSwitch.advance(0f);
        shipSwitch.advance(0f);

        assertEquals(1, switched.size());
        assertNull(switched.get(0));
        assertSame(target, selected.get(0));
        assertTrue(shipSwitch.runWhilePaused());
    }
}
