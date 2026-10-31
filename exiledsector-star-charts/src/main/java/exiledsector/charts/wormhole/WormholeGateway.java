package exiledsector.charts.wormhole;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ExiledSector;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.ui.VanillaText;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;

import java.util.List;

public class WormholeGateway extends BaseIndustry {

    public static final String INDUSTRY_ID = "exiledSector_wormhole_gateway";
    public static final String ADVANCED_INDUSTRY_ID = "exiledSector_wormhole_gateway_advanced";
    public static final String PERFECTED_INDUSTRY_ID = "exiledSector_wormhole_gateway_perfected";
    public static final List<String> INDUSTRY_IDS = List.of(INDUSTRY_ID, ADVANCED_INDUSTRY_ID, PERFECTED_INDUSTRY_ID);
    public static final String UNLOCKED_FLAG = "$exiledSector_gatewayUnlocked";
    private static final float TOOLTIP_PAD = 10f;

    public enum Status {
        NONE, INACTIVE, WORKING
    }

    public static boolean isGateway(String industryId) {
        return INDUSTRY_IDS.contains(industryId);
    }

    public static Industry gatewayAt(MarketAPI market) {
        if (market == null) {
            return null;
        }
        for (String industryId : INDUSTRY_IDS) {
            Industry gateway = market.getIndustry(industryId);
            if (gateway != null) {
                return gateway;
            }
        }
        return null;
    }

    static boolean hasGateway(MarketAPI market) {
        for (String industryId : INDUSTRY_IDS) {
            if (market.hasIndustry(industryId)) {
                return true;
            }
        }
        return false;
    }

    public static Status statusAt(SectorEntityToken dockedAt) {
        MarketAPI market = dockedAt == null ? null : dockedAt.getMarket();
        if (market == null || !isPlayers(market)) {
            return Status.NONE;
        }
        Industry gateway = gatewayAt(market);
        if (gateway == null) {
            return Status.NONE;
        }
        return gateway.isFunctional() ? Status.WORKING : Status.INACTIVE;
    }

    private static boolean isPlayers(MarketAPI market) {
        return market.isPlayerOwned() || Factions.PLAYER.equals(market.getFactionId());
    }

    @Override
    public void apply() {
        super.apply(true);
    }

    @Override
    public void unapply() {
        super.unapply();
    }

    public static StarSystemAPI playerGatewaySystem(SectorAPI sector) {
        MarketAPI market = playerGatewayMarket(sector);
        return market == null ? null : market.getStarSystem();
    }

    public static String playerGatewayId(SectorAPI sector) {
        Industry gateway = gatewayAt(playerGatewayMarket(sector));
        return gateway == null ? null : gateway.getId();
    }

    private static MarketAPI playerGatewayMarket(SectorAPI sector) {
        if (sector == null || sector.getEconomy() == null) {
            return null;
        }
        for (MarketAPI market : sector.getEconomy().getMarketsCopy()) {
            if (isPlayers(market) && hasGateway(market)) {
                return market;
            }
        }
        return null;
    }

    static boolean isUnlockedFor(MarketAPI market) {
        if (researchGated()) {
            return VaultsOfKnowledgeCompat.isGatewayResearched(market);
        }
        return blueprintUsed();
    }

    public static boolean researchGated() {
        return VaultsOfKnowledgeCompat.isModEnabled() && !ColonyFreeAtlas.active();
    }

    static boolean blueprintUsed() {
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

    static String predecessorOf(String industryId) {
        if (ADVANCED_INDUSTRY_ID.equals(industryId)) {
            return INDUSTRY_ID;
        }
        return PERFECTED_INDUSTRY_ID.equals(industryId) ? ADVANCED_INDUSTRY_ID : null;
    }

    static String researchFor(String industryId) {
        if (ADVANCED_INDUSTRY_ID.equals(industryId)) {
            return VaultsOfKnowledgeCompat.GATEWAY_ADVANCED_RESEARCH_ID;
        }
        return PERFECTED_INDUSTRY_ID.equals(industryId) ? VaultsOfKnowledgeCompat.GATEWAY_PERFECTED_RESEARCH_ID : null;
    }

    static boolean upgradeResearched(String industryId) {
        String research = researchFor(industryId);
        return research != null && VaultsOfKnowledgeCompat.isModEnabled() && VaultsOfKnowledgeCompat.isResearchedForPlayer(research);
    }

    @Override
    public boolean isAvailableToBuild() {
        if (market == null || !StarChartSettings.enabled() || ColonyFreeAtlas.active() || !super.isAvailableToBuild()) {
            return false;
        }
        String predecessor = predecessorOf(getId());
        if (predecessor != null) {
            return upgradeResearched(getId()) && market.hasIndustry(predecessor);
        }
        return isUnlockedFor(market) && otherGatewayOf(market) == null;
    }

    @Override
    public boolean showWhenUnavailable() {
        return market != null && StarChartSettings.enabled() && !ColonyFreeAtlas.active() && super.showWhenUnavailable();
    }

    @Override
    public boolean canUpgrade() {
        return getSpec().getUpgrade() != null && researchGated();
    }

    @Override
    public String getUnavailableReason() {
        if (market == null) {
            return super.getUnavailableReason();
        }
        if (predecessorOf(getId()) != null) {
            return Translation.gameText(ADVANCED_INDUSTRY_ID.equals(getId()) ? "chart.gateway.unavailable.researchAdvanced"
                    : "chart.gateway.unavailable.researchPerfected");
        }
        if (!isUnlockedFor(market)) {
            return Translation.gameText(VaultsOfKnowledgeCompat.isModEnabled() ? "chart.gateway.unavailable.research"
                    : "chart.gateway.unavailable.blueprint");
        }
        MarketAPI otherGateway = otherGatewayOf(market);
        if (otherGateway != null) {
            return I18n.forGameText(() -> Translation.msg("chart.gateway.unavailable.onePerFaction").arg("colony", otherGateway.getName()).text());
        }
        return super.getUnavailableReason();
    }

    static MarketAPI otherGatewayOf(MarketAPI market) {
        SectorAPI sector = Global.getSector();
        if (market == null || market.getFactionId() == null || sector == null || sector.getEconomy() == null) {
            return null;
        }
        List<MarketAPI> markets = sector.getEconomy().getMarketsCopy();
        for (MarketAPI factionMarket : markets) {
            if (!isSameColony(factionMarket, market) && market.getFactionId().equals(factionMarket.getFactionId()) && hasGateway(factionMarket)) {
                return factionMarket;
            }
        }
        return null;
    }

    private static boolean isSameColony(MarketAPI first, MarketAPI second) {
        return first == second || first.getId() != null && first.getId().equals(second.getId());
    }

    @Override
    public boolean canInstallAICores() {
        return false;
    }

    @Override
    public boolean canImprove() {
        return false;
    }

    @Override
    protected void addPostDescriptionSection(TooltipMakerAPI tooltip, IndustryTooltipMode mode) {
        VanillaText.plainPara(tooltip, Translation.gameText("chart.gateway.tooltip.opens"), TOOLTIP_PAD, Misc.getTextColor());
        if (researchGated()) {
            int maxTier = GatewayTierLimit.maxTierFor(getId());
            VanillaText.plainPara(tooltip, maxTier >= ChartTiers.CORRUPTED_TIER ? Translation.gameText("chart.gateway.tooltip.everyTier")
                    : I18n.forGameText(() -> Translation.msg("chart.gateway.tooltip.tierLimit").arg("tier", maxTier).text()), TOOLTIP_PAD,
                    Misc.getTextColor());
        }
    }
}
