package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;

public final class ChartRelays {

    static final String REVEAL_MOD_ID = ChartIds.PREFIX + "relay_reveal";

    private ChartRelays() {
    }

    static boolean wake(ChartRun run, LocationAPI system, SectorEntityToken relay, RelayKind kind) {
        if (!RelayStates.add(run, kind, RelayStates.WOKEN)) {
            return false;
        }
        applyWoken(system, relay, kind);
        return true;
    }

    static void applyWoken(LocationAPI system, SectorEntityToken relay, RelayKind kind) {
        relay.setFaction(Factions.PLAYER);
        if (kind.givesVanillaBonus()) {
            relay.getMemoryWithoutUpdate().unset(MemFlags.OBJECTIVE_NON_FUNCTIONAL);
        }
        if (kind == RelayKind.COMM_RELAY) {
            revealFleets(system);
        }
    }

    static int revealFleets(LocationAPI system) {
        if (system == null) {
            return 0;
        }
        int revealed = 0;
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (!fleet.isPlayerFleet()) {
                reveal(fleet);
                revealed++;
            }
        }
        return revealed;
    }

    static void reveal(CampaignFleetAPI fleet) {
        fleet.getStats().getDynamic().getStat(Stats.DETECTED_BY_PLAYER_RANGE_MULT).modifyMult(REVEAL_MOD_ID, RelayRules.REVEAL_RANGE_MULT);
        fleet.setExtendedDetectedAtRange(RelayRules.REVEAL_EXTENDED_RANGE);
    }
}
