package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.anomaly.AnomalyDialog;
import exiledsector.charts.anomaly.AnomalyEncounters;
import exiledsector.charts.anomaly.ChartRaid;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;

public class ExcavationSiteDialog extends AnomalyDialog {

    static final String DESCEND = "exiledSector_excavationDescend";
    static final String RAID_OPTION = "exiledSector_excavationRaid";
    static final String CONTINUE = "exiledSector_excavationContinue";
    static final String DROP_TYPE_KEY_PREFIX = "chart.dropType.";

    private final ChartRaid.Session raid = new ChartRaid.Session(closedDialog -> showMain());
    private SectorEntityToken planet;
    private FleetInteractionDialogPluginImpl encounter;

    public static boolean wantsToHandle(ChartRun run, SectorEntityToken target) {
        return target instanceof PlanetAPI planet && !planet.isStar() && ExcavationStates.standingAt(run, planet.getId()) != null;
    }

    static boolean hasShaftShips(List<FleetMemberAPI> combatReady) {
        for (FleetMemberAPI member : combatReady) {
            if (member.canBeDeployedForCombat() && !member.isMothballed() && !member.isCivilian()
                    && member.getHullSpec() != null && ExcavationRules.fitsShaft(member.getHullSpec().getHullSize())) {
                return true;
            }
        }
        return false;
    }

    static boolean hasShaftShips(CampaignFleetAPI playerFleet) {
        return playerFleet != null && playerFleet.getFleetData() != null && hasShaftShips(playerFleet.getFleetData().getCombatReadyMembersListCopy());
    }

    static StyledText leanLine(DropType lean) {
        StyledText items = StyledText.styled(Translation.gameText(DROP_TYPE_KEY_PREFIX + lean.name()), Style.HIGHLIGHT);
        return I18n.forGameText(() -> Translation.msg("chart.excavation.dialog.lean").arg("items", items).styled());
    }

    @Override
    protected void open() {
        planet = dialog().getInteractionTarget();
        showMain();
    }

    private ChartRun run() {
        return ChartRuns.liveIn(planet.getContainingLocation());
    }

    private ExcavationSite site() {
        return ExcavationStates.at(run(), planet.getId());
    }

    private void showMain() {
        dialog().getVisualPanel().showPlanetInfo(planet);
        TextPanelAPI text = text();
        OptionPanelAPI options = options();
        options.clearOptions();
        ExcavationSite site = site();
        String planetName = planet.getName();
        if (site == null || site.cleared()) {
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.excavation.dialog.cleared").arg("planet", planetName).text()));
        } else {
            String descriptionKey = site.access() == ExcavationSite.Access.OPEN ? "chart.excavation.dialog.open" : "chart.excavation.dialog.collapsed";
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg(descriptionKey).arg("planet", planetName).text()));
            VanillaText.addPara(text, leanLine(site.lean()), Misc.getTextColor());
            if (site.access() == ExcavationSite.Access.OPEN) {
                options.addOption(Translation.gameText("chart.excavation.dialog.option.descend"), DESCEND,
                        Translation.gameText("chart.excavation.dialog.descendTooltip"));
                if (!hasShaftShips(Global.getSector().getPlayerFleet())) {
                    options.setEnabled(DESCEND, false);
                    options.setTooltip(DESCEND, Translation.gameText("chart.excavation.dialog.noShaftShips"));
                }
            } else {
                options.addOption(Translation.gameText("chart.excavation.dialog.option.raid"), RAID_OPTION);
            }
        }
        options.addOption(Translation.gameText("chart.excavation.dialog.option.continue"), CONTINUE);
        addLeaveOption();
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        ExcavationSite site = site();
        boolean standing = site != null && !site.cleared();
        if (raid.handles(optionData)) {
            raid.optionSelected(optionData);
        } else if (DESCEND.equals(optionData) && standing && site.access() == ExcavationSite.Access.OPEN
                && hasShaftShips(Global.getSector().getPlayerFleet())) {
            descend(site);
        } else if (RAID_OPTION.equals(optionData) && standing && site.access() == ExcavationSite.Access.COLLAPSED) {
            raid.launch(dialog(), run().tier(), site.seed() * 31L, this::objectives);
        } else if (CONTINUE.equals(optionData)) {
            continueAfter(ExcavationSites.INSTANCE, planet);
        } else {
            dialog().dismiss();
        }
    }

    private void descend(ExcavationSite site) {
        ChartRun run = run();
        CampaignFleetAPI fleet = ShaftFleets.fleetFor(planet, run, site);
        if (fleet == null) {
            VanillaText.plainPara(text(), Translation.gameText("chart.excavation.dialog.silent"));
            ExcavationStates.clear(run, site);
            showMain();
            return;
        }
        encounter = new ShaftEncounterDialog(ShaftEncounters.config(leaving -> surface(leaving, site, fleet)));
        AnomalyEncounters.engage(dialog(), fleet, encounter);
    }

    private void surface(InteractionDialogAPI leaving, ExcavationSite site, CampaignFleetAPI fleet) {
        returnHere(leaving, planet);
        boolean won = AnomalyEncounters.wonOutright(encounter);
        encounter = null;
        if (won || fleet.isEmpty()) {
            ExcavationStates.clear(run(), site);
            ShaftFleets.forget(planet);
        }
        showMain();
    }

    List<GroundRaidObjectivePlugin> objectives() {
        List<GroundRaidObjectivePlugin> objectives = new ArrayList<>();
        ChartRun run = run();
        ExcavationSite site = ExcavationStates.standingAt(run, planet.getId());
        if (site != null && site.access() == ExcavationSite.Access.COLLAPSED) {
            objectives.add(new ExcavationSiteObjective(planet.getMarket(), site, ChartRaid.danger(run.tier()), raid::effectiveness));
        }
        return objectives;
    }
}
