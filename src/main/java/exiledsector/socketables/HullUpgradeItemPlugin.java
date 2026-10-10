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

public class HullUpgradeItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(HullUpgradeItemPlugin.class);

    private HullUpgradeData upgrade;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        upgrade = stack == null ? null : HullUpgradeData.of(stack.getSpecialDataIfSpecial());
    }

    @Override
    public String getName() {
        return I18n.forGameText(() -> upgrade == null ? Translation.text("socketable.unknown") : upgrade.name());
    }

    @Override
    public float getTooltipWidth() {
        return SocketableTooltip.WIDTH;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        HullUpgradeTooltip.write(tooltip, upgrade, () -> List.of(Translation.styled("upgrade.tooltip.storage")));
        addCostLabel(tooltip, SocketableTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        SpriteAPI sprite = upgrade == null ? null : SPRITES.sprite(upgrade.iconPath());
        if (sprite == null) {
            return;
        }
        sprite.setSize(w, h);
        sprite.setAlphaMult(alphaMult);
        sprite.setNormalBlend();
        sprite.renderAtCenter(x + w / 2f, y + h / 2f);
    }
}
