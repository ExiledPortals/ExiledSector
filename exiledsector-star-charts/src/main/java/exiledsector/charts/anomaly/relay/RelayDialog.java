package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
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
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

public class RelayDialog extends AnomalyDialog {

    static final String RESTORE = "exiledSector_relayRestore";

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
        } else {
            dialog().dismiss();
        }
    }
}
