package exiledsector.console;

import exiledsector.items.schematic.LogisticsSchematic;

import java.util.List;

public class GrantLogisticsSchematicsCommand extends GrantSchematicsCommand {

    public GrantLogisticsSchematicsCommand() {
        super(List.of(LogisticsSchematic.INSTANCE), 1, "Logistics Schematic", "Logistics Schematics");
    }
}
