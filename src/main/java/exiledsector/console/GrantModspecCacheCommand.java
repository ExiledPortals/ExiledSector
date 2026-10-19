package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemType;
import exiledsector.items.ModspecCaches;
import exiledsector.items.SubroutineKind;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GrantModspecCacheCommand implements BaseCommand {

    static final int SEED_SEARCH_ATTEMPTS = 100_000;

    private final Random random;

    public GrantModspecCacheCommand() {
        this(new Random());
    }

    GrantModspecCacheCommand(Random random) {
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
        Integer requestedNodeCount = requestedNodeCount(args.trim());
        if (!args.isBlank() && requestedNodeCount == null) {
            Console.showMessage("Expected a node count of 1 or more, or nothing.");
            return CommandResult.BAD_SYNTAX;
        }
        List<ItemDefinition> definitions = modspecCaches();
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
        Console.showMessage("Added " + added + (added == 1 ? " modspec cache" : " modspec caches") + " to your cargo.");
        return CommandResult.SUCCESS;
    }

    static Integer requestedNodeCount(String argument) {
        if (argument.isEmpty()) {
            return null;
        }
        try {
            int nodeCount = Integer.parseInt(argument);
            return nodeCount >= 1 ? nodeCount : null;
        } catch (NumberFormatException e) {
            return null;
        }
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

    static List<ItemDefinition> modspecCaches() {
        List<ItemDefinition> caches = new ArrayList<>();
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.SUBROUTINE)) {
            if (definition.subroutineKind() == SubroutineKind.MODSPEC_CACHE) {
                caches.add(definition);
            }
        }
        return caches;
    }
}
