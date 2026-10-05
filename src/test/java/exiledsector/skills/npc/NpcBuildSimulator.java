package exiledsector.skills.npc;

import exiledsector.skills.npc.NpcArchetypes.Archetype;
import org.json.JSONException;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class NpcBuildSimulator {

    static final String USAGE = "Usage: NpcBuildSimulator <archetype> <designType|-> <factionRegion|-> <nodeCount> <seed>"
            + " [opCostPerNode] [removableHullMod=opCost,csv] [permanentHullMods,csv]";

    private NpcBuildSimulator() {
    }

    public static void main(String[] args) {
        PrintStream out = System.out;
        if (args.length < 5 || args.length > 8) {
            out.println(USAGE);
            return;
        }
        try {
            simulate(args, out);
        } catch (IOException | JSONException | RuntimeException e) {
            out.println("ERROR: " + e);
        }
    }

    private static void simulate(String[] args, PrintStream out) throws IOException, JSONException {
        RealSkillData.load();
        Archetype archetype = NpcArchetypes.ALL.stream().filter(a -> a.name().equals(args[0])).findFirst().orElse(null);
        if (archetype == null) {
            out.println("Unknown archetype. Archetypes: " + NpcArchetypes.ALL.stream().map(Archetype::name).toList());
            return;
        }
        String designType = "-".equals(args[1]) ? null : args[1];
        String faction = "-".equals(args[2]) ? null : args[2];
        int opCostPerNode = args.length > 5 ? Integer.parseInt(args[5]) : 1;
        Map<String, Integer> costs = new TreeMap<>();
        for (String entry : args.length > 6 ? csv(args[6]) : Set.<String>of()) {
            String[] parts = entry.split("=");
            costs.put(parts[0], parts.length > 1 ? Integer.parseInt(parts[1]) : 0);
        }
        NpcHullMods hullMods = new NpcHullMods(costs.keySet(), args.length > 7 ? csv(args[7]) : Set.of());
        NpcFreedOp freedOp = new NpcFreedOp(opCostPerNode, costs, NpcSkillTreeBuilder.MAX_NODE_COUNT);
        NpcTreeBuild build = NpcSkillTreeBuilder.generate(new NpcBuildRequest(archetype.profile(), designType, faction, hullMods,
                freedOp, Integer.parseInt(args[3])), new Random(Long.parseLong(args[4])));
        out.println(archetype.name() + ", " + designType + ", faction " + faction + ", " + args[3] + " nodes, seed " + args[4]);
        int count = 0;
        for (NpcBuildStep step : build.steps()) {
            String number = step.isCharged() ? String.format("%2d", ++count) : "  ";
            out.println(String.format("  %s %-40s %s", number, step.nodeId(), step.outcome()));
        }
        out.println("Stripped hullmods: " + build.strippedHullModIds());
    }

    private static Set<String> csv(String value) {
        Set<String> ids = new TreeSet<>(Arrays.asList(value.split(",")));
        ids.remove("");
        return ids;
    }
}
