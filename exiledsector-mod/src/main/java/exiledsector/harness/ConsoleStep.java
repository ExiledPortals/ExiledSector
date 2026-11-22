package exiledsector.harness;

import com.fs.starfarer.api.Global;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;

final class ConsoleStep implements HarnessStep {

    private final String commandName;
    private final String arguments;

    ConsoleStep(String commandLine) {
        String trimmed = commandLine == null ? "" : commandLine.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("console needs a command, such as \"ExiledSectorShowcaseLoot spawn\"");
        }
        int space = trimmed.indexOf(' ');
        commandName = space < 0 ? trimmed : trimmed.substring(0, space);
        arguments = space < 0 ? "" : trimmed.substring(space + 1).trim();
    }

    @Override
    public String name() {
        return "console " + commandName + (arguments.isEmpty() ? "" : " " + arguments);
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("console");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        CommandStore.StoredCommand stored = CommandStore.retrieveCommand(commandName);
        if (stored == null) {
            return StepOutcome.failed("Console Commands has no command \"" + commandName + "\"");
        }
        if (!(Global.getSettings().getInstanceOfScript(stored.getCommandClass().getName()) instanceof BaseCommand command)) {
            return StepOutcome.failed("could not create " + stored.getCommandClass().getName());
        }
        BaseCommand.CommandResult result = command.runCommand(arguments, BaseCommand.CommandContext.CAMPAIGN_MAP);
        return result == BaseCommand.CommandResult.SUCCESS
                ? StepOutcome.passed("ran " + stored.getName() + ": " + result)
                : StepOutcome.failed(stored.getName() + " returned " + result);
    }
}
