package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.NavBuoyEntityPlugin;
import com.fs.starfarer.api.impl.campaign.SensorArrayEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyDialog;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.ScarText;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

import java.util.List;

public class RelayDialog extends AnomalyDialog {

    static final String RESTORE = "exiledSector_relayRestore";

    record ChoiceOption(int index) {
    }

    private SectorEntityToken relay;
    private RelayKind kind;

    @Override
    protected void open() {
        relay = dialog().getInteractionTarget();
        kind = RelayKind.of(relay);
        showEntityVisual(relay);
        ChartRun run = ChartRuns.liveOwning(relay);
        if (kind != null) {
            VanillaText.plainPara(text(), Translation.gameText("chart.anomaly.relay_network." + kind.key() + ".description"));
        }
        if (kind != null && run != null) {
            VanillaText.plainPara(text(), Translation.gameText(RelayText.trailKey(RelayStages.liveStage(Global.getSector(), run))));
        }
        showState();
    }

    private void showState() {
        TextPanelAPI text = text();
        OptionPanelAPI options = options();
        options.clearOptions();
        ChartRun run = ChartRuns.liveOwning(relay);
        if (run == null || kind == null) {
            VanillaText.plainPara(text, Translation.gameText("chart.anomaly.relay_network.stale"));
        } else if (RelayStates.isWoken(run, kind)) {
            AtlasAnomalyRecord.Chain stage = RelayStages.liveStage(Global.getSector(), run);
            VanillaText.plainPara(text, Translation.gameText("chart.anomaly.relay_network.awake"));
            VanillaText.plainPara(text, Translation.gameText(RelayText.logKey(kind, stage)));
            VanillaText.addPara(text, wokenEffect(kind), Misc.getTextColor());
            VanillaText.plainPara(text, RelayText.status(run, stage));
            showChoice(run, text, options);
        } else {
            showDormant(run, text, options);
        }
        addLeaveOption();
    }

    private void showDormant(ChartRun run, TextPanelAPI text, OptionPanelAPI options) {
        boolean synced = RelayStates.has(run, kind, RelayStates.SYNCED);
        VanillaText.plainPara(text, Translation.gameText(synced ? "chart.anomaly.relay_network.synced" : "chart.anomaly.relay_network.wake." + kind.key()));
        if (RelayStates.has(run, kind, RelayStates.PAID)) {
            VanillaText.plainPara(text, Translation.gameText("chart.anomaly.relay_network.powered"));
            return;
        }
        RelayRules.Cost cost = ChartRelays.costFor(run);
        Misc.showCost(text, Translation.gameText("chart.anomaly.relay_network.cost.title"), true, null, null,
                new String[] {Commodities.SUPPLIES, Commodities.HEAVY_MACHINERY}, new int[] {cost.supplies(), cost.machinery()});
        options.addOption(Translation.gameText("chart.anomaly.relay_network.option.restore"), RESTORE);
        options.setEnabled(RESTORE, ChartRelays.canAfford(PlayerFleet.cargoOrNull(), cost));
    }

    private void showChoice(ChartRun run, TextPanelAPI text, OptionPanelAPI options) {
        SectorAPI sector = Global.getSector();
        if (RelayChoices.picked(run, kind) != null) {
            showPicked(sector, run, text);
            return;
        }
        List<RelayChoice> offers = RelayChoices.offers(sector, run, kind);
        if (offers.isEmpty()) {
            return;
        }
        HullFacts bossHull = RelayChoices.bossHull(sector, run);
        VanillaText.plainPara(text, Translation.gameText(RelayText.PREFIX + "choice.intro"));
        VanillaText.plainPara(text, Translation.gameText(RelayText.PREFIX + "choice.rule"));
        for (int index = 0; index < offers.size(); index++) {
            RelayChoice offer = offers.get(index);
            VanillaText.addPara(text, RelayText.offerLine(offer), Misc.getTextColor());
            VanillaText.addPara(text, ScarText.effectsLine(offer.scarId(), bossHull), Misc.getTextColor());
            options.addOption(RelayText.optionText(offer), new ChoiceOption(index));
        }
    }

    private void showPicked(SectorAPI sector, ChartRun run, TextPanelAPI text) {
        RelayChoice picked = RelayChoices.picked(run, kind);
        if (picked != null) {
            VanillaText.addPara(text, RelayText.pickedLine(picked), Misc.getTextColor());
            VanillaText.addPara(text, ScarText.effectsLine(picked.scarId(), RelayChoices.bossHull(sector, run)), Misc.getTextColor());
        }
    }

    static StyledText wokenEffect(RelayKind kind) {
        return I18n.forGameText(() -> switch (kind) {
            case NAV_BUOY -> Translation.msg("chart.anomaly.relay_network.effect.nav_buoy").arg("value", Math.round(NavBuoyEntityPlugin.NAV_BONUS)).styled();
            case SENSOR_ARRAY -> Translation.msg("chart.anomaly.relay_network.effect.sensor_array").arg("value", Math.round(SensorArrayEntityPlugin.SENSOR_BONUS)).styled();
            case COMM_RELAY -> Translation.msg("chart.anomaly.relay_network.effect.comm_relay").styled();
        });
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        ChartRun run = ChartRuns.liveOwning(relay);
        if (RESTORE.equals(optionData) && run != null) {
            CargoAPI cargo = PlayerFleet.cargoOrNull();
            ChartRelays.restorePower(run, cargo, relay.getContainingLocation(), relay, kind, ChartRelays.costFor(run));
            showState();
        } else if (optionData instanceof ChoiceOption choice && run != null && RelayChoices.pick(Global.getSector(), run, kind, choice.index())) {
            RelayLogIntel.report(Global.getSector(), run);
            showPicked(Global.getSector(), run, text());
            options().clearOptions();
            addLeaveOption();
        } else {
            dialog().dismiss();
        }
    }
}
