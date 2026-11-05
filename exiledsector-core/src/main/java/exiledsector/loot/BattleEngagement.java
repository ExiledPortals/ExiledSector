package exiledsector.loot;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record BattleEngagement(Function<FleetMemberAPI, CampaignFleetAPI> sourceFleets, float difficultyMultiplier, float defeatedDeploymentPoints,
                               List<FleetMemberAPI> lostEnemies) {

    public BattleEngagement {
        sourceFleets = sourceFleets == null ? member -> null : sourceFleets;
        lostEnemies = lostEnemies == null ? List.of() : lostEnemies;
    }

    public BattleEngagement(BattleAPI battle, float difficultyMultiplier, float defeatedDeploymentPoints, List<FleetMemberAPI> lostEnemies) {
        this(battle == null ? null : battle::getSourceFleet, difficultyMultiplier, defeatedDeploymentPoints, lostEnemies);
    }

    public static BattleEngagement unfought(CampaignFleetAPI fleet, List<FleetMemberAPI> members) {
        return new BattleEngagement(member -> fleet, 1f, deploymentPoints(members), members);
    }

    public CampaignFleetAPI sourceFleetOf(FleetMemberAPI member) {
        return member == null ? null : sourceFleets.apply(member);
    }

    public Map<CampaignFleetAPI, List<FleetMemberAPI>> lostEnemiesBySourceFleet() {
        Map<CampaignFleetAPI, List<FleetMemberAPI>> bySourceFleet = new LinkedHashMap<>();
        for (FleetMemberAPI member : lostEnemies) {
            if (member != null) {
                bySourceFleet.computeIfAbsent(sourceFleetOf(member), fleet -> new ArrayList<>()).add(member);
            }
        }
        return bySourceFleet;
    }

    public Map<String, Float> deploymentPointsLostByFaction() {
        Map<String, Float> byFaction = new LinkedHashMap<>();
        lostEnemiesBySourceFleet().forEach((sourceFleet, members) -> {
            String factionId = sourceFleet == null || sourceFleet.getFaction() == null ? null : sourceFleet.getFaction().getId();
            if (factionId != null) {
                byFaction.merge(factionId, deploymentPoints(members), Float::sum);
            }
        });
        return byFaction;
    }

    public static float deploymentPoints(List<FleetMemberAPI> members) {
        float total = 0f;
        for (FleetMemberAPI member : members) {
            total += member.getDeploymentPointsCost();
        }
        return total;
    }
}
