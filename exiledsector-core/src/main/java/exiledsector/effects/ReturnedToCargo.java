package exiledsector.effects;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.CampaignMessages;

import java.util.List;
import java.util.function.UnaryOperator;

final class ReturnedToCargo {

    private ReturnedToCargo() {
    }

    static void announce(FleetMemberAPI member, List<String> returnedIds, UnaryOperator<String> nameOf, String messageKey, String nameArg) {
        if (returnedIds.isEmpty()) {
            return;
        }
        member.setStatUpdateNeeded(true);
        member.updateStats();
        if (!CampaignMessages.available()) {
            return;
        }
        for (String returnedId : returnedIds) {
            String name = nameOf.apply(returnedId);
            CampaignMessages.plain(I18n.forGameText(() -> Translation.msg(messageKey).arg(nameArg, name).text()), Misc.getTextColor());
        }
    }
}
