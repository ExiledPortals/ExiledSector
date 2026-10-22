package exiledsector.loot;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.List;

public record BattleEngagement(BattleAPI battle, float difficultyMultiplier, float defeatedDeploymentPoints, List<FleetMemberAPI> lostEnemies) {
}
