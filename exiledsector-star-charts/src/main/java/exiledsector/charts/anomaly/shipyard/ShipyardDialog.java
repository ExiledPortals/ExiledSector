package exiledsector.charts.anomaly.shipyard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyLoot;
import exiledsector.charts.anomaly.GuardedSiteDialog;
import exiledsector.charts.run.ChartRun;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.Random;

public class ShipyardDialog extends GuardedSiteDialog {

    static final String SALVAGE = "exiledSector_shipyardSalvage";
    static final String LOOT_SITE = "exiledSector_chart_abyssal_shipyard";
    static final String LOOT_SCOPE = "abyssal_shipyard";
    static final AnomalyLoot LOOT = new AnomalyLoot(LOOT_SCOPE, LOOT_SITE, 2f, 0.35f, 1.5f, null, 0f);
    static final long LOOT_SALT = 0x5348504c4f4fL;

    @Override
    protected String descriptionKey() {
        return "chart.anomaly.abyssal_shipyard.description";
    }

    @Override
    protected String guardedKey() {
        return "chart.anomaly.abyssal_shipyard.guarded";
    }

    @Override
    protected CampaignFleetAPI musterGuards() {
        return ShipyardGuards.muster(site(), AbyssalShipyard.liveRunFor(site()));
    }

    @Override
    protected void showSite() {
        options().clearOptions();
        if (AbyssalShipyard.salvaged(site())) {
            VanillaText.plainPara(text(), Translation.gameText("chart.anomaly.abyssal_shipyard.stripped"));
        } else {
            VanillaText.plainPara(text(), Translation.gameText("chart.anomaly.abyssal_shipyard.open"));
            options().addOption(Translation.gameText("chart.anomaly.abyssal_shipyard.option.salvage"), SALVAGE);
        }
        addLeaveOption();
    }

    @Override
    protected void siteOptionSelected(Object optionData) {
        ChartRun run = AbyssalShipyard.liveRunFor(site());
        if (!SALVAGE.equals(optionData) || run == null || AbyssalShipyard.salvaged(site())) {
            dialog().dismiss();
            return;
        }
        SectorEntityToken shipyard = site();
        shipyard.getMemoryWithoutUpdate().set(AbyssalShipyard.SALVAGED_KEY, true);
        CargoAPI loot = LOOT.roll(Global.getSector(), run, List.of(), 1f, new Random(run.runId().hashCode() ^ LOOT_SALT));
        dialog().getVisualPanel().showLoot(Translation.gameText("chart.anomaly.abyssal_shipyard.lootTitle"), loot, false, true, true,
                new CoreInteractionListener() {
                    @Override
                    public void coreUIDismissed() {
                        showEntityVisual(shipyard);
                        showSite();
                    }
                });
    }
}
