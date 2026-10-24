package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledlib.i18n.I18n;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledsector.ui.socket.ItemCodexLink;

import java.util.List;

public class ItemPlugin extends AnimatedCargoItemPlugin {

    private Item previewItem;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        ItemData itemData = stack == null ? null : decode(stack.getSpecialDataIfSpecial());
        previewItem = itemData == null ? null : itemData.preview();
    }

    protected ItemData decode(SpecialItemData special) {
        return ItemData.of(special);
    }

    protected String unknownNameKey() {
        return "socketable.unknown";
    }

    protected List<StyledText> footer(boolean showCodexHint) {
        return showCodexHint ? List.of(Translation.styled("socketable.tooltip.storage"), Translation.styled("socketable.tooltip.codexHint"))
                : List.of(Translation.styled("socketable.tooltip.storage"));
    }

    @Override
    public String getName() {
        return I18n.forGameText(() -> previewItem == null ? Translation.text(unknownNameKey()) : previewItem.name());
    }

    @Override
    public int getPrice(MarketAPI market, SubmarketAPI submarket) {
        ItemTypeSupport support = previewItem == null ? null : ItemTypes.of(previewItem.type());
        boolean forSale = stack != null && ItemData.isMarketListing(stack.getSpecialDataIfSpecial());
        Integer price = support == null ? null : support.cargoPrice(previewItem, forSale);
        return price == null ? super.getPrice(market, submarket) : price;
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
        String codexEntryId = previewItem == null ? null : ItemCodexLink.existingEntryId(previewItem.definitionId());
        if (codexEntryId != null) {
            tooltip.setCodexEntryId(codexEntryId);
        }
        boolean showCodexHint = codexEntryId != null && expanded;
        ItemTooltip.write(tooltip, previewItem, () -> footer(showCodexHint), expanded);
        addCostLabel(tooltip, ItemTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    protected CargoIconLook cargoLook() {
        return CargoIconLooks.forItem(previewItem);
    }

    @Override
    protected String cargoIconPath() {
        return previewItem == null ? ItemDefinition.FALLBACK_ICON : previewItem.iconPath();
    }
}
