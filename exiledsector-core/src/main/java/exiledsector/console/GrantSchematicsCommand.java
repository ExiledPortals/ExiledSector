package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.schematic.ShipSchematic;
import org.lazywizard.console.Console;

import java.util.List;

public abstract class GrantSchematicsCommand extends PlayerFleetCommand {

    static final int MAX_COPIES = 50;

    private final List<? extends ShipSchematic> schematics;
    private final int defaultCopies;
    private final String singularName;
    private final String pluralName;

    protected GrantSchematicsCommand(List<? extends ShipSchematic> schematics, int defaultCopies, String singularName, String pluralName) {
        this.schematics = schematics;
        this.defaultCopies = defaultCopies;
        this.singularName = singularName;
        this.pluralName = pluralName;
    }

    @Override
    protected final CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        Integer copies = ConsoleArgs.copiesOrComplain(args, defaultCopies, MAX_COPIES);
        if (copies == null) {
            return CommandResult.BAD_SYNTAX;
        }
        for (ShipSchematic schematic : schematics) {
            playerFleet.getCargo().addSpecial(schematic.toSpecialItem(), copies);
        }
        Console.showMessage("Added " + grantedDescription(counted(copies, singularName, pluralName)) + " to your cargo.");
        return CommandResult.SUCCESS;
    }

    protected String grantedDescription(String countedSchematics) {
        return countedSchematics;
    }
}
