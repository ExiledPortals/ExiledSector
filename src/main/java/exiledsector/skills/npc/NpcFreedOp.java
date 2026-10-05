package exiledsector.skills.npc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.SkillNodeOpCost;

import java.util.HashMap;
import java.util.Map;

public record NpcFreedOp(int opCostPerNode, Map<String, Integer> hullModOpCosts, Map<String, Integer> hullModFighterBays,
                         int maxNodeCount) {

    public static final NpcFreedOp NONE = new NpcFreedOp(1, Map.of(), NpcSkillTreeBuilder.MAX_NODE_COUNT);

    public NpcFreedOp {
        opCostPerNode = Math.max(1, opCostPerNode);
        hullModOpCosts = hullModOpCosts == null ? Map.of() : Map.copyOf(hullModOpCosts);
        hullModFighterBays = hullModFighterBays == null ? Map.of() : Map.copyOf(hullModFighterBays);
    }

    public NpcFreedOp(int opCostPerNode, Map<String, Integer> hullModOpCosts, int maxNodeCount) {
        this(opCostPerNode, hullModOpCosts, Map.of(), maxNodeCount);
    }

    public static NpcFreedOp of(FleetMemberAPI member, NpcHullMods hullMods) {
        HullSize hullSize = member.getHullSpec() == null ? null : member.getHullSpec().getHullSize();
        SettingsAPI settings = Global.getSettings();
        MutableStat bays = member.getStats() == null ? null : member.getStats().getNumFighterBays();
        Map<String, Integer> costs = new HashMap<>();
        Map<String, Integer> fighterBays = new HashMap<>();
        for (String hullModId : hullMods.removable()) {
            HullModSpecAPI spec = settings == null || hullSize == null ? null : settings.getHullModSpec(hullModId);
            if (spec != null) {
                costs.put(hullModId, spec.getCostFor(hullSize));
            }
            MutableStat.StatMod added = bays == null ? null : bays.getFlatStatMod(hullModId);
            if (added != null && added.getValue() > 0f) {
                fighterBays.put(hullModId, Math.round(added.getValue()));
            }
        }
        return new NpcFreedOp(SkillNodeOpCost.perNode(member.getHullSpec()), costs, fighterBays,
                ShipLevelConfig.maxAllocatedNodesBesidesRoot());
    }
}
