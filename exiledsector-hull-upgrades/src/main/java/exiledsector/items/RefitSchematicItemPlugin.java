package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.Translation;

import java.util.List;

public class RefitSchematicItemPlugin extends SchematicItemPlugin {

    private RefitSchematic schematic;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        schematic = stack == null ? null : RefitSchematic.of(stack.getSpecialDataIfSpecial());
    }

    @Override
    protected ShipSchematic schematic() {
        return schematic;
    }

    @Override
    protected String iconPath() {
        return schematic == null ? null : schematic.iconPath();
    }

    @Override
    protected void writeTooltip(TooltipMakerAPI tooltip) {
        RefitSchematicTooltip.write(tooltip, schematic, () -> List.of(Translation.styled("schematic.tooltip.storage")));
    }
}
