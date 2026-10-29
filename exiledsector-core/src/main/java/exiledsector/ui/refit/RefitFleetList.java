package exiledsector.ui.refit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RefitFleetList {

    public record Roster(List<FleetMemberAPI> members, int selectedIndex, int shownIndex) {

        static final Roster EMPTY = new Roster(List.of(), -1, -1);
    }

    public record Shown(FleetMemberAPI member, ShipVariantAPI variant) {
    }

    private static final Logger LOG = Logger.getLogger(RefitFleetList.class);
    private static boolean failureLogged;

    private RefitFleetList() {
    }

    // java:S1181: UiReflection calls declare Throwable, and a changed refit screen may only cost the fleet list, never the skill tree
    @SuppressWarnings("java:S1181")
    public static Roster read() {
        try {
            Object refitTab = refitTab();
            return refitTab == null ? Roster.EMPTY : roster(refitTab);
        } catch (Throwable e) {
            logFailure(e);
            return Roster.EMPTY;
        }
    }

    // java:S1181: UiReflection calls declare Throwable, and a changed refit screen may only cost the ship switch
    @SuppressWarnings("java:S1181")
    public static Shown select(FleetMemberAPI member) {
        try {
            Object refitTab = refitTab();
            return refitTab == null ? null : select(refitTab, member);
        } catch (Throwable e) {
            logFailure(e);
            return null;
        }
    }

    static Shown select(Object refitTab, FleetMemberAPI member) throws Throwable {
        Object button = buttonFor(refitTab, member);
        if (button == null) {
            return null;
        }
        UiReflection.call(refitTab, "actionPerformed", null, button);
        Object refitPanel = RefitShipDisplay.refitPanelOf(refitTab);
        if (UiReflection.call(refitPanel, RefitShipDisplay.GET_MEMBER) != member) {
            return null;
        }
        Object shipDisplay = UiReflection.call(refitPanel, RefitShipDisplay.GET_SHIP_DISPLAY);
        return UiReflection.call(shipDisplay, RefitShipDisplay.GET_CURRENT_VARIANT) instanceof ShipVariantAPI variant
                ? new Shown(member, variant) : null;
    }

    private static Object refitTab() throws Throwable {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
        if (campaignUI == null || campaignUI.getCurrentCoreTab() != CoreUITabId.REFIT) {
            return null;
        }
        return UiReflection.call(UiReflection.coreUI(campaignUI), "getCurrentTab");
    }

    static Roster roster(Object refitTab) throws Throwable {
        List<?> items = listItems(refitTab);
        Map<?, ?> buttonToMember = buttonToMember(refitTab);
        Object shownMember = UiReflection.call(RefitShipDisplay.refitPanelOf(refitTab), RefitShipDisplay.GET_MEMBER);
        List<FleetMemberAPI> members = new ArrayList<>();
        int highlightedIndex = -1;
        int shownIndex = -1;
        for (Object item : items) {
            if (!(buttonToMember.get(item) instanceof FleetMemberAPI member)) {
                continue;
            }
            if (item instanceof ButtonAPI button && button.isHighlighted() && highlightedIndex < 0) {
                highlightedIndex = members.size();
            }
            if (member == shownMember) {
                shownIndex = members.size();
            }
            members.add(member);
        }
        return new Roster(List.copyOf(members), shownIndex >= 0 ? shownIndex : highlightedIndex, shownIndex);
    }

    private static Object buttonFor(Object refitTab, FleetMemberAPI member) throws Throwable {
        Map<?, ?> buttonToMember = buttonToMember(refitTab);
        for (Object item : listItems(refitTab)) {
            if (buttonToMember.get(item) == member) {
                return item;
            }
        }
        return null;
    }

    private static List<?> listItems(Object refitTab) throws Throwable {
        return UiReflection.call(UiReflection.call(refitTab, "getFleetList"), "getItems") instanceof List<?> items
                ? new ArrayList<>(items) : List.of();
    }

    private static Map<?, ?> buttonToMember(Object refitTab) throws Throwable {
        return UiReflection.call(refitTab, "getButtonToMember") instanceof Map<?, ?> map ? map : Map.of();
    }

    private static void logFailure(Throwable e) {
        if (!failureLogged) {
            failureLogged = true;
            LOG.warn("[ExiledSector] Couldn't read the refit screen's fleet list; the skill tree's ship list stays closed", e);
        }
    }
}
