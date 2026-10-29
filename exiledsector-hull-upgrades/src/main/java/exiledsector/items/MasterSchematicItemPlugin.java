package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.util.List;

public class MasterSchematicItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(MasterSchematicItemPlugin.class);

    @Override
    public String getName() {
        return I18n.forGameText(MasterSchematic.INSTANCE::displayName);
    }

    @Override
    public float getTooltipWidth() {
        return ItemTooltip.WIDTH;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        HullUpgradeTooltip.writeMasterSchematic(tooltip, () -> List.of(Translation.styled("upgrade.tooltip.storage")));
        addCostLabel(tooltip, ItemTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        SpriteDraw.drawAtCenter(SPRITES, MasterSchematic.ICON_PATH, x + w / 2f, y + h / 2f, w, h, null, alphaMult);
    }
}
