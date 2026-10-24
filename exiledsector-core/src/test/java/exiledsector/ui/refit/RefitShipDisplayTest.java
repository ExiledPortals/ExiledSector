package exiledsector.ui.refit;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RefitShipDisplayTest {

    public static class FakeShipDisplay {
        final List<String> calls;
        final FakeRefitPanel refitPanel;
        ShipVariantAPI currentVariant;
        Object rebuiltMember;
        Object rebuiltVariant;

        FakeShipDisplay(FakeRefitPanel refitPanel, ShipVariantAPI currentVariant) {
            this.refitPanel = refitPanel;
            this.calls = refitPanel.calls;
            this.currentVariant = currentVariant;
        }

        public ShipVariantAPI getCurrentVariant() {
            return currentVariant;
        }

        public void setFleetMember(Object member, Object variant) {
            calls.add("setFleetMember forced=" + refitPanel.forceSetMemberNextCall);
            rebuiltMember = member;
            rebuiltVariant = variant;
        }
    }

    public static class FakeRefitPanel {
        final List<String> calls = new ArrayList<>();
        final FleetMemberAPI member;
        FakeShipDisplay shipDisplay;
        boolean forceSetMemberNextCall;

        FakeRefitPanel(FleetMemberAPI member) {
            this.member = member;
        }

        public FleetMemberAPI getMember() {
            return member;
        }

        public FakeShipDisplay getShipDisplay() {
            return shipDisplay;
        }

        public void setForceSetMemberNextCall(boolean force) {
            calls.add("force=" + force);
            forceSetMemberNextCall = force;
        }
    }

    private static FakeRefitPanel refitPanel(FleetMemberAPI member, ShipVariantAPI shownVariant) {
        FakeRefitPanel refitPanel = new FakeRefitPanel(member);
        refitPanel.shipDisplay = new FakeShipDisplay(refitPanel, shownVariant);
        return refitPanel;
    }

    @Test
    void rebuildingForcesTheShipDisplayToResetItsMemberWithTheEditedVariant() throws Throwable {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        FakeRefitPanel refitPanel = refitPanel(member, variant);

        assertTrue(RefitShipDisplay.rebuildSlots(refitPanel, member, variant));

        assertEquals(List.of("force=true", "setFleetMember forced=true", "force=false"), refitPanel.calls);
        assertSame(member, refitPanel.shipDisplay.rebuiltMember);
        assertSame(variant, refitPanel.shipDisplay.rebuiltVariant);
    }

    @Test
    void aRefitScreenShowingAnotherShipOrVariantIsLeftAlone() throws Throwable {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        FakeRefitPanel otherShip = refitPanel(mock(FleetMemberAPI.class), variant);
        FakeRefitPanel otherVariant = refitPanel(member, mock(ShipVariantAPI.class));

        assertFalse(RefitShipDisplay.rebuildSlots(otherShip, member, variant));
        assertFalse(RefitShipDisplay.rebuildSlots(otherVariant, member, variant));
        assertFalse(RefitShipDisplay.rebuildSlots(null, member, variant));

        assertTrue(otherShip.calls.isEmpty());
        assertTrue(otherVariant.calls.isEmpty());
    }
}
