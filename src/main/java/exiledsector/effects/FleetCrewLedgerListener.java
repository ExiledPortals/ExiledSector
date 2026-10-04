package exiledsector.effects;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
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

    static Outcome apply(CrewChange change, CargoAPI cargo) {
        int net = change.net();
        if (net > 0) {
            int joined = Math.max(0, Math.min(net, cargo.getFreeCrewSpace()));
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
        TextPanelAPI text = dialog == null ? null : dialog.getTextPanel();
        if (text == null) {
            return;
        }
        Color good = Misc.getPositiveHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();
        line(text, "combat.crewLedger.stolen", change.stolen(), good);
        line(text, "combat.liveMunitions.crewLost", change.sacrificed(), bad);
        line(text, "combat.crewLedger.joined", outcome.joined(), good);
        line(text, "combat.crewLedger.noRoom", outcome.noRoom(), Misc.getHighlightColor());
        if (change.stolen() > 0) {
            line(text, "combat.crewLedger.netLoss", outcome.lost(), bad);
        }
    }

    private static void line(TextPanelAPI text, String key, int count, Color color) {
        if (count > 0) {
            VanillaText.addPara(text, Translation.msg(key).count(count).arg("count", count).styled(), color);
        }
    }

    private static final class ApplyAfterEngagement implements EveryFrameScript {

        private final CrewChange change;
        private boolean done;

        private ApplyAfterEngagement(CrewChange change) {
            this.change = change;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            if (done) {
                return;
            }
            done = true;
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            if (playerFleet == null) {
                return;
            }
            Outcome outcome = apply(change, playerFleet.getCargo());
            I18n.forGameText(() -> report(change, outcome));
        }
    }
}
