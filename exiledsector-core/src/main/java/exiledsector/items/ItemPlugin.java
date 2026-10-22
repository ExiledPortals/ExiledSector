package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledlib.i18n.I18n;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.items.Item;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemTooltip;
import exiledlib.items.ItemTypeSupport;
import exiledlib.items.ItemTypes;
import exiledsector.ui.socket.ItemCodexLink;
import exiledlib.ui.util.SpriteCache;
import exiledsector.ui.cargo.CargoIconAnimation;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

import java.util.List;
import java.util.function.Supplier;

public class ItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(ItemPlugin.class);

    private final CargoIconAnimation animation = new CargoIconAnimation();
    private Item previewItem;
    private CargoIconLook look;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        ItemData itemData = stack == null ? null : ItemData.of(stack.getSpecialDataIfSpecial());
        previewItem = itemData == null ? null : itemData.preview();
    }

    @Override
    public String getName() {
        return I18n.forGameText(() -> previewItem == null ? Translation.text("socketable.unknown") : previewItem.name());
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
        Supplier<List<StyledText>> footer = () -> showCodexHint
                ? List.of(Translation.styled("socketable.tooltip.storage"), Translation.styled("socketable.tooltip.codexHint"))
                : List.of(Translation.styled("socketable.tooltip.storage"));
        ItemTooltip.write(tooltip, previewItem, footer, expanded);
        addCostLabel(tooltip, ItemTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        if (look == null) {
            look = CargoIconLooks.forItem(previewItem);
        }
        String iconPath = previewItem == null ? ItemDefinition.FALLBACK_ICON : previewItem.iconPath();
        CargoIconFx.render(SPRITES, iconPath, look, x, y, w, h, alphaMult, glowMult, animation.phaseSeed(), animation.burstAge(look, stack));
    }
}
