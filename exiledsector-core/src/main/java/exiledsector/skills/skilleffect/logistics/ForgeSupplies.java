package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;

import java.util.Random;

public final class ForgeSupplies {

    static final String CONTRIBUTION_KEY = "exiledSector_forgeSuppliesPerDefeatedDeploymentPointContribution";

    private ForgeSupplies() {
    }

    public static void addExtra(CampaignFleetAPI playerFleet, CargoAPI loot, float defeatedDeploymentPoints, Random random) {
        if (playerFleet == null || playerFleet.getFleetData() == null || loot == null || defeatedDeploymentPoints <= 0f) {
            return;
        }
        float sharePercent = FleetContributions.sum(playerFleet.getFleetData().getMembersListCopy(), CONTRIBUTION_KEY);
        int supplies = SalvagedArmaments.extraCount(defeatedDeploymentPoints, sharePercent, random);
        if (supplies > 0) {
            loot.addSupplies(supplies);
        }
    }
}
