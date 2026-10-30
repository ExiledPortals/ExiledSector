package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.anomaly.facility.FacilityStates;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.ItemDrops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ExcavationSites implements ChartAnomaly {

    public static final String ID = "excavation_sites";
    public static final String EXTRA_KEY = ID + ".extra";
    public static final String LOOT_KEY = ID + ".loot";
    public static final ExcavationSites INSTANCE = new ExcavationSites();
    static final long PLACEMENT_SALT = 0x45584341564154L;

    private ExcavationSites() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int minPlanets() {
        return ExcavationRules.MIN_PLANETS;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || !run.runId().equals(site.runId())) {
            throw new IllegalStateException("no run to record the excavation sites in");
        }
        if (place(site, run, Global.getSector()).isEmpty()) {
            throw new IllegalStateException("no planet for an excavation site");
        }
    }

    static List<ExcavationSite> place(AnomalyContentSite site, ChartRun run, SectorAPI sector) {
        Random random = site.random(PLACEMENT_SALT);
        List<PlanetAPI> planets = candidates(site.system());
        planets.removeIf(planet -> FacilityStates.holds(run, planet.getId()));
        Collections.shuffle(planets, random);
        int count = Math.min(planets.size(), ExcavationRules.siteCount(random, Math.round(site.context().anomalyPercent(EXTRA_KEY))));
        List<ExcavationSite> placed = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            ExcavationSite placedSite = new ExcavationSite(index, planets.get(index).getId(), ExcavationRules.access(random),
                    ExcavationRules.lean(sector, random), random.nextLong(), false);
            ExcavationStates.record(run, placedSite);
            placed.add(placedSite);
        }
        return placed;
    }

    static List<PlanetAPI> candidates(StarSystemAPI system) {
        List<PlanetAPI> solid = new ArrayList<>();
        List<PlanetAPI> any = new ArrayList<>();
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet.isStar() || !ExcavationSite.encodable(planet.getId())) {
                continue;
            }
            any.add(planet);
            if (!planet.isGasGiant()) {
                solid.add(planet);
            }
        }
        return solid.isEmpty() ? any : solid;
    }

    static boolean inRunSystem(ChartRun run, LocationAPI location) {
        return run != null && run.runState().isLive() && location != null && run.systemId().equals(ChartIds.locationIdOf(location));
    }

    @Override
    public boolean isActiveIn(LocationAPI location) {
        ChartRun run = ChartRuns.currentOrNull();
        return inRunSystem(run, location) && !ExcavationStates.standing(run).isEmpty();
    }

    @Override
    public InteractionDialogPlugin dialogFor(SectorEntityToken target) {
        return ExcavationSiteDialog.wantsToHandle(target) ? new ExcavationSiteDialog() : null;
    }

    @Override
    public BattleCreationPlugin battleFor(SectorEntityToken opponent) {
        return ShaftEncounters.isShaftBattle(opponent) ? new ShaftBattleCreationPlugin() : null;
    }

    @Override
    public ItemDrops.SiteLoot fleetItems(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, float share, Random random) {
        return ExcavationLoot.fleetItems(sector, fleet, run, context, share, random);
    }

    @Override
    public String intelText(ChartRun run, LocationAPI location) {
        List<StyledText> planets = new ArrayList<>();
        for (ExcavationSite site : ExcavationStates.standing(run)) {
            SectorEntityToken planet = location == null ? null : location.getEntityById(site.planetId());
            if (planet != null) {
                planets.add(StyledText.styled(planet.getName(), Style.HIGHLIGHT));
            }
        }
        if (planets.isEmpty()) {
            return null;
        }
        return I18n.forGameText(() -> Translation.msg("chart.excavation.intel").count(planets.size()).arg("planets", Translation.list(planets)).text());
    }
}
