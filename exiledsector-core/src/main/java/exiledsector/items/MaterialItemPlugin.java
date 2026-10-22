package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.Translation;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconAnimation;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledlib.skills.skilleffect.SpriteCache;

public class MaterialItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(MaterialItemPlugin.class);
    private static final float TOOLTIP_PAD = 10f;

    private final CargoIconAnimation animation = new CargoIconAnimation();
    private CargoIconLook look;
    private String iconPath;

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        tooltip.addTitle(getName(), MaterialItems.color(itemId));
        String description = spec == null ? null : spec.getDesc();
        if (description != null && !description.isEmpty()) {
            VanillaText.plainPara(tooltip, description, TOOLTIP_PAD, Misc.getGrayColor());
        }
        if (alsoWorksOnStarCharts(itemId)) {
            VanillaText.plainPara(tooltip, Translation.text("chart.ui.currency.alsoStarCharts"), TOOLTIP_PAD, Misc.getGrayColor());
        }
        addCostLabel(tooltip, TOOLTIP_PAD, transferHandler, stackSource);
    }

    static boolean alsoWorksOnStarCharts(String itemId) {
        ItemCurrency currency = ItemCurrencies.byItemId(itemId);
        return currency != null && ItemCurrencies.appliesTo(currency, ItemType.STAR_CHART)
                && (ItemCurrencies.appliesTo(currency, ItemType.HULL_ITEM) || ItemCurrencies.appliesTo(currency, ItemType.SUBROUTINE));
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        if (look == null) {
            look = CargoIconLooks.forMaterial(itemId);
            iconPath = MaterialItems.iconPath(itemId);
        }
        CargoIconFx.render(SPRITES, iconPath, look, x, y, w, h, alphaMult, glowMult, animation.phaseSeed(), animation.burstAge(look, stack));
    }
}
