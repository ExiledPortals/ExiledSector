package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.listeners.BaseIndustryOptionProvider;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;
import exiledsector.ui.atlas.AtlasScreen;

import java.util.List;

public class WormholeGatewayOption extends BaseIndustryOptionProvider {

    static final Object OPEN_ATLAS = new Object();

    @Override
    public List<IndustryOptionProvider.IndustryOptionData> getIndustryOptions(Industry industry) {
        if (!StarChartSettings.enabled() || isUnsuitable(industry, true) || !WormholeGateway.isGateway(industry.getId())
                || !industry.getMarket().isPlayerOwned()) {
            return List.of();
        }
        return List.of(new IndustryOptionProvider.IndustryOptionData(Translation.gameText("chart.gateway.option.open"), OPEN_ATLAS, industry, this));
    }

    @Override
    public void createTooltip(IndustryOptionProvider.IndustryOptionData option, TooltipMakerAPI tooltip, float width) {
        if (option.id == OPEN_ATLAS) {
            VanillaText.plainPara(tooltip, Translation.gameText("chart.gateway.option.tooltip"), 0f, Misc.getTextColor());
        }
    }

    @Override
    public void optionSelected(IndustryOptionProvider.IndustryOptionData option, DialogCreatorUI ui) {
        if (option.id == OPEN_ATLAS) {
            AtlasScreen.open(Global.getSector().getCampaignUI());
        }
    }
}
