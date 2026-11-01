package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.i18n.Translation;

public interface RunParticipant extends ChartAnomaly {

    default String intelText(AnomalyRun run) {
        return null;
    }

    default void checkRun(SectorAPI sector, AnomalyRun run) {
    }

    default void fleetGone(AnomalyRun run, CampaignFleetAPI fleet) {
    }

    default InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return null;
    }

    default BattleCreationPlugin battleFor(SectorEntityToken opponent) {
        return null;
    }

    default void clearFrom(CampaignFleetAPI fleet) {
    }

    default String intelLine() {
        return Translation.gameText("chart.anomaly." + id() + ".intel");
    }
}
