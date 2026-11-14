package exiledsector.items;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.Translation;

import java.util.List;

public class MasterSchematicItemPlugin extends SchematicItemPlugin {

    @Override
    protected ShipSchematic schematic() {
        return MasterSchematic.INSTANCE;
    }

    @Override
    protected String iconPath() {
        return MasterSchematic.ICON_PATH;
    }

    @Override
    protected void writeTooltip(TooltipMakerAPI tooltip) {
        RefitSchematicTooltip.writeMasterSchematic(tooltip, () -> List.of(Translation.styled("schematic.tooltip.storage")));
    }
}
