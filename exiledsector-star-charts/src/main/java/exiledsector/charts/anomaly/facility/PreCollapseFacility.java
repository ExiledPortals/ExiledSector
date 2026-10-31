package exiledsector.charts.anomaly.facility;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.LayoutShaper;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class PreCollapseFacility implements LayoutShaper, ContentPlacer, RunParticipant, LootShaper {

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
        if (place(site, site.run()) == null) {
            throw new IllegalStateException("no planet for a pre-collapse facility");
        }
    }

    static PlanetAPI place(AnomalyContentSite site, ChartRun run) {
        List<PlanetAPI> planets = candidates(site);
        if (planets.isEmpty()) {
            return null;
        }
        Random random = site.random(PLACEMENT_SALT);
        PlanetAPI planet = planets.get(random.nextInt(planets.size()));
        FacilityStates.record(run, planet.getId());
        site.claim(planet);
        return planet;
    }

    static List<PlanetAPI> candidates(AnomalyContentSite site) {
        List<PlanetAPI> solid = new ArrayList<>();
        List<PlanetAPI> any = new ArrayList<>();
        for (PlanetAPI planet : site.system().getPlanets()) {
            if (planet.isStar() || site.isClaimed(planet)) {
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

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return FacilityDialog.wantsToHandle(run.run(), target) ? new FacilityDialog() : null;
    }

    @Override
    public String intelText(AnomalyRun run) {
        String planetId = FacilityStates.standing(run.run()) ? FacilityStates.planetId(run.run()) : null;
        SectorEntityToken planet = planetId == null ? null : run.system().getEntityById(planetId);
        if (planet == null) {
            return null;
        }
        StyledText name = StyledText.styled(planet.getName(), Style.HIGHLIGHT);
        return I18n.forGameText(() -> Translation.msg("chart.facility.intel").arg("planet", name).text());
    }
}
