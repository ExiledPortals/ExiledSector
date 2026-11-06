package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.LogisticsSchematic;
import org.lazywizard.console.Console;

public class GrantLogisticsSchematicsCommand extends PlayerFleetCommand {

    static final int DEFAULT_COPIES = 1;
    static final int MAX_COPIES = 50;

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        Integer copies = ConsoleArgs.copiesOrComplain(args, DEFAULT_COPIES, MAX_COPIES);
        if (copies == null) {
            return CommandResult.BAD_SYNTAX;
        }
        playerFleet.getCargo().addSpecial(LogisticsSchematic.INSTANCE.toSpecialItem(), copies);
        Console.showMessage("Added " + counted(copies, "Logistics Schematic", "Logistics Schematics") + " to your cargo.");
        return CommandResult.SUCCESS;
    }
}
