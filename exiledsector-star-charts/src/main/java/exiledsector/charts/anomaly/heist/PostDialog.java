package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.atlas.AtlasProgress;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PostDialog implements InteractionDialogPlugin {

    static final String OPEN = "exiledSector_postOpen";
    static final String ENGAGE = "exiledSector_postEngage";
    static final String LEAVE = "exiledSector_postLeave";

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI dialog;
    private SectorEntityToken post;

    public static boolean wantsToHandle(SectorEntityToken target) {
        return ListeningPost.isPost(target);
    }

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        dialog = interactionDialog;
        post = interactionDialog.getInteractionTarget();
        if (post.getCustomInteractionDialogImageVisual() != null) {
            dialog.getVisualPanel().showImageVisual(post.getCustomInteractionDialogImageVisual());
        }
        VanillaText.plainPara(dialog.getTextPanel(), Translation.gameText("chart.post.dialog.description"));
        showMain();
    }

    private ChartRun run() {
        ChartRun run = ChartRuns.currentOrNull();
        return ListeningPost.inRunSystem(run, post.getContainingLocation()) ? run : null;
    }

    private PostScript script() {
        return PostScript.in(post.getContainingLocation());
    }

    void showMain() {
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        ChartRun run = run();
        PostState state = PostState.of(run);
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
                options.addOption(Translation.gameText("chart.post.dialog.option.engage"), ENGAGE);
            } else {
                options.addOption(Translation.gameText("chart.post.dialog.option.openPurged"), OPEN);
            }
        }
        options.addOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        ChartRun run = run();
        PostState state = PostState.of(run);
        PostScript script = script();
        boolean open = state != null && !state.taken() && (!state.alarm() || script == null || !script.defended());
        if (OPEN.equals(optionData) && open) {
            openVault(run, state);
        } else if (ENGAGE.equals(optionData) && state != null && state.alarm() && !state.taken() && script != null) {
            engage(script.gatherDefenders());
        } else {
            dialog.dismiss();
        }
    }

    void openVault(ChartRun run, PostState state) {
        PostState taken = state.asTaken();
        PostState.record(run, taken);
        TextPanelAPI text = dialog.getTextPanel();
        if (taken.clean() && AtlasProgress.revealLanesAround(Global.getSector(), run)) {
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.post.dialog.lanes").count(AtlasReach.LISTENING_POST_LANES).text()));
        }
        CargoAPI vault = VaultLoot.roll(Global.getSector(), run, state.vaultShare(), new Random(state.seed()));
        if (vault == null || vault.isEmpty()) {
            VanillaText.plainPara(text, Translation.gameText("chart.post.dialog.empty"));
            showMain();
            return;
        }
        dialog.getVisualPanel().showLoot(Translation.gameText("chart.post.dialog.vaultTitle"), vault, false, true, true, new CoreInteractionListener() {
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
        FleetInteractionDialogPluginImpl encounter = new FleetInteractionDialogPluginImpl(config());
        dialog.setInteractionTarget(guard);
        dialog.setPlugin(encounter);
        encounter.init(dialog);
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
                leaving.setPlugin(PostDialog.this);
                leaving.setInteractionTarget(post);
                if (post.getCustomInteractionDialogImageVisual() != null) {
                    leaving.getVisualPanel().showImageVisual(post.getCustomInteractionDialogImageVisual());
                }
                showMain();
            }
        };
        return config;
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }
}
