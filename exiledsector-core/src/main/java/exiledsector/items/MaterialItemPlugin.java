package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

import java.util.List;

public class MaterialItemPlugin extends AnimatedCargoItemPlugin {

    private static final float TOOLTIP_PAD = 10f;
    private static final float LINE_PAD = 3f;

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        tooltip.addTitle(getName(), MaterialItems.color(itemId));
        String description = spec == null ? null : spec.getDesc();
        if (description != null && !description.isEmpty()) {
            VanillaText.plainPara(tooltip, description, TOOLTIP_PAD, Misc.getGrayColor());
        }
        ItemCurrency currency = ItemCurrencies.byItemId(itemId);
        float linePad = TOOLTIP_PAD;
        for (StyledText line : currency == null ? List.<StyledText>of() : currency.grantedLines()) {
            VanillaText.addPara(tooltip, line, linePad, Misc.getTextColor());
            linePad = LINE_PAD;
        }
        if (alsoWorksOnStarCharts(itemId)) {
            VanillaText.plainPara(tooltip, Translation.text("chart.ui.currency.alsoStarCharts"), TOOLTIP_PAD, Misc.getGrayColor());
        }
        addCostLabel(tooltip, TOOLTIP_PAD, transferHandler, stackSource);
    }

    static boolean alsoWorksOnStarCharts(String itemId) {
        ItemCurrency currency = ItemCurrencies.byItemId(itemId);
        return currency != null && ItemCurrencies.appliesTo(currency, ItemType.STAR_CHART)
                && ItemUnlock.available(ItemType.STAR_CHART, Global.getSector())
                && (ItemCurrencies.appliesTo(currency, ItemType.HULL_ITEM) || ItemCurrencies.appliesTo(currency, ItemType.SUBROUTINE));
    }

    @Override
    protected CargoIconLook cargoLook() {
        return CargoIconLooks.forMaterial(itemId);
    }

    @Override
    protected String cargoIconPath() {
        return MaterialItems.iconPath(itemId);
    }
}
