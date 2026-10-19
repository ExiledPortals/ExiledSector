package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemType;
import exiledsector.items.SubroutineKind;
import exiledsector.items.VolumeSubroutines;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GrantVolumeSubroutinesCommand implements BaseCommand {

    private final Random random;

    public GrantVolumeSubroutinesCommand() {
        this(new Random());
    }

    GrantVolumeSubroutinesCommand(Random random) {
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
        String requestedVolume = args.trim().toLowerCase(Locale.ROOT);
        List<ItemDefinition> grantedDefinitions = matchingVolumeSubroutines(requestedVolume);
        if (grantedDefinitions.isEmpty()) {
            Console.showMessage(requestedVolume.isEmpty() ? "No volume subroutines are defined." : "No volume subroutine matches \""
                    + requestedVolume + "\".");
            return requestedVolume.isEmpty() ? CommandResult.ERROR : CommandResult.BAD_SYNTAX;
        }
        List<String> inertIds = new ArrayList<>();
        for (ItemDefinition definition : grantedDefinitions) {
            playerFleet.getCargo().addSpecial(ItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
            if (!VolumeSubroutines.areaActive(definition.id())) {
                inertIds.add(definition.id());
            }
        }
        Console.showMessage("Added " + grantedDefinitions.size() + (grantedDefinitions.size() == 1 ? " volume subroutine" : " volume subroutines")
                + " to your cargo." + (inertIds.isEmpty() ? "" : " Inert because their area is turned off: " + String.join(", ", inertIds) + "."));
        return CommandResult.SUCCESS;
    }

    static List<ItemDefinition> matchingVolumeSubroutines(String requestedVolume) {
        List<ItemDefinition> matching = new ArrayList<>();
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.SUBROUTINE)) {
            if (definition.subroutineKind() != SubroutineKind.VOLUME) {
                continue;
            }
            String volume = VolumeSubroutines.volume(definition.id());
            if (requestedVolume.isEmpty() || volume != null && volume.toLowerCase(Locale.ROOT).equals(requestedVolume)
                    || definition.id().toLowerCase(Locale.ROOT).contains(requestedVolume)) {
                matching.add(definition);
            }
        }
        return matching;
    }
}
