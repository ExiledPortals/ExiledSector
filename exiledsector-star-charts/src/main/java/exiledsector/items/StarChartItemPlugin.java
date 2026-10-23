package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledlib.i18n.I18n;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.charts.StarChartSettings;
import exiledsector.ui.cargo.CargoIconAnimation;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

import java.util.List;

public class StarChartItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(StarChartItemPlugin.class);

    private final CargoIconAnimation animation = new CargoIconAnimation();
    private Item previewChart;
    private CargoIconLook look;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        ItemData chartData = stack == null ? null : ItemData.ofStarChart(stack.getSpecialDataIfSpecial());
        previewChart = chartData == null ? null : chartData.preview();
    }

    @Override
    public String getName() {
        return I18n.forGameText(() -> previewChart == null ? Translation.text("chart.item.unknown") : previewChart.name());
    }

    @Override
    public float getTooltipWidth() {
        return ItemTooltip.WIDTH;
    }

    @Override
    public boolean isTooltipExpandable() {
        return true;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        ItemTooltip.write(tooltip, previewChart, this::footerLines, expanded);
        addCostLabel(tooltip, ItemTooltip.PAD, transferHandler, stackSource);
    }

    private List<StyledText> footerLines() {
        if (!StarChartSettings.enabled()) {
            return List.of(Translation.styled("chart.item.storage"), Translation.styled("chart.item.disabled"));
        }
        return List.of(Translation.styled("chart.item.storage"));
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        if (look == null) {
            look = CargoIconLooks.forItem(previewChart);
        }
        String iconPath = previewChart == null ? ItemDefinition.FALLBACK_ICON : previewChart.iconPath();
        CargoIconFx.render(SPRITES, iconPath, look, x, y, w, h, alphaMult, glowMult, animation.phaseSeed(), animation.burstAge(look, stack));
    }
}
