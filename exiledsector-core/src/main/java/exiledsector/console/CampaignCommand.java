package exiledsector.console;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public abstract class CampaignCommand implements BaseCommand {

    @Override
    public final CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        return runInCampaign(args == null ? "" : args.trim());
    }

    protected abstract CommandResult runInCampaign(String args);

    protected static String counted(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }
}
