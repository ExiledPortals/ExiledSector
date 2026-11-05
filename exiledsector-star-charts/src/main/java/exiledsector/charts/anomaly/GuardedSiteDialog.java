package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageDefenderInteraction;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;

public abstract class GuardedSiteDialog extends AnomalyDialog {

    public static final String BEAT_DEFENDERS_CONTINUE = "salBeatDefendersContinue";

    private SectorEntityToken site;

    protected abstract String descriptionKey();

    protected abstract String guardedKey();

    protected abstract CampaignFleetAPI musterGuards();

    protected abstract void showSite();

    protected void siteOptionSelected(Object optionData) {
        dialog().dismiss();
    }

    protected final SectorEntityToken site() {
        return site;
    }

    @Override
    protected final void open() {
        site = dialog().getInteractionTarget();
        showEntityVisual(site);
        VanillaText.plainPara(text(), Translation.gameText(descriptionKey()));
        if (!engageGuards()) {
            showSite();
        }
    }

    private boolean engageGuards() {
        CampaignFleetAPI guards = musterGuards();
        if (guards == null) {
            return false;
        }
        VanillaText.plainPara(text(), Translation.gameText(guardedKey()));
        return new SalvageDefenderInteraction().execute(null, dialog(), new ArrayList<>(), getMemoryMap());
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (BEAT_DEFENDERS_CONTINUE.equals(optionData)) {
            showEntityVisual(site);
            showSite();
        } else {
            siteOptionSelected(optionData);
        }
    }
}
