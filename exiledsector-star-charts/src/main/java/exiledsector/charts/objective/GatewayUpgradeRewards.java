package exiledsector.charts.objective;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.items.WormholeTechItems;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GatewayUpgradeRewards {

    public static final String ADVANCED_GRANTED = "$exiledSector_gatewayAdvancedGranted";
    public static final String PERFECTED_GRANTED = "$exiledSector_gatewayPerfectedGranted";
    static final String PENDING_KEY = "$exiledSector_gatewayUpgradesPending";
    static final int ADVANCED_TIER = 5;
    static final int PERFECTED_TIER = 10;
    private static final String SEPARATOR = ",";
    private static final Logger LOG = Logger.getLogger(GatewayUpgradeRewards.class);

    private GatewayUpgradeRewards() {
    }

    public static List<String> completed(SectorAPI sector, int tier) {
        if (sector == null || !WormholeGateway.researchGated()) {
            return List.of();
        }
        return grantUpTo(sector, tier);
    }

    public static void onGameLoad(SectorAPI sector) {
        sector.removeTransientScriptsOfClass(GatewayUpgradeNotice.class);
        try {
            AtlasState state = WormholeGateway.researchGated() ? AtlasState.getIfCreated(sector) : null;
            if (state != null) {
                grantUpTo(sector, state.clearedTiers().stream().mapToInt(Integer::intValue).max().orElse(0));
            }
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Granting Wormhole Gateway upgrades for already cleared tiers failed", e);
        }
        if (!pending(sector).isEmpty() && !sector.hasTransientScript(GatewayUpgradeNotice.class)) {
            sector.addTransientScript(new GatewayUpgradeNotice());
        }
    }

    static List<String> grantUpTo(SectorAPI sector, int tier) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        MemoryAPI sectorMemory = sector.getMemoryWithoutUpdate();
        if (playerFleet == null || playerFleet.getCargo() == null || sectorMemory == null) {
            return List.of();
        }
        List<String> granted = new ArrayList<>();
        grantOnce(tier >= ADVANCED_TIER, ADVANCED_GRANTED, WormholeTechItems.UPGRADE_ADVANCED, playerFleet, sectorMemory, granted);
        grantOnce(tier >= PERFECTED_TIER, PERFECTED_GRANTED, WormholeTechItems.UPGRADE_PERFECTED, playerFleet, sectorMemory, granted);
        if (!granted.isEmpty()) {
            List<String> queued = new ArrayList<>(pending(sector));
            queued.addAll(granted);
            sectorMemory.set(PENDING_KEY, String.join(SEPARATOR, queued));
            if (!sector.hasTransientScript(GatewayUpgradeNotice.class)) {
                sector.addTransientScript(new GatewayUpgradeNotice());
            }
        }
        return granted;
    }

    private static void grantOnce(boolean reached, String flag, String itemId, CampaignFleetAPI playerFleet, MemoryAPI sectorMemory,
                                  List<String> granted) {
        if (!reached || sectorMemory.getBoolean(flag)) {
            return;
        }
        WormholeTechItems.addTo(playerFleet.getCargo(), itemId);
        sectorMemory.set(flag, true);
        granted.add(itemId);
    }

    static List<String> pending(SectorAPI sector) {
        MemoryAPI sectorMemory = sector.getMemoryWithoutUpdate();
        Object stored = sectorMemory == null ? null : sectorMemory.get(PENDING_KEY);
        if (!(stored instanceof String text) || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(SEPARATOR)).filter(itemId -> !itemId.isBlank()).toList();
    }

    static void clearPending(SectorAPI sector) {
        MemoryAPI sectorMemory = sector.getMemoryWithoutUpdate();
        if (sectorMemory != null) {
            sectorMemory.unset(PENDING_KEY);
        }
    }
}
