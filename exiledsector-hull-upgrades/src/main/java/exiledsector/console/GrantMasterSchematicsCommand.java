package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.MasterSchematic;
import org.lazywizard.console.Console;

public class GrantMasterSchematicsCommand extends PlayerFleetCommand {

    static final int DEFAULT_COPIES = 1;
    static final int MAX_COPIES = 50;

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        Integer copies = ConsoleArgs.count(args, DEFAULT_COPIES, 1, MAX_COPIES);
        if (copies == null) {
            Console.showMessage("Give a number of copies from 1 to " + MAX_COPIES + ".");
            return CommandResult.BAD_SYNTAX;
        }
        playerFleet.getCargo().addSpecial(MasterSchematic.INSTANCE.toSpecialItem(), copies);
        Console.showMessage("Added " + counted(copies, "Master Schematic", "Master Schematics") + " to your cargo.");
        return CommandResult.SUCCESS;
    }
}
