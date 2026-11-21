package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerText;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.Map;

public final class ExiledSectorOfficerGreeting extends BaseCommandPlugin {

    public static final String CUT_COMM_LINK = "cutCommLink";
    static final String GREETING_KEY_PREFIX = "chart.officer.greeting.";

    @Override
    public boolean doesCommandAddOptions() {
        return true;
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) {
            return false;
        }
        AtlasOfficer officer = dialog.getInteractionTarget() instanceof CampaignFleetAPI fleet ? OfficerFleets.officerOf(fleet.getMemoryWithoutUpdate()) : null;
        if (officer == null) {
            VanillaText.plainPara(dialog.getTextPanel(), Translation.gameText(GREETING_KEY_PREFIX + "static"));
        } else {
            AtlasGenerator generator = AtlasRuntime.generator(Global.getSector());
            VanillaText.plainPara(dialog.getTextPanel(), I18n.forGameText(() -> greeting(generator, officer)));
            VanillaText.plainPara(dialog.getTextPanel(), Translation.gameText(GREETING_KEY_PREFIX + "closed"));
        }
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption(Translation.gameText(GREETING_KEY_PREFIX + "cut"), CUT_COMM_LINK);
        return true;
    }

    static String greeting(AtlasGenerator generator, AtlasOfficer officer) {
        OfficerText.HistoryEntry latest = OfficerText.latestEntry(officer);
        String key = latest == null ? null : GREETING_KEY_PREFIX + latest.event();
        if (key == null || !Translation.has(key)) {
            return Translation.msg(GREETING_KEY_PREFIX + "default").arg("title", OfficerText.title(officer)).text();
        }
        return Translation.msg(key).arg("title", OfficerText.title(officer)).arg("region", OfficerText.regionOf(generator, latest.latest()))
                .arg("from", OfficerText.regionOf(generator, latest.earliest())).text();
    }
}
