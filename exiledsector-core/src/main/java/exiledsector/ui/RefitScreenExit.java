package exiledsector.ui;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.refit.CoreUiDismissal;
import lunalib.backend.ui.refit.RefitButtonAdder;
import lunalib.lunaRefit.BaseRefitButton;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

final class RefitScreenExit implements EveryFrameScript {

    static final int MAX_WAIT_FRAMES = 300;

    enum Step {AWAIT_PANEL_REMOVAL, DISMISS_CORE, AWAIT_CORE_GONE, DISMISS_DIALOG, DONE}

    private final String message;
    private final InteractionDialogAPI hostDialog;
    private final BooleanSupplier panelGone;
    private final Predicate<CampaignUIAPI> coreDismisser;
    private Step step = Step.AWAIT_PANEL_REMOVAL;
    private int waitedFrames;

    RefitScreenExit(String message, InteractionDialogAPI hostDialog, BooleanSupplier panelGone, Predicate<CampaignUIAPI> coreDismisser) {
        this.message = message;
        this.hostDialog = hostDialog;
        this.panelGone = panelGone;
        this.coreDismisser = coreDismisser;
    }

    static void leave(BaseRefitButton refitButton, String message) {
        if (refitButton != null) {
            refitButton.closePanel();
        }
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
        if (campaignUI == null) {
            return;
        }
        sector.addTransientScript(new RefitScreenExit(message, campaignUI.getCurrentInteractionDialog(), RefitScreenExit::lunaPanelGone,
                CoreUiDismissal::dismiss));
    }

    private static boolean lunaPanelGone() {
        RefitButtonAdder adder = RefitPanelLauncher.refitButtonAdder();
        return adder == null || adder.getActivePanel() == null;
    }

    Step step() {
        return step;
    }

    @Override
    public boolean isDone() {
        return step == Step.DONE;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
        if (campaignUI == null) {
            finish();
            return;
        }
        if (step == Step.AWAIT_PANEL_REMOVAL) {
            if (!panelGone.getAsBoolean() && !waitedTooLong()) {
                return;
            }
            step = Step.DISMISS_CORE;
        }
        if (step == Step.DISMISS_CORE) {
            if (campaignUI.getCurrentCoreTab() == null) {
                step = Step.DISMISS_DIALOG;
            } else if (coreDismisser.test(campaignUI)) {
                waitedFrames = 0;
                step = Step.AWAIT_CORE_GONE;
                return;
            } else {
                finish();
                return;
            }
        }
        if (step == Step.AWAIT_CORE_GONE) {
            if (campaignUI.getCurrentCoreTab() != null) {
                if (waitedTooLong()) {
                    finish();
                }
                return;
            }
            step = Step.DISMISS_DIALOG;
        }
        if (step == Step.DISMISS_DIALOG) {
            InteractionDialogAPI currentDialog = campaignUI.getCurrentInteractionDialog();
            if (hostDialog != null && currentDialog == hostDialog) {
                currentDialog.dismiss();
            }
            finish();
        }
    }

    private boolean waitedTooLong() {
        waitedFrames++;
        return waitedFrames > MAX_WAIT_FRAMES;
    }

    private void finish() {
        if (step == Step.DONE) {
            return;
        }
        step = Step.DONE;
        if (message != null) {
            CampaignMessages.plain(message, Misc.getPositiveHighlightColor());
        }
    }
}
