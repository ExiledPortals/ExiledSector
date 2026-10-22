package exiledsector.effects;

import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record PlayerEngagement(EngagementResultAPI result, boolean playerWon, float enemyDeploymentPointsDefeated, float difficulty,
                               Set<String> playerLossIds) {

    static PlayerEngagement of(EngagementResultAPI result, float difficulty) {
        boolean playerWon = result.didPlayerWin();
        EngagementResultForFleetAPI playerResult = playerWon ? result.getWinnerResult() : result.getLoserResult();
        EngagementResultForFleetAPI enemyResult = playerWon ? result.getLoserResult() : result.getWinnerResult();
        Set<String> playerLossIds = new HashSet<>();
        for (FleetMemberAPI member : lostBy(playerResult)) {
            playerLossIds.add(member.getId());
        }
        float enemyDeploymentPointsLost = 0f;
        for (FleetMemberAPI member : lostBy(enemyResult)) {
            enemyDeploymentPointsLost += member.getDeploymentPointsCost();
        }
        return new PlayerEngagement(result, playerWon, enemyDeploymentPointsLost, difficulty, Collections.unmodifiableSet(playerLossIds));
    }

    private static List<FleetMemberAPI> lostBy(EngagementResultForFleetAPI fleetResult) {
        List<FleetMemberAPI> lostMembers = new ArrayList<>();
        if (fleetResult == null) return lostMembers;
        if (fleetResult.getDestroyed() != null) {
            lostMembers.addAll(fleetResult.getDestroyed());
        }
        if (fleetResult.getDisabled() != null) {
            lostMembers.addAll(fleetResult.getDisabled());
        }
        return lostMembers;
    }
}
