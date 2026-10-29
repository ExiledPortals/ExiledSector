package exiledsector.ui.refit;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefitFleetListTest {

    public static class FakeFleetList {
        final List<Object> items = new ArrayList<>();

        public List<Object> getItems() {
            return items;
        }
    }

    public static class FakeShipDisplay {
        ShipVariantAPI currentVariant;

        public ShipVariantAPI getCurrentVariant() {
            return currentVariant;
        }
    }

    public static class FakeRefitPanel {
        final FakeShipDisplay shipDisplay = new FakeShipDisplay();
        FleetMemberAPI member;

        public FleetMemberAPI getMember() {
            return member;
        }

        public FakeShipDisplay getShipDisplay() {
            return shipDisplay;
        }
    }

    public static class FakeRefitTab {
        final FakeFleetList fleetList = new FakeFleetList();
        final Map<Object, FleetMemberAPI> buttonToMember = new HashMap<>();
        final Map<FleetMemberAPI, ShipVariantAPI> variants = new HashMap<>();
        final FakeRefitPanel refitPanel = new FakeRefitPanel();
        final List<Object> pressed = new ArrayList<>();
        boolean blockedByDialog;

        public FakeFleetList getFleetList() {
            return fleetList;
        }

        public Map<Object, FleetMemberAPI> getButtonToMember() {
            return buttonToMember;
        }

        public FakeRefitPanel getRefitPanel() {
            return refitPanel;
        }

        public void actionPerformed(Object input, Object button) {
            pressed.add(button);
            if (!blockedByDialog) {
                refitPanel.member = buttonToMember.get(button);
                refitPanel.shipDisplay.currentVariant = variants.get(refitPanel.member);
            }
        }

        ButtonAPI add(FleetMemberAPI member, boolean highlighted) {
            ButtonAPI button = mock(ButtonAPI.class);
            when(button.isHighlighted()).thenReturn(highlighted);
            fleetList.items.add(button);
            buttonToMember.put(button, member);
            variants.put(member, mock(ShipVariantAPI.class));
            return button;
        }
    }

    @Test
    void listsShipsInTheRefitScreensOrderWithTheHighlightedOneSelected() throws Throwable {
        FakeRefitTab refitTab = new FakeRefitTab();
        FleetMemberAPI first = mock(FleetMemberAPI.class);
        FleetMemberAPI second = mock(FleetMemberAPI.class);
        FleetMemberAPI third = mock(FleetMemberAPI.class);
        refitTab.add(first, false);
        refitTab.fleetList.items.add(new Object());
        refitTab.add(second, true);
        refitTab.add(third, false);
        refitTab.refitPanel.member = mock(FleetMemberAPI.class);

        RefitFleetList.Roster roster = RefitFleetList.roster(refitTab);

        assertEquals(List.of(first, second, third), roster.members());
        assertEquals(1, roster.selectedIndex());
        assertEquals(-1, roster.shownIndex());
    }

    @Test
    void theShipOnTheRefitScreenOutranksAnotherHighlightedButton() throws Throwable {
        FakeRefitTab refitTab = new FakeRefitTab();
        FleetMemberAPI first = mock(FleetMemberAPI.class);
        FleetMemberAPI second = mock(FleetMemberAPI.class);
        refitTab.add(first, true);
        refitTab.add(second, false);
        refitTab.refitPanel.member = second;

        RefitFleetList.Roster roster = RefitFleetList.roster(refitTab);

        assertEquals(1, roster.selectedIndex());
        assertEquals(1, roster.shownIndex());
    }

    @Test
    void selectingPressesThatShipsListButtonAndReturnsWhatTheRefitScreenNowShows() throws Throwable {
        FakeRefitTab refitTab = new FakeRefitTab();
        FleetMemberAPI first = mock(FleetMemberAPI.class);
        FleetMemberAPI second = mock(FleetMemberAPI.class);
        refitTab.add(first, true);
        ButtonAPI secondButton = refitTab.add(second, false);
        refitTab.refitPanel.member = first;

        RefitFleetList.Shown shown = RefitFleetList.select(refitTab, second);

        assertEquals(List.of(secondButton), refitTab.pressed);
        assertSame(second, shown.member());
        assertSame(refitTab.variants.get(second), shown.variant());
    }

    @Test
    void aSwitchHeldUpByAVanillaDialogReportsNothingShown() throws Throwable {
        FakeRefitTab refitTab = new FakeRefitTab();
        FleetMemberAPI first = mock(FleetMemberAPI.class);
        FleetMemberAPI second = mock(FleetMemberAPI.class);
        refitTab.add(first, true);
        refitTab.add(second, false);
        refitTab.refitPanel.member = first;
        refitTab.blockedByDialog = true;

        assertNull(RefitFleetList.select(refitTab, second));
    }

    @Test
    void aShipMissingFromTheListIsNeverPressed() throws Throwable {
        FakeRefitTab refitTab = new FakeRefitTab();
        refitTab.add(mock(FleetMemberAPI.class), true);

        assertNull(RefitFleetList.select(refitTab, mock(FleetMemberAPI.class)));
        assertEquals(List.of(), refitTab.pressed);
    }
}
