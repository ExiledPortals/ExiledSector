package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.ChartRaid;
import exiledsector.charts.anomaly.PlanetSiteDialog;
import exiledsector.charts.run.ChartRun;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ExcavationSiteDialog extends PlanetSiteDialog {

    static final String DESCEND = "exiledSector_excavationDescend";
    static final String CORRUPT_DESCEND = "exiledSector_excavationCorruptDescend";
    static final String RAID_OPTION = "exiledSector_excavationRaid";
    static final String CONTINUE = "exiledSector_excavationContinue";
    static final String DEEPER = "exiledSector_excavationDeeper";
    static final String CORRUPT_DEEPER = "exiledSector_excavationCorruptDeeper";
    static final String SURFACE = "exiledSector_excavationSurface";
    static final String SCOUTED_KEY_PREFIX = "chart.anomaly.excavation_sites.dialog.scouted.";
    static final String NO_SHAFT_SHIPS_KEY = "chart.anomaly.excavation_sites.dialog.noShaftShips";
    static final String FRIGATES_SUFFIX = ".frigates";
    static final String CRUISERS_SUFFIX = ".cruisers";

    private final ChartRaid.Session raid = new ChartRaid.Session(closedDialog -> showMain());
    private boolean secondCavernOffered;

    public static boolean wantsToHandle(ChartRun run, SectorEntityToken target) {
        return target instanceof PlanetAPI planet && !planet.isStar() && ExcavationStates.standingAt(run, planet.getId()) != null;
    }

    static boolean hasExcavationShips(List<FleetMemberAPI> combatReady, int maxShipSize) {
        for (FleetMemberAPI member : combatReady) {
            if (member.canBeDeployedForCombat() && !member.isMothballed() && !member.isCivilian()
                    && member.getHullSpec() != null && ExcavationRules.fitsExcavation(member.getHullSpec().getHullSize(), maxShipSize)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasExcavationShips(CampaignFleetAPI playerFleet, int maxShipSize) {
        return playerFleet != null && playerFleet.getFleetData() != null
                && hasExcavationShips(playerFleet.getFleetData().getCombatReadyMembersListCopy(), maxShipSize);
    }

    static String sizedKey(String baseKey, int maxShipSize) {
        if (maxShipSize <= ExcavationRules.FRIGATES_ONLY_SHIP_SIZE) {
            return baseKey + FRIGATES_SUFFIX;
        }
        return maxShipSize >= ExcavationRules.WIDENED_SHIP_SIZE ? baseKey + CRUISERS_SUFFIX : baseKey;
    }

    static StyledText findsLine() {
        StyledText items = StyledText.styled(Translation.gameText("chart.anomaly.excavation_sites.dialog.findsItems"), Style.HIGHLIGHT);
        return I18n.forGameText(() -> Translation.msg("chart.anomaly.excavation_sites.dialog.finds").arg("items", items).styled());
    }

    static String descriptionKey(ExcavationSite.Access access) {
        return "chart.anomaly.excavation_sites.dialog." + access.name().toLowerCase(Locale.ROOT);
    }

    static String scoutedKey(ExcavationSite.Access access) {
        return SCOUTED_KEY_PREFIX + access.name().toLowerCase(Locale.ROOT);
    }

    private ExcavationSite site() {
        return ExcavationStates.at(run(), planet().getId());
    }

    private ExcavationTuning tuning() {
        return ExcavationTuning.ofRun(run());
    }

    @Override
    protected void showMain() {
        dialog().getVisualPanel().showPlanetInfo(planet());
        TextPanelAPI text = text();
        OptionPanelAPI options = options();
        options.clearOptions();
        ExcavationSite site = site();
        String planetName = planet().getName();
        if (site == null || site.cleared()) {
            secondCavernOffered = false;
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.anomaly.excavation_sites.dialog.cleared").arg("planet", planetName).text()));
        } else if (secondCavernOffered && site.hasOpenShaft()) {
            showSecondCavern();
            return;
        } else {
            secondCavernOffered = false;
            if (ExcavationStates.scout(run(), site)) {
                describeFirstSighting(text, site, planetName);
            } else {
                VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg(descriptionKey(site.access())).arg("planet", planetName).text()));
            }
            VanillaText.addPara(text, findsLine(), Misc.getTextColor());
            if (site.hasOpenShaft()) {
                int maxShipSize = ExcavationRules.maxShipSize(tuning());
                addGoingDown(DESCEND, CORRUPT_DESCEND, sizedKey("chart.anomaly.excavation_sites.dialog.option.descend", maxShipSize),
                        sizedKey("chart.anomaly.excavation_sites.dialog.descendTooltip", maxShipSize));
            }
            if (site.hasCollapsedShaft()) {
                options.addOption(Translation.gameText("chart.anomaly.excavation_sites.dialog.option.raid"), RAID_OPTION);
            }
        }
        options.addOption(Translation.gameText("chart.anomaly.excavation_sites.dialog.option.continue"), CONTINUE);
        addLeaveOption();
    }

    private void showSecondCavern() {
        VanillaText.plainPara(text(), Translation.gameText("chart.anomaly.excavation_sites.dialog.secondCavern"));
        addGoingDown(DEEPER, CORRUPT_DEEPER, "chart.anomaly.excavation_sites.dialog.option.deeper", "chart.anomaly.excavation_sites.dialog.deeperTooltip");
        options().addOption(Translation.gameText("chart.anomaly.excavation_sites.dialog.option.surface"), SURFACE);
    }

    private void addGoingDown(String goOption, String corruptOption, String goTextKey, String goTooltipKey) {
        OptionPanelAPI options = options();
        int maxShipSize = ExcavationRules.maxShipSize(tuning());
        boolean hasShips = hasExcavationShips(PlayerFleet.orNull(), maxShipSize);
        options.addOption(Translation.gameText(goTextKey), goOption, Translation.gameText(goTooltipKey));
        String noShipsKey = sizedKey(NO_SHAFT_SHIPS_KEY, maxShipSize);
        if (!hasShips) {
            options.setEnabled(goOption, false);
            options.setTooltip(goOption, Translation.gameText(noShipsKey));
        }
        if (ExcavationCorruption.offered(run(), tuning(), planet())) {
            ExcavationCorruption.addOption(options, corruptOption, hasShips, noShipsKey, PlayerFleet.orNull());
        }
    }

    private static void describeFirstSighting(TextPanelAPI text, ExcavationSite site, String planetName) {
        VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.anomaly.excavation_sites.dialog.found").arg("planet", planetName).text()));
        VanillaText.plainPara(text, Translation.gameText("chart.anomaly.excavation_sites.dialog.identified"));
        VanillaText.plainPara(text, Translation.gameText(scoutedKey(site.access())));
        VanillaText.plainPara(text, Translation.gameText("chart.anomaly.excavation_sites.dialog.stores"));
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        ExcavationSite site = site();
        boolean openShaft = site != null && site.hasOpenShaft();
        boolean canGoDown = openShaft && hasExcavationShips(PlayerFleet.orNull(), ExcavationRules.maxShipSize(tuning()));
        if (raid.handles(optionData)) {
            raid.optionSelected(optionData);
        } else if (DESCEND.equals(optionData) && canGoDown && !secondCavernOffered) {
            descend(site, ExcavationGuardians.FIRST_CAVERN, false);
        } else if (CORRUPT_DESCEND.equals(optionData) && canGoDown && !secondCavernOffered) {
            descend(site, ExcavationGuardians.FIRST_CAVERN, true);
        } else if (DEEPER.equals(optionData) && canGoDown && secondCavernOffered) {
            descend(site, new ExcavationGuardians(true, false), false);
        } else if (CORRUPT_DEEPER.equals(optionData) && canGoDown && secondCavernOffered) {
            descend(site, new ExcavationGuardians(true, false), true);
        } else if (SURFACE.equals(optionData)) {
            closeOpenShaft();
            showMain();
        } else if (RAID_OPTION.equals(optionData) && site != null && site.hasCollapsedShaft() && !secondCavernOffered) {
            raid.launch(dialog(), run().tier(), site.seed() * 31L, this::objectives);
        } else if (CONTINUE.equals(optionData)) {
            continueAfter(ExcavationSites.INSTANCE, planet());
        } else {
            if (secondCavernOffered) {
                closeOpenShaft();
            }
            dialog().dismiss();
        }
    }

    private void descend(ExcavationSite site, ExcavationGuardians guardians, boolean corrupt) {
        ChartRun run = run();
        ExcavationGuardians going = guardians;
        if (corrupt) {
            if (!ExcavationCorruption.offered(run, tuning(), planet()) || !ExcavationCorruption.pay(PlayerFleet.orNull())) {
                showMain();
                return;
            }
            going = guardians.asCorrupted();
        }
        CampaignFleetAPI fleet = ExcavationFleets.fleetFor(planet(), run, site, going);
        if (fleet == null) {
            VanillaText.plainPara(text(), Translation.gameText("chart.anomaly.excavation_sites.dialog.silent"));
            closeOpenShaft();
            showMain();
            return;
        }
        boolean secondCavern = going.secondCavern();
        fightHeldGuards(fleet, onLeave -> new ExcavationEncounterDialog(ExcavationEncounters.config(leavingCavern(secondCavern, onLeave))),
                () -> cavernCleared(secondCavern));
    }

    private Consumer<InteractionDialogAPI> leavingCavern(boolean secondCavern, Consumer<InteractionDialogAPI> onLeave) {
        if (!secondCavern) {
            return onLeave;
        }
        return leaving -> {
            closeOpenShaft();
            onLeave.accept(leaving);
        };
    }

    private void cavernCleared(boolean secondCavern) {
        ExcavationFleets.forget(planet());
        if (!secondCavern && tuning().secondCavern()) {
            secondCavernOffered = true;
        } else {
            closeOpenShaft();
        }
    }

    private void closeOpenShaft() {
        secondCavernOffered = false;
        ExcavationFleets.forget(planet());
        ExcavationStates.clearShaft(run(), site(), ExcavationSite.Access.OPEN);
    }

    List<GroundRaidObjectivePlugin> objectives() {
        List<GroundRaidObjectivePlugin> objectives = new ArrayList<>();
        ChartRun run = run();
        ExcavationSite site = ExcavationStates.standingAt(run, planet().getId());
        if (site != null && site.hasCollapsedShaft()) {
            objectives.add(new ExcavationSiteObjective(planet().getMarket(), site, ChartRaid.danger(run.tier()), raid::effectiveness));
        }
        return objectives;
    }
}
