package exiledsector.ui.refit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.apache.log4j.Logger;

public final class RefitShipDisplay {

    static final String GET_SHIP_DISPLAY = "getShipDisplay";
    static final String GET_MEMBER = "getMember";
    static final String GET_CURRENT_VARIANT = "getCurrentVariant";
    static final String SET_FLEET_MEMBER = "setFleetMember";
    static final String SET_FORCE_SET_MEMBER_NEXT_CALL = "setForceSetMemberNextCall";
    private static final Logger LOG = Logger.getLogger(RefitShipDisplay.class);
    private static boolean failureLogged;

    private RefitShipDisplay() {
    }

    // java:S1181: UiReflection calls declare Throwable, and a changed refit screen may only cost the slot refresh, never the edit
    @SuppressWarnings("java:S1181")
    public static void rebuildSlots(FleetMemberAPI member, ShipVariantAPI variant) {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
        if (campaignUI == null || campaignUI.getCurrentCoreTab() != CoreUITabId.REFIT) {
            return;
        }
        try {
            rebuildSlots(refitPanelOf(UiReflection.call(UiReflection.coreUI(campaignUI), "getCurrentTab")), member, variant);
        } catch (Throwable e) {
            if (!failureLogged) {
                failureLogged = true;
                LOG.warn("[ExiledSector] Couldn't rebuild the refit screen's weapon slots; they update once the ship is reselected", e);
            }
        }
    }

    static boolean rebuildSlots(Object refitPanel, FleetMemberAPI member, ShipVariantAPI variant) throws Throwable {
        Object shipDisplay = UiReflection.call(refitPanel, GET_SHIP_DISPLAY);
        if (shipDisplay == null || member == null || variant == null || UiReflection.call(refitPanel, GET_MEMBER) != member
                || UiReflection.call(shipDisplay, GET_CURRENT_VARIANT) != variant) {
            return false;
        }
        UiReflection.call(refitPanel, SET_FORCE_SET_MEMBER_NEXT_CALL, true);
        try {
            UiReflection.call(shipDisplay, SET_FLEET_MEMBER, member, variant);
        } finally {
            UiReflection.call(refitPanel, SET_FORCE_SET_MEMBER_NEXT_CALL, false);
        }
        return true;
    }

    static Object refitPanelOf(Object refitTab) throws Throwable {
        Object refitPanel = UiReflection.call(refitTab, "getRefitPanel");
        return refitPanel != null ? refitPanel : UiReflection.childWithMethod(refitTab, GET_SHIP_DISPLAY, 0);
    }
}
