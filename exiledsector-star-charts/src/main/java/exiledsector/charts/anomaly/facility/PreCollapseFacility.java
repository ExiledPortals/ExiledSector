package exiledsector.charts.anomaly.facility;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.anomaly.excavation.ExcavationStates;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class PreCollapseFacility implements ChartAnomaly {

    public static final String ANOMALY_ID = "pre_collapse_facility";
    public static final PreCollapseFacility INSTANCE = new PreCollapseFacility();
    static final long PLACEMENT_SALT = 0x50524546414331L;

    private PreCollapseFacility() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public List<String> lootScopes() {
        return List.of(FacilityRules.LOOT_SCOPE);
    }

    @Override
    public int minPlanets() {
        return 1;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || !run.runId().equals(site.runId())) {
            throw new IllegalStateException("no run to record the pre-collapse facility in");
        }
        if (place(site, run) == null) {
            throw new IllegalStateException("no planet for a pre-collapse facility");
        }
    }

    static PlanetAPI place(AnomalyContentSite site, ChartRun run) {
        List<PlanetAPI> planets = candidates(site.system(), run);
        if (planets.isEmpty()) {
            return null;
        }
        Random random = site.random(PLACEMENT_SALT);
        PlanetAPI planet = planets.get(random.nextInt(planets.size()));
        FacilityStates.record(run, planet.getId());
        return planet;
    }

    static List<PlanetAPI> candidates(StarSystemAPI system, ChartRun run) {
        List<PlanetAPI> solid = new ArrayList<>();
        List<PlanetAPI> any = new ArrayList<>();
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet.isStar() || ExcavationStates.at(run, planet.getId()) != null) {
                continue;
            }
            any.add(planet);
            if (!planet.isGasGiant()) {
                solid.add(planet);
            }
        }
        return solid.isEmpty() ? any : solid;
    }

    static long seed(ChartRun run, String planetId, long salt) {
        return ((long) run.runId().hashCode() << 32) ^ (planetId == null ? 0 : planetId.hashCode()) ^ salt;
    }

    static boolean inRunSystem(ChartRun run, LocationAPI location) {
        return run != null && run.runState().isLive() && location != null && run.systemId().equals(ChartIds.locationIdOf(location));
    }

    @Override
    public boolean isActiveIn(LocationAPI location) {
        ChartRun run = ChartRuns.currentOrNull();
        return inRunSystem(run, location) && FacilityStates.standing(run);
    }

    @Override
    public InteractionDialogPlugin dialogFor(SectorEntityToken target) {
        return FacilityDialog.wantsToHandle(target) ? new FacilityDialog() : null;
    }

    @Override
    public String intelText(ChartRun run, LocationAPI location) {
        SectorEntityToken planet = location == null || !FacilityStates.standing(run) ? null : location.getEntityById(FacilityStates.planetId(run));
        if (planet == null) {
            return null;
        }
        StyledText name = StyledText.styled(planet.getName(), Style.HIGHLIGHT);
        return I18n.forGameText(() -> Translation.msg("chart.facility.intel").arg("planet", name).text());
    }
}
