package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRunState;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.CampaignMessages;

import java.awt.Color;

public final class ChartRelays {

    static final String REVEAL_MOD_ID = ChartIds.PREFIX + "relay_reveal";
    static final String SYNC_SINCE_MEMORY_KEY = "$exiledSector_chartRelaySyncSince";
    static final String SYNC_SEEN_MEMORY_KEY = "$exiledSector_chartRelaySyncSeen";
    static final Color MESSAGE_COLOUR = new Color(120, 200, 255);

    private ChartRelays() {
    }

    public static ChartRun liveRunFor(SectorEntityToken relay) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || relay == null || !run.runState().isLive() || !ChartIds.belongsToRun(relay, run.runId())) {
            return null;
        }
        return run;
    }

    public static boolean check(SectorAPI sector) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || run.runState() != ChartRunState.INSIDE || !run.anomalyIds().contains(RelayNetwork.ID)) {
            return false;
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        StarSystemAPI system = sector.getStarSystem(run.systemId());
        if (playerFleet == null || system == null || playerFleet.getContainingLocation() != system || playerFleet.isInHyperspaceTransition()) {
            return false;
        }
        if (RelayStates.isWoken(run, RelayKind.COMM_RELAY)) {
            revealFleets(system);
        }
        boolean changed = false;
        for (SectorEntityToken relay : system.getEntitiesWithTag(RelayNetwork.RELAY_TAG)) {
            RelayKind kind = RelayKind.of(relay);
            if (kind == null || RelayStates.has(run, kind, RelayStates.SYNCED)) {
                continue;
            }
            if (synced(sector.getClock(), playerFleet, relay, kind)) {
                RelayStates.add(run, kind, RelayStates.SYNCED);
                announce("chart.relay.message.synced", relay);
                wakeIfReady(run, system, relay, kind);
                changed = true;
            }
        }
        return changed;
    }

    static boolean synced(CampaignClockAPI clock, CampaignFleetAPI playerFleet, SectorEntityToken relay, RelayKind kind) {
        float gap = Math.max(0f, Misc.getDistance(playerFleet.getLocation(), relay.getLocation()) - playerFleet.getRadius() - relay.getRadius());
        boolean conditionMet = switch (kind) {
            case NAV_BUOY -> gap <= RelayRules.STATION_KEEPING_RANGE
                    && playerFleet.getVelocity().length() <= Misc.getSpeedForBurnLevel(RelayRules.STATION_KEEPING_BURN);
            case COMM_RELAY -> gap <= RelayRules.WAKE_RANGE && playerFleet.isTransponderOn();
            case SENSOR_ARRAY -> gap <= RelayRules.WAKE_RANGE && sensorBurstActive(playerFleet);
        };
        float holdDays = switch (kind) {
            case NAV_BUOY -> RelayRules.STATION_KEEPING_DAYS;
            case COMM_RELAY -> RelayRules.HANDSHAKE_DAYS;
            case SENSOR_ARRAY -> 0f;
        };
        return held(clock, relay.getMemoryWithoutUpdate(), conditionMet, holdDays);
    }

    static boolean held(CampaignClockAPI clock, MemoryAPI memory, boolean conditionMet, float holdDays) {
        if (!conditionMet) {
            memory.unset(SYNC_SINCE_MEMORY_KEY);
            return false;
        }
        if (holdDays <= 0f) {
            return true;
        }
        boolean lapsed = memory.contains(SYNC_SEEN_MEMORY_KEY) && clock.getElapsedDaysSince(memory.getLong(SYNC_SEEN_MEMORY_KEY)) > RelayRules.HOLD_GAP_DAYS;
        memory.set(SYNC_SEEN_MEMORY_KEY, clock.getTimestamp());
        if (lapsed || !memory.contains(SYNC_SINCE_MEMORY_KEY)) {
            memory.set(SYNC_SINCE_MEMORY_KEY, clock.getTimestamp());
            return false;
        }
        return clock.getElapsedDaysSince(memory.getLong(SYNC_SINCE_MEMORY_KEY)) >= holdDays;
    }

    private static boolean sensorBurstActive(CampaignFleetAPI playerFleet) {
        AbilityPlugin burst = playerFleet.getAbility(Abilities.SENSOR_BURST);
        return burst != null && burst.isActiveOrInProgress();
    }

    public static RelayRules.Cost costFor(ChartRun run) {
        return RelayRules.costFor(run.tier(), ChartRunEffects.contextOf(run).anomalyMult(RelayNetwork.COST_KEY));
    }

    public static boolean canAfford(CargoAPI cargo, RelayRules.Cost cost) {
        return cargo != null && cargo.getSupplies() >= cost.supplies() && cargo.getCommodityQuantity(Commodities.HEAVY_MACHINERY) >= cost.machinery();
    }

    public static boolean restorePower(ChartRun run, CargoAPI cargo, LocationAPI system, SectorEntityToken relay, RelayKind kind, RelayRules.Cost cost) {
        if (RelayStates.has(run, kind, RelayStates.PAID) || RelayStates.isWoken(run, kind) || !canAfford(cargo, cost)) {
            return false;
        }
        cargo.removeSupplies(cost.supplies());
        cargo.removeCommodity(Commodities.HEAVY_MACHINERY, cost.machinery());
        RelayStates.add(run, kind, RelayStates.PAID);
        wakeIfReady(run, system, relay, kind);
        return true;
    }

    static boolean wakeIfReady(ChartRun run, LocationAPI system, SectorEntityToken relay, RelayKind kind) {
        if (!RelayStates.readyToWake(run, kind) || !wake(run, system, relay, kind)) {
            return false;
        }
        announce("chart.relay.message.woken", relay);
        if (RelayStates.allWoken(run)) {
            CampaignMessages.plain(Translation.gameText("chart.relay.message.allWoken"), MESSAGE_COLOUR);
        }
        return true;
    }

    private static void announce(String key, SectorEntityToken relay) {
        String relayName = relay.getName();
        CampaignMessages.plain(I18n.forGameText(() -> Translation.msg(key).arg("relay", relayName).text()), MESSAGE_COLOUR);
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
        relay.getMemoryWithoutUpdate().unset(SYNC_SINCE_MEMORY_KEY);
        relay.getMemoryWithoutUpdate().unset(SYNC_SEEN_MEMORY_KEY);
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
            if (!fleet.isPlayerFleet() && fleet.getExtendedDetectedAtRange() < RelayRules.REVEAL_EXTENDED_RANGE) {
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
