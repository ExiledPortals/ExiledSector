package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemType;
import exiledsector.items.WormholeAnchorDefinitions;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Random;

public class GrantWormholeAnchorsCommand extends PlayerFleetCommand {

    private final Random random;

    public GrantWormholeAnchorsCommand() {
        this(new Random());
    }

    GrantWormholeAnchorsCommand(Random random) {
        this.random = random;
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        List<ItemDefinition> grantedDefinitions = ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR).stream()
                .filter(definition -> args.isEmpty() || args.equalsIgnoreCase(WormholeAnchorDefinitions.volume(definition.id())))
                .toList();
        if (grantedDefinitions.isEmpty()) {
            Console.showMessage(args.isEmpty() ? "No wormhole anchors are defined." : "No wormhole anchor leads to the volume \"" + args + "\".");
            return args.isEmpty() ? CommandResult.ERROR : CommandResult.BAD_SYNTAX;
        }
        List<ItemData> grantedItems = ConsoleItems.grantRolled(playerFleet.getCargo(), grantedDefinitions, 1, random);
        List<String> inertIds = grantedItems.stream()
                .map(ItemData::definitionId)
                .filter(id -> !WormholeAnchorDefinitions.areaActive(id))
                .toList();
        WormholeAnchorDefinitions.recordFound(grantedItems, Global.getSector(), "granted by the console");
        Console.showMessage("Added " + counted(grantedDefinitions.size(), "wormhole anchor", "wormhole anchors") + " to your cargo."
                + (inertIds.isEmpty() ? "" : " Inert because their area is off: " + String.join(", ", inertIds) + "."));
        return CommandResult.SUCCESS;
    }
}
