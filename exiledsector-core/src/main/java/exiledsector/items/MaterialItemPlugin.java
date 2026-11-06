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
    public void setId(String id) {
        super.setId(MaterialIdAliases.currentId(id));
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        tooltip.addTitle(getName(), MaterialItems.color(itemId));
        String description = spec == null ? null : spec.getDesc();
        if (description != null && !description.isEmpty()) {
            VanillaText.plainPara(tooltip, description, TOOLTIP_PAD, Misc.getTextColor());
        }
        CraftingMaterialFamily craftingItem = CraftingMaterialFamilies.byItemId(itemId);
        float linePad = TOOLTIP_PAD;
        for (StyledText line : craftingItem == null ? List.<StyledText>of() : craftingItem.grantedLines()) {
            VanillaText.addPara(tooltip, line, linePad, Misc.getTextColor());
            linePad = LINE_PAD;
        }
        if (alsoWorksOnStarCharts(itemId)) {
            VanillaText.plainPara(tooltip, Translation.text("chart.ui.craftingItem.alsoStarCharts"), TOOLTIP_PAD, Misc.getGrayColor());
        }
        addCostLabel(tooltip, TOOLTIP_PAD, transferHandler, stackSource);
    }

    static boolean alsoWorksOnStarCharts(String itemId) {
        CraftingMaterialFamily craftingItem = CraftingMaterialFamilies.byItemId(itemId);
        return craftingItem != null && CraftingMaterialFamilies.appliesTo(craftingItem, ItemType.STAR_CHART)
                && ItemUnlock.available(ItemType.STAR_CHART, Global.getSector())
                && craftingItem.appliesToShipItems();
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
