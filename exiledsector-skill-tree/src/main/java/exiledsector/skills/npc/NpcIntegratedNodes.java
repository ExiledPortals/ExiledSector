package exiledsector.skills.npc;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class NpcIntegratedNodes {

    public static final int FROM_FITTED_S_MODS = -1;

    private NpcIntegratedNodes() {
    }

    public static int averageSModsBeforeInflation(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.isInflated() || !(fleet.getInflater() instanceof DefaultFleetInflater inflater)) {
            return FROM_FITTED_S_MODS;
        }
        return inflater.getAverageNumSMods();
    }

    public static int count(FleetMemberAPI member, int averageSModsBeforeInflation, Random random) {
        int fittedSMods = member.getVariant().getSMods().size();
        if (averageSModsBeforeInflation <= 0 || member.isCivilian()) {
            return fittedSMods;
        }
        return Math.max(fittedSMods, DefaultFleetInflater.getMaxSMods(member.getVariant(), averageSModsBeforeInflation, random));
    }

    public static void mark(ShipSkillData shipData, int count, Random random) {
        if (shipData == null || count <= 0) {
            return;
        }
        List<String> candidateNodeIds = integrableNodeIds(shipData);
        Collections.shuffle(candidateNodeIds, random);
        candidateNodeIds.subList(0, Math.min(count, candidateNodeIds.size())).forEach(shipData::integrate);
    }

    static void restore(ShipSkillData shipData, List<String> savedNodeIds) {
        List<String> integrableNodeIds = integrableNodeIds(shipData);
        for (String nodeId : savedNodeIds) {
            if (integrableNodeIds.contains(nodeId)) {
                shipData.integrate(nodeId);
            }
        }
    }

    static List<String> integrableNodeIds(ShipSkillData shipData) {
        List<String> nodeIds = new ArrayList<>();
        for (AllocatedNode allocated : AllocatedNode.of(shipData)) {
            if (allocated.effectiveType() != null && allocated.effectiveType().isIntegrable() && !shipData.isFleetGranted(allocated.node().getId())) {
                nodeIds.add(allocated.node().getId());
            }
        }
        return nodeIds;
    }
}
