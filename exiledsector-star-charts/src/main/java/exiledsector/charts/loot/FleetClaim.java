package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;

public record FleetClaim(CampaignFleetAPI fleet, float claimedShare, float startDeploymentPoints, float startFleetPoints, int mergedFleets,
                         boolean boss) {

    public float lootDeploymentPoints() {
        return claimedShare * startDeploymentPoints;
    }

    public float chartShare(ChartLootContext loot) {
        return claimedShare * startFleetPoints / Math.max(1f, ChartFleetLoot.expectedFleetPoints(loot.tier(), loot.effects().twinnedBoss()));
    }

    public float mergedFleetShare() {
        return claimedShare * mergedFleets;
    }
}
