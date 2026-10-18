package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.ui.chart.StarChartScreen;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class StarChartsCommand implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            Console.showMessage("No player fleet found.");
            return CommandResult.ERROR;
        }
        Console.showDialogOnClose(StarChartScreen.dialogPlugin(), playerFleet);
        Console.showMessage("The Star Chart screen opens when the console closes.");
        return CommandResult.SUCCESS;
    }
}
