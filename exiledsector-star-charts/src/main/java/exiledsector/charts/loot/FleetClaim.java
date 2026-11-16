package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;

public record FleetClaim(CampaignFleetAPI fleet, float claimedShare, float startDeploymentPoints, float startFleetPoints, int mergedFleets,
                         boolean boss, float spoilsMult, float lootQuantityMult) {

    public float lootDeploymentPoints() {
        return claimedShare * startDeploymentPoints * spoilsMult * lootQuantityMult;
    }

    public float chartShare(int tier) {
        return claimedShare * startFleetPoints / Math.max(1f, ChartFleetLoot.expectedFleetPoints(tier)) * spoilsMult * lootQuantityMult;
    }

    public float anomalyShare() {
        return claimedShare * mergedFleets * lootQuantityMult;
    }

    public float bossShare() {
        return anomalyShare() * spoilsMult;
    }
}
