package exiledsector.socketables;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.ui.inspect.SocketableCodex;
import exiledsector.ui.util.SpriteCache;

import java.util.List;
import java.util.function.Supplier;

public class SocketableItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(SocketableItemPlugin.class);

    private Socketable previewSocketable;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        SocketableItemData itemData = stack == null ? null : SocketableItemData.of(stack.getSpecialDataIfSpecial());
        previewSocketable = itemData == null ? null : itemData.preview();
    }

    @Override
    public String getName() {
        return I18n.forGameText(() -> previewSocketable == null ? Translation.text("socketable.unknown") : previewSocketable.name());
    }

    @Override
    public float getTooltipWidth() {
        return SocketableTooltip.WIDTH;
    }

    @Override
    public boolean isTooltipExpandable() {
        return true;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        String codexEntryId = previewSocketable == null ? null : SocketableCodex.existingEntryId(previewSocketable.definitionId());
        if (codexEntryId != null) {
            tooltip.setCodexEntryId(codexEntryId);
        }
        boolean codexLinked = codexEntryId != null;
        Supplier<List<StyledText>> footer = () -> codexLinked
                ? List.of(Translation.styled("socketable.tooltip.storage"), Translation.styled("socketable.tooltip.codexHint"))
                : List.of(Translation.styled("socketable.tooltip.storage"));
        SocketableTooltip.write(tooltip, previewSocketable, footer, expanded);
        addCostLabel(tooltip, SocketableTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        SpriteAPI sprite = SPRITES.sprite(previewSocketable == null ? SocketableDefinition.FALLBACK_ICON : previewSocketable.iconPath());
        if (sprite == null) {
            return;
        }
        sprite.setSize(w, h);
        sprite.setAlphaMult(alphaMult);
        sprite.setNormalBlend();
        sprite.renderAtCenter(x + w / 2f, y + h / 2f);
    }
}
