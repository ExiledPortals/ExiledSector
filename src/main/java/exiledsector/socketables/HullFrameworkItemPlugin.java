package exiledsector.socketables;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.util.SpriteCache;

import java.util.List;

public class HullFrameworkItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(HullFrameworkItemPlugin.class);

    private HullFramework previewFramework;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        HullFrameworkData frameworkData = stack == null ? null : HullFrameworkData.of(stack.getSpecialDataIfSpecial());
        previewFramework = frameworkData == null ? null : frameworkData.preview();
    }

    @Override
    public String getName() {
        return I18n.forGameText(() -> previewFramework == null ? Translation.text("socketable.unknown") : previewFramework.name());
    }

    @Override
    public float getTooltipWidth() {
        return SocketableTooltip.WIDTH;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        HullFrameworkTooltip.write(tooltip, previewFramework, () -> List.of(Translation.styled("framework.tooltip.storage")));
        addCostLabel(tooltip, SocketableTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        SpriteAPI sprite = SPRITES.sprite(previewFramework == null ? HullFrameworkData.ICON : previewFramework.iconPath());
        if (sprite == null) {
            return;
        }
        sprite.setSize(w, h);
        sprite.setAlphaMult(alphaMult);
        sprite.setNormalBlend();
        sprite.renderAtCenter(x + w / 2f, y + h / 2f);
    }
}
