package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ModspecCaches;
import exiledsector.items.SubroutineKind;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Random;

public class GrantModspecCacheCommand extends RandomPlayerFleetCommand {

    static final int SEED_SEARCH_ATTEMPTS = 100_000;

    public GrantModspecCacheCommand() {
    }

    GrantModspecCacheCommand(Random random) {
        super(random);
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        Integer requestedNodeCount = ConsoleArgs.wholeNumber(args, 1, Integer.MAX_VALUE);
        if (!args.isEmpty() && requestedNodeCount == null) {
            Console.showMessage("Expected a node count of 1 or more, or nothing.");
            return CommandResult.BAD_SYNTAX;
        }
        List<ItemDefinition> definitions = SubroutineKind.MODSPEC_CACHE.definitions();
        if (definitions.isEmpty()) {
            Console.showMessage("No modspec caches are defined.");
            return CommandResult.ERROR;
        }
        int added = 0;
        for (ItemDefinition definition : definitions) {
            Long seed = seedFor(definition, requestedNodeCount);
            if (seed != null) {
                playerFleet.getCargo().addSpecial(ItemData.rolled(definition, seed).toSpecialItem(), 1f);
                added++;
            }
        }
        if (added == 0) {
            Console.showMessage("No modspec cache can roll " + requestedNodeCount + " nodes.");
            return CommandResult.ERROR;
        }
        Console.showMessage("Added " + counted(added, "modspec cache", "modspec caches") + " to your cargo.");
        return CommandResult.SUCCESS;
    }

    Long seedFor(ItemDefinition definition, Integer requestedNodeCount) {
        ModspecCaches.CacheBase base = ModspecCaches.base(definition.id());
        if (requestedNodeCount == null || base == null) {
            return random.nextLong();
        }
        for (int attempt = 0; attempt < SEED_SEARCH_ATTEMPTS; attempt++) {
            long seed = random.nextLong();
            if (base.nodeCount(seed) == requestedNodeCount) {
                return seed;
            }
        }
        return null;
    }
}
