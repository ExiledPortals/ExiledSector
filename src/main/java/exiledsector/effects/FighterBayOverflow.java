package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.CampaignMessages;

import java.util.ArrayList;
import java.util.List;

public final class FighterBayOverflow {

    private FighterBayOverflow() {
    }

    public static List<String> returnUnhousedWings(FleetMemberAPI member, ShipVariantAPI variant, CargoAPI cargo) {
        List<String> returnedWingIds = new ArrayList<>();
        if (member == null || variant == null || cargo == null || member.getStats() == null) {
            return returnedWingIds;
        }
        int bayCount = Math.max(0, Math.round(member.getStats().getNumFighterBays().getModifiedValue()));
        for (int bayIndex = variant.getWings().size() - 1; bayIndex >= bayCount; bayIndex--) {
            String wingId = variant.getWingId(bayIndex);
            if (wingId == null || wingId.isEmpty() || variant.getHullSpec().isBuiltInWing(bayIndex)) {
                continue;
            }
            variant.setWingId(bayIndex, null);
            cargo.addFighters(wingId, 1);
            returnedWingIds.add(wingId);
        }
        return returnedWingIds;
    }

    public static void returnUnhousedWingsAndAnnounce(FleetMemberAPI member, ShipVariantAPI variant, CargoAPI cargo) {
        List<String> returnedWingIds = returnUnhousedWings(member, variant, cargo);
        if (returnedWingIds.isEmpty()) {
            return;
        }
        member.setStatUpdateNeeded(true);
        member.updateStats();
        if (!CampaignMessages.available()) {
            return;
        }
        for (String wingId : returnedWingIds) {
            FighterWingSpecAPI wing = Global.getSettings().getFighterWingSpec(wingId);
            String name = wing == null ? wingId : wing.getWingName();
            CampaignMessages.plain(I18n.forGameText(() -> Translation.msg("fighterBay.returned").arg("wing", name).text()), Misc.getTextColor());
        }
    }
}
