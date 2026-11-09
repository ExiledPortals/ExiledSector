package exiledsector.items;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

public class DeintegrationSchematicItemPlugin extends SkillTreeSchematicItemPlugin {

    public DeintegrationSchematicItemPlugin() {
        super(DeintegrationSchematic.INSTANCE, DeintegrationSchematic.ICON_PATH, "deintegrationSchematic.tooltip.flavour");
    }

    @Override
    StyledText body() {
        return Translation.styled("deintegrationSchematic.tooltip.body");
    }
}
