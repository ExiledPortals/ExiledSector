package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.ui.inspect.ItemCodex;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.util.List;
import java.util.function.Supplier;

public class ItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(ItemPlugin.class);

    private Item previewItem;

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
    public float getTooltipWidth() {
        return ItemTooltip.WIDTH;
    }

    @Override
    public boolean isTooltipExpandable() {
        return true;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        String codexEntryId = previewItem == null ? null : ItemCodex.existingEntryId(previewItem.definitionId());
        if (codexEntryId != null) {
            tooltip.setCodexEntryId(codexEntryId);
        }
        boolean codexLinked = codexEntryId != null;
        Supplier<List<StyledText>> footer = () -> codexLinked
                ? List.of(Translation.styled("socketable.tooltip.storage"), Translation.styled("socketable.tooltip.codexHint"))
                : List.of(Translation.styled("socketable.tooltip.storage"));
        ItemTooltip.write(tooltip, previewItem, footer, expanded);
        addCostLabel(tooltip, ItemTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        String iconPath = previewItem == null ? ItemDefinition.FALLBACK_ICON : previewItem.iconPath();
        SpriteDraw.drawAtCenter(SPRITES, iconPath, x + w / 2f, y + h / 2f, w, h, null, alphaMult);
    }
}
