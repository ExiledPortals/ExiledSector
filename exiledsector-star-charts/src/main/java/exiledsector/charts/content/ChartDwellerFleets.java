package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.rulecmd.DwellerCMD;

import java.util.List;
import java.util.Map;

public final class ChartDwellerFleets {

    public static final String FACTION_ID = Factions.DWELLER;
    static final ChartAbyssalFleets.Makeup MAKEUP = new ChartAbyssalFleets.Makeup(FACTION_ID, ShipRoles.DWELLER_MAW,
            List.of(Map.entry(ShipRoles.DWELLER_TENDRIL, 10f), Map.entry(ShipRoles.DWELLER_MAELSTROM, 1.5f), Map.entry(ShipRoles.DWELLER_EYE, 1f),
                    Map.entry(ShipRoles.DWELLER_MAW, 0.5f)));

    private ChartDwellerFleets() {
    }

    static boolean builds(String factionId) {
        return FACTION_ID.equals(factionId);
    }

    static CampaignFleetAPI create(FleetParamsV3 params) {
        CampaignFleetAPI fleet = ChartAbyssalFleets.build(MAKEUP, params, ChartDwellerFleets::addDrops);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        return fleet;
    }

    private static void addDrops(CampaignFleetAPI fleet, FleetMemberAPI member) {
        ShipHullSpecAPI hull = member.getHullSpec();
        List<String> dropGroups = hull == null ? null : DwellerCMD.DROP_GROUPS.get(hull.getRestoredToHullId());
        if (dropGroups != null) {
            dropGroups.forEach(group -> fleet.addDropRandom(group, 1));
        }
    }
}
