package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.DoctrineArchives;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemType;
import exiledsector.items.SubroutineKind;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GrantDoctrineArchivesCommand implements BaseCommand {

    private final Random random;

    public GrantDoctrineArchivesCommand() {
        this(new Random());
    }

    GrantDoctrineArchivesCommand(Random random) {
        this.random = random;
    }

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
        String requestedFaction = args.trim().toLowerCase(Locale.ROOT);
        List<ItemDefinition> grantedDefinitions = matchingDoctrines(requestedFaction);
        if (grantedDefinitions.isEmpty()) {
            Console.showMessage(requestedFaction.isEmpty() ? "No Doctrine Archives are defined." : "No Doctrine Archive matches \""
                    + requestedFaction + "\".");
            return requestedFaction.isEmpty() ? CommandResult.ERROR : CommandResult.BAD_SYNTAX;
        }
        List<String> inertIds = new ArrayList<>();
        for (ItemDefinition definition : grantedDefinitions) {
            playerFleet.getCargo().addSpecial(ItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
            if (DoctrineArchives.get(definition.id()) == null) {
                inertIds.add(definition.id());
            }
        }
        Console.showMessage("Added " + grantedDefinitions.size() + (grantedDefinitions.size() == 1 ? " Doctrine Archive" : " Doctrine Archives")
                + " to your cargo." + (inertIds.isEmpty() ? "" : " Inert because they have no row in " + DoctrineArchives.DATA_PATH + ": "
                + String.join(", ", inertIds) + "."));
        return CommandResult.SUCCESS;
    }

    static List<ItemDefinition> matchingDoctrines(String requestedFaction) {
        List<ItemDefinition> matching = new ArrayList<>();
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.SUBROUTINE)) {
            if (definition.subroutineKind() == SubroutineKind.DOCTRINE
                    && (requestedFaction.isEmpty() || definition.id().toLowerCase(Locale.ROOT).contains(requestedFaction))) {
                matching.add(definition);
            }
        }
        return matching;
    }
}
