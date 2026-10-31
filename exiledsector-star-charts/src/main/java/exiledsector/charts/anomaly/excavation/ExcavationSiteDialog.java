package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.ChartRaid;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ExcavationSiteDialog implements InteractionDialogPlugin, ChartRaid.Host {

    static final String DESCEND = "exiledSector_excavationDescend";
    static final String RAID_OPTION = "exiledSector_excavationRaid";
    static final String CONTINUE = "exiledSector_excavationContinue";
    static final String LEAVE = "exiledSector_excavationLeave";
    static final String DROP_TYPE_KEY_PREFIX = "chart.dropType.";

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI dialog;
    private SectorEntityToken planet;
    private ChartRaid raid;
    private FleetInteractionDialogPluginImpl encounter;
    private int raidsLaunched;

    public static boolean wantsToHandle(SectorEntityToken target) {
        if (!(target instanceof PlanetAPI planet) || planet.isStar()) {
            return false;
        }
        ChartRun run = ChartRuns.currentOrNull();
        return ExcavationSites.inRunSystem(run, planet.getContainingLocation()) && ExcavationStates.standingAt(run, planet.getId()) != null;
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
    public void init(InteractionDialogAPI interactionDialog) {
        dialog = interactionDialog;
        planet = interactionDialog.getInteractionTarget();
        showMain();
    }

    private ExcavationSite site() {
        return ExcavationStates.at(ChartRuns.currentOrNull(), planet.getId());
    }

    private void showMain() {
        dialog.getVisualPanel().showPlanetInfo(planet);
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
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
        options.addOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        ExcavationSite site = site();
        boolean standing = site != null && !site.cleared();
        if (raid != null && ChartRaid.handles(optionData)) {
            raid.optionSelected(optionData);
        } else if (DESCEND.equals(optionData) && standing && site.access() == ExcavationSite.Access.OPEN
                && hasShaftShips(Global.getSector().getPlayerFleet())) {
            descend(site);
        } else if (RAID_OPTION.equals(optionData) && standing && site.access() == ExcavationSite.Access.COLLAPSED) {
            long seed = site.seed() * 31L + raidsLaunched++;
            raid = new ChartRaid(dialog, ExcavationRules.raidDifficulty(ChartRuns.currentOrNull().tier()), this::objectives, new Random(seed), this);
            raid.open();
        } else if (CONTINUE.equals(optionData)) {
            InteractionDialogPlugin next = ChartAnomalies.dialogFor(planet, ExcavationSites.INSTANCE);
            InteractionDialogPlugin planetDialog = next == null ? new RuleBasedInteractionDialogPluginImpl() : next;
            dialog.setPlugin(planetDialog);
            planetDialog.init(dialog);
        } else {
            dialog.dismiss();
        }
    }

    private void descend(ExcavationSite site) {
        ChartRun run = ChartRuns.currentOrNull();
        CampaignFleetAPI fleet = ShaftFleets.fleetFor(planet, run, site);
        if (fleet == null) {
            VanillaText.plainPara(dialog.getTextPanel(), Translation.gameText("chart.excavation.dialog.silent"));
            ExcavationStates.clear(run, site);
            showMain();
            return;
        }
        encounter = new ShaftEncounterDialog(ShaftEncounters.config(leaving -> surface(leaving, site, fleet)));
        dialog.setInteractionTarget(fleet);
        dialog.setPlugin(encounter);
        encounter.init(dialog);
    }

    private void surface(InteractionDialogAPI leaving, ExcavationSite site, CampaignFleetAPI fleet) {
        leaving.setPlugin(this);
        leaving.setInteractionTarget(planet);
        boolean won = encounter != null && encounter.getContext() instanceof FleetEncounterContext context && context.didPlayerWinEncounterOutright();
        encounter = null;
        if (won || fleet.isEmpty()) {
            ExcavationStates.clear(ChartRuns.currentOrNull(), site);
            ShaftFleets.forget(planet);
        }
        showMain();
    }

    List<GroundRaidObjectivePlugin> objectives() {
        List<GroundRaidObjectivePlugin> objectives = new ArrayList<>();
        ChartRun run = ChartRuns.currentOrNull();
        ExcavationSite site = ExcavationStates.standingAt(run, planet.getId());
        if (site != null && site.access() == ExcavationSite.Access.COLLAPSED) {
            objectives.add(new ExcavationSiteObjective(planet.getMarket(), site, ExcavationRules.siteDanger(run.tier()),
                    () -> raid == null ? 0d : raid.effectiveness()));
        }
        return objectives;
    }

    @Override
    public void raidLeft(InteractionDialogAPI raidDialog) {
        raid = null;
        showMain();
    }

    @Override
    public void raidFinished(InteractionDialogAPI raidDialog) {
        raid = null;
        showMain();
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }
}
