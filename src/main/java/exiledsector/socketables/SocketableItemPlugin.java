package exiledsector.socketables;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;
import exiledsector.ui.util.SpriteCache;

public class SocketableItemPlugin extends BaseSpecialItemPlugin {

    private static final float PAD = 10f;
    private static final float LINE_PAD = 3f;
    private static final SpriteCache SPRITES = new SpriteCache(SocketableItemPlugin.class);

    private Socketable preview;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        SocketableItemData item = stack == null ? null : SocketableItemData.of(stack.getSpecialDataIfSpecial());
        preview = item == null ? null : item.preview();
    }

    @Override
    public String getName() {
        return I18n.forGameText(() -> preview == null ? Translation.text("socketable.unknown") : preview.name());
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        tooltip.addTitle(getName());
        I18n.forGameText(() -> {
            if (preview != null) {
                float pad = PAD;
                for (StyledText line : preview.tooltipLines()) {
                    VanillaText.addPara(tooltip, line, pad, Misc.getTextColor());
                    pad = LINE_PAD;
                }
            }
            VanillaText.addPara(tooltip, Translation.styled("socketable.tooltip.storage"), PAD, Misc.getGrayColor());
        });
        addCostLabel(tooltip, PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        SpriteAPI sprite = SPRITES.sprite(preview == null ? SocketableDefinition.FALLBACK_ICON : preview.iconPath());
        if (sprite == null) {
            return;
        }
        sprite.setSize(w, h);
        sprite.setAlphaMult(alphaMult);
        sprite.setNormalBlend();
        sprite.renderAtCenter(x + w / 2f, y + h / 2f);
    }
}
