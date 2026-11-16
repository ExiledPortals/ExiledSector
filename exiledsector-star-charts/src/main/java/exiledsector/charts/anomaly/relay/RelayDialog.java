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
    private RelaySlot slot;

    @Override
    protected void open() {
        relay = dialog().getInteractionTarget();
        slot = RelaySlot.of(relay);
        showEntityVisual(relay);
        ChartRun run = ChartRuns.liveOwning(relay);
        if (slot != null) {
            VanillaText.plainPara(text(), Translation.gameText("chart.anomaly.relay_network." + slot.kind().key() + ".description"));
        }
        if (slot != null && run != null) {
            VanillaText.plainPara(text(), Translation.gameText(RelayText.trailKey(RelayStages.liveStage(Global.getSector(), run))));
        }
        showState();
    }

    private void showState() {
        TextPanelAPI text = text();
        OptionPanelAPI options = options();
        options.clearOptions();
        ChartRun run = ChartRuns.liveOwning(relay);
        if (run == null || slot == null) {
            VanillaText.plainPara(text, Translation.gameText("chart.anomaly.relay_network.stale"));
        } else if (RelayStates.isWoken(run, slot)) {
            AtlasAnomalyRecord.Chain stage = RelayStages.liveStage(Global.getSector(), run);
            VanillaText.plainPara(text, Translation.gameText("chart.anomaly.relay_network.awake"));
            VanillaText.plainPara(text, Translation.gameText(RelayText.logKey(slot.kind(), stage)));
            StyledText effect = wokenEffect(slot.kind());
            if (effect != null) {
                VanillaText.addPara(text, effect, Misc.getTextColor());
            }
            if (ChartRelays.fullArrayRevealing(run)) {
                VanillaText.addPara(text, I18n.forGameText(() -> Translation.styled(RelayText.PREFIX + "effect.full_array")), Misc.getTextColor());
            }
            VanillaText.plainPara(text, RelayText.status(run, stage));
            showChoice(run, text, options);
        } else {
            showDormant(run, text, options);
        }
        addLeaveOption();
    }

    private void showDormant(ChartRun run, TextPanelAPI text, OptionPanelAPI options) {
        boolean synced = RelayStates.has(run, slot, RelayStates.SYNCED);
        VanillaText.plainPara(text, Translation.gameText(synced ? "chart.anomaly.relay_network.synced" : "chart.anomaly.relay_network.wake." + slot.kind().key()));
        if (RelayStates.has(run, slot, RelayStates.PAID)) {
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
        if (RelayChoices.picked(run, slot) != null) {
            showPicked(sector, run, text);
            return;
        }
        List<RelayChoice> offers = RelayChoices.offers(sector, run, slot);
        if (offers.isEmpty()) {
            return;
        }
        HullFacts bossHull = RelayChoices.bossHull(sector, run);
        VanillaText.plainPara(text, RelayText.choiceIntro(offers.size()));
        VanillaText.plainPara(text, Translation.gameText(RelayText.PREFIX + "choice.rule"));
        for (int index = 0; index < offers.size(); index++) {
            RelayChoice offer = offers.get(index);
            VanillaText.addPara(text, RelayText.offerLine(offer), Misc.getTextColor());
            showScars(text, offer, bossHull);
            options.addOption(RelayText.optionText(offer), new ChoiceOption(index));
        }
    }

    private void showPicked(SectorAPI sector, ChartRun run, TextPanelAPI text) {
        RelayChoice picked = RelayChoices.picked(run, slot);
        if (picked != null) {
            VanillaText.addPara(text, RelayText.pickedLine(picked), Misc.getTextColor());
            showScars(text, picked, RelayChoices.bossHull(sector, run));
        }
    }

    private static void showScars(TextPanelAPI text, RelayChoice choice, HullFacts bossHull) {
        for (String scarId : choice.scarIds()) {
            VanillaText.addPara(text, ScarText.effectsLine(scarId, bossHull), Misc.getTextColor());
        }
    }

    static StyledText wokenEffect(RelayKind kind) {
        return I18n.forGameText(() -> switch (kind) {
            case NAV_BUOY -> Translation.msg("chart.anomaly.relay_network.effect.nav_buoy").arg("value", Math.round(NavBuoyEntityPlugin.NAV_BONUS)).styled();
            case SENSOR_ARRAY -> Translation.msg("chart.anomaly.relay_network.effect.sensor_array").arg("value", Math.round(SensorArrayEntityPlugin.SENSOR_BONUS)).styled();
            case COMM_RELAY -> null;
        });
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        ChartRun run = ChartRuns.liveOwning(relay);
        if (RESTORE.equals(optionData) && run != null) {
            CargoAPI cargo = PlayerFleet.cargoOrNull();
            ChartRelays.restorePower(run, cargo, relay.getContainingLocation(), relay, slot, ChartRelays.costFor(run));
            showState();
        } else if (optionData instanceof ChoiceOption choice && run != null && RelayChoices.pick(Global.getSector(), run, slot, choice.index())) {
            RelayLogIntel.report(Global.getSector(), run);
            showPicked(Global.getSector(), run, text());
            options().clearOptions();
            addLeaveOption();
        } else {
            dialog().dismiss();
        }
    }
}
