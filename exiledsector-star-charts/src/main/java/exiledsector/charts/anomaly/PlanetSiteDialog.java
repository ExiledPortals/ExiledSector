package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.function.Consumer;

public abstract class PlanetSiteDialog extends AnomalyDialog {

    public interface EncounterFactory {

        FleetInteractionDialogPluginImpl create(Consumer<InteractionDialogAPI> onLeave);
    }

    private SectorEntityToken planet;
    private FleetInteractionDialogPluginImpl encounter;

    protected abstract void showMain();

    @Override
    protected final void open() {
        planet = dialog().getInteractionTarget();
        showMain();
    }

    protected final SectorEntityToken planet() {
        return planet;
    }

    protected final ChartRun run() {
        return ChartRuns.liveIn(planet.getContainingLocation());
    }

    protected final void fightHeldGuards(CampaignFleetAPI guards, EncounterFactory encounterFactory, Runnable onCleared) {
        encounter = encounterFactory.create(leaving -> returnFromFight(leaving, guards, onCleared));
        AnomalyEncounters.engage(dialog(), guards, encounter);
    }

    private void returnFromFight(InteractionDialogAPI leaving, CampaignFleetAPI guards, Runnable onCleared) {
        returnHere(leaving, planet);
        boolean won = AnomalyEncounters.wonOutright(encounter);
        encounter = null;
        if (won || guards.isEmpty()) {
            onCleared.run();
        }
        showMain();
    }
}
