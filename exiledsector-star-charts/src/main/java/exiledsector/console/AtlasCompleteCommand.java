package exiledsector.console;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.items.StarChart;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Locale;

public class AtlasCompleteCommand extends AtlasTargetCommand {

    static final int DEFAULT_TIER = StarChart.LOWEST_TIER;

    static Integer tier(List<String> rest) {
        if (rest.isEmpty()) {
            return DEFAULT_TIER;
        }
        if (rest.size() > 1) {
            return null;
        }
        String text = rest.get(0).toLowerCase(Locale.ROOT).startsWith("tier=") ? rest.get(0).substring("tier=".length()) : rest.get(0);
        return ConsoleArgs.wholeNumber(text, StarChart.LOWEST_TIER, StarChart.HIGHEST_TIER);
    }

    @Override
    protected String usage() {
        return "ExiledSectorAtlasComplete <x> <y> | <site id> [tier], tier " + StarChart.LOWEST_TIER + " to " + StarChart.HIGHEST_TIER
                + " (default " + DEFAULT_TIER + ").";
    }

    @Override
    protected CommandResult runAt(SectorAPI sector, AtlasGenerator generator, AtlasState state, AtlasCell cell, List<String> rest) {
        Integer tier = tier(rest);
        if (tier == null) {
            Console.showMessage("Can't use \"" + String.join(" ", rest) + "\".\nUsage: " + usage());
            return CommandResult.BAD_SYNTAX;
        }
        if (state.isCompleted(cell)) {
            Console.showMessage(AtlasCommandText.cell(cell) + " is already completed.");
            return CommandResult.SUCCESS;
        }
        int points = state.atlasPoints();
        int fragments = state.keyFragments();
        AtlasDevTools.complete(generator, state, cell, tier);
        Console.showMessage("Completed " + AtlasCommandText.node(generator.node(cell), state) + " as a tier " + tier + " run. Atlas points +"
                + (state.atlasPoints() - points) + ", key fragments +" + (state.keyFragments() - fragments) + ".");
        return CommandResult.SUCCESS;
    }
}
