package exiledsector.effects;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DataForEncounterSide;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.FleetCrewLedger;
import exiledsector.skills.skilleffect.FleetCrewLedger.CrewChange;
import exiledsector.ui.VanillaText;

import java.awt.Color;

public class FleetCrewLedgerListener extends BaseCampaignEventListener {

    public FleetCrewLedgerListener() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        CrewChange change = FleetCrewLedger.drain();
        if (change.stolen() <= 0 && change.sacrificed() <= 0) {
            return;
        }
        Global.getSector().addTransientScript(new ApplyAfterEngagement(change));
    }

    record Outcome(int joined, int noRoom, int lost) {
    }

    static Outcome apply(CrewChange change, CargoAPI cargo, int freeCrewSpace) {
        int net = change.net();
        if (net > 0) {
            int joined = Math.max(0, Math.min(net, freeCrewSpace));
            if (joined > 0) {
                cargo.addCrew(joined);
            }
            return new Outcome(joined, net - joined, 0);
        }
        int lost = Math.min(-net, cargo.getCrew());
        if (lost > 0) {
            cargo.removeCrew(lost);
        }
        return new Outcome(0, 0, Math.max(0, lost));
    }

    private static void report(CrewChange change, Outcome outcome) {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        TextPanelAPI textPanel = dialog == null ? null : dialog.getTextPanel();
        if (textPanel == null) {
            return;
        }
        Color good = Misc.getPositiveHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();
        line(textPanel, "combat.crewLedger.stolen", change.stolen(), good);
        line(textPanel, "combat.liveMunitions.crewLost", change.stolen() > 0 ? change.sacrificed() : outcome.lost(), bad);
        line(textPanel, "combat.crewLedger.joined", outcome.joined(), good);
        line(textPanel, "combat.crewLedger.noRoom", outcome.noRoom(), Misc.getHighlightColor());
        if (change.stolen() > 0) {
            line(textPanel, "combat.crewLedger.netLoss", outcome.lost(), bad);
        }
    }

    static int crewStillToBeRecovered(CampaignFleetAPI playerFleet) {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null || dialog.getPlugin() == null
                || !(dialog.getPlugin().getContext() instanceof FleetEncounterContext context)
                || !context.didPlayerWinMostRecentBattleOfEncounter()) {
            return 0;
        }
        DataForEncounterSide data = context.getDataFor(playerFleet);
        CrewCompositionAPI recoverable = data == null ? null : data.getRecoverableCrewLosses();
        return recoverable == null ? 0 : recoverable.getCrewInt();
    }

    private static void line(TextPanelAPI textPanel, String messageKey, int count, Color color) {
        if (count > 0) {
            VanillaText.addPara(textPanel, Translation.msg(messageKey).count(count).arg("count", count).styled(), color);
        }
    }

    private static final class ApplyAfterEngagement implements EveryFrameScript {

        private final CrewChange crewChange;
        private boolean applied;

        private ApplyAfterEngagement(CrewChange crewChange) {
            this.crewChange = crewChange;
        }

        @Override
        public boolean isDone() {
            return applied;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            if (applied) {
                return;
            }
            applied = true;
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            if (playerFleet == null) {
                return;
            }
            CargoAPI cargo = playerFleet.getCargo();
            int freeCrewSpace = Math.max(0, cargo.getFreeCrewSpace() - crewStillToBeRecovered(playerFleet));
            Outcome outcome = apply(crewChange, cargo, freeCrewSpace);
            I18n.forGameText(() -> report(crewChange, outcome));
        }
    }
}
