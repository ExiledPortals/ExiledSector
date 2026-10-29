package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.NavBuoyEntityPlugin;
import com.fs.starfarer.api.impl.campaign.SensorArrayEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.run.ChartRun;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public class RelayDialog implements InteractionDialogPlugin {

    static final String RESTORE = "exiledSector_relayRestore";
    static final String LEAVE = "exiledSector_relayLeave";

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI interactionDialog;
    private SectorEntityToken relay;
    private RelayKind kind;

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        this.interactionDialog = interactionDialog;
        this.relay = interactionDialog.getInteractionTarget();
        this.kind = RelayKind.of(relay);
        if (relay != null && relay.getCustomInteractionDialogImageVisual() != null) {
            interactionDialog.getVisualPanel().showImageVisual(relay.getCustomInteractionDialogImageVisual());
        }
        VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText("chart.relay." + kind.key() + ".description"));
        showState();
    }

    private void showState() {
        TextPanelAPI text = interactionDialog.getTextPanel();
        OptionPanelAPI options = interactionDialog.getOptionPanel();
        options.clearOptions();
        ChartRun run = ChartRelays.liveRunFor(relay);
        if (run == null) {
            VanillaText.plainPara(text, Translation.gameText("chart.relay.stale"));
        } else if (RelayStates.isWoken(run, kind)) {
            VanillaText.plainPara(text, Translation.gameText("chart.relay.awake"));
            VanillaText.addPara(text, wokenEffect(kind), Misc.getTextColor());
        } else {
            showDormant(run, text, options);
        }
        options.addOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void showDormant(ChartRun run, TextPanelAPI text, OptionPanelAPI options) {
        boolean synced = RelayStates.has(run, kind, RelayStates.SYNCED);
        VanillaText.plainPara(text, Translation.gameText(synced ? "chart.relay.synced" : "chart.relay.wake." + kind.key()));
        if (RelayStates.has(run, kind, RelayStates.PAID)) {
            VanillaText.plainPara(text, Translation.gameText("chart.relay.powered"));
            return;
        }
        RelayRules.Cost cost = ChartRelays.costFor(run);
        Misc.showCost(text, Translation.gameText("chart.relay.cost.title"), true, null, null,
                new String[] {Commodities.SUPPLIES, Commodities.HEAVY_MACHINERY}, new int[] {cost.supplies(), cost.machinery()});
        options.addOption(Translation.gameText("chart.relay.option.restore"), RESTORE);
        options.setEnabled(RESTORE, ChartRelays.canAfford(cargo(), cost));
    }

    static StyledText wokenEffect(RelayKind kind) {
        return switch (kind) {
            case NAV_BUOY -> Translation.msg("chart.relay.effect.nav_buoy").arg("value", Math.round(NavBuoyEntityPlugin.NAV_BONUS)).styled();
            case SENSOR_ARRAY -> Translation.msg("chart.relay.effect.sensor_array").arg("value", Math.round(SensorArrayEntityPlugin.SENSOR_BONUS)).styled();
            case COMM_RELAY -> Translation.msg("chart.relay.effect.comm_relay").styled();
        };
    }

    private static CargoAPI cargo() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        return playerFleet == null ? null : playerFleet.getCargo();
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        ChartRun run = ChartRelays.liveRunFor(relay);
        if (RESTORE.equals(optionData) && run != null) {
            ChartRelays.restorePower(run, cargo(), relay.getContainingLocation(), relay, kind, ChartRelays.costFor(run));
            showState();
        } else {
            interactionDialog.dismiss();
        }
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
