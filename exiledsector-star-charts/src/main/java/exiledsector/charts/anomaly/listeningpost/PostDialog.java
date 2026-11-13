package exiledsector.charts.anomaly.listeningpost;

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
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyDialog;
import exiledsector.charts.anomaly.AnomalyEncounters;
import exiledsector.charts.atlas.AtlasProgress;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.i18n.StyledText;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.PrototypeImplicits;
import exiledsector.items.RolledModifier;
import exiledsector.loot.LootSources;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.Random;

public class PostDialog extends AnomalyDialog {

    static final String INFILTRATE = "exiledSector_postInfiltrate";
    static final String OPEN = "exiledSector_postOpen";
    static final String ENGAGE_OPTION = "exiledSector_postEngage";

    private static final String[] INFILTRATION_KEYS = {
            "chart.anomaly.listening_post.dialog.infiltrate.approach",
            "chart.anomaly.listening_post.dialog.infiltrate.entry",
            "chart.anomaly.listening_post.dialog.infiltrate.resistance",
            "chart.anomaly.listening_post.dialog.infiltrate.silence",
            "chart.anomaly.listening_post.dialog.infiltrate.vault"
    };

    private SectorEntityToken post;

    record PrototypeChoice(int index) {
    }

    @Override
    protected void open() {
        post = dialog().getInteractionTarget();
        showEntityVisual(post);
        String faction = PostText.factionPrefix(post.getFaction());
        VanillaText.plainPara(text(), I18n.forGameText(() -> Translation.msg("chart.anomaly.listening_post.dialog.description").arg("faction", faction).text()));
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
            VanillaText.plainPara(text, Translation.gameText("chart.anomaly.listening_post.dialog.stale"));
        } else if (state.taken()) {
            VanillaText.plainPara(text, Translation.gameText("chart.anomaly.listening_post.dialog.taken"));
        } else if (!state.alarm()) {
            VanillaText.plainPara(text, Translation.gameText("chart.anomaly.listening_post.dialog.quiet"));
            options.addOption(Translation.gameText("chart.anomaly.listening_post.dialog.option.infiltrate"), INFILTRATE);
        } else {
            int vault = PostText.percent(state.vaultShare());
            boolean defended = script != null && script.defended();
            String key = defended ? "chart.anomaly.listening_post.dialog.guarded" : "chart.anomaly.listening_post.dialog.purged";
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg(key).arg("value", vault).text()));
            if (defended) {
                options.addOption(Translation.gameText("chart.anomaly.listening_post.dialog.option.engage"), ENGAGE_OPTION);
            } else {
                options.addOption(Translation.gameText("chart.anomaly.listening_post.dialog.option.openPurged"), OPEN);
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
        if (INFILTRATE.equals(optionData) && state != null && !state.taken() && !state.alarm()) {
            infiltrate();
        } else if (OPEN.equals(optionData) && open) {
            openVault(run, state);
        } else if (optionData instanceof PrototypeChoice choice && state != null && state.quiet()) {
            List<ItemData> offered = VaultPrototypes.offered(Global.getSector(), state.seed());
            takeVault(run, state, choice.index() < offered.size() ? offered.get(choice.index()) : null);
        } else if (ENGAGE_OPTION.equals(optionData) && state != null && state.alarm() && !state.taken() && script != null) {
            engage(script.gatherDefenders());
        } else {
            dialog().dismiss();
        }
    }

    void infiltrate() {
        TextPanelAPI text = text();
        for (String key : INFILTRATION_KEYS) {
            VanillaText.plainPara(text, Translation.gameText(key));
        }
        OptionPanelAPI options = options();
        options.clearOptions();
        options.addOption(Translation.gameText("chart.anomaly.listening_post.dialog.option.open"), OPEN);
        addLeaveOption();
    }

    void openVault(ChartRun run, PostState state) {
        if (state.quiet()) {
            List<ItemData> offered = VaultPrototypes.offered(Global.getSector(), state.seed());
            if (!offered.isEmpty()) {
                showPrototypes(offered);
                return;
            }
        }
        takeVault(run, state, state.alarm() ? VaultPrototypes.leftAfterAlarm(Global.getSector(), state.seed()) : null);
    }

    void showPrototypes(List<ItemData> offered) {
        TextPanelAPI text = text();
        OptionPanelAPI options = options();
        options.clearOptions();
        VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.anomaly.listening_post.dialog.prototypes").count(offered.size()).text()));
        for (int index = 0; index < offered.size(); index++) {
            ItemData prototype = offered.get(index);
            String baseName = baseName(prototype);
            VanillaText.addPara(text, I18n.forGameText(() -> Translation.msg("chart.anomaly.listening_post.dialog.prototype").arg("item", baseName).styled()),
                    Misc.getTextColor());
            Item preview = prototype.preview();
            RolledModifier prototypeImplicit = preview == null ? null : PrototypeImplicits.prototypeImplicit(preview);
            if (prototypeImplicit != null) {
                for (StyledText line : preview.implicitLines(prototypeImplicit)) {
                    VanillaText.addPara(text, line, Misc.getTextColor());
                }
            }
            options.addOption(I18n.forGameText(() -> Translation.msg("chart.anomaly.listening_post.dialog.option.prototype").arg("item", baseName).text()),
                    new PrototypeChoice(index));
        }
    }

    private static String baseName(ItemData prototype) {
        ItemDefinition definition = prototype.definition();
        return definition == null ? prototype.definitionId() : definition.displayName();
    }

    void takeVault(ChartRun run, PostState state, ItemData prototype) {
        PostState taken = state.asTaken();
        PostState.record(run, taken);
        TextPanelAPI text = text();
        if (taken.clean() && AtlasProgress.revealLanesAround(Global.getSector(), run)) {
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.anomaly.listening_post.dialog.lanes").count(AtlasReach.LISTENING_POST_LANES).text()));
        }
        CargoAPI vault = VaultLoot.roll(Global.getSector(), run, state.vaultShare(), new Random(state.seed()));
        if (prototype != null) {
            if (vault == null) {
                vault = Global.getFactory().createCargo(true);
            }
            vault.addSpecial(LootSources.shapeDrops(List.of(prototype)).get(0).toSpecialItem(), 1f);
        }
        if (vault == null || vault.isEmpty()) {
            VanillaText.plainPara(text, Translation.gameText("chart.anomaly.listening_post.dialog.empty"));
            showMain();
            return;
        }
        dialog().getVisualPanel().showLoot(Translation.gameText("chart.anomaly.listening_post.dialog.vaultTitle"), vault, false, true, true, new CoreInteractionListener() {
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
