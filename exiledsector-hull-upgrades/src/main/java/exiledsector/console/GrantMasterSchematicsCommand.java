package exiledsector.console;

import exiledsector.items.MasterSchematic;

import java.util.List;

public class GrantMasterSchematicsCommand extends GrantSchematicsCommand {

    public GrantMasterSchematicsCommand() {
        super(List.of(MasterSchematic.INSTANCE), 1, "Master Schematic", "Master Schematics");
    }
}
