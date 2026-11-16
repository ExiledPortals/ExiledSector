package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.anomaly.debris.AnomalousDebris;
import exiledsector.charts.anomaly.debris.DormantFleetDialog;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.Nanoswarm;

final class ExcavationCorruption {

    static final String CORRUPTION_ITEM_ID = Nanoswarm.CORRUPTION.itemId();

    private ExcavationCorruption() {
    }

    static boolean offered(ChartRun run, ExcavationTuning tuning, SectorEntityToken planet) {
        return run != null && tuning.corruptibleGuardians() && run.anomalyIds().contains(AnomalousDebris.ANOMALY_ID)
                && !ExcavationFleets.isHeld(planet);
    }

    static int corruptionItemsHeld(CampaignFleetAPI playerFleet) {
        CargoAPI cargo = playerFleet == null ? null : playerFleet.getCargo();
        return cargo == null ? 0 : CraftingMaterials.forPlayer(cargo).count(CORRUPTION_ITEM_ID);
    }

    static DormantFleetDialog.Options choices(boolean hasShips, CampaignFleetAPI playerFleet) {
        return DormantFleetDialog.options(hasShips, corruptionItemsHeld(playerFleet));
    }

    static void addOption(OptionPanelAPI options, String optionId, boolean hasShips, String noShipsKey, CampaignFleetAPI playerFleet) {
        DormantFleetDialog.Options choices = choices(hasShips, playerFleet);
        String itemName = Translation.gameText("material." + CORRUPTION_ITEM_ID + ".name");
        options.addOption(I18n.forGameText(() -> Translation.msg("chart.anomaly.anomalous_debris.dialog.corrupt").arg("count", ExcavationRules.CORRUPTION_COST)
                .arg("craftingItem", itemName).text()), optionId, Translation.gameText("chart.anomaly.excavation_sites.dialog.corruptTooltip"));
        options.setEnabled(optionId, choices.canCorrupt());
        if (!hasShips) {
            options.setTooltip(optionId, Translation.gameText(noShipsKey));
        } else if (choices.corruptBlockedKey() != null) {
            options.setTooltip(optionId, I18n.forGameText(() -> Translation.msg(choices.corruptBlockedKey()).arg("craftingItem", itemName).text()));
        }
    }

    static boolean pay(CampaignFleetAPI playerFleet) {
        return playerFleet != null && choices(true, playerFleet).canCorrupt()
                && CraftingMaterials.forPlayer(playerFleet.getCargo()).take(CORRUPTION_ITEM_ID, ExcavationRules.CORRUPTION_COST);
    }
}
