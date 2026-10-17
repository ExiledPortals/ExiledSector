package exiledsector.hullitems;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.ui.inspect.HullItemCodex;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.util.List;
import java.util.function.Supplier;

public class HullItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(HullItemPlugin.class);

    private HullItem previewHullItem;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        HullItemData itemData = stack == null ? null : HullItemData.of(stack.getSpecialDataIfSpecial());
        previewHullItem = itemData == null ? null : itemData.preview();
    }

    @Override
    public String getName() {
        return I18n.forGameText(() -> previewHullItem == null ? Translation.text("socketable.unknown") : previewHullItem.name());
    }

    @Override
    public float getTooltipWidth() {
        return HullItemTooltip.WIDTH;
    }

    @Override
    public boolean isTooltipExpandable() {
        return true;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        String codexEntryId = previewHullItem == null ? null : HullItemCodex.existingEntryId(previewHullItem.definitionId());
        if (codexEntryId != null) {
            tooltip.setCodexEntryId(codexEntryId);
        }
        boolean codexLinked = codexEntryId != null;
        Supplier<List<StyledText>> footer = () -> codexLinked
                ? List.of(Translation.styled("socketable.tooltip.storage"), Translation.styled("socketable.tooltip.codexHint"))
                : List.of(Translation.styled("socketable.tooltip.storage"));
        HullItemTooltip.write(tooltip, previewHullItem, footer, expanded);
        addCostLabel(tooltip, HullItemTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        String iconPath = previewHullItem == null ? HullItemDefinition.FALLBACK_ICON : previewHullItem.iconPath();
        SpriteDraw.drawAtCenter(SPRITES, iconPath, x + w / 2f, y + h / 2f, w, h, null, alphaMult);
    }
}
