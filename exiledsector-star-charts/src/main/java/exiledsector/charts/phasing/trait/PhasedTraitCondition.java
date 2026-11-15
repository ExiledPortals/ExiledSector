package exiledsector.charts.phasing.trait;

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.StarChartSettings;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.List;

public abstract class PhasedTraitCondition extends BaseMarketConditionPlugin {

    static final float TOOLTIP_PAD = 10f;

    protected abstract List<TraitEffect> effects();

    @Override
    public void apply(String id) {
        super.apply(id);
        boolean active = StarChartSettings.enabled();
        for (TraitEffect effect : effects()) {
            if (active) {
                effect.apply(market, id, condition.getName());
            } else {
                effect.unapply(market, id);
            }
        }
    }

    @Override
    public void unapply(String id) {
        super.unapply(id);
        for (TraitEffect effect : effects()) {
            effect.unapply(market, id);
        }
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);
        for (TraitEffect effect : effects()) {
            VanillaText.plainPara(tooltip, I18n.forGameText(effect::line), TOOLTIP_PAD,
                    effect.harmful() ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor());
        }
        if (!StarChartSettings.enabled()) {
            VanillaText.plainPara(tooltip, Translation.gameText("chart.phasing.trait.inactive"), TOOLTIP_PAD, Misc.getGrayColor());
        }
    }
}
