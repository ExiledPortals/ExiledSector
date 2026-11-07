package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.plugins.DModAdderPlugin;
import exiledsector.PlayerFleet;
import exiledsector.skills.skilleffect.Chance;

import java.util.List;

public final class RecoveryDModAvoidance implements DModAdderPlugin {

    static final String CHANCE_PERCENT_KEY = "exiledSector_recoveredShipDModAvoidChancePercent";
    static final float MAX_CHANCE_PERCENT = 30f;
    private static final int NOT_HANDLED = -1;

    public static void install(SectorAPI sector) {
        GenericPluginManagerAPI plugins = sector == null ? null : sector.getGenericPlugins();
        if (plugins != null && !plugins.hasPlugin(RecoveryDModAvoidance.class)) {
            plugins.addPlugin(new RecoveryDModAvoidance(), true);
        }
    }

    @Override
    public int getHandlingPriority(Object params) {
        return params instanceof DModAdderParams dModParams && avoidsDMods(dModParams, PlayerFleet.orNull())
                ? GenericPluginManagerAPI.MOD_SPECIFIC : NOT_HANDLED;
    }

    @Override
    public void addDMods(DModAdderParams params) {
    }

    static boolean avoidsDMods(DModAdderParams params, CampaignFleetAPI playerFleet) {
        if (playerFleet == null || params.recoverer != playerFleet || params.random == null || playerFleet.getFleetData() == null) {
            return false;
        }
        return Chance.roll(params.random, chancePercent(playerFleet.getFleetData().getMembersListCopy()));
    }

    static float chancePercent(List<FleetMemberAPI> members) {
        return Math.min(MAX_CHANCE_PERCENT, FleetContributions.sum(members, CHANCE_PERCENT_KEY));
    }
}
