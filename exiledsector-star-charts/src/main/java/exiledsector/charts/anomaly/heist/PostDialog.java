package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyDialog;
import exiledsector.charts.anomaly.AnomalyEncounters;
import exiledsector.charts.atlas.AtlasProgress;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

import java.util.Random;

public class PostDialog extends AnomalyDialog {

    static final String OPEN = "exiledSector_postOpen";
    static final String ENGAGE_OPTION = "exiledSector_postEngage";

    private SectorEntityToken post;

    @Override
    protected void open() {
        post = dialog().getInteractionTarget();
        showEntityVisual(post);
        VanillaText.plainPara(text(), Translation.gameText("chart.post.dialog.description"));
        showMain();
    }

    private ChartRun run() {
        return ChartRuns.liveIn(post.getContainingLocation());
    }

    private PostScript script() {
        return PostScript.in(post.getContainingLocation());
    }

    void showMain() {
        TextPanelAPI text = text();
        OptionPanelAPI options = options();
        options.clearOptions();
        PostState state = PostState.of(run());
        PostScript script = script();
        if (state == null) {
            VanillaText.plainPara(text, Translation.gameText("chart.post.dialog.stale"));
        } else if (state.taken()) {
            VanillaText.plainPara(text, Translation.gameText("chart.post.dialog.taken"));
        } else if (!state.alarm()) {
            VanillaText.plainPara(text, Translation.gameText("chart.post.dialog.quiet"));
            options.addOption(Translation.gameText("chart.post.dialog.option.open"), OPEN);
        } else {
            int vault = PostText.percent(state.vaultShare());
            boolean defended = script != null && script.defended();
            String key = defended ? "chart.post.dialog.guarded" : "chart.post.dialog.purged";
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg(key).arg("value", vault).text()));
            if (defended) {
                options.addOption(Translation.gameText("chart.post.dialog.option.engage"), ENGAGE_OPTION);
            } else {
                options.addOption(Translation.gameText("chart.post.dialog.option.openPurged"), OPEN);
            }
        }
        addLeaveOption();
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        ChartRun run = run();
        PostState state = PostState.of(run);
        PostScript script = script();
        boolean open = state != null && !state.taken() && (!state.alarm() || script == null || !script.defended());
        if (OPEN.equals(optionData) && open) {
            openVault(run, state);
        } else if (ENGAGE_OPTION.equals(optionData) && state != null && state.alarm() && !state.taken() && script != null) {
            engage(script.gatherDefenders());
        } else {
            dialog().dismiss();
        }
    }

    void openVault(ChartRun run, PostState state) {
        PostState taken = state.asTaken();
        PostState.record(run, taken);
        TextPanelAPI text = text();
        if (taken.clean() && AtlasProgress.revealLanesAround(Global.getSector(), run)) {
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.post.dialog.lanes").count(AtlasReach.LISTENING_POST_LANES).text()));
        }
        CargoAPI vault = VaultLoot.roll(Global.getSector(), run, state.vaultShare(), new Random(state.seed()));
        if (vault == null || vault.isEmpty()) {
            VanillaText.plainPara(text, Translation.gameText("chart.post.dialog.empty"));
            showMain();
            return;
        }
        dialog().getVisualPanel().showLoot(Translation.gameText("chart.post.dialog.vaultTitle"), vault, false, true, true, new CoreInteractionListener() {
            @Override
            public void coreUIDismissed() {
                showMain();
            }
        });
    }

    void engage(CampaignFleetAPI guard) {
        if (guard == null) {
            showMain();
            return;
        }
        AnomalyEncounters.engage(dialog(), guard, new FleetInteractionDialogPluginImpl(config()));
    }

    FIDConfig config() {
        FIDConfig config = new FIDConfig();
        config.leaveAlwaysAvailable = true;
        config.dismissOnLeave = false;
        config.impactsEnemyReputation = false;
        config.impactsAllyReputation = false;
        config.showCommLinkOption = false;
        config.delegate = new BaseFIDDelegate() {
            @Override
            public void notifyLeave(InteractionDialogAPI leaving) {
                returnHere(leaving, post);
                showEntityVisual(post);
                showMain();
            }
        };
        return config;
    }
}
