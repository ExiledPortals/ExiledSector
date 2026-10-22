package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemType;
import exiledsector.items.WormholeAnchorDefinitions;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GrantWormholeAnchorsCommand implements BaseCommand {

    private final Random random;

    public GrantWormholeAnchorsCommand() {
        this(new Random());
    }

    GrantWormholeAnchorsCommand(Random random) {
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
        String requestedVolume = args.trim();
        List<ItemDefinition> grantedDefinitions = new ArrayList<>();
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR)) {
            if (requestedVolume.isEmpty() || requestedVolume.equalsIgnoreCase(WormholeAnchorDefinitions.volume(definition.id()))) {
                grantedDefinitions.add(definition);
            }
        }
        if (grantedDefinitions.isEmpty()) {
            Console.showMessage(requestedVolume.isEmpty() ? "No wormhole anchors are defined." : "No wormhole anchor leads to the volume \""
                    + requestedVolume + "\".");
            return requestedVolume.isEmpty() ? CommandResult.ERROR : CommandResult.BAD_SYNTAX;
        }
        List<String> inertIds = new ArrayList<>();
        for (ItemDefinition definition : grantedDefinitions) {
            playerFleet.getCargo().addSpecial(ItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
            if (!WormholeAnchorDefinitions.areaActive(definition.id())) {
                inertIds.add(definition.id());
            }
        }
        Console.showMessage("Added " + grantedDefinitions.size() + (grantedDefinitions.size() == 1 ? " wormhole anchor" : " wormhole anchors")
                + " to your cargo." + (inertIds.isEmpty() ? "" : " Inert because their area is off: " + String.join(", ", inertIds) + "."));
        return CommandResult.SUCCESS;
    }
}
