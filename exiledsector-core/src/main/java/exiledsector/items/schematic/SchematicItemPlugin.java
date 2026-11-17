package exiledsector.items.schematic;

import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemTooltip;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

public abstract class SchematicItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(SchematicItemPlugin.class);

    protected abstract ShipSchematic schematic();

    protected abstract String iconPath();

    protected abstract void writeTooltip(TooltipMakerAPI tooltip);

    @Override
    public String getName() {
        ShipSchematic schematic = schematic();
        return I18n.forGameText(() -> schematic == null ? Translation.text("socketable.unknown") : schematic.displayName());
    }

    @Override
    public float getTooltipWidth() {
        return ItemTooltip.width();
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        writeTooltip(tooltip);
        addCostLabel(tooltip, ItemTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        String iconPath = iconPath();
        if (iconPath == null) {
            return;
        }
        SpriteDraw.drawAtCenter(SPRITES, iconPath, x + w / 2f, y + h / 2f, w, h, null, alphaMult);
    }
}
