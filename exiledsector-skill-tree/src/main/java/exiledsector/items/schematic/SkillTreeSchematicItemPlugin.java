package exiledsector.items.schematic;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemTooltip;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.ui.VanillaText;

import java.util.List;

abstract class SkillTreeSchematicItemPlugin extends SchematicItemPlugin {

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
    protected ShipSchematic schematic() {
        return schematic;
    }

    @Override
    protected String iconPath() {
        return iconPath;
    }

    @Override
    protected void writeTooltip(TooltipMakerAPI tooltip) {
        ItemTooltip.writeFrame(tooltip, () -> {
            tooltip.addTitle(schematic.displayName(), Misc.getStoryOptionColor());
            VanillaText.addPara(tooltip, body(), ItemTooltip.PAD, Misc.getTextColor());
            VanillaText.addPara(tooltip, Translation.styled(flavourKey), ItemTooltip.PAD, Misc.getGrayColor());
            if (!SkillTreeSetting.enabled()) {
                VanillaText.addPara(tooltip, Translation.styled("skillTreeSchematic.tooltip.disabled"), ItemTooltip.PAD,
                        Misc.getNegativeHighlightColor());
            }
        }, List::of);
    }
}
