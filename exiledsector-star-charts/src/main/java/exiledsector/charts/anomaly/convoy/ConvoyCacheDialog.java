package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyDialog;
import exiledsector.charts.anomaly.ChartRaid;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;

public class ConvoyCacheDialog extends AnomalyDialog {

    static final String RAID_OPTION = "exiledSector_convoyCacheRaid";
    static final String CONTINUE = "exiledSector_convoyCacheContinue";

    private final ChartRaid.Session raid = new ChartRaid.Session(closedDialog -> showMain());
    private SectorEntityToken planet;
    private ConvoyScript script;

    static boolean wantsToHandle(SectorEntityToken target) {
        ConvoyScript script = target == null ? null : ConvoyScript.in(target.getContainingLocation());
        return script != null && !script.cachesAt(target).isEmpty();
    }

    @Override
    protected void open() {
        planet = dialog().getInteractionTarget();
        script = ConvoyScript.in(planet.getContainingLocation());
        showMain();
    }

    List<GroundRaidObjectivePlugin> objectives() {
        List<GroundRaidObjectivePlugin> objectives = new ArrayList<>();
        if (script == null) {
            return objectives;
        }
        for (ConvoyState convoy : script.cachesAt(planet)) {
            objectives.add(new ConvoyCacheObjective(planet.getMarket(), script, convoy.index(), ChartRaid.danger(script.tier())));
        }
        return objectives;
    }

    private void showMain() {
        dialog().getVisualPanel().showPlanetInfo(planet);
        OptionPanelAPI options = options();
        options.clearOptions();
        List<ConvoyState> caches = script == null ? List.of() : script.cachesAt(planet);
        String planetName = planet.getName();
        if (caches.isEmpty()) {
            VanillaText.plainPara(text(), I18n.forGameText(() -> Translation.msg("chart.convoy.cache.empty").arg("planet", planetName).text()));
        } else {
            int tier = script.tier();
            VanillaText.plainPara(text(), I18n.forGameText(() -> Translation.msg("chart.convoy.cache.description")
                    .arg("planet", planetName).text()));
            VanillaText.addPara(text(), I18n.forGameText(() -> Translation.msg("chart.convoy.intel.manifest")
                    .arg("cargo", ConvoyCargo.manifest(tier)).styled()), Misc.getTextColor());
            options.addOption(Translation.gameText("chart.convoy.cache.option.raid"), RAID_OPTION);
        }
        options.addOption(Translation.gameText("chart.convoy.cache.option.continue"), CONTINUE);
        addLeaveOption();
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (raid.handles(optionData)) {
            raid.optionSelected(optionData);
        } else if (RAID_OPTION.equals(optionData) && script != null) {
            raid.launch(dialog(), script.tier(), planet.getId().hashCode() * 31L + script.runId().hashCode(), this::objectives);
        } else if (CONTINUE.equals(optionData)) {
            continueAfter(SupplyConvoy.INSTANCE, planet);
        } else {
            dialog().dismiss();
        }
    }
}
