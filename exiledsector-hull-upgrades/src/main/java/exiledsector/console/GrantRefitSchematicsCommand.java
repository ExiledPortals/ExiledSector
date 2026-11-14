package exiledsector.console;

import exiledsector.items.RefitSchematic;

import java.util.List;

public class GrantRefitSchematicsCommand extends GrantSchematicsCommand {

    public GrantRefitSchematicsCommand() {
        super(List.of(RefitSchematic.values()), 3, "Refit Schematic", "Refit Schematics");
    }

    @Override
    protected String grantedDescription(String countedSchematics) {
        return countedSchematics + " of each tier";
    }
}
