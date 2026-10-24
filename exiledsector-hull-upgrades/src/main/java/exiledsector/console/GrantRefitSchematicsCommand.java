package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.RefitSchematic;
import org.lazywizard.console.Console;

public class GrantRefitSchematicsCommand extends PlayerFleetCommand {

    static final int DEFAULT_COPIES = 3;
    static final int MAX_COPIES = 50;

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        Integer copies = ConsoleArgs.count(args, DEFAULT_COPIES, 1, MAX_COPIES);
        if (copies == null) {
            Console.showMessage("Give a number of copies from 1 to " + MAX_COPIES + ".");
            return CommandResult.BAD_SYNTAX;
        }
        for (RefitSchematic schematic : RefitSchematic.values()) {
            for (int i = 0; i < copies; i++) {
                playerFleet.getCargo().addSpecial(schematic.toSpecialItem(), 1f);
            }
        }
        Console.showMessage("Added " + counted(copies, "Refit Schematic", "Refit Schematics") + " of each tier to your cargo.");
        return CommandResult.SUCCESS;
    }
}
