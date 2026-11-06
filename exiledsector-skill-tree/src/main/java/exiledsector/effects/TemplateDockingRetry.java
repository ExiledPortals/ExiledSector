package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.ui.CampaignMessages;

public class TemplateDockingRetry implements ColonyInteractionListener {

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (market == null || playerFleet == null || playerFleet.getFleetData() == null || !SkillTreeSetting.enabled()) {
            return;
        }
        boolean logisticsChangesAllowed = LogisticsDocking.hasSpaceportOrStation(market);
        boolean allocatedAny = false;
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            int allocatedCount = TemplateLevelUp.spendBankedWhileDocked(member, logisticsChangesAllowed);
            if (allocatedCount > 0) {
                allocatedAny = true;
                announce(member, allocatedCount);
            }
        }
        if (allocatedAny) {
            playerFleet.getFleetData().setSyncNeeded();
        }
    }

    private static void announce(FleetMemberAPI member, int allocatedCount) {
        if (CampaignMessages.available()) {
            String message = I18n.forGameText(() -> Translation.msg("template.dockAllocated").count(allocatedCount)
                    .arg("ship", member.getShipName()).text());
            CampaignMessages.plain(message, Misc.getPositiveHighlightColor());
        }
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
    }

    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {
    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
    }
}
