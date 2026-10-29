package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.util.List;

public class HullUpgradeItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(HullUpgradeItemPlugin.class);

    private RefitSchematic schematic;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        schematic = stack == null ? null : RefitSchematic.of(stack.getSpecialDataIfSpecial());
    }

    @Override
    public String getName() {
        return I18n.forGameText(() -> schematic == null ? Translation.text("socketable.unknown") : schematic.displayName());
    }

    @Override
    public float getTooltipWidth() {
        return ItemTooltip.WIDTH;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        HullUpgradeTooltip.write(tooltip, schematic, () -> List.of(Translation.styled("upgrade.tooltip.storage")));
        addCostLabel(tooltip, ItemTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        if (schematic == null) {
            return;
        }
        SpriteDraw.drawAtCenter(SPRITES, schematic.iconPath(), x + w / 2f, y + h / 2f, w, h, null, alphaMult);
    }
}
