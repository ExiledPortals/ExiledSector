package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.listeners.BaseIndustryOptionProvider;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.WormholeGateway;
import exiledlib.i18n.Translation;
import exiledsector.ui.ModificationBenchCargoLauncher;
import exiledsector.ui.VanillaText;

import java.util.List;

public class WormholeGatewayOption extends BaseIndustryOptionProvider {

    static final Object OPEN_STAR_CHARTS = new Object();

    @Override
    public List<IndustryOptionProvider.IndustryOptionData> getIndustryOptions(Industry industry) {
        if (!StarChartSettings.enabled() || isUnsuitable(industry, true) || !WormholeGateway.INDUSTRY_ID.equals(industry.getId())
                || !industry.getMarket().isPlayerOwned()) {
            return List.of();
        }
        return List.of(new IndustryOptionProvider.IndustryOptionData(Translation.gameText("chart.gateway.option.open"), OPEN_STAR_CHARTS, industry, this));
    }

    @Override
    public void createTooltip(IndustryOptionProvider.IndustryOptionData option, TooltipMakerAPI tooltip, float width) {
        if (option.id == OPEN_STAR_CHARTS) {
            VanillaText.plainPara(tooltip, Translation.gameText("chart.gateway.option.tooltip"), 0f, Misc.getTextColor());
        }
    }

    @Override
    public void optionSelected(IndustryOptionProvider.IndustryOptionData option, DialogCreatorUI ui) {
        if (option.id == OPEN_STAR_CHARTS) {
            ModificationBenchCargoLauncher.openStarCharts(Global.getSector().getCampaignUI());
        }
    }
}
