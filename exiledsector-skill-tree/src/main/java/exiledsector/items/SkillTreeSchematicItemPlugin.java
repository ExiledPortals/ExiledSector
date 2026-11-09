package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.VanillaText;
import exiledsector.ui.util.SpriteDraw;

import java.util.List;

abstract class SkillTreeSchematicItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(SkillTreeSchematicItemPlugin.class);

    private final ShipSchematic schematic;
    private final String iconPath;
    private final String flavourKey;

    SkillTreeSchematicItemPlugin(ShipSchematic schematic, String iconPath, String flavourKey) {
        this.schematic = schematic;
        this.iconPath = iconPath;
        this.flavourKey = flavourKey;
    }

    abstract StyledText body();

    @Override
    public String getName() {
        return I18n.forGameText(schematic::displayName);
    }

    @Override
    public float getTooltipWidth() {
        return ItemTooltip.width();
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        ItemTooltip.writeFrame(tooltip, () -> {
            tooltip.addTitle(schematic.displayName(), Misc.getStoryOptionColor());
            VanillaText.addPara(tooltip, body(), ItemTooltip.PAD, Misc.getTextColor());
            VanillaText.addPara(tooltip, Translation.styled(flavourKey), ItemTooltip.PAD, Misc.getGrayColor());
            if (!SkillTreeSetting.enabled()) {
                VanillaText.addPara(tooltip, Translation.styled("skillTreeSchematic.tooltip.disabled"), ItemTooltip.PAD,
                        Misc.getNegativeHighlightColor());
            }
        }, List::of);
        addCostLabel(tooltip, ItemTooltip.PAD, transferHandler, stackSource);
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        SpriteDraw.drawAtCenter(SPRITES, iconPath, x + w / 2f, y + h / 2f, w, h, null, alphaMult);
    }
}
