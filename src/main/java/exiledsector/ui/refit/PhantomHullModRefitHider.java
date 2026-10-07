package exiledsector.ui.refit;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CharacterStatsRefreshListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.PhantomHullModStatus;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static exiledsector.ui.refit.UiReflection.call;

public class PhantomHullModRefitHider implements CharacterStatsRefreshListener {

    private static final Logger LOG = Logger.getLogger(PhantomHullModRefitHider.class);
    static final int MAX_VISIBLE_ROWS = 10;

    private static boolean failed;
    private static HideOnce pendingHide;

    @Override
    public void reportAboutToRefreshCharacterStatEffects() {
    }

    @Override
    public void reportRefreshedCharacterStatEffects() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (campaignUI != null && campaignUI.getCurrentCoreTab() == CoreUITabId.REFIT) {
            hideInRefitScreen(campaignUI);
        }
    }

    public static void requestRefresh() {
        if (failed || Global.getCurrentState() != GameState.CAMPAIGN) return;
        SectorAPI sector = Global.getSector();
        if (sector == null || pendingHide != null && !pendingHide.done && sector.hasTransientScript(HideOnce.class)) return;
        pendingHide = new HideOnce();
        sector.addTransientScript(pendingHide);
    }

    static void resetForTests() {
        failed = false;
        pendingHide = null;
        SkillTreeChipClickTarget.resetForTests();
        SkillTreeModsButton.resetForTests();
    }

    private static void hideInRefitScreen(CampaignUIAPI campaignUI) {
        if (failed) return;
        try {
            hidePhantomRows(coreUI(campaignUI));
        } catch (Throwable e) {
            failed = true;
            LOG.error("[ExiledSector] Could not hide phantom hull mods in the refit screen; they stay visible until the game is restarted", e);
        }
    }

    private static Object coreUI(CampaignUIAPI campaignUI) throws Throwable {
        InteractionDialogAPI dialog = campaignUI.getCurrentInteractionDialog();
        Object dialogCore = call(dialog, "getCoreUI");
        return dialogCore != null ? dialogCore : call(campaignUI, "getCore");
    }

    static void hidePhantomRows(Object coreUi) throws Throwable {
        Object refitPanel = call(call(coreUi, "getCurrentTab"), "getRefitPanel");
        Object modDisplay = call(refitPanel, "getModDisplay");
        if (modDisplay == null || !(call(call(refitPanel, "getShipDisplay"), "getCurrentVariant") instanceof ShipVariantAPI variant)) {
            return;
        }
        Object refitMods = call(modDisplay, "getMods");
        hidePhantomRows(refitMods, variant);
        attachChipClick(refitMods);
        SkillTreeModsButton.attach(refitMods);
        for (Object dialog : UiReflection.children(coreUi)) {
            for (Object child : UiReflection.children(dialog)) {
                if (child != modDisplay && child.getClass() == modDisplay.getClass()) {
                    hidePhantomRows(call(child, "getMods"), variant);
                }
            }
        }
    }

    private static void hidePhantomRows(Object modWidget, ShipVariantAPI variant) throws Throwable {
        Object modList = UiReflection.childWithMethod(modWidget, "collapseEmptySlots", 0);
        if (!(call(modList, "getItems") instanceof List<?> modRows)) return;

        List<Object> phantomRows = new ArrayList<>();
        for (Object row : modRows) {
            HullModSpecAPI spec = UiReflection.fieldOfType(row, HullModSpecAPI.class);
            if (spec != null && PhantomHullModStatus.isActive(spec.getId()) && InstalledHullMods.isInstalledBySkillTree(variant, spec.getId())) {
                phantomRows.add(row);
            }
        }
        if (phantomRows.isEmpty()) return;
        for (Object row : phantomRows) {
            call(modList, "removeItem", row);
        }
        call(modList, "collapseEmptySlots");
        resize(modWidget, modList, modRows.size());
    }

    private static void attachChipClick(Object modWidget) throws Throwable {
        Object modList = UiReflection.childWithMethod(modWidget, "collapseEmptySlots", 0);
        Object chipRow = null;
        if (call(modList, "getItems") instanceof List<?> modRows) {
            for (Object row : modRows) {
                HullModSpecAPI spec = UiReflection.fieldOfType(row, HullModSpecAPI.class);
                if (spec != null && SkillTreeHullMod.ID.equals(spec.getId())) {
                    chipRow = row;
                }
            }
        }
        SkillTreeChipClickTarget.attach(modWidget, modList, chipRow);
    }

    private static void resize(Object modWidget, Object modList, int rowCount) throws Throwable {
        if (!(modList instanceof UIComponentAPI listComponent)) return;
        float rowHeight = ((Number) call(modList, "getItemHeight")).floatValue() + ((Number) call(modList, "getItemPad")).floatValue();
        PositionAPI listPosition = listComponent.getPosition();
        listPosition.setSize(listPosition.getWidth(), rowHeight * Math.min(MAX_VISIBLE_ROWS, rowCount));
        call(modWidget, "pack");
    }

    private static final class HideOnce implements EveryFrameScript {

        private boolean done;

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            if (done) return;
            done = true;
            CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
            if (campaignUI != null) {
                hideInRefitScreen(campaignUI);
            }
        }
    }
}
