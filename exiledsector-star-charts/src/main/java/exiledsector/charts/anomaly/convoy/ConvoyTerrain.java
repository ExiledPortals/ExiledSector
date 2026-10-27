package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidBeltTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;

import java.util.ArrayList;
import java.util.List;

final class ConvoyTerrain {

    static final String BURN_MOD_ID = "exiledSector_convoy_formation";

    private ConvoyTerrain() {
    }

    static boolean slowsConvoys(CampaignTerrainPlugin plugin) {
        return plugin instanceof AsteroidBeltTerrainPlugin || plugin instanceof AsteroidFieldTerrainPlugin || plugin instanceof NebulaTerrainPlugin;
    }

    static List<CampaignTerrainPlugin> slowingTerrain(LocationAPI location) {
        List<CampaignTerrainPlugin> slowing = new ArrayList<>();
        for (CampaignTerrainAPI terrain : location.getTerrainCopy()) {
            if (slowsConvoys(terrain.getPlugin())) {
                slowing.add(terrain.getPlugin());
            }
        }
        return slowing;
    }

    static boolean inside(CampaignFleetAPI fleet, List<CampaignTerrainPlugin> slowing) {
        for (CampaignTerrainPlugin terrain : slowing) {
            if (terrain.containsEntity(fleet)) {
                return true;
            }
        }
        return false;
    }

    static float formationMult(float freighterBurn, float slowestEscortBurn) {
        return freighterBurn > 0f && slowestEscortBurn > 0f && slowestEscortBurn < freighterBurn ? slowestEscortBurn / freighterBurn : 1f;
    }

    static void apply(CampaignFleetAPI fleet, float burnMult) {
        StatBonus burn = fleet.getStats() == null ? null : fleet.getStats().getFleetwideMaxBurnMod();
        if (burn == null) {
            return;
        }
        if (burnMult == 1f) {
            burn.unmodify(BURN_MOD_ID);
        } else {
            burn.modifyMult(BURN_MOD_ID, burnMult);
        }
    }

    static float unmodifiedBurn(CampaignFleetAPI fleet) {
        return fleet.getFleetData() == null ? 0f : fleet.getFleetData().getMinBurnLevelUnmodified();
    }
}
