package exiledsector.console;

import exiledsector.items.DeintegrationSchematic;

import java.util.List;

public class GrantDeintegrationSchematicsCommand extends GrantSchematicsCommand {

    public GrantDeintegrationSchematicsCommand() {
        super(List.of(DeintegrationSchematic.INSTANCE), 1, "Deintegration Schematic", "Deintegration Schematics");
    }
}
