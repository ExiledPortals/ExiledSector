package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.socketables.HullFrameworkData;
import exiledsector.socketables.HullFrameworkRoller;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Random;

public class GrantFrameworksCommand implements BaseCommand {

    static final int DEFAULT_COPIES = 3;
    static final int MAX_COPIES = 50;
    private static final List<HullSize> HULL_SIZES = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);

    private final Random random;

    public GrantFrameworksCommand() {
        this(new Random());
    }

    GrantFrameworksCommand(Random random) {
        this.random = random;
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        int copies;
        try {
            copies = args.isBlank() ? DEFAULT_COPIES : Integer.parseInt(args.trim());
        } catch (NumberFormatException e) {
            return CommandResult.BAD_SYNTAX;
        }
        if (copies < 1 || copies > MAX_COPIES) {
            Console.showMessage("Give a number of copies from 1 to " + MAX_COPIES + ".");
            return CommandResult.BAD_SYNTAX;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            Console.showMessage("No player fleet found.");
            return CommandResult.ERROR;
        }
        for (HullSize hullSize : HULL_SIZES) {
            for (int i = 0; i < copies; i++) {
                HullFrameworkData framework = HullFrameworkRoller.roll(hullSize, random);
                playerFleet.getCargo().addSpecial(framework.toSpecialItem(), 1f);
            }
        }
        Console.showMessage("Added " + copies + (copies == 1 ? " hull framework" : " hull frameworks") + " for each hull size to your cargo.");
        return CommandResult.SUCCESS;
    }
}
