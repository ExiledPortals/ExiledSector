package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.charts.anomaly.ChartRaid;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ConvoyCacheDialog implements InteractionDialogPlugin, ChartRaid.Host {

    static final String RAID = "exiledSector_convoyCacheRaid";
    static final String CONTINUE = "exiledSector_convoyCacheContinue";
    static final String LEAVE = "exiledSector_convoyCacheLeave";

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI dialog;
    private SectorEntityToken planet;
    private ConvoyScript script;
    private ChartRaid raid;
    private int raidsLaunched;

    static boolean wantsToHandle(SectorEntityToken target) {
        ConvoyScript script = target == null ? null : ConvoyScript.in(target.getContainingLocation());
        return script != null && !script.cachesAt(target).isEmpty();
    }

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        dialog = interactionDialog;
        planet = interactionDialog.getInteractionTarget();
        script = ConvoyScript.in(planet.getContainingLocation());
        showMain();
    }

    List<GroundRaidObjectivePlugin> objectives() {
        List<GroundRaidObjectivePlugin> objectives = new ArrayList<>();
        if (script == null) {
            return objectives;
        }
        for (ConvoyState convoy : script.cachesAt(planet)) {
            objectives.add(new ConvoyCacheObjective(planet.getMarket(), script, convoy.index(), ConvoyRules.cacheDanger(script.tier())));
        }
        return objectives;
    }

    private void showMain() {
        dialog.getVisualPanel().showPlanetInfo(planet);
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        List<ConvoyState> caches = script == null ? List.of() : script.cachesAt(planet);
        String planetName = planet.getName();
        if (caches.isEmpty()) {
            VanillaText.plainPara(dialog.getTextPanel(), I18n.forGameText(() -> Translation.msg("chart.convoy.cache.empty").arg("planet", planetName).text()));
        } else {
            int tier = script.tier();
            VanillaText.plainPara(dialog.getTextPanel(), I18n.forGameText(() -> Translation.msg("chart.convoy.cache.description")
                    .arg("planet", planetName).text()));
            VanillaText.addPara(dialog.getTextPanel(), I18n.forGameText(() -> Translation.msg("chart.convoy.intel.manifest")
                    .arg("cargo", ConvoyCargo.manifest(tier)).styled()), Misc.getTextColor());
            options.addOption(Translation.gameText("chart.convoy.cache.option.raid"), RAID);
        }
        options.addOption(Translation.gameText("chart.convoy.cache.option.continue"), CONTINUE);
        options.addOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (raid != null && ChartRaid.handles(optionData)) {
            raid.optionSelected(optionData);
        } else if (RAID.equals(optionData) && script != null) {
            long seed = planet.getId().hashCode() * 31L + script.runId().hashCode() + raidsLaunched++;
            raid = new ChartRaid(dialog, ConvoyRules.raidDifficulty(script.tier()), this::objectives, new Random(seed), this);
            raid.open();
        } else if (CONTINUE.equals(optionData)) {
            RuleBasedInteractionDialogPluginImpl planetDialog = new RuleBasedInteractionDialogPluginImpl();
            dialog.setPlugin(planetDialog);
            planetDialog.init(dialog);
        } else {
            dialog.dismiss();
        }
    }

    @Override
    public void raidLeft(InteractionDialogAPI raidDialog) {
        raid = null;
        showMain();
    }

    @Override
    public void raidFinished(InteractionDialogAPI raidDialog) {
        raid = null;
        showMain();
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
