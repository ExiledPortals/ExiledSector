package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.listeners.BaseIndustryOptionProvider;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.ChartSupply;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.WormholeGateway;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.ui.ModificationBenchCargoLauncher;
import exiledsector.ui.VanillaText;
import exiledsector.ui.socket.BenchMode;

import java.util.List;
import java.util.Random;

public class WormholeGatewayOption extends BaseIndustryOptionProvider {

    static final Object OPEN_STAR_CHARTS = new Object();
    static final Object BUY_STAR_CHART = new Object();

    @Override
    public List<IndustryOptionProvider.IndustryOptionData> getIndustryOptions(Industry industry) {
        if (!StarChartSettings.enabled() || isUnsuitable(industry, true) || !WormholeGateway.INDUSTRY_ID.equals(industry.getId())
                || !industry.getMarket().isPlayerOwned()) {
            return List.of();
        }
        IndustryOptionProvider.IndustryOptionData buy = new IndustryOptionProvider.IndustryOptionData(
                I18n.forGameText(() -> Translation.msg("chart.gateway.option.buy").arg("tier", ChartSupply.VENDOR_TIER)
                        .arg("credits", Misc.getDGSCredits(ChartSupply.VENDOR_PRICE)).text()), BUY_STAR_CHART, industry, this);
        buy.enabled = Global.getSector().getPlayerFleet() != null
                && Global.getSector().getPlayerFleet().getCargo().getCredits().get() >= ChartSupply.VENDOR_PRICE;
        return List.of(new IndustryOptionProvider.IndustryOptionData(Translation.gameText("chart.gateway.option.open"), OPEN_STAR_CHARTS, industry, this),
                buy);
    }

    @Override
    public void createTooltip(IndustryOptionProvider.IndustryOptionData option, TooltipMakerAPI tooltip, float width) {
        if (option.id == OPEN_STAR_CHARTS) {
            VanillaText.plainPara(tooltip, Translation.gameText("chart.gateway.option.tooltip"), 0f, Misc.getTextColor());
        } else if (option.id == BUY_STAR_CHART) {
            VanillaText.plainPara(tooltip, I18n.forGameText(() -> Translation.msg("chart.gateway.option.buy.tooltip").arg("tier", ChartSupply.VENDOR_TIER)
                    .arg("credits", Misc.getDGSCredits(ChartSupply.VENDOR_PRICE)).text()), 0f, Misc.getTextColor());
        }
    }

    @Override
    public void optionSelected(IndustryOptionProvider.IndustryOptionData option, DialogCreatorUI ui) {
        if (option.id == OPEN_STAR_CHARTS) {
            ModificationBenchCargoLauncher.openMode(Global.getSector().getCampaignUI(), BenchMode.STAR_CHARTS);
        } else if (option.id == BUY_STAR_CHART) {
            ChartSupply.buyFromGateway(Global.getSector(), new Random());
        }
    }
}
