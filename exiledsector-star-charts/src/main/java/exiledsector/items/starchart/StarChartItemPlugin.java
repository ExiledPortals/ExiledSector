package exiledsector.items.starchart;

import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.StarChartSettings;
import exiledsector.items.ItemData;
import exiledsector.items.ItemPlugin;

import java.util.List;

public class StarChartItemPlugin extends ItemPlugin {

    @Override
    protected ItemData decode(SpecialItemData special) {
        return ItemData.ofStarChart(special);
    }

    @Override
    protected String unknownNameKey() {
        return "chart.item.unknown";
    }

    @Override
    protected List<StyledText> footer(boolean showCodexHint) {
        if (!StarChartSettings.enabled()) {
            return List.of(Translation.styled("chart.item.storage"), Translation.styled("chart.item.disabled"));
        }
        return List.of(Translation.styled("chart.item.storage"));
    }
}
