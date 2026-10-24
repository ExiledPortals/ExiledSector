package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.DoctrineArchives;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.SubroutineKind;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GrantDoctrineArchivesCommand extends PlayerFleetCommand {

    private final Random random;

    public GrantDoctrineArchivesCommand() {
        this(new Random());
    }

    GrantDoctrineArchivesCommand(Random random) {
        this.random = random;
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        String requestedFaction = args.toLowerCase(Locale.ROOT);
        List<ItemDefinition> grantedDefinitions = matchingDoctrines(requestedFaction);
        if (grantedDefinitions.isEmpty()) {
            Console.showMessage(requestedFaction.isEmpty() ? "No Doctrine Archives are defined." : "No Doctrine Archive matches \""
                    + requestedFaction + "\".");
            return requestedFaction.isEmpty() ? CommandResult.ERROR : CommandResult.BAD_SYNTAX;
        }
        List<String> inertIds = ConsoleItems.grantRolled(playerFleet.getCargo(), grantedDefinitions, 1, random).stream()
                .map(ItemData::definitionId)
                .filter(id -> DoctrineArchives.get(id) == null)
                .toList();
        Console.showMessage("Added " + counted(grantedDefinitions.size(), "Doctrine Archive", "Doctrine Archives") + " to your cargo."
                + (inertIds.isEmpty() ? "" : " Inert because they have no row in " + DoctrineArchives.DATA_PATH + ": "
                + String.join(", ", inertIds) + "."));
        return CommandResult.SUCCESS;
    }

    static List<ItemDefinition> matchingDoctrines(String requestedFaction) {
        return SubroutineKind.DOCTRINE.definitions().stream()
                .filter(definition -> requestedFaction.isEmpty() || definition.id().toLowerCase(Locale.ROOT).contains(requestedFaction))
                .toList();
    }
}
