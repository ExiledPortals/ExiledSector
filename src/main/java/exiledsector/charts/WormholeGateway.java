package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.skills.unlock.UnlockCondition;

import java.util.List;

public class WormholeGateway extends BaseIndustry {

    public static final String ID = "exiledSector_wormhole_gateway";
    public static final String JANUS_FLAG = "$playerCanUseGates";
    static final List<UnlockCondition> JANUS_UNLOCK = List.of(UnlockCondition.memoryFlag(JANUS_FLAG));
    private static final float TOOLTIP_PAD = 10f;

    public enum Status {
        NONE, INACTIVE, WORKING
    }

    public static Status statusAt(SectorEntityToken dockedAt) {
        MarketAPI market = dockedAt == null ? null : dockedAt.getMarket();
        if (market == null || !market.isPlayerOwned()) {
            return Status.NONE;
        }
        Industry gateway = market.getIndustry(ID);
        if (gateway == null) {
            return Status.NONE;
        }
        return gateway.isFunctional() ? Status.WORKING : Status.INACTIVE;
    }

    @Override
    public void apply() {
        super.apply(true);
    }

    @Override
    public void unapply() {
        super.unapply();
    }

    static boolean isUnlockedFor(MarketAPI market) {
        if (VaultsOfKnowledgeCompat.isModEnabled()) {
            return VaultsOfKnowledgeCompat.isGatewayResearched(market);
        }
        return !SkillTypeUnlockStatus.isLocked(JANUS_UNLOCK, null);
    }

    @Override
    public boolean isAvailableToBuild() {
        return market != null && super.isAvailableToBuild() && isUnlockedFor(market) && otherGatewayOf(market) == null;
    }

    @Override
    public boolean showWhenUnavailable() {
        return market != null && super.showWhenUnavailable();
    }

    @Override
    public String getUnavailableReason() {
        if (market == null) {
            return super.getUnavailableReason();
        }
        if (!isUnlockedFor(market)) {
            return Translation.gameText(VaultsOfKnowledgeCompat.isModEnabled() ? "chart.gateway.unavailable.research"
                    : "chart.gateway.unavailable.janus");
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
            if (!isSameColony(factionMarket, market) && market.getFactionId().equals(factionMarket.getFactionId()) && factionMarket.hasIndustry(ID)) {
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
        tooltip.addPara(Translation.gameText("chart.gateway.tooltip.opens"), TOOLTIP_PAD);
    }
}
