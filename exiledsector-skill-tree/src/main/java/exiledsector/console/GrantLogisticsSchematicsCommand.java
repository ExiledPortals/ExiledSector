package exiledsector.console;

import exiledsector.items.LogisticsSchematic;

import java.util.List;

public class GrantLogisticsSchematicsCommand extends GrantSchematicsCommand {

    public GrantLogisticsSchematicsCommand() {
        super(List.of(LogisticsSchematic.INSTANCE), 1, "Logistics Schematic", "Logistics Schematics");
    }
}
