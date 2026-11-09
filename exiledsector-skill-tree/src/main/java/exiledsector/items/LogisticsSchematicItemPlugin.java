package exiledsector.items;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.LogisticsNodes;

public class LogisticsSchematicItemPlugin extends SkillTreeSchematicItemPlugin {

    public LogisticsSchematicItemPlugin() {
        super(LogisticsSchematic.INSTANCE, LogisticsSchematic.ICON_PATH, "logisticsSchematic.tooltip.flavour");
    }

    @Override
    StyledText body() {
        return Translation.msg("logisticsSchematic.tooltip.body").arg("cap", LogisticsNodes.DEFAULT_CAP).styled();
    }
}
