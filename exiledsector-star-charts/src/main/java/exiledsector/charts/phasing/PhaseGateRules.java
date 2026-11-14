package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.ExiledSector;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.domain.ExiledSectorDomain;

public final class PhaseGateRules {

    public static final String UNLOCKED_FLAG = "$exiledSector_phaseGateUnlocked";

    public enum Blocker {
        NOT_UNLOCKED, NO_GATEWAY, NO_HYPERSHUNT
    }

    private PhaseGateRules() {
    }

    public static boolean isUnlockedFor(MarketAPI market) {
        if (WormholeGateway.researchGated()) {
            return VaultsOfKnowledgeCompat.isResearchedAt(VaultsOfKnowledgeCompat.PHASE_GATE_RESEARCH_ID, market);
        }
        return ExiledSector.domains().stream().anyMatch(ExiledSectorDomain::ignoresMemoryFlagUnlocks) || isUnlocked(Global.getSector());
    }

    public static boolean isUnlocked(SectorAPI sector) {
        MemoryAPI sectorMemory = sector == null ? null : sector.getMemoryWithoutUpdate();
        return sectorMemory != null && sectorMemory.getBoolean(UNLOCKED_FLAG);
    }

    public static void unlock(SectorAPI sector) {
        if (sector != null && sector.getMemoryWithoutUpdate() != null) {
            sector.getMemoryWithoutUpdate().set(UNLOCKED_FLAG, true);
        }
    }

    public static boolean holdsRequiredGateway(MarketAPI market) {
        Industry gateway = WormholeGateway.gatewayAt(market);
        return gateway != null && (!WormholeGateway.researchGated() || WormholeGateway.NEXUS_INDUSTRY_ID.equals(gateway.getId()));
    }

    public static Blocker blocker(MarketAPI market) {
        if (!isUnlockedFor(market)) {
            return Blocker.NOT_UNLOCKED;
        }
        if (!holdsRequiredGateway(market)) {
            return Blocker.NO_GATEWAY;
        }
        return HypershuntPower.reaches(market) ? null : Blocker.NO_HYPERSHUNT;
    }

    public static String blockerKey(Blocker blocker) {
        return switch (blocker) {
            case NOT_UNLOCKED -> WormholeGateway.researchGated() ? "chart.phasing.unavailable.research" : "chart.phasing.unavailable.theorem";
            case NO_GATEWAY -> WormholeGateway.researchGated() ? "chart.phasing.unavailable.nexus" : "chart.phasing.unavailable.gateway";
            case NO_HYPERSHUNT -> "chart.phasing.unavailable.hypershunt";
        };
    }
}
