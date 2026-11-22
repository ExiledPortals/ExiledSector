package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager;
import com.fs.starfarer.api.impl.combat.threat.ThreatFIDConfig;
import exiledsector.i18n.Translation;

import java.util.List;
import java.util.Map;

public final class ChartThreatFleets {

    public static final String FACTION_ID = "threat";
    static final String FABRICATOR_ROLE = "threatFabricator";
    static final String HIVE_ROLE = "threatHive";
    static final String OVERSEER_ROLE = "threatOverseer";
    static final ChartAbyssalFleets.Makeup MAKEUP = new ChartAbyssalFleets.Makeup(FACTION_ID, FABRICATOR_ROLE,
            List.of(Map.entry(ShipRoles.COMBAT_SMALL, 4f), Map.entry(ShipRoles.COMBAT_MEDIUM, 3f), Map.entry(ShipRoles.COMBAT_LARGE, 2f),
                    Map.entry(OVERSEER_ROLE, 1.5f), Map.entry(HIVE_ROLE, 1f)));

    private ChartThreatFleets() {
    }

    static boolean builds(String factionId) {
        return FACTION_ID.equals(factionId);
    }

    static CampaignFleetAPI create(FleetParamsV3 params) {
        CampaignFleetAPI fleet = ChartAbyssalFleets.build(MAKEUP, params, (builtFleet, member) -> {
        });
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, new ThreatFIDConfig());
        fleet.getDetectedRangeMod().modifyMult(DisposableThreatFleetManager.THREAT_DETECTED_RANGE_MULT_ID,
                DisposableThreatFleetManager.THREAT_DETECTED_RANGE_MULT, Translation.gameText("chart.fleet.threatStealth"));
        return fleet;
    }
}
