package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ui.ShownScreens;
import exiledsector.ui.SkillTreeCanvasPlugin;
import exiledsector.ui.SkillTreeRefitButton;
import exiledsector.ui.refit.CoreUiDismissal;

import java.util.List;

final class OpenSkillTreeStep implements HarnessStep {

    static final String FLAGSHIP = "flagship";

    private final String shipSelector;
    private final boolean openTree;
    private FleetMemberAPI member;
    private boolean refitRequested;
    private boolean panelRequested;

    OpenSkillTreeStep(String shipSelector) {
        this(shipSelector, true);
    }

    OpenSkillTreeStep(String shipSelector, boolean openTree) {
        this.shipSelector = shipSelector;
        this.openTree = openTree;
    }

    @Override
    public String name() {
        return (openTree ? "openSkillTree " : "openRefit ") + shipSelector;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        if (context.firstFrame()) {
            member = member(Global.getSector().getPlayerFleet(), shipSelector);
            if (member == null) {
                return StepOutcome.failed("no fleet member matches \"" + shipSelector + "\"; use flagship, a fleet index, a hull id or a ship name");
            }
            CampaignUIAPI campaignUi = Global.getSector().getCampaignUI();
            if (campaignUi.getCurrentCoreTab() != null && !CoreUiDismissal.dismiss(campaignUi)) {
                return StepOutcome.failed("the open " + campaignUi.getCurrentCoreTab() + " screen would not close before switching to " + shipName());
            }
            return StepOutcome.RUNNING;
        }
        if (!refitRequested) {
            CampaignUIAPI campaignUi = Global.getSector().getCampaignUI();
            if (campaignUi.getCurrentCoreTab() != null) {
                return new StepOutcome(false, false, "the open " + campaignUi.getCurrentCoreTab() + " screen never closed");
            }
            campaignUi.showCoreUITab(CoreUITabId.REFIT, member);
            refitRequested = true;
            return StepOutcome.RUNNING;
        }
        if (!openTree) {
            return Global.getSector().getCampaignUI().getCurrentCoreTab() == CoreUITabId.REFIT
                    ? StepOutcome.passed("the refit screen opened on " + shipName())
                    : new StepOutcome(false, false, "the refit screen never opened on " + shipName());
        }
        if (!panelRequested) {
            panelRequested = SkillTreeRefitButton.openPanel(null);
            return new StepOutcome(false, false, "the refit screen never accepted the skill tree button for " + shipName());
        }
        ShownScreens.Shown shown = ShownScreens.top();
        if (shown != null && shown.screen() instanceof SkillTreeCanvasPlugin) {
            String openedLine = context.log().lastLineContaining("[perf] Skill tree opened", context.firstLogLine());
            return StepOutcome.passed(shipName() + (openedLine == null ? "" : ": " + openedLine));
        }
        return new StepOutcome(false, false, "the skill tree for " + shipName() + " never appeared");
    }

    private String shipName() {
        return member.getShipName() + " (" + member.getHullSpec().getHullId() + ")";
    }

    static FleetMemberAPI member(CampaignFleetAPI fleet, String selector) {
        if (FLAGSHIP.equalsIgnoreCase(selector)) {
            return fleet.getFlagship();
        }
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        if (selector.chars().allMatch(Character::isDigit) && !selector.isEmpty()) {
            int index = Integer.parseInt(selector);
            return index < members.size() ? members.get(index) : null;
        }
        for (FleetMemberAPI candidate : members) {
            String hullId = candidate.getHullSpec().getHullId();
            String baseHullId = candidate.getHullSpec().getBaseHullId();
            if (selector.equals(hullId) || selector.equals(baseHullId) || selector.equalsIgnoreCase(candidate.getShipName())) {
                return candidate;
            }
        }
        return null;
    }
}
