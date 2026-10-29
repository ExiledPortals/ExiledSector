package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;

public class AtlasCatalystItemPlugin extends AnimatedCargoItemPlugin {

    private static final float TOOLTIP_PAD = 10f;
    private static final float CATEGORY_PAD = 2f;

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        AtlasCatalysts.Catalyst catalyst = AtlasCatalysts.byItemId(itemId);
        tooltip.addTitle(getName(), AtlasCatalystItems.color(catalyst == null ? null : catalyst.family()));
        VanillaText.plainPara(tooltip, Translation.text("catalyst.category"), CATEGORY_PAD, Misc.getGrayColor());
        if (catalyst != null) {
            VanillaText.plainPara(tooltip, AtlasCatalystItems.addsLine(catalyst), TOOLTIP_PAD, Misc.getTextColor());
        }
        String flavour = spec == null ? null : spec.getDesc();
        if (flavour != null && !flavour.isEmpty()) {
            VanillaText.plainPara(tooltip, flavour, TOOLTIP_PAD, Misc.getGrayColor());
        }
        addCostLabel(tooltip, TOOLTIP_PAD, transferHandler, stackSource);
    }

    @Override
    protected CargoIconLook cargoLook() {
        AtlasCatalysts.Catalyst catalyst = AtlasCatalysts.byItemId(itemId);
        return AtlasCatalystItems.look(catalyst == null ? null : catalyst.family());
    }

    @Override
    protected String cargoIconPath() {
        AtlasCatalysts.Catalyst catalyst = AtlasCatalysts.byItemId(itemId);
        return catalyst == null || catalyst.iconPath().isEmpty() ? null : catalyst.iconPath();
    }
}
