package exiledsector.charts.expansion;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import exiledlib.i18n.Translation;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRunEffects;
import exiledsector.charts.ChartRuns;
import exiledsector.charts.ChartSites;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BreachCoreDialog implements InteractionDialogPlugin {

    static final String CLOSE = "exiledSector_breachClose";
    static final String LEAVE = "exiledSector_breachLeave";
    static final long SALVAGE_SALT = 0x42524352454c56L;
    private static final Logger LOG = Logger.getLogger(BreachCoreDialog.class);

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI interactionDialog;
    private SectorEntityToken core;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.interactionDialog = dialog;
        this.core = dialog.getInteractionTarget();
        BreachTerrainPlugin breach = core == null ? null : SpatialBreach.breachIn(core.getContainingLocation());
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        if (breach == null) {
            dialog.getTextPanel().addPara(Translation.gameText("chart.breach.core.inert"));
        } else {
            dialog.getTextPanel().addPara(Translation.gameText("chart.breach.core.description"));
            options.addOption(Translation.gameText("chart.breach.core.option.close"), CLOSE);
            options.addOptionConfirmation(CLOSE, Translation.gameText("chart.breach.core.confirm"),
                    Translation.gameText("chart.breach.core.confirm.yes"), Translation.gameText("chart.breach.core.confirm.no"));
        }
        options.addOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        BreachTerrainPlugin breach = core == null ? null : SpatialBreach.breachIn(core.getContainingLocation());
        if (!CLOSE.equals(optionData) || breach == null) {
            interactionDialog.dismiss();
            return;
        }
        CargoAPI salvage;
        try {
            salvage = closeBreach(breach);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Closing a Spatial Breach failed", e);
            interactionDialog.dismiss();
            removeCore();
            return;
        }
        interactionDialog.getOptionPanel().clearOptions();
        interactionDialog.getTextPanel().addPara(Translation.gameText("chart.breach.core.closed"));
        interactionDialog.getVisualPanel().showLoot(Translation.gameText("chart.breach.core.salvage"), salvage, false, true, true, () -> {
            interactionDialog.dismiss();
            removeCore();
        });
    }

    private CargoAPI closeBreach(BreachTerrainPlugin breach) {
        float grownFraction = breach.close();
        float itemMult = BreachRules.coreItemMult(grownFraction, breach.coreItemMult());
        MemoryAPI memory = core.getMemoryWithoutUpdate();
        memory.set(SpatialBreach.ITEM_MULT_KEY, itemMult);
        memory.set(SpatialBreach.RARITY_KEY, BreachRules.coreRarityPercent(grownFraction));
        Random random = new Random(core.getId().hashCode() ^ SALVAGE_SALT);
        return ChartSites.siteSalvage(core, breach.tier(), runContext(breach.runId()), BreachRules.coreSalvageMult(breach.tier(), itemMult), random);
    }

    private static ChartGenContext runContext(String runId) {
        ChartRun run = ChartRuns.currentOrNull();
        return run != null && run.runId().equals(runId) ? ChartRunEffects.contextOf(run) : ChartGenContext.none();
    }

    private void removeCore() {
        LocationAPI location = core == null ? null : core.getContainingLocation();
        if (location != null) {
            location.removeEntity(core);
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
