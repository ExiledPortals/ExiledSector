package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasState;
import org.lazywizard.console.Console;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class AtlasStatusCommand extends CampaignCommand {

    static final int SITES_PER_KIND = 3;
    static final int REACHABLE_LISTED = 20;

    @Override
    protected CommandResult runInCampaign(String args) {
        SectorAPI sector = Global.getSector();
        AtlasState state = AtlasState.get(sector);
        AtlasGenerator generator = AtlasRuntime.generator(sector);
        Console.showMessage(describe(generator, state));
        return CommandResult.SUCCESS;
    }

    static String describe(AtlasGenerator generator, AtlasState state) {
        Set<AtlasCell> reachable = AtlasReach.reachable(generator, state);
        StringBuilder text = new StringBuilder("Atlas seed ").append(state.seed()).append(" (generator version ").append(state.generatorVersion())
                .append("): ").append(counted(state.completedCells().size(), "completed system", "completed systems")).append(" counting the Gateway, ")
                .append(reachable.size()).append(" reachable, ").append(AtlasReach.visible(generator, state).size()).append(" visible, ")
                .append(counted(state.recordedSites(), "biome site", "biome sites")).append(" recorded.")
                .append("\nAtlas points ").append(state.atlasPoints()).append(", key fragments ").append(state.keyFragments())
                .append(", cleared tiers ").append(state.clearedTiers().isEmpty() ? "none" : state.clearedTiers().toString())
                .append(", unlock flags ").append(generator.unlockFlags().isEmpty() ? "none" : new TreeSet<>(generator.unlockFlags()).toString()).append(".");
        text.append("\nNearest special sites (x y, distance, state):");
        Map<String, StringBuilder> sites = new LinkedHashMap<>();
        Map<String, Integer> listed = new LinkedHashMap<>();
        for (AtlasDevTools.Site site : AtlasDevTools.sites(generator, AtlasDevTools.SITE_SEARCH_RADIUS)) {
            String id = site.special().id();
            int count = listed.merge(id, 1, Integer::sum);
            if (count > SITES_PER_KIND) {
                continue;
            }
            StringBuilder line = sites.computeIfAbsent(id, key -> new StringBuilder());
            if (!line.isEmpty()) {
                line.append("; ");
            }
            line.append(AtlasCommandText.cell(site.cell())).append(" ").append(String.format(Locale.ROOT, "%.1f", site.cell().centreDistance()))
                    .append(" ").append(AtlasCommandText.stateOf(site.cell(), state));
        }
        if (sites.isEmpty()) {
            text.append(" none within ").append(AtlasDevTools.SITE_SEARCH_RADIUS).append(" cells.");
        }
        sites.forEach((id, line) -> text.append("\n   ").append(id).append(": ").append(line));
        text.append("\nReachable systems:");
        List<AtlasCell> shown = reachable.stream().limit(REACHABLE_LISTED).toList();
        for (AtlasCell cell : shown) {
            text.append("\n   ").append(AtlasCommandText.node(generator, cell, state));
        }
        if (reachable.size() > shown.size()) {
            text.append("\n   and ").append(reachable.size() - shown.size()).append(" more.");
        }
        return text.toString();
    }
}
