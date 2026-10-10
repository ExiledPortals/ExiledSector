package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.socketables.HullUpgradeData;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;


public class GrantHullUpgradesCommand implements BaseCommand {

    static final int DEFAULT_COPIES = 3;
    static final int MAX_COPIES = 50;

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
        for (HullSize hullSize : HullUpgradeData.HULL_SIZES) {
            for (int i = 0; i < copies; i++) {
                playerFleet.getCargo().addSpecial(new HullUpgradeData(hullSize).toSpecialItem(), 1f);
            }
        }
        Console.showMessage("Added " + copies + (copies == 1 ? " Hull Upgrade" : " Hull Upgrades") + " for each hull size to your cargo.");
        return CommandResult.SUCCESS;
    }
}
