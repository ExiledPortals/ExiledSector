package exiledsector.ui.refit;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.CharacterStatsRefreshListener;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.PhantomHullModStatus;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static exiledsector.ui.refit.UiReflection.call;

public class PhantomHullModRefitHider implements CharacterStatsRefreshListener, CoreUITabListener {

    private static final Logger LOG = Logger.getLogger(PhantomHullModRefitHider.class);
    static final int MAX_VISIBLE_ROWS = 10;

    private boolean failed;

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

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId tab, Object param) {
        if (tab == CoreUITabId.REFIT && !failed) {
            Global.getSector().addTransientScript(new HideOnceOpened(this));
        }
    }

    private void hideInRefitScreen(CampaignUIAPI campaignUI) {
        if (failed) return;
        try {
            hidePhantomRows(coreUI(campaignUI));
        } catch (Throwable e) {
            failed = true;
            LOG.error("[ExiledSector] Could not hide phantom hull mods in the refit screen; they stay visible until the game is reloaded", e);
        }
    }

    private static Object coreUI(CampaignUIAPI campaignUI) throws Throwable {
        InteractionDialogAPI dialog = campaignUI.getCurrentInteractionDialog();
        Object core = call(dialog, "getCoreUI");
        return core != null ? core : call(campaignUI, "getCore");
    }

    static void hidePhantomRows(Object core) throws Throwable {
        Object refitPanel = call(call(core, "getCurrentTab"), "getRefitPanel");
        Object modWidget = call(call(refitPanel, "getModDisplay"), "getMods");
        if (modWidget == null || !(call(call(refitPanel, "getShipDisplay"), "getCurrentVariant") instanceof ShipVariantAPI variant)) {
            return;
        }
        Object list = UiReflection.childWithMethod(modWidget, "collapseEmptySlots", 0);
        if (!(call(list, "getItems") instanceof List<?> rows)) return;

        List<Object> phantomRows = new ArrayList<>();
        for (Object row : rows) {
            HullModSpecAPI spec = UiReflection.fieldOfType(row, HullModSpecAPI.class);
            if (spec != null && PhantomHullModStatus.isActive(spec.getId()) && InstalledHullMods.isInstalledBySkillTree(variant, spec.getId())) {
                phantomRows.add(row);
            }
        }
        if (phantomRows.isEmpty()) return;
        for (Object row : phantomRows) {
            call(list, "removeItem", row);
        }
        call(list, "collapseEmptySlots");
        resize(modWidget, list, rows.size());
    }

    private static void resize(Object modWidget, Object list, int rowCount) throws Throwable {
        if (!(list instanceof UIComponentAPI component)) return;
        float rowHeight = ((Number) call(list, "getItemHeight")).floatValue() + ((Number) call(list, "getItemPad")).floatValue();
        PositionAPI position = component.getPosition();
        position.setSize(position.getWidth(), rowHeight * Math.min(MAX_VISIBLE_ROWS, rowCount));
        call(modWidget, "pack");
    }

    private static final class HideOnceOpened implements EveryFrameScript {

        private final PhantomHullModRefitHider hider;
        private boolean done;

        private HideOnceOpened(PhantomHullModRefitHider hider) {
            this.hider = hider;
        }

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
                hider.hideInRefitScreen(campaignUI);
            }
        }
    }
}
